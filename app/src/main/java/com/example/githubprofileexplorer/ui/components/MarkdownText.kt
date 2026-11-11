package com.example.githubprofileexplorer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.githubprofileexplorer.domain.MdBlock

private const val MAX_RENDERED_BLOCKS = 300

/** Sanitizes text to normal format with no special markdown symbols or characters like #, $, %, ^, *, _, `, etc. */
fun sanitizeNormalText(input: String): String {
    return input
        .replace("%", "")
        .replace("^", "")
        .replace("_", "")
        .replace("*", "")
        .replace("~", "")
        .replace("|", "")
        .replace("<", "")
        .replace(">", "")
        .replace("[", "")
        .replace("]", "")
        .replace("{", "")
        .replace("}", "")
        .replace("#", "")
        .replace("$", "")
        .replace("`", "")
        .replace("\\", "")
        .replace("\r\n", "\n")
        .trim()
}

/** Renders README blocks and responses in normal font with no special characters. */
@Composable
fun MarkdownText(blocks: List<MdBlock>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        blocks.take(MAX_RENDERED_BLOCKS).forEach { block ->
            val rawText = when (block) {
                is MdBlock.Heading -> block.text
                is MdBlock.Paragraph -> block.text
                is MdBlock.Bullet -> block.text
                is MdBlock.Numbered -> block.text
                is MdBlock.Quote -> block.text
                is MdBlock.Code -> block.text
                MdBlock.Rule -> null
            }
            if (rawText != null) {
                val cleaned = sanitizeNormalText(rawText)
                if (cleaned.isNotBlank()) {
                    Text(
                        text = cleaned,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}


