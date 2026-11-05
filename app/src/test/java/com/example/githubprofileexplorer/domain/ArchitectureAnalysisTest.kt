package com.example.githubprofileexplorer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.example.githubprofileexplorer.data.repository.ArchitectureRepository

class ArchitectureAnalysisTest {
    @Test fun parsesGroovyKotlinAndAliasesWithoutExecutingScripts() {
        val result = GradleManifestParser.parse(
            mapOf(
                "settings.gradle" to "include ':app', ':core'",
                "build.gradle" to "implementation 'org.example:root:1.0'",
                "app/build.gradle.kts" to """
                    dependencies {
                      implementation("org.example:lib:1.2.3")
                      testImplementation(libs.junit)
                      implementation(project(":core"))
                    }
                """.trimIndent(),
                "gradle/libs.versions.toml" to """junit = { group = "junit", name = "junit", version = "4.13.2" }"""
            )
        )
        assertEquals(setOf(":", ":app", ":core"), result.modules.map { it.path }.toSet())
        assertTrue(result.declarations.any { it.coordinate == "org.example:lib" && it.version == "1.2.3" })
        assertTrue(result.declarations.any { it.coordinate == "junit:junit" })
        assertTrue(result.declarations.any { it.coordinate == ":core" })
    }

    @Test fun detectsCyclesSharedDependenciesAndIsolatedModules() {
        val parsed = GradleParseResult(
            listOf(ArchitectureModule(":a", "a/build.gradle"), ArchitectureModule(":b", "b/build.gradle"), ArchitectureModule(":c", "c/build.gradle")),
            listOf(
                ManifestDeclaration(":b", null, ArchitectureScope.IMPLEMENTATION, "a/build.gradle", 1),
                ManifestDeclaration(":a", null, ArchitectureScope.API, "b/build.gradle", 1),
                ManifestDeclaration("g:a", "1", ArchitectureScope.IMPLEMENTATION, "a/build.gradle", 2),
                ManifestDeclaration("g:a", "1", ArchitectureScope.IMPLEMENTATION, "b/build.gradle", 2)
            ),
            emptyList()
        )
        val graph = ArchitectureGraphAnalyzer.build(RepositoryRef(1, "o", "r", "main"), "sha", parsed, false)
        val analysis = ArchitectureGraphAnalyzer.analyze(graph)
        assertTrue(analysis.cycles.isNotEmpty())
        assertTrue(analysis.sharedExternalDependencies.contains("external:g:a"))
        assertTrue(analysis.isolatedModules.contains("module::c"))
    }

    @Test fun validatesRepositoryAndBranchIdentifiers() {
        ArchitectureRepository.validateIdentifier("owner", "repo")
        ArchitectureRepository.validateBranch("feature/name")
        assertTrue(runCatching { ArchitectureRepository.validateIdentifier("owner/name", "repo") }.isFailure)
        assertTrue(runCatching { ArchitectureRepository.validateBranch("../main") }.isFailure)
    }

    @Test
    fun comparesProjectsNodesVersionsAndParserSupportSeparately() {
        val before = ArchitectureGraph(
            RepositoryRef(1, "o", "r", "main"), "old",
            listOf(ArchitectureNode("external:NODE:react", "react", ArchitectureNodeType.EXTERNAL, "^18", ecosystem = ArchitectureEcosystem.NODE, projectRoot = "web")),
            emptyList(), emptyList(), false, parsedManifests = listOf("package.json")
        )
        val after = before.copy(
            commitSha = "new",
            nodes = listOf(
                ArchitectureNode("external:NODE:react", "react", ArchitectureNodeType.EXTERNAL, "^19", resolvedVersion = "19.0.0", ecosystem = ArchitectureEcosystem.NODE, projectRoot = "web"),
                ArchitectureNode("external:PYTHON:requests", "requests", ArchitectureNodeType.EXTERNAL, "2", ecosystem = ArchitectureEcosystem.PYTHON, projectRoot = "api")
            ),
            parsedManifests = listOf("package.json", "requirements.txt")
        )
        val comparison = ArchitectureSnapshotComparator.compare(before, after)
        assertTrue(comparison.addedProjects.contains("PYTHON:api"))
        assertTrue(comparison.changedVersions.contains("external:NODE:react"))
        assertTrue(comparison.parserSupportChanged)
    }
}
