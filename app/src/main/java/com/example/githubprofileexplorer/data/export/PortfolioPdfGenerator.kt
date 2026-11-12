package com.example.githubprofileexplorer.data.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.githubprofileexplorer.domain.PortfolioPdfData
import com.example.githubprofileexplorer.domain.PortfolioPdfDataMapper
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PortfolioPdfGenerator(private val context: Context) {
    suspend fun generate(data: PortfolioPdfData): File = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "portfolio-pdfs").apply { mkdirs() }
        directory.listFiles()?.filter { it.isFile && it.lastModified() < System.currentTimeMillis() - CLEANUP_AFTER_MILLIS }
            ?.forEach(File::delete)
        val file = File(directory, PortfolioPdfDataMapper.sanitizeFilename(data.profile.username))
        val document = PdfDocument()
        val renderer = Renderer(document, data, loadAvatar(data.profile.avatarUrl))
        renderer.render()
        file.outputStream().use { document.writeTo(it) }
        document.close()
        file
    }

    private fun loadAvatar(url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        val connection = (URL(url).openConnection() as? HttpURLConnection) ?: return null
        return try {
            connection.connectTimeout = 2_000
            connection.readTimeout = 2_000
            connection.doInput = true
            connection.connect()
            connection.inputStream.use(BitmapFactory::decodeStream)
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }

    }

    private companion object {
        const val CLEANUP_AFTER_MILLIS = 24 * 60 * 60 * 1000L
    }

    private class Renderer(
        private val document: PdfDocument,
        private val data: PortfolioPdfData,
        private val avatar: Bitmap?
    ) {
        private val width = 595
        private val height = 842
        private val left = 42f
        private val right = 553f
        private val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f; color = 0xff202124.toInt() }
        private val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 8f; color = 0xff5f6368.toInt() }
        private val heading = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 18f; color = 0xff1a237e.toInt(); typeface = Typeface.DEFAULT_BOLD
        }
        private var page: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var y = 44f
        private var pageNumber = 0

        fun render() {
            startPage()
            text("Repo Radar", heading, 24f)
            text("Candidate portfolio", heading, 48f)
            avatar?.let {
                val size = 72
                canvas?.drawBitmap(it, null, android.graphics.RectF(right - size, 22f, right, 22f + size), body)
            } ?: run {
                body.style = Paint.Style.STROKE
                canvas?.drawCircle(right - 36f, 58f, 28f, body)
                body.style = Paint.Style.FILL
            }
            text("@${data.profile.username}", heading, 88f)
            data.profile.displayName?.let { text(it, body) }
            line("GitHub ID", data.profile.githubId?.toString() ?: "Not provided")
            link("Profile", data.profile.profileUrl)
            line("Bio", data.profile.bio ?: "Not provided")
            line("Company", data.profile.company ?: "Not provided")
            line("Location", data.profile.location ?: "Not provided")
            line("Blog", data.profile.blog ?: "Not provided")
            line("Account created", PortfolioPdfDataMapper.formatTimestamp(data.profile.createdAt))
            line("Exported", PortfolioPdfDataMapper.formatLocalTimestamp(data.generatedAt))
            line("Source data fetched", PortfolioPdfDataMapper.formatLocalTimestamp(data.fetchedAt))
            if (data.isStale) {
                text("STALE DATA: source data is older than 15 minutes or its fetch time is unavailable.", small)
            }
            text("Based on publicly available GitHub data.", body)
            text("Informational document only; not a skill assessment or hiring recommendation.", small)
            text("Repository selections: ${data.repositories.size}", body)
            if (data.repositories.isEmpty()) {
                text("No repositories selected. Select projects in Candidate portfolio before exporting.", body)
            } else {
                data.repositories.forEachIndexed { index, repo -> renderRepository(index + 1, repo) }
            }
            finishPage()
        }

        private fun renderRepository(number: Int, repo: com.example.githubprofileexplorer.domain.PortfolioPdfRepository) {
            ensureSpace(150f)
            text("$number. ${repo.name}", heading, 18f)
            line("Full name", repo.fullName)
            line("GitHub ID", repo.githubId?.toString() ?: "Not provided")
            link("Repository", repo.url)
            line("Description", repo.description ?: "No description provided")
            line("Primary language", repo.language ?: "Not specified")
            line("Topics", repo.topics.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "Not provided")
            line("Stars", repo.stars?.toString() ?: "Not available")
            line("Forks", repo.forks?.toString() ?: "Not available")
            line("Open issues", repo.openIssues?.toString() ?: "Not available")
            line("License", listOfNotNull(repo.licenseName, repo.licenseSpdxId).joinToString(" / ").ifBlank { "Not provided" })
            line("Default branch", repo.defaultBranch ?: "Not provided")
            line("Created", PortfolioPdfDataMapper.formatTimestamp(repo.createdAt))
            line("Updated", PortfolioPdfDataMapper.formatTimestamp(repo.updatedAt))
            line("Last pushed", PortfolioPdfDataMapper.formatTimestamp(repo.pushedAt))
            line("Archived", repo.archived?.toString() ?: "Not available")
            line("Fork", repo.fork?.toString() ?: "Not available")
            repo.readmeUrl?.let { link("README", it) }
            text("Repository metrics are context only and do not measure skill or project quality.", small)
        }

        private fun line(label: String, value: String) {
            text("$label: $value", body)
        }

        private fun link(label: String, value: String) {
            text("$label: $value", small)
        }

        private fun text(value: String, paint: Paint, spacing: Float = 0f) {
            val maxWidth = right - left
            wrap(value, paint, maxWidth).forEach { line ->
                ensureSpace(paint.textSize + 4f)
                canvas?.drawText(line, left, y, paint)
                y += paint.textSize + 4f
            }
            y += spacing
        }

        private fun wrap(value: String, paint: Paint, maxWidth: Float): List<String> {
            if (value.isBlank()) return listOf("")
            val result = mutableListOf<String>()
            var current = ""
            value.split(Regex("\\s+")).forEach { word ->
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    current = candidate
                } else if (paint.measureText(word) > maxWidth) {
                    if (current.isNotEmpty()) result += current
                    current = ""
                    var remaining = word
                    while (remaining.isNotEmpty()) {
                        var end = remaining.length
                        while (end > 1 && paint.measureText(remaining.substring(0, end)) > maxWidth) end--
                        result += remaining.substring(0, end)
                        remaining = remaining.substring(end)
                    }
                } else {
                    if (current.isNotEmpty()) result += current
                    current = word
                }
            }
            if (current.isNotEmpty()) result += current
            return result
        }

        private fun ensureSpace(required: Float) {
            if (y + required <= height - 34f) return
            finishPage()
            startPage()
        }

        private fun startPage() {
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(width, height, pageNumber).create())
            canvas = page?.canvas
            y = 34f
        }

        private fun finishPage() {
            canvas?.drawText("Page $pageNumber - GitHub public source context - ${PortfolioPdfDataMapper.formatLocalTimestamp(data.generatedAt)}", left, height - 16f, small)
            page?.let(document::finishPage)
            page = null
            canvas = null
        }

        companion object {
            private const val CLEANUP_AFTER_MILLIS = 24 * 60 * 60 * 1000L
        }
    }
}
