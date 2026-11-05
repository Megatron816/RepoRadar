package com.example.githubprofileexplorer.domain

import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Pure, deterministic adapters. Repository scripts are text only: nothing is executed or evaluated.
 * A recognized manifest without an adapter is deliberately reported as unsupported.
 */
object ManifestParserRegistry {
    private val adapters: List<ManifestParserAdapter> = listOf(
        GradleAdapter, PackageJsonAdapter, PythonRequirementsAdapter, CargoAdapter, GoModAdapter,
        ComposerAdapter, MavenAdapter, DotNetAdapter, RubyAdapter, SwiftAdapter, DartAdapter,
        ElixirAdapter, ScalaAdapter, CppAdapter, PythonProjectAdapter
    )

    fun parse(files: Map<String, String>): MultiEcosystemParseResult {
        val detected = files.keys.filter(::isKnownManifest).sorted()
        val parsed = mutableListOf<String>()
        val unsupported = mutableListOf<String>()
        val modules = mutableListOf<ArchitectureModule>()
        val declarations = mutableListOf<ManifestDeclaration>()
        val warnings = mutableListOf<ArchitectureWarning>()
        val names = mutableListOf<String>()

        adapters.forEach { adapter ->
            if (!files.keys.any(adapter::recognizes)) return@forEach
            val result = adapter.parse(files.filterKeys(adapter::recognizes))
            parsed += result.parsedManifests
            modules += result.modules
            declarations += result.declarations
            warnings += result.warnings.map { it.copy(message = "[${adapter.name}] ${it.message}") }
            names += "${adapter.name}@${adapter.version}"
        }
        detected.filterNot { it in parsed }.forEach {
            unsupported += it
            warnings += ArchitectureWarning("Manifest detected but unsupported or not safely parsed.", it)
        }
        val mergedDeclarations = declarations.groupBy {
            "${it.ecosystem}:${it.projectRoot}:${it.coordinate}"
        }.values.map { group ->
            group.firstOrNull { it.dependencyType != "lock" }?.copy(
                resolvedVersion = group.firstNotNullOfOrNull {
                    it.resolvedVersion ?: if (it.dependencyType == "lock") it.version else null
                }
            ) ?: group.first()
        }
        return MultiEcosystemParseResult(
            modules.distinctBy { "${it.ecosystem}:${it.path}" }.sortedBy { "${it.ecosystem}:${it.path}" },
            mergedDeclarations.distinctBy { "${it.ecosystem}:${it.path}:${it.line}:${it.coordinate}:${it.scope}" }
                .sortedWith(compareBy({ it.ecosystem.name }, { it.path }, { it.line ?: 0 }, { it.coordinate })),
            warnings.distinctBy { "${it.message}:${it.sourcePath}:${it.line}" },
            detected, parsed.distinct().sorted(), unsupported.sorted(), names.sorted()
        )
    }

    fun isKnownManifest(path: String): Boolean {
        val name = path.substringAfterLast('/').lowercase()
        return name in setOf(
            "settings.gradle", "settings.gradle.kts", "build.gradle", "build.gradle.kts",
            "gradle-wrapper.properties",
            "pom.xml", "package.json", "package-lock.json", "npm-shrinkwrap.json", "yarn.lock",
            "pnpm-lock.yaml", "pyproject.toml", "requirements.txt", "pipfile", "pipfile.lock",
            "poetry.lock", "uv.lock", "cargo.toml", "cargo.lock", "go.mod", "go.work", "go.sum",
            "packages.lock.json", "gemfile", "gemfile.lock", "composer.json", "composer.lock",
            "package.swift", "package.resolved", "pubspec.yaml", "pubspec.lock", "mix.exs",
            "mix.lock", "build.sbt", "cmakelists.txt", "meson.build", "vcpkg.json",
            "conanfile.py", "conanfile.txt", "directory.build.props", "directory.packages.props"
        ) || name.endsWith(".csproj") || name.endsWith(".fsproj") || name.endsWith(".gemspec") ||
            name.endsWith(".sln")
    }

