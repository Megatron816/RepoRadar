package com.example.githubprofileexplorer.domain

/**
 * Parses only declarative Gradle syntax. Scripts are untrusted text; no Gradle code is executed.
 */
object GradleManifestParser {
    private val moduleInclude = Regex("""(?m)\binclude\s*(?:\(\s*)?([^\n)]*)""")
    private val includedModule = Regex("""['"](:[^'"]+)['"]""")
    private val dependency = Regex("""(?m)^\s*(implementation|api|compileOnly|runtimeOnly|testImplementation|androidTestImplementation)\s*(?:\(\s*)?["']([^"']+)["']""")
    private val aliasDependency = Regex("""(?m)^\s*(implementation|api|compileOnly|runtimeOnly|testImplementation|androidTestImplementation)\s*(?:\(\s*)?libs\.([A-Za-z0-9_.-]+)""")
    private val projectDependency = Regex("""project\s*\(\s*["'](:[^"']+)["']\s*\)""")
    private val catalog = Regex("""(?m)^\s*([A-Za-z0-9_.-]+)\s*=\s*\{[^}]*group\s*=\s*["']([^"']+)["'][^}]*name\s*=\s*["']([^"']+)["'][^}]*version(?:\.ref)?\s*=\s*["']([^"']+)["']""")

    fun parse(files: Map<String, String>): GradleParseResult {
        val warnings = mutableListOf<ArchitectureWarning>()
        val settings = files.entries.firstOrNull { it.key.substringAfterLast('/').startsWith("settings.gradle") }
        val modules = linkedSetOf<String>()
        modules.add(":")
        settings?.let { match ->
            moduleInclude.findAll(match.value).forEach { result ->
                includedModule.findAll(result.groupValues[1]).forEach { included ->
                    modules.add(included.groupValues[1])
                }
            }
        } ?: warnings.add(ArchitectureWarning("No settings.gradle(.kts) found; module discovery may be incomplete."))

        val versions = buildMap {
            files["gradle/libs.versions.toml"]?.let { text ->
                catalog.findAll(text).forEach { put(it.groupValues[1].replace('.', '-'), "${it.groupValues[2]}:${it.groupValues[3]}:${it.groupValues[4]}") }
            }
        }
        val declarations = mutableListOf<ManifestDeclaration>()
        files.filterKeys { it.endsWith(".gradle") || it.endsWith(".gradle.kts") }.forEach { (path, text) ->
            val module = path.substringBeforeLast('/').takeIf { '/' in path }?.let { ":${it.replace('/', ':')}" } ?: ":"
            if (module != ":" && modules.none { it == module || it.startsWith("$module:") }) modules.add(module)
            dependency.findAll(text).forEach { m ->
                val coordinate = m.groupValues[2]
                val parts = coordinate.split(':')
                val unresolved = parts.size < 3 || parts.any { it.contains('$') || it.contains('+') }
                declarations += ManifestDeclaration(coordinate.substringBeforeLast(':').ifBlank { coordinate }, parts.getOrNull(2), scope(m.groupValues[1]), path, text.lineNumber(m.range.first), unresolved)
            }
            aliasDependency.findAll(text).forEach { m ->
                val alias = m.groupValues[2].replace('.', '-')
                val resolved = versions[alias]
                declarations += if (resolved == null) ManifestDeclaration("libs.$alias", null, scope(m.groupValues[1]), path, text.lineNumber(m.range.first), true, "Version catalog alias could not be resolved")
                else {
                    val parts = resolved.split(':')
                    ManifestDeclaration("${parts[0]}:${parts[1]}", parts.getOrNull(2), scope(m.groupValues[1]), path, text.lineNumber(m.range.first))
                }
            }
            projectDependency.findAll(text).forEach { m ->
                declarations += ManifestDeclaration(m.groupValues[1], null, scope("implementation"), path, text.lineNumber(m.range.first))
            }
            if (text.contains("versionCatalogs") || text.contains("plugins {")) warnings += ArchitectureWarning("Dynamic or convention-plugin Gradle logic may not be resolved.", path)
        }
        return GradleParseResult(modules.map { ArchitectureModule(it, manifestFor(it, files)) }, declarations, warnings)
    }

    private fun manifestFor(module: String, files: Map<String, String>): String? {
        val base = module.removePrefix(":").replace(':', '/')
        return listOf("$base/build.gradle.kts", "$base/build.gradle", "build.gradle.kts", "build.gradle")
            .firstOrNull { it in files }
    }

    private fun scope(value: String) = when {
        value == "api" -> ArchitectureScope.API
        value == "compileOnly" -> ArchitectureScope.COMPILE_ONLY
        value == "runtimeOnly" -> ArchitectureScope.RUNTIME_ONLY
        value == "testImplementation" -> ArchitectureScope.TEST
        value == "androidTestImplementation" -> ArchitectureScope.ANDROID_TEST
        else -> ArchitectureScope.IMPLEMENTATION
    }

    private fun String.lineNumber(index: Int): Int = substring(0, index).count { it == '\n' } + 1
}
