package com.rafgittools.domain.model

enum class SyncState {
    SYNCED,
    BEHIND,
    AHEAD,
    DIVERGED,
    CONFLICT
}