    private object GradleAdapter : ManifestParserAdapter {
        override val name = "Gradle static subset"
        override val version = "1"
        override val ecosystem = ArchitectureEcosystem.GRADLE
        override val capabilities = ParserCapabilities(internalRelationships = true)
        override fun recognizes(path: String) = path.endsWith(".gradle") || path.endsWith(".gradle.kts") ||
            path == "gradle/libs.versions.toml"
        override fun parse(files: Map<String, String>): AdapterParseResult {
            val result = GradleManifestParser.parse(files)
            return AdapterParseResult(ecosystem, result.modules.map { it.copy(ecosystem = ecosystem) },
                result.declarations.map { it.copy(ecosystem = ecosystem) }, result.warnings,
                files.keys.sorted(), capabilities)
        }
    }

    private object PackageJsonAdapter : ManifestParserAdapter {
        override val name = "npm package.json"
        override val version = "1"
        override val ecosystem = ArchitectureEcosystem.NODE
        override val capabilities = ParserCapabilities(workspaceRelationships = true)
        override fun recognizes(path: String) = path.substringAfterLast('/').lowercase() in
            setOf("package.json", "package-lock.json", "npm-shrinkwrap.json")
        override fun parse(files: Map<String, String>): AdapterParseResult {
            val modules = mutableListOf<ArchitectureModule>()
            val declarations = mutableListOf<ManifestDeclaration>()
            val warnings = mutableListOf<ArchitectureWarning>()
            files.toSortedMap().forEach { (path, text) ->
                val root = path.substringBeforeLast('/', "")
                val module = if (root.isBlank()) ":" else root
                if (path.substringAfterLast('/').equals("package.json", true)) {
                    modules += ArchitectureModule(module, path, ecosystem, root)
                }
                try {
                    val json = JsonParser.parseString(text).asJsonObject
                    if (path.substringAfterLast('/').equals("package.json", true)) {
                        addJsonDependencies(json, "dependencies", ArchitectureScope.IMPLEMENTATION, path, root, declarations)
                        addJsonDependencies(json, "devDependencies", ArchitectureScope.DEV, path, root, declarations)
                        addJsonDependencies(json, "peerDependencies", ArchitectureScope.PEER, path, root, declarations)
                        addJsonDependencies(json, "optionalDependencies", ArchitectureScope.OPTIONAL, path, root, declarations)
                        json.getAsJsonObject("workspaces")?.getAsJsonArray("packages")?.forEach {
                            modules += ArchitectureModule(it.asString, path, ecosystem, root)
                        }
                    } else {
                        json.getAsJsonObject("packages")?.entrySet()?.forEach { (key, value) ->
                            val name = key.removePrefix("node_modules/").takeIf { it.isNotBlank() } ?: return@forEach
                            value.asJsonObject.get("version")?.asString?.let { resolved ->
                                declarations += ManifestDeclaration(name, null, ArchitectureScope.RUNTIME, path, null,
                                    ecosystem = ecosystem, projectRoot = root, resolvedVersion = resolved, dependencyType = "lock")
                            }
                        }
                    }
                } catch (_: Exception) {
                    warnings += ArchitectureWarning("Malformed package.json; dependencies were not guessed.", path)
                }
            }
            return AdapterParseResult(ecosystem, modules, declarations, warnings, files.keys.sorted(), capabilities)
        }
        private fun addJsonDependencies(json: JsonObject, key: String, scope: ArchitectureScope, path: String,
                                         root: String, output: MutableList<ManifestDeclaration>) {
            json.getAsJsonObject(key)?.entrySet()?.sortedBy { it.key }?.forEach { (name, value) ->
                val version = value.asString
                output += ManifestDeclaration(name, version, scope, path, null, version.contains('$') || version == "*",
                    ecosystem = ecosystem, projectRoot = root, dependencyType = key)
            }
        }
    }

