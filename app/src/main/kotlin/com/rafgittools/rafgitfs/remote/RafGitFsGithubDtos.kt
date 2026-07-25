package com.rafgittools.rafgitfs.remote

import com.google.gson.annotations.SerializedName

data class RafGitFsObjectRefDto(
    val sha: String,
    val url: String? = null
)

data class RafGitFsTagDto(
    val name: String,
    val commit: RafGitFsObjectRefDto
)

data class RafGitFsCommitDto(
    val sha: String,
    val commit: RafGitFsCommitPayloadDto
)

data class RafGitFsCommitPayloadDto(
    val tree: RafGitFsObjectRefDto
)

data class RafGitFsTreeDto(
    val sha: String,
    val url: String? = null,
    val tree: List<RafGitFsTreeEntryDto>,
    val truncated: Boolean = false
)

data class RafGitFsTreeEntryDto(
    val path: String,
    val mode: String,
    val type: String,
    val sha: String?,
    val size: Long? = null,
    val url: String? = null
)

data class RafGitFsBlobDto(
    val sha: String,
    val size: Long,
    val content: String?,
    val encoding: String?,
    val url: String? = null
)

data class RafGitFsApiErrorDto(
    val message: String? = null,
    @SerializedName("documentation_url") val documentationUrl: String? = null
)
