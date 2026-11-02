package com.example.githubprofileexplorer.ui.components

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

private val KnownLanguageColors = mapOf(
    "Kotlin" to Color(0xFFA97BFF),
    "Java" to Color(0xFFB07219),
    "JavaScript" to Color(0xFFF1E05A),
    "TypeScript" to Color(0xFF3178C6),
    "Python" to Color(0xFF3572A5),
    "C++" to Color(0xFFF34B7D),
    "C" to Color(0xFF555555),
    "C#" to Color(0xFF178600),
    "Go" to Color(0xFF00ADD8),
    "Rust" to Color(0xFFDEA584),
    "Swift" to Color(0xFFF05138),
    "Dart" to Color(0xFF00B4AB),
    "HTML" to Color(0xFFE34C26),
    "CSS" to Color(0xFF563D7C),
    "Shell" to Color(0xFF89E051),
    "PHP" to Color(0xFF4F5D95),
    "Ruby" to Color(0xFF701516),
    "Jupyter Notebook" to Color(0xFFDA5B0B),
    "Vue" to Color(0xFF41B883),
    "Lua" to Color(0xFF000080)
)

/** GitHub's language colors for popular languages, and a stable generated color for the rest. */
fun languageColor(language: String): Color =
    KnownLanguageColors[language]
        ?: Color.hsv(hue = (abs(language.hashCode()) % 360).toFloat(), saturation = 0.55f, value = 0.75f)