    private object PythonRequirementsAdapter : ManifestParserAdapter {
        override val name = "Python requirements"
        override val version = "1"
        override val ecosystem = ArchitectureEcosystem.PYTHON
        override val capabilities = ParserCapabilities()
        override fun recognizes(path: String) = path.substringAfterLast('/').lowercase() == "requirements.txt" ||
            path.substringAfterLast('/').lowercase().startsWith("requirements-")
        override fun parse(files: Map<String, String>): AdapterParseResult {
            val declarations = files.toSortedMap().flatMap { (path, text) ->
                text.lineSequence().mapIndexedNotNull { index, raw ->
                    val line = raw.trim()
                    if (line.isBlank() || line.startsWith("#") || line.startsWith("-")) null else {
                        val match = Regex("^([A-Za-z0-9_.-]+)\\s*(?:([<>=!~]+)\\s*([^;\\s]+))?").find(line)
                        match?.let { ManifestDeclaration(it.groupValues[1], it.groupValues[3].ifBlank { null },
                            if (path.contains("dev", true) || path.contains("test", true)) ArchitectureScope.DEV else ArchitectureScope.IMPLEMENTATION,
                            path, index + 1, it.groupValues[3].isBlank(), ecosystem = ecosystem, dependencyType = "requirements") }
                    }
                }.toList()
            }
            return AdapterParseResult(ecosystem, listOf(ArchitectureModule(":", files.keys.minOrNull(), ecosystem)),
                declarations, emptyList(), files.keys.sorted(), capabilities)
        }
    }

    private object CargoAdapter : LineManifestAdapter(ArchitectureEcosystem.RUST, "Cargo.toml")
    private object GoModAdapter : ManifestParserAdapter {
        override val name = "Go modules"
        override val version = "1"
        override val ecosystem = ArchitectureEcosystem.GO
        override val capabilities = ParserCapabilities()
        override fun recognizes(path: String) = path.substringAfterLast('/').equals("go.mod", true)
        override fun parse(files: Map<String, String>): AdapterParseResult {
            val declarations = files.toSortedMap().flatMap { (path, text) ->
                text.lineSequence().mapIndexedNotNull { index, raw ->
                    val line = raw.trim().removePrefix("(").trim()
                    val match = Regex("^([A-Za-z0-9._~:/@-]+)\\s+([^\\s]+)").find(line)
                    if (match == null || line.startsWith("module ") || line.startsWith("go ")) null else
                        ManifestDeclaration(match.groupValues[1], match.groupValues[2], ArchitectureScope.IMPLEMENTATION,
                            path, index + 1, match.groupValues[2].contains("$"), ecosystem = ecosystem)
                }.toList()
            }
            return AdapterParseResult(ecosystem, listOf(ArchitectureModule(":", files.keys.minOrNull(), ecosystem)),
                declarations, emptyList(), files.keys.sorted(), capabilities)
        }
    }

    private object ComposerAdapter : JsonManifestAdapter(ArchitectureEcosystem.PHP, "composer.json")

    private object MavenAdapter : ManifestParserAdapter {
        override val name = "Maven XML subset"
        override val version = "1"
        override val ecosystem = ArchitectureEcosystem.MAVEN
        override val capabilities = ParserCapabilities(internalRelationships = true)
        override fun recognizes(path: String) = path.substringAfterLast('/').equals("pom.xml", true)
        override fun parse(files: Map<String, String>): AdapterParseResult {
            val declarations = files.toSortedMap().flatMap { (path, text) ->
                Regex("""(?s)<dependency>\s*(.*?)\s*</dependency>""").findAll(text).mapNotNull { match ->
                    val body = match.groupValues[1]
                    val group = Regex("""<groupId>\s*([^<]+)""").find(body)?.groupValues?.get(1)?.trim()
                    val artifact = Regex("""<artifactId>\s*([^<]+)""").find(body)?.groupValues?.get(1)?.trim()
                    if (group == null || artifact == null) null else {
                        val version = Regex("""<version>\s*([^<]+)""").find(body)?.groupValues?.get(1)?.trim()
                        val scope = when (Regex("""<scope>\s*([^<]+)""").find(body)?.groupValues?.get(1)?.trim()) {
                            "test" -> ArchitectureScope.TEST
                            "provided" -> ArchitectureScope.COMPILE_ONLY
                            "runtime" -> ArchitectureScope.RUNTIME_ONLY
                            else -> ArchitectureScope.IMPLEMENTATION
                        }
                        ManifestDeclaration("$group:$artifact", version, scope, path, text.lineNumber(match.range.first),
                            version == null || version.contains("$"), ecosystem = ecosystem,
                            projectRoot = path.substringBeforeLast('/', ""))
                    }
                }.toList()
            }
            val modules = files.keys.sorted().map { path ->
                ArchitectureModule(path.substringBeforeLast('/', ""), path, ecosystem, path.substringBeforeLast('/', ""))
            }
            return AdapterParseResult(ecosystem, modules, declarations, emptyList(), files.keys.sorted(), capabilities)
        }
    }

