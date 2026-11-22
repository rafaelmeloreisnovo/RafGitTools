package com.rafgittools.setupwizard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleDriveSafGateTest {
    @Test fun googleDrivePackagePasses() {
        assertTrue(GoogleDriveSafGate.isGoogleDrivePackage("com.google.android.apps.docs"))
    }

    @Test fun declaredAuthoritiesAreMatchedExactlyToInstalledDriveProvider() {
        assertTrue(GoogleDriveSafGate.isDeclaredByGoogleDrive(
            "com.google.android.apps.docs.storage",
            listOf("com.google.android.apps.docs.storage")
        ))
        assertTrue(GoogleDriveSafGate.isDeclaredByGoogleDrive(
            "com.google.android.apps.docs.storage",
            listOf("another.authority;com.google.android.apps.docs.storage")
        ))
        assertFalse(GoogleDriveSafGate.isDeclaredByGoogleDrive(
            "com.google.android.apps.docs.storage.fake",
            listOf("com.google.android.apps.docs.storage")
        ))
        assertFalse(GoogleDriveSafGate.isDeclaredByGoogleDrive(
            "com.google.android.apps.docs.storage",
            listOf("com.google.android.apps.docs.storage.fake")
        ))
        assertFalse(GoogleDriveSafGate.isDeclaredByGoogleDrive(null,
            listOf("com.google.android.apps.docs.storage")))
    }

    @Test fun localAndImpersonatingPackagesFailClosed() {
        assertFalse(GoogleDriveSafGate.isGoogleDrivePackage(null))
        assertFalse(GoogleDriveSafGate.isGoogleDrivePackage(""))
        assertFalse(GoogleDriveSafGate.isGoogleDrivePackage("com.android.externalstorage"))
        assertFalse(GoogleDriveSafGate.isGoogleDrivePackage("com.google.android.apps.docs.fake"))
        assertFalse(GoogleDriveSafGate.isGoogleDrivePackage("com.google.android.apps.docs.storage"))
    }
}
