package com.example.githubprofileexplorer.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.example.githubprofileexplorer.data.local.DownloadDao
import com.example.githubprofileexplorer.data.local.DownloadEntity
import com.example.githubprofileexplorer.data.local.DownloadError
import com.example.githubprofileexplorer.data.local.DownloadStatus
import com.example.githubprofileexplorer.data.model.GitHubRepo
import com.example.githubprofileexplorer.data.remote.GithubDownloadApi
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody

/** Bytes received so far; [total] is -1 when GitHub does not announce the size. */
data class DownloadProgress(val bytes: Long, val total: Long) {
    val fraction: Float? get() = if (total > 0) (bytes.toFloat() / total).coerceIn(0f, 1f) else null
}

class DownloadException(val error: DownloadError) : Exception(error.name)

/**
 * Downloads repositories as ZIP archives into the public Downloads folder (via MediaStore, so no storage
 * permission is needed) and records every download in the Room-backed history.
 *
 * Downloads run in [scope] (the application scope), so they keep going while the user navigates the app.
 */
class DownloadRepository(
    private val context: Context,
    private val dao: DownloadDao,
    private val api: GithubDownloadApi,
    private val scope: CoroutineScope
) {
    val history: Flow<List<DownloadEntity>> = dao.observeAll()

    private val _progress = MutableStateFlow<Map<Long, DownloadProgress>>(emptyMap())
    val progress: StateFlow<Map<Long, DownloadProgress>> = _progress.asStateFlow()

    private val jobs = ConcurrentHashMap<Long, Job>()

    /** Starts downloading the default branch of [repo]; returns the history id (existing one if already running). */
    suspend fun enqueue(repo: GitHubRepo): Long {
        val owner = repo.ownerLogin
        dao.findByStatus(owner, repo.name, DownloadStatus.DOWNLOADING)?.let { return it.id }
        val id = dao.insert(
            DownloadEntity(
                owner = owner,
                repoName = repo.name,
                branch = repo.defaultBranch,
                htmlUrl = repo.htmlUrl,
                fileName = fileNameFor(owner, repo.name, repo.defaultBranch),
                status = DownloadStatus.DOWNLOADING,
                createdAt = System.currentTimeMillis()
            )
        )
        start(id)
        return id
    }

    suspend fun retry(id: Long) {
        val entity = dao.get(id) ?: return
        if (entity.status != DownloadStatus.FAILED) return
        dao.update(
            entity.copy(
                status = DownloadStatus.DOWNLOADING,
                error = null,
                createdAt = System.currentTimeMillis()
            )
        )
        start(id)
    }

    /** Removes one history entry and deletes its ZIP from the Downloads folder. */
    suspend fun delete(id: Long) {
        jobs.remove(id)?.cancelAndJoin()
        dao.get(id)?.fileUri?.let(::deleteFile)
        dao.delete(id)
        _progress.update { it - id }
    }

    /** Removes every history entry and deletes the ZIP files. */
    suspend fun clearAll() {
        jobs.values.toList().forEach { it.cancelAndJoin() }
        jobs.clear()
        dao.getAll().forEach { entity -> entity.fileUri?.let(::deleteFile) }
        dao.deleteAll()
        _progress.value = emptyMap()
    }

    /** Called once at app start: a download cannot survive the process being killed. */
    suspend fun markInterrupted() {
        dao.changeStatus(DownloadStatus.DOWNLOADING, DownloadStatus.FAILED, DownloadError.INTERRUPTED)
    }

    /** True when the saved file can still be opened (the user may have removed it in a file manager). */
    suspend fun isFileAvailable(uriString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openFileDescriptor(Uri.parse(uriString), "r")?.use { true } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun start(id: Long) {
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                run(id)
            } finally {
                jobs.remove(id)
            }
        }
        jobs[id] = job
        job.start()
    }

    private suspend fun run(id: Long) {
        val entity = dao.get(id) ?: return
        var target: Uri? = null
        try {
            val response = api.downloadZip(entity.owner, entity.repoName, entity.branch)
            if (!response.isSuccessful) {
                response.errorBody()?.close()
                throw DownloadException(errorFor(response.code()))
            }
            val body = response.body() ?: throw DownloadException(DownloadError.UNKNOWN)
            val uri = createTarget(entity.fileName)
            target = uri
            val written = body.use { copyTo(it, uri, id) }
            if (written <= 0L) throw DownloadException(DownloadError.UNKNOWN)
            markPublished(uri)
            dao.update(
                entity.copy(
                    status = DownloadStatus.COMPLETED,
                    error = null,
                    fileUri = uri.toString(),
                    sizeBytes = written,
                    completedAt = System.currentTimeMillis()
                )
            )
        } catch (e: CancellationException) {
            target?.let { deleteFile(it.toString()) }
            throw e
        } catch (e: Exception) {
            target?.let { deleteFile(it.toString()) }
            dao.update(entity.copy(status = DownloadStatus.FAILED, error = e.toDownloadError()))
        } finally {
            _progress.update { it - id }
        }
    }

    private suspend fun copyTo(body: ResponseBody, target: Uri, id: Long): Long =
        withContext(Dispatchers.IO) {
            val total = body.contentLength()
            val output = context.contentResolver.openOutputStream(target)
                ?: throw DownloadException(DownloadError.STORAGE)
            var written = 0L
            var lastReported = 0L
            output.use { out ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        ensureActive()
                        val read = try {
                            input.read(buffer)
                        } catch (e: IOException) {
                            throw DownloadException(DownloadError.NETWORK)
                        }
                        if (read == -1) break
                        try {
                            out.write(buffer, 0, read)
                        } catch (e: IOException) {
                            throw DownloadException(DownloadError.STORAGE)
                        }
                        written += read
                        if (written - lastReported >= REPORT_STEP_BYTES) {
                            lastReported = written
                            _progress.update { it + (id to DownloadProgress(written, total)) }
                        }
                    }
                }
            }
            _progress.update { it + (id to DownloadProgress(written, total)) }
            written
        }

    private fun createTarget(fileName: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/zip")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER_NAME")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val inserted = try {
            context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        } catch (e: Exception) {
            null
        }
        return inserted ?: throw DownloadException(DownloadError.STORAGE)
    }

    private fun markPublished(uri: Uri) {
        val values = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
        context.contentResolver.update(uri, values, null, null)
    }

    private fun deleteFile(uriString: String) {
        try {
            context.contentResolver.delete(Uri.parse(uriString), null, null)
        } catch (e: Exception) {
            // The file is already gone or no longer ours; nothing else to clean up.
        }
    }

    private fun errorFor(code: Int): DownloadError = when (ProfileRepository.errorForCode(code)) {
        ProfileError.NOT_FOUND -> DownloadError.NOT_FOUND
        ProfileError.RATE_LIMITED -> DownloadError.RATE_LIMITED
        ProfileError.NETWORK -> DownloadError.NETWORK
        else -> DownloadError.UNKNOWN
    }

    private fun Exception.toDownloadError(): DownloadError = when (this) {
        is DownloadException -> error
        is IOException -> DownloadError.NETWORK
        else -> DownloadError.UNKNOWN
    }

    companion object {
        private const val FOLDER_NAME = "GitHubProfileExplorer"
        private const val BUFFER_SIZE = 16 * 1024
        private const val REPORT_STEP_BYTES = 128L * 1024

        /** `owner-repo-branch.zip`, with anything outside `[A-Za-z0-9._-]` replaced by `_`. */
        fun fileNameFor(owner: String, repo: String, branch: String): String =
            "$owner-$repo-$branch.zip".replace(Regex("[^A-Za-z0-9._-]"), "_")
    }
}
