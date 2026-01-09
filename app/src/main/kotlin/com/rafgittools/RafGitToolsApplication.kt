package com.rafgittools

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * RafGitTools Application class
 * 
 * This is the main application class that initializes Hilt dependency injection
 * and other application-level components.
 */
@HiltAndroidApp
class RafGitToolsApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize application-level components
        // This is where we would set up crash reporting, analytics, etc.
    }
}
