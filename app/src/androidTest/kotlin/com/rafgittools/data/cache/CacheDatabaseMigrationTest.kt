package com.rafgittools.data.cache

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CacheDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        CacheDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    private val testDb = "migration-test"

    @Test
    fun migrate1To2_populatesLastAccessedAtFromCreatedAt() {
        helper.createDatabase(testDb, 1).apply {
            execSQL(
                """
                INSERT INTO cache_entries (`key`, content, contentType, createdAt, expiresAt, etag)
                VALUES ('cache_key', 'payload', 'json', 111, 222, NULL)
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(
            testDb,
            2,
            true,
            CacheDatabase.MIGRATION_1_2
        )

        helper.openDatabase(testDb).use { db ->
            db.query("SELECT createdAt, lastAccessedAt FROM cache_entries WHERE `key` = 'cache_key'").use { cursor ->
                assert(cursor.moveToFirst())
                val createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("createdAt"))
                val lastAccessedAt = cursor.getLong(cursor.getColumnIndexOrThrow("lastAccessedAt"))
                assert(createdAt == 111L)
                assert(lastAccessedAt == createdAt)
            }
        }
    }
}
