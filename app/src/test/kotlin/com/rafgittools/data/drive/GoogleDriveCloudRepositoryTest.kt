package com.rafgittools.data.drive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GoogleDriveCloudRepositoryTest {
    @Test fun rootAndRealIdsAreAccepted() {
        assertEquals("root", GoogleDriveCloudRepository.checkedId("root"))
        assertEquals("1aBc_-9", GoogleDriveCloudRepository.checkedId("1aBc_-9"))
    }

    @Test fun invalidFolderIdFailsClosed() {
        for (id in listOf("", "' or trashed = false", "a/b", "a b", "https://drive.google.com")) {
            assertThrows(IllegalArgumentException::class.java) {
                GoogleDriveCloudRepository.checkedId(id)
            }
        }
    }

    @Test fun pathSegmentsCannotEscapeOrBreakJsonlStructure() {
        for (name in listOf("", "\u0000")) {
            assertThrows(IllegalArgumentException::class.java) {
                GoogleDriveCloudRepository.checkedName(name)
            }
        }
        assertEquals("NOVOexport.zip", GoogleDriveCloudRepository.checkedName("NOVOexport.zip"))
        assertEquals("a%2Fb", GoogleDriveCloudRepository.encodedSegment("a/b"))
        assertEquals("a%5Cb", GoogleDriveCloudRepository.encodedSegment("a\\b"))
        assertEquals("%2E%2E", GoogleDriveCloudRepository.encodedSegment(".."))
    }

    @Test fun restrictedDriveScopeIsAnExplicitAuthorizationBoundary() {
        assertEquals("https://www.googleapis.com/auth/drive", GoogleDriveCloudRepository.SCOPE)
        assertEquals(5000, GoogleDriveCloudRepository.MAX_DOCUMENTS)
    }
}
