package com.rafgittools.rafgitfs.index

data class RafGitFsRepositoryRefresh(
    val repositoriesObserved: Int,
    val pagesFetched: Int,
    val complete: Boolean
)

data class RafGitFsRefRefresh(
    val repositoryFullName: String,
    val branchesObserved: Int,
    val tagsObserved: Int,
    val complete: Boolean,
    val indexedAt: Long
)

data class RafGitFsTreeRefresh(
    val repositoryFullName: String,
    val refName: String,
    val commitSha: String,
    val treeSha: String,
    val entriesIndexed: Int,
    val changed: Boolean,
    val complete: Boolean,
    val indexedAt: Long
)

data class RafGitFsContentSnapshot(
    val repositoryFullName: String,
    val refName: String,
    val path: String,
    val blobSha: String,
    val sizeBytes: Long,
    val bytes: ByteArray,
    val textUtf8: String?,
    val observedAt: Long
)
