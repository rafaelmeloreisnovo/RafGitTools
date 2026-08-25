package com.rafgittools.rafgitfs.data

/**
 * Canonical SQL for CacheDatabase 5 -> 6.
 *
 * Keeping the statements in one object makes the migration auditable without
 * duplicating SQL across application code and tests.
 */
object RafGitFsRoomV6 {
    const val FROM_VERSION = 5
    const val TO_VERSION = 6

    val expectedTables = setOf(
        "storage_profiles",
        "repository_refs",
        "virtual_tree_entries",
        "content_cache",
        "workspaces",
        "transfer_jobs",
        "staged_operations",
        "sync_conflicts",
        "operation_receipts"
    )

    val createStatements = listOf(
        """CREATE TABLE IF NOT EXISTS `storage_profiles` (
            `profileId` TEXT NOT NULL,
            `displayName` TEXT NOT NULL,
            `provider` TEXT NOT NULL,
            `scope` TEXT NOT NULL,
            `owner` TEXT NOT NULL,
            `selectedRepositoriesJson` TEXT NOT NULL,
            `defaultRef` TEXT NOT NULL,
            `accessMode` TEXT NOT NULL,
            `cachePolicy` TEXT NOT NULL,
            `writePolicy` TEXT NOT NULL,
            `maxCacheBytes` INTEGER NOT NULL,
            `receiptRequired` INTEGER NOT NULL,
            `protectedBranchWrite` INTEGER NOT NULL,
            `deleteEnabled` INTEGER NOT NULL,
            `claimAllowed` INTEGER NOT NULL,
            `isEnabled` INTEGER NOT NULL,
            `createdAt` INTEGER NOT NULL,
            `updatedAt` INTEGER NOT NULL,
            PRIMARY KEY(`profileId`)
        )""".trimIndent(),
        "CREATE INDEX IF NOT EXISTS `idx_storage_profiles_owner` ON `storage_profiles` (`owner`)",
        "CREATE INDEX IF NOT EXISTS `idx_storage_profiles_enabled` ON `storage_profiles` (`isEnabled`)",

        """CREATE TABLE IF NOT EXISTS `repository_refs` (
            `profileId` TEXT NOT NULL,
            `repositoryFullName` TEXT NOT NULL,
            `refName` TEXT NOT NULL,
            `refType` TEXT NOT NULL,
            `gitSha` TEXT,
            `isDefault` INTEGER NOT NULL,
            `lastIndexedAt` INTEGER NOT NULL,
            PRIMARY KEY(`profileId`, `repositoryFullName`, `refName`),
            FOREIGN KEY(`profileId`) REFERENCES `storage_profiles`(`profileId`) ON UPDATE NO ACTION ON DELETE CASCADE
        )""".trimIndent(),
        "CREATE INDEX IF NOT EXISTS `idx_repository_refs_profile` ON `repository_refs` (`profileId`)",
        "CREATE INDEX IF NOT EXISTS `idx_repository_refs_repo` ON `repository_refs` (`repositoryFullName`)",
        "CREATE INDEX IF NOT EXISTS `idx_repository_refs_sha` ON `repository_refs` (`gitSha`)",

        """CREATE TABLE IF NOT EXISTS `virtual_tree_entries` (
            `profileId` TEXT NOT NULL,
            `repositoryFullName` TEXT NOT NULL,
            `refName` TEXT NOT NULL,
            `path` TEXT NOT NULL,
            `parentPath` TEXT NOT NULL,
            `name` TEXT NOT NULL,
            `entryType` TEXT NOT NULL,
            `gitSha` TEXT,
            `sizeBytes` INTEGER,
            `mimeType` TEXT,
            `cacheState` TEXT NOT NULL,
            `localPath` TEXT,
            `isFavorite` INTEGER NOT NULL,
            `lastIndexedAt` INTEGER NOT NULL,
            `lastAccessedAt` INTEGER NOT NULL,
            PRIMARY KEY(`profileId`, `repositoryFullName`, `refName`, `path`),
            FOREIGN KEY(`profileId`) REFERENCES `storage_profiles`(`profileId`) ON UPDATE NO ACTION ON DELETE CASCADE
        )""".trimIndent(),
        "CREATE INDEX IF NOT EXISTS `idx_virtual_tree_profile` ON `virtual_tree_entries` (`profileId`)",
        "CREATE INDEX IF NOT EXISTS `idx_virtual_tree_children` ON `virtual_tree_entries` (`profileId`, `repositoryFullName`, `refName`, `parentPath`)",
        "CREATE INDEX IF NOT EXISTS `idx_virtual_tree_sha` ON `virtual_tree_entries` (`gitSha`)",
        "CREATE INDEX IF NOT EXISTS `idx_virtual_tree_favorite` ON `virtual_tree_entries` (`isFavorite`)",

        """CREATE TABLE IF NOT EXISTS `content_cache` (
            `cacheKey` TEXT NOT NULL,
            `profileId` TEXT NOT NULL,
            `repositoryFullName` TEXT NOT NULL,
            `refName` TEXT NOT NULL,
            `path` TEXT NOT NULL,
            `gitSha` TEXT NOT NULL,
            `localPath` TEXT NOT NULL,
            `sizeBytes` INTEGER NOT NULL,
            `cacheState` TEXT NOT NULL,
            `pinned` INTEGER NOT NULL,
            `checksumAlgorithm` TEXT NOT NULL,
            `checksumHex` TEXT NOT NULL,
            `createdAt` INTEGER NOT NULL,
            `lastAccessedAt` INTEGER NOT NULL,
            `expiresAt` INTEGER,
            PRIMARY KEY(`cacheKey`),
            FOREIGN KEY(`profileId`) REFERENCES `storage_profiles`(`profileId`) ON UPDATE NO ACTION ON DELETE CASCADE
        )""".trimIndent(),
        "CREATE INDEX IF NOT EXISTS `idx_content_cache_profile` ON `content_cache` (`profileId`)",
        "CREATE UNIQUE INDEX IF NOT EXISTS `idx_content_cache_identity` ON `content_cache` (`profileId`, `repositoryFullName`, `refName`, `path`, `gitSha`)",
        "CREATE INDEX IF NOT EXISTS `idx_content_cache_lru` ON `content_cache` (`pinned`, `lastAccessedAt`)",
        "CREATE INDEX IF NOT EXISTS `idx_content_cache_expiry` ON `content_cache` (`expiresAt`)",

        """CREATE TABLE IF NOT EXISTS `workspaces` (
            `workspaceId` TEXT NOT NULL,
            `profileId` TEXT NOT NULL,
            `repositoryFullName` TEXT NOT NULL,
            `baseRef` TEXT NOT NULL,
            `branchName` TEXT,
            `localRoot` TEXT NOT NULL,
            `state` TEXT NOT NULL,
            `createdAt` INTEGER NOT NULL,
            `updatedAt` INTEGER NOT NULL,
            `claimAllowed` INTEGER NOT NULL,
            PRIMARY KEY(`workspaceId`),
            FOREIGN KEY(`profileId`) REFERENCES `storage_profiles`(`profileId`) ON UPDATE NO ACTION ON DELETE CASCADE
        )""".trimIndent(),
        "CREATE INDEX IF NOT EXISTS `idx_workspaces_profile` ON `workspaces` (`profileId`)",
        "CREATE INDEX IF NOT EXISTS `idx_workspaces_repo` ON `workspaces` (`repositoryFullName`)",
        "CREATE INDEX IF NOT EXISTS `idx_workspaces_state` ON `workspaces` (`state`)",

        """CREATE TABLE IF NOT EXISTS `transfer_jobs` (
            `jobId` TEXT NOT NULL,
            `profileId` TEXT NOT NULL,
            `requestId` TEXT NOT NULL,
            `operationType` TEXT NOT NULL,
            `phase` TEXT NOT NULL,
            `syncState` TEXT NOT NULL,
            `repositoryFullName` TEXT,
            `refName` TEXT,
            `path` TEXT,
            `bytesTotal` INTEGER,
            `bytesCompleted` INTEGER NOT NULL,
            `retryCount` INTEGER NOT NULL,
            `maxRetries` INTEGER NOT NULL,
            `lastErrorCode` TEXT,
            `createdAt` INTEGER NOT NULL,
            `updatedAt` INTEGER NOT NULL,
            `claimAllowed` INTEGER NOT NULL,
            PRIMARY KEY(`jobId`),
            FOREIGN KEY(`profileId`) REFERENCES `storage_profiles`(`profileId`) ON UPDATE NO ACTION ON DELETE CASCADE
        )""".trimIndent(),
        "CREATE INDEX IF NOT EXISTS `idx_transfer_jobs_profile` ON `transfer_jobs` (`profileId`)",
        "CREATE INDEX IF NOT EXISTS `idx_transfer_jobs_state` ON `transfer_jobs` (`syncState`)",
        "CREATE INDEX IF NOT EXISTS `idx_transfer_jobs_updated` ON `transfer_jobs` (`updatedAt`)",

        """CREATE TABLE IF NOT EXISTS `staged_operations` (
            `operationId` TEXT NOT NULL,
            `jobId` TEXT,
            `workspaceId` TEXT,
            `operationType` TEXT NOT NULL,
            `repositoryFullName` TEXT NOT NULL,
            `refName` TEXT,
            `path` TEXT,
            `baseSha` TEXT,
            `localSha` TEXT,
            `payloadHash` TEXT,
            `state` TEXT NOT NULL,
            `createdAt` INTEGER NOT NULL,
            PRIMARY KEY(`operationId`)
        )""".trimIndent(),
        "CREATE INDEX IF NOT EXISTS `idx_staged_operations_job` ON `staged_operations` (`jobId`)",
        "CREATE INDEX IF NOT EXISTS `idx_staged_operations_workspace` ON `staged_operations` (`workspaceId`)",
        "CREATE INDEX IF NOT EXISTS `idx_staged_operations_state` ON `staged_operations` (`state`)",

        """CREATE TABLE IF NOT EXISTS `sync_conflicts` (
            `conflictId` TEXT NOT NULL,
            `jobId` TEXT,
            `workspaceId` TEXT,
            `repositoryFullName` TEXT NOT NULL,
            `refName` TEXT NOT NULL,
            `path` TEXT NOT NULL,
            `conflictState` TEXT NOT NULL,
            `localSha` TEXT,
            `remoteSha` TEXT,
            `resolution` TEXT,
            `detectedAt` INTEGER NOT NULL,
            `resolvedAt` INTEGER,
            PRIMARY KEY(`conflictId`)
        )""".trimIndent(),
        "CREATE INDEX IF NOT EXISTS `idx_sync_conflicts_job` ON `sync_conflicts` (`jobId`)",
        "CREATE INDEX IF NOT EXISTS `idx_sync_conflicts_workspace` ON `sync_conflicts` (`workspaceId`)",
        "CREATE INDEX IF NOT EXISTS `idx_sync_conflicts_resolved` ON `sync_conflicts` (`resolvedAt`)",

        """CREATE TABLE IF NOT EXISTS `operation_receipts` (
            `receiptId` TEXT NOT NULL,
            `requestId` TEXT NOT NULL,
            `profileId` TEXT NOT NULL,
            `operationType` TEXT NOT NULL,
            `finalPhase` TEXT NOT NULL,
            `allowed` INTEGER NOT NULL,
            `result` TEXT NOT NULL,
            `evidenceState` TEXT NOT NULL,
            `target` TEXT NOT NULL,
            `observedSha` TEXT,
            `requestHash` TEXT NOT NULL,
            `receiptHash` TEXT NOT NULL,
            `hashAlgorithm` TEXT NOT NULL,
            `fOkJson` TEXT NOT NULL,
            `fGapJson` TEXT NOT NULL,
            `fNextJson` TEXT NOT NULL,
            `createdAt` INTEGER NOT NULL,
            `claimAllowed` INTEGER NOT NULL,
            PRIMARY KEY(`receiptId`)
        )""".trimIndent(),
        "CREATE UNIQUE INDEX IF NOT EXISTS `idx_operation_receipts_request` ON `operation_receipts` (`requestId`)",
        "CREATE INDEX IF NOT EXISTS `idx_operation_receipts_profile` ON `operation_receipts` (`profileId`)",
        "CREATE INDEX IF NOT EXISTS `idx_operation_receipts_created` ON `operation_receipts` (`createdAt`)"
    )
}
