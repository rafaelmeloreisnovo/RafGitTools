package com.rafgittools.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Domain model representing a Git diff for a file
 */
@Parcelize
data class GitDiff(
    val oldPath: String?,
    val newPath: String?,
    val changeType: DiffChangeType,
    val oldContent: String?,
    val newContent: String?,
    val hunks: List<DiffHunk> = emptyList()
) : Parcelable

/**
 * Type of change in a diff
 */
enum class DiffChangeType {
    ADD,
    MODIFY,
    DELETE,
    RENAME,
    COPY
}

/**
 * A hunk represents a block of changes in a diff
 */
@Parcelize
data class DiffHunk(
    val oldStart: Int,
    val oldLines: Int,
    val newStart: Int,
    val newLines: Int,
    val lines: List<DiffLine>
) : Parcelable

/**
 * A single line in a diff
 */
@Parcelize
data class DiffLine(
    val type: DiffLineType,
    val content: String,
    val oldLineNumber: Int?,
    val newLineNumber: Int?
) : Parcelable

/**
 * Type of diff line
 */
enum class DiffLineType {
    CONTEXT,
    ADD,
    DELETE
}
