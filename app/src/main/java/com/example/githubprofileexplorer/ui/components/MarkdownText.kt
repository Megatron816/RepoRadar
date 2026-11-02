package com.example.githubprofileexplorer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.githubprofileexplorer.domain.MdBlock

private const val MAX_RENDERED_BLOCKS = 300

private val inlineRegex =
    Regex("`([^`]+)`|\\*\\*([^*]+)\\*\\*|__([^_]+)__|\\*([^*\\s][^*]*)\\*|\\[([^\\]]+)]\\(([^)\\s]+)[^)]*\\)")

/** Renders parsed README blocks with headings, lists, quotes, code blocks and tappable links. */
@Composable
fun MarkdownText(blocks: List<MdBlock>, modifier: Modifier = Modifier) {
    val linkColor = MaterialTheme.colorScheme.primary
    val codeBackground = MaterialTheme.colorScheme.surfaceContainerHigh

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        blocks.take(MAX_RENDERED_BLOCKS).forEach { block ->
            when (block) {
                is MdBlock.Heading -> Text(
                    text = inline(block.text, linkColor, codeBackground),
                    modifier = Modifier.padding(top = if (block.level <= 2) 6.dp else 2.dp),
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.headlineSmall
                        2 -> MaterialTheme.typography.titleLarge
                        3 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    }
                )

                is MdBlock.Paragraph -> Text(
                    text = inline(block.text, linkColor, codeBackground),
                    style = MaterialTheme.typography.bodyMedium
                )

                is MdBlock.Bullet -> ListRow(
                    marker = "•",
                    text = inline(block.text, linkColor, codeBackground),
                    indent = (block.depth * 16).coerceAtMost(48)
                )

                is MdBlock.Numbered -> ListRow(
                    marker = "${block.number}.",
                    text = inline(block.text, linkColor, codeBackground),
                    indent = 0
                )

                is MdBlock.Quote -> Row(Modifier.height(IntrinsicSize.Min)) {
                    Surface(
                        modifier = Modifier
                            .width(3.dp)
                            .fillMaxHeight(),
                        color = MaterialTheme.colorScheme.outline
                    ) {}
                    Text(
                        text = inline(block.text, linkColor, codeBackground),
                        modifier = Modifier.padding(start = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                is MdBlock.Code -> Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = codeBackground
                ) {
                    Text(
                        text = block.text,
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        softWrap = false
                    )
                }

                MdBlock.Rule -> HorizontalDivider(Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun ListRow(marker: String, text: AnnotatedString, indent: Int) {
    Row(Modifier.padding(start = indent.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(marker, style = MaterialTheme.typography.bodyMedium)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun inline(text: String, linkColor: Color, codeBackground: Color): AnnotatedString =
    remember(text, linkColor, codeBackground) { buildInline(text, linkColor, codeBackground) }

private fun buildInline(text: String, linkColor: Color, codeBackground: Color): AnnotatedString =
    buildAnnotatedString {
        var last = 0
        for (match in inlineRegex.findAll(text)) {
            append(text.substring(last, match.range.first))
            val groups = match.groupValues
            when {
                groups[1].isNotEmpty() -> withStyle(
                    SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground)
                ) { append(groups[1]) }

                groups[2].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(groups[2]) }
                groups[3].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(groups[3]) }
                groups[4].isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(groups[4]) }
                else -> {
                    val label = groups[5]
                    val url = groups[6]
                    val style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        withLink(LinkAnnotation.Url(url, TextLinkStyles(style = style))) { append(label) }
                    } else {
                        // Relative links inside a README cannot be opened from here.
                        withStyle(SpanStyle(color = linkColor)) { append(label) }
                    }
                }
            }
            last = match.range.last + 1
        }
        append(text.substring(last))
    }
