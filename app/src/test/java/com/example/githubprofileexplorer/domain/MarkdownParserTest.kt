package com.example.githubprofileexplorer.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownParserTest {

    @Test
    fun parsesCommonBlocks() {
        val markdown = listOf(
            "# Title",
            "",
            "Some **text** here.",
            "- one",
            "- two",
            "",
            "```kotlin",
            "val x = 1 < 2",
            "```",
            "> quote",
            "---",
            "1. first"
        ).joinToString("\n")

        assertEquals(
            listOf(
                MdBlock.Heading(1, "Title"),
                MdBlock.Paragraph("Some **text** here."),
                MdBlock.Bullet("one"),
                MdBlock.Bullet("two"),
                MdBlock.Code("val x = 1 < 2"),
                MdBlock.Quote("quote"),
                MdBlock.Rule,
                MdBlock.Numbered(1, "first")
            ),
            MarkdownParser.parse(markdown)
        )
    }

    @Test
    fun joinsWrappedParagraphLines() {
        assertEquals(
            listOf(MdBlock.Paragraph("first line second line")),
            MarkdownParser.parse("first line\nsecond line")
        )
    }

    @Test
    fun stripsHtmlImagesAndComments() {
        val markdown = "<!-- hidden -->\n![badge](https://img.shields.io/x.svg)\n<p align=\"center\">Hello</p>"
        assertEquals(listOf(MdBlock.Paragraph("Hello")), MarkdownParser.parse(markdown))
    }

    @Test
    fun rendersTableRowsAsText() {
        val markdown = "| a | b |\n|---|---|\n| 1 | 2 |"
        assertEquals(
            listOf(MdBlock.Paragraph("a  ·  b"), MdBlock.Paragraph("1  ·  2")),
            MarkdownParser.parse(markdown)
        )
    }

    @Test
    fun nestedBulletsKeepDepth() {
        assertEquals(
            listOf(MdBlock.Bullet("top", 0), MdBlock.Bullet("child", 1)),
            MarkdownParser.parse("- top\n  - child")
        )
    }

    @Test
    fun closesUnterminatedCodeFence() {
        assertEquals(listOf(MdBlock.Code("a = 1")), MarkdownParser.parse("```\na = 1"))
    }
}
