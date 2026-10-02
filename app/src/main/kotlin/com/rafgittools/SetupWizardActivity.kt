package com.rafgittools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rafgittools.setupwizard.SetupWizardCustodyLedger
import com.rafgittools.setupwizard.SetupWizardPreferences
import com.rafgittools.ui.screens.setupwizard.SetupWizardScreen
import com.rafgittools.ui.theme.RafGitToolsTheme

class SetupWizardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val preferences = SetupWizardPreferences(applicationContext)
        val ledger = SetupWizardCustodyLedger(applicationContext)

        setContent {
            RafGitToolsTheme {
                SetupWizardScreen(
                    preferences = preferences,
                    ledger = ledger,
                    onClose = { finish() }
                )
            }
        }
    }
}
