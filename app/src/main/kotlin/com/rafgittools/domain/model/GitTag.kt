package com.rafgittools.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Domain model representing a Git tag
 */
@Parcelize
data class GitTag(
    val name: String,
    val sha: String,
    val message: String?,
    val tagger: GitAuthor?,
    val timestamp: Long?,
    val isAnnotated: Boolean
) : Parcelable
