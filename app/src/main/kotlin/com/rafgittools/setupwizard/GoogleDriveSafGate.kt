package com.rafgittools.setupwizard

import android.content.Context
import android.net.Uri

/**
 * SAF returns a URI from whichever provider the user picked.
 * It does NOT authenticate Google Drive and it can return a local URI.
 * Reject non-Google providers before any inventory, ZIP read or write.
 */
object GoogleDriveSafGate {
    const val DRIVE_ANDROID_PACKAGE = "com.google.android.apps.docs"

    fun isGoogleDrivePackage(packageName: String?): Boolean =
        packageName == DRIVE_ANDROID_PACKAGE

    fun isGoogleDrive(context: Context, uri: Uri): Boolean {
        val authority = uri.authority ?: return false
        return try {
            val info = context.packageManager.resolveContentProvider(authority, 0)
            isGoogleDrivePackage(info?.packageName)
        } catch (_: RuntimeException) {
            // Unknown/hidden provider must not silently become an authorized Drive.
            false
        }
    }
}