    private object DotNetAdapter : XmlManifestAdapter(ArchitectureEcosystem.DOTNET)
    private object RubyAdapter : LineDependencyAdapter(ArchitectureEcosystem.RUBY)
    private object SwiftAdapter : LineDependencyAdapter(ArchitectureEcosystem.SWIFT)
    private object DartAdapter : LineDependencyAdapter(ArchitectureEcosystem.DART)
    private object ElixirAdapter : LineDependencyAdapter(ArchitectureEcosystem.ELIXIR)
    private object ScalaAdapter : LineDependencyAdapter(ArchitectureEcosystem.SCALA)
    private object CppAdapter : LineDependencyAdapter(ArchitectureEcosystem.CPP)
    private object PythonProjectAdapter : LineDependencyAdapter(ArchitectureEcosystem.PYTHON)

    private abstract class LineDependencyAdapter(
        override val ecosystem: ArchitectureEcosystem
    ) : ManifestParserAdapter {
        override val name = "$ecosystem static subset"
        override val version = "1"
        override val capabilities = ParserCapabilities()
        override fun recognizes(path: String): Boolean {
            val name = path.substringAfterLast('/').lowercase()
            return when (ecosystem) {
                ArchitectureEcosystem.RUBY -> name == "gemfile" || name.endsWith(".gemspec") || name == "gemfile.lock"
                ArchitectureEcosystem.SWIFT -> name == "package.swift" || name == "package.resolved"
                ArchitectureEcosystem.DART -> name == "pubspec.yaml" || name == "pubspec.lock"
                ArchitectureEcosystem.ELIXIR -> name == "mix.exs" || name == "mix.lock"
                ArchitectureEcosystem.SCALA -> name == "build.sbt"
                ArchitectureEcosystem.CPP -> name in setOf("cmakelists.txt", "meson.build", "vcpkg.json", "conanfile.py", "conanfile.txt")
                ArchitectureEcosystem.PYTHON -> name in setOf("pyproject.toml", "pipfile", "poetry.lock", "uv.lock")
                else -> false
            }
        }
        override fun parse(files: Map<String, String>): AdapterParseResult {
            val declarations = files.toSortedMap().flatMap { (path, text) ->
                text.lineSequence().mapIndexedNotNull { index, raw ->
                    val line = raw.trim()
                    if (line.isBlank() || line.startsWith("#") || line.startsWith("//") || line.startsWith("*")) null else {
                        val match = Regex("""["']?([A-Za-z0-9_.@:/-]+)["']?\s*(?:=|==|>=|>|~>|:)\s*["']?([A-Za-z0-9+*.^<>=~|,-]+)""").find(line)
                        match?.let {
                            ManifestDeclaration(it.groupValues[1], it.groupValues[2],
                                if (path.contains("dev", true) || line.contains("test", true)) ArchitectureScope.DEV else ArchitectureScope.IMPLEMENTATION,
                                path, index + 1, it.groupValues[2].contains("*") || it.groupValues[2].contains("$"),
                                ecosystem = ecosystem, projectRoot = path.substringBeforeLast('/', ""), dependencyType = "static-subset")
                        }
                    }
                }.toList()
            }
            val warnings = if (declarations.isEmpty()) listOf(
                ArchitectureWarning("Only simple dependency declarations are supported; dynamic syntax may be unresolved.", files.keys.firstOrNull())
            ) else emptyList()
            return AdapterParseResult(ecosystem, listOf(ArchitectureModule(":", files.keys.minOrNull(), ecosystem)),
                declarations, warnings, files.keys.sorted(), capabilities)
        }
    }

