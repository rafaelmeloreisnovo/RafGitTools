package com.rafgittools.library

import android.content.Context
import android.net.Uri
import com.rafgittools.bridge.CatalogTreeExportResult
import com.rafgittools.bridge.CorpusCatalogTreeGate

/**
 * Reuses the verified SAF catalog export gate.
 * Raw corpus files are not exported by this coordinator.
 */
object LibraryCatalogTreeExporter {
    fun export(
        context: Context,
        destinationTree: Uri,
        materialization: LibraryCatalogMaterialization,
        createdAtEpochMs: Long = System.currentTimeMillis()
    ): CatalogTreeExportResult =
        CorpusCatalogTreeGate.exportCatalog(
            context = context,
            treeUri = destinationTree,
            artifacts = listOf(
                materialization.catalogFile,
                materialization.receiptFile
            ),
            createdAtEpochMs = createdAtEpochMs
        )
}
