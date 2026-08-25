package com.rafgittools.rafgitfs.data

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rafgittools.data.cache.CacheDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RafGitFsMigration5To6Test {
    private lateinit var context: Context
    private val databaseName = "rafgitfs-migration-5-6-test.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(databaseName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migrationCreatesAllTablesIndexesAndCriticalConstraints() {
        openAtVersion5().use { helper ->
            helper.writableDatabase.close()
        }

        openAtVersion6().use { helper ->
            val database = helper.writableDatabase
            database.execSQL("PRAGMA foreign_keys=ON")

            val tables = mutableSetOf<String>()
            database.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'"
            ).use { cursor ->
                while (cursor.moveToNext()) tables += cursor.getString(0)
            }
            assertTrue(tables.containsAll(RafGitFsRoomV6.expectedTables))

            val indexes = mutableSetOf<String>()
            database.query(
                "SELECT name FROM sqlite_master WHERE type='index' AND name LIKE 'idx_%'"
            ).use { cursor ->
                while (cursor.moveToNext()) indexes += cursor.getString(0)
            }
            assertTrue("receipt request must be unique", indexes.contains("idx_operation_receipts_request"))
            assertTrue("LRU index must exist", indexes.contains("idx_content_cache_lru"))

            insertProfile(database)
            database.execSQL(
                """INSERT INTO repository_refs
                   (profileId, repositoryFullName, refName, refType, gitSha, isDefault, lastIndexedAt)
                   VALUES ('test-profile', 'owner/repo', 'main', 'BRANCH', 'abc123', 1, 1)"""
            )
            database.execSQL("DELETE FROM storage_profiles WHERE profileId='test-profile'")
            assertEquals(0L, scalarLong(database, "SELECT COUNT(*) FROM repository_refs"))

            insertReceipt(database, receiptId = "receipt-1", requestId = "request-1")
            var uniqueRequestRejected = false
            try {
                insertReceipt(database, receiptId = "receipt-2", requestId = "request-1")
            } catch (_: SQLiteConstraintException) {
                uniqueRequestRejected = true
            }
            assertTrue("duplicate requestId must be rejected", uniqueRequestRejected)
        }
    }

    private fun openAtVersion5(): SupportSQLiteOpenHelper =
        FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(object : SupportSQLiteOpenHelper.Callback(5) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE baseline_v5 (id INTEGER NOT NULL PRIMARY KEY)")
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )

    private fun openAtVersion6(): SupportSQLiteOpenHelper =
        FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(object : SupportSQLiteOpenHelper.Callback(6) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        assertEquals(5, oldVersion)
                        assertEquals(6, newVersion)
                        CacheDatabase.MIGRATION_5_6.migrate(db)
                    }
                })
                .build()
        )

    private fun insertProfile(database: SupportSQLiteDatabase) {
        database.execSQL(
            """INSERT INTO storage_profiles
               (profileId, displayName, provider, scope, owner, selectedRepositoriesJson,
                defaultRef, accessMode, cachePolicy, writePolicy, maxCacheBytes,
                receiptRequired, protectedBranchWrite, deleteEnabled, claimAllowed,
                isEnabled, createdAt, updatedAt)
               VALUES
               ('test-profile', 'Test', 'GITHUB', 'AUTHENTICATED_USER', 'owner', '[]',
                'main', 'READ_ONLY', 'ON_DEMAND', 'BLOCKED', 1048576,
                1, 0, 0, 0, 1, 1, 1)"""
        )
    }

    private fun insertReceipt(database: SupportSQLiteDatabase, receiptId: String, requestId: String) {
        database.execSQL(
            """INSERT INTO operation_receipts
               (receiptId, requestId, profileId, operationType, finalPhase, allowed, result,
                evidenceState, target, observedSha, requestHash, receiptHash, hashAlgorithm,
                fOkJson, fGapJson, fNextJson, createdAt, claimAllowed)
               VALUES
               ('$receiptId', '$requestId', 'test-profile', 'LIST_TREE', 'RECEIPT', 1, 'SUCCESS',
                'OBSERVED', 'owner/repo@main:', NULL, 'reqhash', 'rechash', 'SHA-256',
                '[]', '[]', '[]', 1, 0)"""
        )
    }

    private fun scalarLong(database: SupportSQLiteDatabase, sql: String): Long =
        database.query(sql).use { cursor ->
            check(cursor.moveToFirst())
            cursor.getLong(0)
        }
}
