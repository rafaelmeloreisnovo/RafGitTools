package com.rafgittools.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Domain model representing a file in a Git repository
 */
@Parcelize
data class GitFile(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val mode: String,
    val sha: String?
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
