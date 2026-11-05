package com.example.githubprofileexplorer.domain

data class ArchitectureSnapshotComparison(
    val addedProjects: Set<String>,
    val removedProjects: Set<String>,
    val addedNodes: Set<String>,
    val removedNodes: Set<String>,
    val changedVersions: Set<String>,
    val parserSupportChanged: Boolean
)

object ArchitectureSnapshotComparator {
    fun compare(before: ArchitectureGraph, after: ArchitectureGraph): ArchitectureSnapshotComparison {
        val beforeProjects = before.nodes.map { "${it.ecosystem}:${it.projectRoot}" }.toSet()
        val afterProjects = after.nodes.map { "${it.ecosystem}:${it.projectRoot}" }.toSet()
        val beforeNodes = before.nodes.associateBy { it.id }
        val afterNodes = after.nodes.associateBy { it.id }
        val changed = beforeNodes.keys.intersect(afterNodes.keys).filterTo(mutableSetOf()) { id ->
            beforeNodes.getValue(id).version != afterNodes.getValue(id).version ||
                beforeNodes.getValue(id).resolvedVersion != afterNodes.getValue(id).resolvedVersion
        }
        return ArchitectureSnapshotComparison(
            afterProjects - beforeProjects,
            beforeProjects - afterProjects,
            afterNodes.keys - beforeNodes.keys,
            beforeNodes.keys - afterNodes.keys,
            changed,
            before.parsedManifests.toSet() != after.parsedManifests.toSet() ||
                before.unsupportedManifests.toSet() != after.unsupportedManifests.toSet()
        )
    }
}
