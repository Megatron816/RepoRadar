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
        private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 22f; color = 0xff1a237e.toInt(); typeface = Typeface.DEFAULT_BOLD
        }
        private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12f; color = 0xff1a237e.toInt(); typeface = Typeface.DEFAULT_BOLD
        }
        private val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f; color = 0xff1a237e.toInt(); typeface = Typeface.DEFAULT_BOLD
        }
        private val usernamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9f; color = 0xff5f6368.toInt()
        }
        private var page: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var y = 40f
        private var pageNumber = 0

        fun render() {
            startPage()

            // 1. Write the candidate portfolio in the top as headline
            canvas?.drawText("CANDIDATE PORTFOLIO", left, y, titlePaint)
            y += 28f

            // Top right: Profile picture (avatar), and name of user below their profile picture
            val avatarSize = 64f
            val avatarLeft = right - avatarSize
            val avatarTop = y
            val avatarBottom = avatarTop + avatarSize

            avatar?.let {
                canvas?.drawBitmap(it, null, android.graphics.RectF(avatarLeft, avatarTop, right, avatarBottom), body)
            } ?: run {
                body.style = Paint.Style.STROKE
                canvas?.drawCircle(avatarLeft + avatarSize / 2f, avatarTop + avatarSize / 2f, avatarSize / 2f, body)
                body.style = Paint.Style.FILL
            }

            // Name of user below their profile picture
            val nameText = data.profile.displayName ?: data.profile.username
            val usernameText = "@${data.profile.username}"
            canvas?.drawText(nameText, avatarLeft, avatarBottom + 14f, namePaint)
            canvas?.drawText(usernameText, avatarLeft, avatarBottom + 26f, usernamePaint)

            // Left side profile details
            val startProfileY = y
            y = avatarTop
            val profileMaxWidth = (right - left) - avatarSize - 20f

            fun profileLine(label: String, value: String) {
                val lineText = "$label: $value"
                wrap(lineText, body, profileMaxWidth).forEach { wrapped ->
                    canvas?.drawText(wrapped, left, y, body)
                    y += body.textSize + 3f
                }
            }

            profileLine("GitHub ID", data.profile.githubId?.toString() ?: "Not provided")
            profileLine("Profile", data.profile.profileUrl)
            profileLine("Bio", data.profile.bio ?: "Not provided")
            profileLine("Company", data.profile.company ?: "Not provided")
            profileLine("Location", data.profile.location ?: "Not provided")
            profileLine("Blog", data.profile.blog ?: "Not provided")
            profileLine("Created", PortfolioPdfDataMapper.formatTimestamp(data.profile.createdAt))
            profileLine("Exported", PortfolioPdfDataMapper.formatLocalTimestamp(data.generatedAt))
            profileLine("Fetched", PortfolioPdfDataMapper.formatLocalTimestamp(data.fetchedAt))

            y = maxOf(y, avatarBottom + 36f)
            y += 12f

            if (data.isStale) {
                text("STALE DATA: source data is older than 15 minutes or its fetch time is unavailable.", small)
                y += 6f
            }

            // 4. Add more details in portfolio PDF, use Gemini API for portfolio writing (AI Portfolio Summary)
            data.aiPortfolioWriting?.takeIf { it.isNotBlank() }?.let { aiWriting ->
                renderContainerBox("Professional Portfolio Summary (Gemini AI)", aiWriting)
            }

            y += 8f
            canvas?.drawText("Selected Repositories (${data.repositories.size})", left, y, subtitlePaint)
            y += 16f

            if (data.repositories.isEmpty()) {
                text("No repositories selected. Select projects in Candidate portfolio before exporting.", body)
            } else {
                // 3. If there are more than one repo in the portfolio, contain each portfolio in a container
                data.repositories.forEachIndexed { index, repo -> renderRepository(index + 1, repo) }
            }

            finishPage()
        }

        private fun renderContainerBox(title: String, content: String) {
            val lines = mutableListOf<Pair<String, Paint>>()
            lines.add(title to subtitlePaint)
            wrap(content, body, right - left - 16f).forEach { wrapped ->
                lines.add(wrapped to body)
            }

            val padding = 8f
            var totalHeight = padding * 2
            for ((_, paint) in lines) {
                totalHeight += paint.textSize + 4f
            }

            ensureSpace(totalHeight + 16f)

            val boxTop = y
            val boxBottom = y + totalHeight

            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = 0xfff0f4f8.toInt() }
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = 0xff90caf9.toInt(); strokeWidth = 1f }
            canvas?.drawRoundRect(left, boxTop, right, boxBottom, 6f, 6f, fillPaint)
            canvas?.drawRoundRect(left, boxTop, right, boxBottom, 6f, 6f, strokePaint)

            y += padding
            for ((lineText, paint) in lines) {
                canvas?.drawText(lineText, left + padding, y + paint.textSize, paint)
                y += paint.textSize + 4f
            }
            y += padding + 12f
        }

        private fun renderRepository(number: Int, repo: com.example.githubprofileexplorer.domain.PortfolioPdfRepository) {
            val lines = mutableListOf<Pair<String, Paint>>()
            lines.add("$number. ${repo.name}" to namePaint)
            lines.add("Full name: ${repo.fullName}" to body)
            repo.githubId?.let { lines.add("GitHub ID: $it" to body) }
            lines.add("Repository: ${repo.url}" to small)
            repo.description?.takeIf { it.isNotBlank() }?.let { lines.add("Description: $it" to body) }
            lines.add("Primary language: ${repo.language ?: "Not specified"}" to body)
            if (repo.topics.isNotEmpty()) {
                lines.add("Topics: ${repo.topics.joinToString(", ")}" to body)
            }
            repo.stars?.let { lines.add("Stars: $it | Forks: ${repo.forks ?: 0} | Open issues: ${repo.openIssues ?: 0}" to body) }
            repo.licenseName?.let { lines.add("License: $it" to body) }
            repo.defaultBranch?.let { lines.add("Default branch: $it" to body) }
            repo.createdAt?.let { lines.add("Created: ${PortfolioPdfDataMapper.formatTimestamp(it)}" to small) }
            repo.updatedAt?.let { lines.add("Updated: ${PortfolioPdfDataMapper.formatTimestamp(it)}" to small) }
            repo.readmeUrl?.let { lines.add("README: $it" to small) }

            val contentWidth = (right - left) - 16f
            val wrappedLines = mutableListOf<Pair<String, Paint>>()
            for ((textVal, paint) in lines) {
                wrap(textVal, paint, contentWidth).forEach { wrappedLine ->
                    wrappedLines.add(wrappedLine to paint)
                }
            }

            val padding = 8f
            var totalHeight = padding * 2
            for ((_, paint) in wrappedLines) {
                totalHeight += paint.textSize + 4f
            }

            ensureSpace(totalHeight + 16f)

            val boxTop = y
            val boxBottom = y + totalHeight

            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = 0xfff9fafb.toInt() }
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = 0xffd1d5db.toInt(); strokeWidth = 1f }
            canvas?.drawRoundRect(left, boxTop, right, boxBottom, 6f, 6f, fillPaint)
            canvas?.drawRoundRect(left, boxTop, right, boxBottom, 6f, 6f, strokePaint)

            y += padding
            for ((wrappedLine, paint) in wrappedLines) {
                canvas?.drawText(wrappedLine, left + padding, y + paint.textSize, paint)
                y += paint.textSize + 4f
            }
            y += padding + 12f
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
    }
}
