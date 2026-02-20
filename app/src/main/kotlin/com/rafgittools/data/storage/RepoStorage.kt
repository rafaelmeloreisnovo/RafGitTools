package com.rafgittools.data.storage

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RepoStorage @Inject constructor(
    @ApplicationContext context: Context
) {
    val baseDir: File = File(context.getExternalFilesDir(null), "repositories").apply {
        if (!exists()) {
            mkdirs()
        }
    }
}
