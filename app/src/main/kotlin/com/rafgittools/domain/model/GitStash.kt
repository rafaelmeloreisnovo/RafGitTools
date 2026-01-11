package com.rafgittools.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Domain model representing a Git stash entry
 */
@Parcelize
data class GitStash(
    val index: Int,
    val message: String,
    val sha: String,
    val branch: String,
    val timestamp: Long
) : Parcelable
