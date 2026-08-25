package com.rafgittools.ui.screens.rafgitfs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.rafgitfs.data.StorageProfileDao
import com.rafgittools.rafgitfs.data.StorageProfileEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.URLDecoder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StorageSettingsUiState(
    val profile: StorageProfileEntity? = null,
    val maxCacheMiBText: String = "256",
    val status: RafGitFsUiStatus = RafGitFsUiStatus()
)

@HiltViewModel
class StorageSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val storageProfileDao: StorageProfileDao
) : ViewModel() {

    val profileId: String = savedStateHandle.get<String>("profileId")
        ?.let { URLDecoder.decode(it, "UTF-8") }
        .orEmpty()

    private val _uiState = MutableStateFlow(StorageSettingsUiState())
    val uiState: StateFlow<StorageSettingsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val profile = storageProfileDao.getById(profileId)
            _uiState.value = if (profile == null) {
                StorageSettingsUiState(
                    status = RafGitFsUiStatus(
                        RafGitFsUiEvidence.TOKEN_VAZIO,
                        "TOKEN_VAZIO",
                        "PROFILE_NOT_FOUND"
                    )
                )
            } else {
                StorageSettingsUiState(
                    profile = profile,
                    maxCacheMiBText = (profile.maxCacheBytes / MIB).toString()
                )
            }
        }
    }

    fun setCachePolicy(policy: String) {
        val current = _uiState.value.profile ?: return
        if (policy !in ALLOWED_CACHE_POLICIES) return
        _uiState.value = _uiState.value.copy(profile = current.copy(cachePolicy = policy))
    }

    fun setMaxCacheMiB(value: String) {
        if (value.length > 5 || value.any { !it.isDigit() }) return
        _uiState.value = _uiState.value.copy(maxCacheMiBText = value)
    }

    fun save() {
        val current = _uiState.value.profile ?: return
        val maxMiB = _uiState.value.maxCacheMiBText.toLongOrNull()?.coerceIn(16L, 4096L)
        if (maxMiB == null) {
            _uiState.value = _uiState.value.copy(
                status = RafGitFsUiStatus(
                    RafGitFsUiEvidence.TOKEN_VAZIO,
                    "Invalid cache budget",
                    "Use a value between 16 and 4096 MiB."
                )
            )
            return
        }
        viewModelScope.launch {
            val safeProfile = current.copy(
                accessMode = "READ_ONLY",
                writePolicy = "BLOCKED",
                maxCacheBytes = maxMiB * MIB,
                receiptRequired = true,
                protectedBranchWrite = false,
                deleteEnabled = false,
                claimAllowed = false,
                updatedAt = System.currentTimeMillis()
            )
            storageProfileDao.upsert(safeProfile)
            _uiState.value = _uiState.value.copy(
                profile = safeProfile,
                maxCacheMiBText = maxMiB.toString(),
                status = RafGitFsUiStatus(
                    RafGitFsUiEvidence.OBSERVED,
                    "Settings saved",
                    "Local cache policy updated; remote writes remain blocked."
                )
            )
        }
    }

    companion object {
        private const val MIB = 1024L * 1024L
        val ALLOWED_CACHE_POLICIES = setOf("METADATA_ONLY", "ON_DEMAND", "SELECTIVE_OFFLINE")
    }
}
