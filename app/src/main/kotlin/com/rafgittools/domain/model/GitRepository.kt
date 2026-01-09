package com.rafgittools.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Domain model representing a Git repository
 */
@Parcelize
data class GitRepository(
    val id: String,
    val name: String,
    val path: String,
    val remoteUrl: String?,
    val currentBranch: String?,
    val lastUpdated: Long,
    val description: String? = null,
    val isPrivate: Boolean = false
) : Parcelable
