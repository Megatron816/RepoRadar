package com.example.githubprofileexplorer.domain

/** Block-level pieces of a README, enough for a readable (not pixel-perfect) rendering. */
sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data class Bullet(val text: String, val depth: Int = 0) : MdBlock
    data class Numbered(val number: Int, val text: String) : MdBlock
    data class Quote(val text: String) : MdBlock
    data class Code(val text: String) : MdBlock
    data object Rule : MdBlock
}

object MarkdownParser {

    private val commentRegex = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
    private val htmlTagRegex = Regex("<[^>]+>")
    private val imageRegex = Regex("!\\[[^\\]]*]\\([^)]*\\)")
    private val emptyLinkRegex = Regex("\\[\\s*]\\([^)]*\\)")
    private val headingRegex = Regex("^\\s{0,3}(#{1,6})\\s+(.*?)\\s*#*\\s*$")
    private val ruleRegex = Regex("^\\s{0,3}([-*_])(\\s*\\1){2,}\\s*$")
    private val bulletRegex = Regex("^(\\s*)[-*+]\\s+(.*)$")
    private val numberedRegex = Regex("^\\s*(\\d{1,9})[.)]\\s+(.*)$")
    private val quoteRegex = Regex("^\\s{0,3}>\\s?(.*)$")
    private val tableSeparatorRegex = Regex("^\\s*\\|?[\\s:|-]*-[\\s:|-]*\\|?\\s*$")
    private val fenceRegex = Regex("^\\s*(```|~~~)")

    fun parse(source: String): List<MdBlock> {
        val lines = source.replace("\r\n", "\n").replace('\r', '\n')
            .replace(commentRegex, "")
            .split('\n')

        val blocks = mutableListOf<MdBlock>()
        val paragraph = mutableListOf<String>()
        val code = mutableListOf<String>()
        var inCode = false

        fun flushParagraph() {
            if (paragraph.isNotEmpty()) {
                blocks += MdBlock.Paragraph(paragraph.joinToString(" "))
                paragraph.clear()
            }
        }

        for (raw in lines) {
            if (fenceRegex.containsMatchIn(raw)) {
                if (inCode) {
                    blocks += MdBlock.Code(code.joinToString("\n"))
                    code.clear()
                    inCode = false
                } else {
                    flushParagraph()
                    inCode = true
                }
                continue
            }
            if (inCode) {
                code += raw
                continue
            }

            val line = clean(raw)
            if (line.isBlank()) {
                // Lines that were only HTML or badges clean to blank; a truly blank raw line ends the paragraph.
                if (raw.isBlank()) flushParagraph()
                continue
            }

            val heading = headingRegex.matchEntire(line)
            val bullet = bulletRegex.matchEntire(line)
            val numbered = numberedRegex.matchEntire(line)
            val quote = quoteRegex.matchEntire(line)
            when {
                ruleRegex.matches(line) -> { flushParagraph(); blocks += MdBlock.Rule }
                heading != null -> {
                    flushParagraph()
                    blocks += MdBlock.Heading(heading.groupValues[1].length, heading.groupValues[2])
                }
                bullet != null -> {
                    flushParagraph()
                    blocks += MdBlock.Bullet(bullet.groupValues[2], bullet.groupValues[1].length / 2)
                }
                numbered != null -> {
                    flushParagraph()
                    blocks += MdBlock.Numbered(numbered.groupValues[1].toInt(), numbered.groupValues[2])
                }
                quote != null -> { flushParagraph(); blocks += MdBlock.Quote(quote.groupValues[1]) }
                line.trimStart().startsWith("|") -> {
                    flushParagraph()
                    if (!tableSeparatorRegex.matches(line)) {
                        blocks += MdBlock.Paragraph(
                            line.trim().trim('|').split('|').joinToString("  ·  ") { it.trim() }
                        )
                    }
                }
                else -> paragraph += line.trim()
            }
        }
        if (inCode && code.isNotEmpty()) blocks += MdBlock.Code(code.joinToString("\n"))
        flushParagraph()
        return blocks
    }

    /** Drops HTML tags, images and badge links, which cannot be shown as plain text. */
    private fun clean(line: String): String =
        line.replace(imageRegex, "")
            .replace(emptyLinkRegex, "")
            .replace(htmlTagRegex, "")
}
