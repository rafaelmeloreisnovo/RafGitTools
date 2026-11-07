package com.rafgittools.setupwizard

import org.junit.Assert.*
import org.junit.Test

class HyperAtlasV4IncrementalTest {
    private val v = HyperAtlasV4Incremental
    private val scope = v.digest("content://private-capability/root")
    private fun f(id: String, name: String, directory: Boolean = false, size: Long? = null) =
        HyperAtlasV4Incremental.Child(id, name, if (directory) "vnd.android.document/directory" else "text/plain", size, directory)
    private class Disk : HyperAtlasV4Incremental.Store {
        var checkpoint: HyperAtlasV4Incremental.Checkpoint? = null
        val batches = mutableMapOf<String, List<HyperAtlasV4Incremental.Row>>()
        var failCheckpointOnce = false
        var failBatchOnce = false
        override fun load() = checkpoint
        override fun commitDirectory(parentId: String, rows: List<HyperAtlasV4Incremental.Row>) {
            if (failBatchOnce) { failBatchOnce = false; error("SIMULATED_BATCH_FAILURE") }
            val prior = batches[parentId]
            check(prior == null || prior == rows) { "V4_DIVERGENT_REPLAY" }
            batches[parentId] = rows.toList()
        }
        override fun save(checkpoint: HyperAtlasV4Incremental.Checkpoint) {
            if (failCheckpointOnce) { failCheckpointOnce = false; error("SIMULATED_CHECKPOINT_FAILURE") }
            this.checkpoint = checkpoint
        }
    }
    private fun src() = object : HyperAtlasV4Incremental.Source {
        override fun children(parentId: String, maxChildren: Int) = when (parentId) {
            "root" -> listOf(f("dir", "Folder", true), f("a", "zero.txt", size = 0))
            "dir" -> listOf(f("b", "unknown.txt"), f("root", "loop", true))
            else -> emptyList()
        }
    }
    private fun srcOne() = object : HyperAtlasV4Incremental.Source {
        override fun children(parentId: String,maxChildren: Int) = listOf(f("x","a.txt"))
    }
    @Test fun processesIncrementallyAndPreservesUnknownSize() {
        val disk = Disk()
        val result = v.run(scope,"root",src(),disk)
        assertEquals("COMPLETE",result.state)
        assertEquals(2,result.completedDirectories)
        assertEquals(4L,result.records)
        assertNull(disk.batches.getValue("dir")[0].sizeBytes)
        assertEquals(0L,disk.batches.getValue("root")[1].sizeBytes)
        assertFalse(disk.batches.toString().contains("content://"))
        assertEquals(result,v.run(scope,"root",src(),disk))
    }
    @Test fun batchFailureDoesNotAdvanceCheckpoint() {
        val disk = Disk(); disk.failBatchOnce = true
        assertTrue(runCatching { v.run(scope,"root",src(),disk) }.isFailure)
        assertEquals(0,disk.checkpoint!!.completedDirectories)
        assertEquals("COMPLETE",v.run(scope,"root",src(),disk).state)
    }
    @Test fun checkpointFailureReplaysIdenticalBatchOnly() {
        val disk = Disk()
        val source = object : HyperAtlasV4Incremental.Source {
            override fun children(parentId: String, maxChildren: Int): List<HyperAtlasV4Incremental.Child> {
                disk.failCheckpointOnce = true
                return listOf(f("x", "a.txt"))
            }
        }
        assertTrue(runCatching { v.run(scope,"root",source,disk) }.isFailure)
        assertEquals(1,disk.batches.size)
        assertEquals("COMPLETE",v.run(scope,"root",srcOne(),disk).state)
        assertEquals(1,disk.batches.size)
    }
    @Test fun rejectsDivergentReplayWithoutAdvancing() {
        val disk=Disk()
        disk.batches["root"] = listOf(HyperAtlasV4Incremental.Row("wrong","wrong","FILE","x","text/plain",null))
        assertTrue(runCatching { v.run(scope,"root",srcOne(),disk) }.exceptionOrNull()?.message.orEmpty().contains("V4_DIVERGENT_REPLAY"))
        assertEquals(0,disk.checkpoint!!.completedDirectories)
    }
    @Test fun rejectsScopeSwapAndOverLimit() {
        val disk=Disk(); v.run(scope,"root",srcOne(),disk)
        assertTrue(runCatching { v.run(v.digest("other"),"root",srcOne(),disk) }.exceptionOrNull()?.message.orEmpty().contains("SCOPE_MISMATCH"))
        assertEquals("COMPLETE", v.run(scope,"root",srcOne(),Disk(),maxDirectories=1).state)
        assertTrue(runCatching { v.run(scope,"root",src(),Disk(),maxDirectories=1) }.exceptionOrNull()?.message.orEmpty().contains("SCOPE_LIMIT"))
    }
    @Test fun rejectsBadNameAndDuplicateDocumentId() {
        val bad=object: HyperAtlasV4Incremental.Source {
            override fun children(parentId: String,maxChildren: Int) = listOf(f("x","../escape"))
        }
        assertTrue(runCatching { v.run(scope,"root",bad,Disk()) }.isFailure)
        val duplicate=object: HyperAtlasV4Incremental.Source {
            override fun children(parentId: String,maxChildren: Int) = listOf(f("x","a"),f("x","b"))
        }
        assertTrue(runCatching { v.run(scope,"root",duplicate,Disk()) }.isFailure)
    }
}
