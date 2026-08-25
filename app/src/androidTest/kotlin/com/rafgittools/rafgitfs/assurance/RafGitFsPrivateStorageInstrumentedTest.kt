package com.rafgittools.rafgitfs.assurance

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RafGitFsPrivateStorageInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun cacheAndWorkspaceRootsRemainInsideFilesDir() {
        val filesRoot = context.filesDir.canonicalFile
        val cache = File(filesRoot, "rafgitfs-cache-v1").canonicalFile
        val workspace = File(filesRoot, "rafgitfs-workspaces-v1").canonicalFile
        val prefix = filesRoot.canonicalPath + File.separator

        assertTrue(cache.canonicalPath.startsWith(prefix))
        assertTrue(workspace.canonicalPath.startsWith(prefix))
        context.externalFilesDir?.canonicalFile?.let { external ->
            val externalPrefix = external.canonicalPath + File.separator
            assertFalse(cache.canonicalPath.startsWith(externalPrefix))
            assertFalse(workspace.canonicalPath.startsWith(externalPrefix))
        }
    }
}
