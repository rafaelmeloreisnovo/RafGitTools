package com.rafgittools.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Domain model representing a Git commit
 */
@Parcelize
data class GitCommit(
    val sha: String,
    val message: String,
    val author: GitAuthor,
    val committer: GitAuthor,
    val timestamp: Long,
    val parents: List<String> = emptyList()
) : Parcelable

@Parcelize
data class GitAuthor(
    val name: String,
    val email: String
) : Parcelable
