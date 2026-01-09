package com.rafgittools.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Domain model representing Git repository status
 */
@Parcelize
data class GitStatus(
    val branch: String,
    val added: List<String> = emptyList(),
    val changed: List<String> = emptyList(),
    val removed: List<String> = emptyList(),
    val modified: List<String> = emptyList(),
    val untracked: List<String> = emptyList(),
    val conflicting: List<String> = emptyList(),
    val hasUncommittedChanges: Boolean = false
) : Parcelable
