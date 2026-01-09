package com.rafgittools.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Domain model representing a Git remote
 */
@Parcelize
data class GitRemote(
    val name: String,
    val url: String,
    val fetchUrl: String,
    val pushUrl: String
) : Parcelable
