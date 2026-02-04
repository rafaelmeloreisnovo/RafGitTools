package com.rafgittools.core.localization

import com.rafgittools.data.preferences.PreferencesRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LocalizationEntryPoint {
    fun localizationManager(): LocalizationManager

    fun preferencesRepository(): PreferencesRepository
}