    private abstract class XmlManifestAdapter(
        override val ecosystem: ArchitectureEcosystem
    ) : ManifestParserAdapter {
        override val name = "$ecosystem XML/static subset"
        override val version = "1"
        override val capabilities = ParserCapabilities()
        override fun recognizes(path: String): Boolean {
            val name = path.substringAfterLast('/').lowercase()
            return name.endsWith(".csproj") || name.endsWith(".fsproj") || name == "packages.lock.json" ||
                name == "directory.build.props" || name == "directory.packages.props"
        }
        override fun parse(files: Map<String, String>): AdapterParseResult {
            val declarations = files.toSortedMap().flatMap { (path, text) ->
                Regex("""(?:<PackageReference[^>]+Include\s*=\s*["']([^"']+)["'][^>]*Version\s*=\s*["']([^"']+)["']|<PackageReference[^>]+Version\s*=\s*["']([^"']+)["'][^>]*Include\s*=\s*["']([^"']+)["'])""")
                    .findAll(text).map {
                        val name = it.groupValues[1].ifBlank { it.groupValues[4] }
                        val version = it.groupValues[2].ifBlank { it.groupValues[3] }
                        ManifestDeclaration(name, version, ArchitectureScope.IMPLEMENTATION, path, text.lineNumber(it.range.first),
                            version.contains("$"), ecosystem = ecosystem, projectRoot = path.substringBeforeLast('/', ""))
                    }.toList()
            }
            return AdapterParseResult(ecosystem, listOf(ArchitectureModule(":", files.keys.minOrNull(), ecosystem)),
                declarations, emptyList(), files.keys.sorted(), capabilities)
        }
    }

    private abstract class LineManifestAdapter(
        override val ecosystem: ArchitectureEcosystem, private val fileName: String
    ) : ManifestParserAdapter {
        override val name = "$ecosystem static manifest"
        override val version = "1"
        override val capabilities = ParserCapabilities()
        override fun recognizes(path: String) = path.substringAfterLast('/').equals(fileName, true)
        override fun parse(files: Map<String, String>): AdapterParseResult {
            val declarations = files.toSortedMap().flatMap { (path, text) ->
                text.lineSequence().mapIndexedNotNull { index, line ->
                    val clean = line.substringBefore('#').trim()
                    val match = Regex("^(?:[A-Za-z0-9_.-]+\\s*=\\s*\\\"?)([^\\\"\\s]+)").find(clean)
                    match?.let { ManifestDeclaration(clean.substringBefore('=').trim(), it.groupValues[1], ArchitectureScope.IMPLEMENTATION,
                        path, index + 1, clean.contains("$"), ecosystem = ecosystem) }
                }.toList()
            }
            return AdapterParseResult(ecosystem, listOf(ArchitectureModule(":", files.keys.minOrNull(), ecosystem)),
                declarations, emptyList(), files.keys.sorted(), capabilities)
        }
    }

    private abstract class JsonManifestAdapter(
        override val ecosystem: ArchitectureEcosystem, private val fileName: String
    ) : ManifestParserAdapter {
        override val name = "$ecosystem manifest"
        override val version = "1"
        override val capabilities = ParserCapabilities()
        override fun recognizes(path: String) = path.substringAfterLast('/').equals(fileName, true)
        override fun parse(files: Map<String, String>): AdapterParseResult {
            val declarations = mutableListOf<ManifestDeclaration>()
            val warnings = mutableListOf<ArchitectureWarning>()
            files.toSortedMap().forEach { (path, text) -> try {
                val json = JsonParser.parseString(text).asJsonObject
                listOf("require" to ArchitectureScope.IMPLEMENTATION, "require-dev" to ArchitectureScope.DEV).forEach { (key, scope) ->
                    json.getAsJsonObject(key)?.entrySet()?.forEach { (name, value) ->
                        declarations += ManifestDeclaration(name, value.asString, scope, path, null, ecosystem = ecosystem)
                    }
                }
            } catch (_: Exception) { warnings += ArchitectureWarning("Malformed structured manifest.", path) } }
            return AdapterParseResult(ecosystem, listOf(ArchitectureModule(":", files.keys.minOrNull(), ecosystem)),
                declarations, warnings, files.keys.sorted(), capabilities)
        }
    }

    private fun String.lineNumber(index: Int): Int = substring(0, index).count { it == '\n' } + 1
}
