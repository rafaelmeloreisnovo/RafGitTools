package com.rafgittools.setupwizard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleDriveSafGateTest {
    @Test fun googleDrivePackagePasses() {
        assertTrue(GoogleDriveSafGate.isGoogleDrivePackage("com.google.android.apps.docs"))
    }

    @Test fun localAndImpersonatingPackagesFailClosed() {
        assertFalse(GoogleDriveSafGate.isGoogleDrivePackage(null))
        assertFalse(GoogleDriveSafGate.isGoogleDrivePackage(""))
        assertFalse(GoogleDriveSafGate.isGoogleDrivePackage("com.android.externalstorage"))
        assertFalse(GoogleDriveSafGate.isGoogleDrivePackage("com.google.android.apps.docs.fake"))
        assertFalse(GoogleDriveSafGate.isGoogleDrivePackage("com.google.android.apps.docs.storage"))
    }
}
