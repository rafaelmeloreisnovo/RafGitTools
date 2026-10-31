package com.rafgittools.benchmark

/**
 * Contextual credits for actual UI paths. This is not a full dependency SBOM.
 * Only mark PROJECT_AUTHORIAL for source authored in the RafGitTools tree;
 * external UI frameworks always retain visible credit.
 */
object RafScreenCredits {
    val privacy = listOf(
        ScreenCredit("Raf Privacy screen", CreditOrigin.PROJECT_AUTHORIAL,
            "Seleção e confirmação autoral para alterações de privacidade de repositórios.",
            "RAFAELIA / RafGitTools contributors", "RafGitTools project license",
            "https://github.com/rafaelmeloreisnovo/RafGitTools",
            "app/src/main/kotlin/com/rafgittools/ui/screens/privacy/RepositoryPrivacyScreen.kt"),
        ScreenCredit("AndroidX Compose / Material3", CreditOrigin.EXTERNAL_API,
            "Renderização, navegação e controles visuais desta tela.",
            "Android Open Source Project / AndroidX contributors", "Apache-2.0",
            "https://github.com/androidx/androidx",
            "app/src/main/kotlin/com/rafgittools/ui/screens/privacy/RepositoryPrivacyScreen.kt"),
        ScreenCredit("AndroidX Hilt Navigation", CreditOrigin.EXTERNAL_API,
            "Injeção/escopo do ViewModel usado pela tela.",
            "AndroidX / Dagger contributors", "Apache-2.0",
            "https://github.com/androidx/androidx",
            "app/src/main/kotlin/com/rafgittools/ui/screens/privacy/RepositoryPrivacyScreen.kt")
    )
    val setup = listOf(
        ScreenCredit("Raf Setup wizard", CreditOrigin.PROJECT_AUTHORIAL,
            "Fluxo autoral de configuração e registro de escolhas.",
            "RAFAELIA / RafGitTools contributors", "RafGitTools project license",
            "https://github.com/rafaelmeloreisnovo/RafGitTools",
            "app/src/main/kotlin/com/rafgittools/ui/screens/setupwizard/SetupWizardScreen.kt"),
        ScreenCredit("AndroidX Compose / Material3", CreditOrigin.EXTERNAL_API,
            "Renderização dos formulários e progressão visual.",
            "Android Open Source Project / AndroidX contributors", "Apache-2.0",
            "https://github.com/androidx/androidx",
            "app/src/main/kotlin/com/rafgittools/ui/screens/setupwizard/SetupWizardScreen.kt")
    )
    val settings = listOf(
        ScreenCredit("RafGitTools Settings", CreditOrigin.PROJECT_AUTHORIAL,
            "Tela autoral de configurações locais e preferências.",
            "RAFAELIA / RafGitTools contributors", "RafGitTools project license",
            "https://github.com/rafaelmeloreisnovo/RafGitTools",
            "app/src/main/kotlin/com/rafgittools/ui/screens/settings/SettingsScreen.kt"),
        ScreenCredit("AndroidX Compose / Material3", CreditOrigin.EXTERNAL_API,
            "Apresentação de opções, switches e navegação.",
            "Android Open Source Project / AndroidX contributors", "Apache-2.0",
            "https://github.com/androidx/androidx",
            "app/src/main/kotlin/com/rafgittools/ui/screens/settings/SettingsScreen.kt")
    )
}
