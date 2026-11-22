package com.rafgittools.setupwizard

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri

/**
 * SAF URI identity gate: Google Drive's installed DocumentsProvider belongs to
 * com.google.android.apps.docs. An authority is NOT itself a package name.
 *
 * Android package visibility can make resolveContentProvider return null even
 * for a genuine tree. Fall back to the authority actually DECLARED by the
 * installed Google Drive APK, never to an unverified URI substring.
 */
object GoogleDriveSafGate {
    const val DRIVE_ANDROID_PACKAGE = "com.google.android.apps.docs"

    fun isGoogleDrivePackage(packageName: String?): Boolean =
        packageName == DRIVE_ANDROID_PACKAGE

    internal fun isDeclaredByGoogleDrive(
        authority: String?,
        declaredAuthorities: Collection<String?>
    ): Boolean = authority != null && declaredAuthorities.any { raw ->
        raw?.split(';')?.any { it == authority } == true
    }

    fun isGoogleDrive(context: Context, uri: Uri): Boolean {
        if (uri.scheme != "content") return false
        val authority = uri.authority ?: return false
        return try {
            val manager = context.packageManager
            val resolved = manager.resolveContentProvider(authority, 0)
            if (resolved != null) {
                // A visibly resolved provider cannot be overridden by an
                // unrelated installed package with a matching name.
                isGoogleDrivePackage(resolved.packageName)
            } else {
                @Suppress("DEPRECATION")
                val drive = manager.getPackageInfo(
                    DRIVE_ANDROID_PACKAGE, PackageManager.GET_PROVIDERS
                )
                isDeclaredByGoogleDrive(authority,
                    drive.providers?.filter { isGoogleDrivePackage(it.packageName) }
                        ?.map { it.authority } ?: emptyList())
            }
        } catch (_: Exception) {
            false // Fail closed if Google Drive cannot be verified.
        }
    }

    fun diagnostic(context: Context, uri: Uri): String {
        if (uri.scheme != "content") return "BLOCKED_SAF_NOT_CONTENT"
        val authority = uri.authority ?: return "BLOCKED_SAF_NO_AUTHORITY"
        if (isGoogleDrive(context, uri)) return "DRIVE_PROVIDER_VERIFIED"
        return try {
            val p = context.packageManager.resolveContentProvider(authority, 0)
            if (p != null) "BLOCKED_SAF_OTHER_PROVIDER"
            else "BLOCKED_SAF_PROVIDER_IDENTITY_NOT_VISIBLE"
        } catch (_: Exception) {
            "BLOCKED_SAF_PROVIDER_LOOKUP_FAILED"
        }
    }
}
