package com.example.githubprofileexplorer.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ManifestParserRegistryTest {
    @Test
    fun parsesNodeScopesAndRecognizesLockFiles() {
        val result = ManifestParserRegistry.parse(mapOf(
            "package.json" to """{"dependencies":{"react":"^18.2.0"},"devDependencies":{"jest":"29.0.0"},"peerDependencies":{"plugin":"*"}}""",
            "package-lock.json" to """{"lockfileVersion":3}"""
        ))
        assertEquals(ArchitectureEcosystem.NODE, result.declarations.first { it.coordinate == "react" }.ecosystem)
        assertTrue(result.declarations.any { it.coordinate == "jest" && it.scope == ArchitectureScope.DEV })
        assertTrue(result.declarations.any { it.coordinate == "plugin" && it.scope == ArchitectureScope.PEER })
        assertTrue(result.parsedManifests.contains("package-lock.json"))
    }

    @Test
    fun combinesGradleAndPythonWithoutExecutingBuildLogic() {
        val result = ManifestParserRegistry.parse(mapOf(
            "settings.gradle.kts" to """include(":app")""",
            "app/build.gradle.kts" to """dependencies { implementation("g:a:1.0") }""",
            "requirements-dev.txt" to """
                pytest>=7
                # comment
                -r base.txt
            """.trimIndent(),
            "base.txt" to "requests==2.31.0"
        ))
        assertTrue(result.modules.any { it.ecosystem == ArchitectureEcosystem.GRADLE })
        assertTrue(result.modules.any { it.ecosystem == ArchitectureEcosystem.PYTHON })
        assertTrue(result.declarations.any { it.coordinate == "pytest" && it.scope == ArchitectureScope.DEV })
    }

    @Test
    fun malformedStructuredManifestProducesWarningNotPartialSuccessClaim() {
        val result = ManifestParserRegistry.parse(mapOf("package.json" to "{"))
        assertTrue(result.warnings.any { it.sourcePath == "package.json" })
        assertTrue(result.complete.not())
    }

    @Test
    fun parsesMavenAndDotNetScopes() {
        val result = ManifestParserRegistry.parse(mapOf(
            "pom.xml" to "<dependencies><dependency><groupId>g</groupId><artifactId>a</artifactId><version>1</version><scope>test</scope></dependency></dependencies>",
            "src/app.csproj" to """<Project><ItemGroup><PackageReference Include="Newtonsoft.Json" Version="13.0.1" /></ItemGroup></Project>"""
        ))
        assertTrue(result.declarations.any { it.ecosystem == ArchitectureEcosystem.MAVEN && it.scope == ArchitectureScope.TEST })
        assertTrue(result.declarations.any { it.ecosystem == ArchitectureEcosystem.DOTNET && it.coordinate == "Newtonsoft.Json" })
    }

    @Test
    fun mergesNodeDeclaredAndLockVersions() {
        val result = ManifestParserRegistry.parse(mapOf(
            "package.json" to """{"dependencies":{"react":"^18.0.0"}}""",
            "package-lock.json" to """{"packages":{"node_modules/react":{"version":"18.2.0"}}}"""
        ))
        val react = result.declarations.single { it.coordinate == "react" }
        assertEquals("^18.0.0", react.version)
        assertEquals("18.2.0", react.resolvedVersion)
    }

    @Test
    fun parsesMultipleManifestAdaptersWithinOneEcosystem() {
        val result = ManifestParserRegistry.parse(mapOf(
            "requirements.txt" to "requests==2.31.0",
            "pyproject.toml" to """requests = ">=2.0""""
        ))
        assertTrue(result.parsedManifests.contains("requirements.txt"))
        assertTrue(result.parsedManifests.contains("pyproject.toml"))
    }
}
