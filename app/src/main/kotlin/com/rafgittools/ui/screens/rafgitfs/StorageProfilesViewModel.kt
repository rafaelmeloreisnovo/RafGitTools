package com.rafgittools.ui.screens.rafgitfs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.rafgitfs.data.StorageProfileDao
import com.rafgittools.rafgitfs.data.StorageProfileEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StorageProfilesViewModel @Inject constructor(
    private val storageProfileDao: StorageProfileDao
) : ViewModel() {

    val profiles: StateFlow<List<StorageProfileEntity>> = storageProfileDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _status = MutableStateFlow(RafGitFsUiStatus())
    val status: StateFlow<RafGitFsUiStatus> = _status.asStateFlow()

    init {
        ensureDefaultProfile()
    }

    fun ensureDefaultProfile() {
        viewModelScope.launch {
            val existing = storageProfileDao.getById(DEFAULT_PROFILE_ID)
            if (existing != null) return@launch
            val now = System.currentTimeMillis()
            storageProfileDao.upsert(
                StorageProfileEntity(
                    profileId = DEFAULT_PROFILE_ID,
                    displayName = "Primary GitHub Storage",
                    provider = "GITHUB",
                    scope = "AUTHENTICATED_USER",
                    owner = "authenticated-user",
                    selectedRepositoriesJson = "[]",
                    defaultRef = "main",
                    accessMode = "READ_ONLY",
                    cachePolicy = "ON_DEMAND",
                    writePolicy = "BLOCKED",
                    maxCacheBytes = 256L * 1024L * 1024L,
                    receiptRequired = true,
                    protectedBranchWrite = false,
                    deleteEnabled = false,
                    claimAllowed = false,
                    isEnabled = true,
                    createdAt = now,
                    updatedAt = now
                )
            )
            _status.value = RafGitFsUiStatus(
                RafGitFsUiEvidence.OBSERVED,
                "Profile created",
                "A local read-only profile was created. No GitHub content was changed."
            )
        }
    }

    fun setEnabled(profileId: String, enabled: Boolean) {
        viewModelScope.launch {
            storageProfileDao.setEnabled(profileId, enabled)
            _status.value = RafGitFsUiStatus(
                RafGitFsUiEvidence.OBSERVED,
                if (enabled) "Profile enabled" else "Profile paused",
                "This changes only local RafGitFS configuration."
            )
        }
    }

    companion object {
        const val DEFAULT_PROFILE_ID = "rafgitfs-github-primary"
    }
}
