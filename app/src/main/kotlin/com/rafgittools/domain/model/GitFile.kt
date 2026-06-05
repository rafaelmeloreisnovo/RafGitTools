package com.rafgittools.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Domain model representing a file in a Git repository
 *
 * [lastModified] is the Unix epoch (ms) of the most recent commit that
 * touched this path. Populated on demand by JGitService.getFileLastModified()
 * or by FileBrowserViewModel when listing files — null if not yet resolved.
 */
@Parcelize
data class GitFile(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val mode: String,
    val sha: String?,
    val lastModified: Long? = null   // P33-18
) : Parcelable

/**
 * Domain model representing file content with metadata
 */
@Parcelize
data class FileContent(
    val path: String,
    val name: String,
    val content: String,
    val encoding: String,
    val size: Long,
    val language: String?,
    val isBinary: Boolean
) : Parcelable
