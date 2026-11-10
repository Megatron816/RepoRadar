package com.example.githubprofileexplorer.data.repository

import com.example.githubprofileexplorer.domain.ArchitectureGraphAnalyzer
import com.example.githubprofileexplorer.domain.ArchitectureScanResultLike

fun ArchitectureScanResult.toAiContext(): ArchitectureScanResultLike =
    object : ArchitectureScanResultLike {
        override fun toStructuredEvidence(): String {
            val analysis = ArchitectureGraphAnalyzer.analyze(graph)
            return buildString {
                appendLine("repository={owner=${graph.repository.owner}, name=${graph.repository.name}, branch=${graph.repository.branch}, commit=${graph.commitSha}, completeness=${graph.completeness}}")
                appendLine("signals={lastPush=${signals.lastPush}, openIssues=${signals.openIssues}, archived=${signals.archived}, fork=${signals.fork}, license=${signals.license}}")
                appendLine("counts={modules=${graph.moduleCount}, externalDependencies=${graph.dependencyCount}, edges=${graph.edges.size}, warnings=${graph.warnings.size}}")
                appendLine("analysis={cycles=${analysis.cycles}, sharedExternalDependencies=${analysis.sharedExternalDependencies}, isolatedModules=${analysis.isolatedModules}, mostConnected=${analysis.mostConnectedModules}, unresolvedCount=${analysis.unresolvedCount}}")
                appendLine("manifests={detected=${graph.detectedManifests}, parsed=${graph.parsedManifests}, unsupported=${graph.unsupportedManifests}}")
                appendLine("repositoryStats=${graph.scanStats}")
                appendLine("nodes=[")
                graph.nodes.take(MAX_NODES).forEach { node ->
                    appendLine("{id=${node.id}, label=${node.label}, type=${node.type}, version=${node.version}, resolvedVersion=${node.resolvedVersion}, source=${node.sourcePath}, ecosystem=${node.ecosystem}, projectRoot=${node.projectRoot}},")
                }
                appendLine("]")
                appendLine("edges=[")
                graph.edges.take(MAX_EDGES).forEach { edge ->
                    appendLine("{from=${edge.from}, to=${edge.to}, scope=${edge.scope}, source=${edge.sourcePath}, kind=${edge.sourceKind}},")
                }
                appendLine("]")
                appendLine("warnings=${graph.warnings.take(MAX_WARNINGS)}")
            }
        }
    }

private const val MAX_NODES = 250
private const val MAX_EDGES = 500
private const val MAX_WARNINGS = 50
