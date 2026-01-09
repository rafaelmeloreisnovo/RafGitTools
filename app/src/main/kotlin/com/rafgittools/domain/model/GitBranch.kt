package com.rafgittools.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Domain model representing a Git branch
 */
@Parcelize
data class GitBranch(
    val name: String,
    val shortName: String,
    val isLocal: Boolean,
    val isRemote: Boolean,
    val isCurrent: Boolean,
    val commitSha: String?,
    val upstream: String? = null
) : Parcelable
