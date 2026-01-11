package com.rafgittools.ui.theme

import androidx.compose.ui.graphics.Color

// Light theme colors
val Primary = Color(0xFF0969DA)        // GitHub blue
val PrimaryVariant = Color(0xFF0550AE)
val Secondary = Color(0xFF6E40C9)      // Purple accent
val SecondaryVariant = Color(0xFF5A32A3)
val Background = Color(0xFFFFFFFF)
val Surface = Color(0xFFF6F8FA)        // GitHub light gray
val Error = Color(0xFFCF222E)          // GitHub red
val OnPrimary = Color(0xFFFFFFFF)
val OnSecondary = Color(0xFFFFFFFF)
val OnBackground = Color(0xFF1F2328)
val OnSurface = Color(0xFF1F2328)
val OnError = Color(0xFFFFFFFF)

// Dark theme colors
val PrimaryDark = Color(0xFF58A6FF)       // GitHub blue (dark)
val PrimaryVariantDark = Color(0xFF1F6FEB)
val SecondaryDark = Color(0xFFBC8CFF)     // Purple accent (dark)
val SecondaryVariantDark = Color(0xFF9E7FCC)
val BackgroundDark = Color(0xFF0D1117)    // GitHub dark background
val SurfaceDark = Color(0xFF161B22)       // GitHub dark surface
val ErrorDark = Color(0xFFFF7B72)         // GitHub red (dark)
val OnPrimaryDark = Color(0xFF0D1117)
val OnSecondaryDark = Color(0xFF0D1117)
val OnBackgroundDark = Color(0xFFC9D1D9)  // GitHub dark text
val OnSurfaceDark = Color(0xFFC9D1D9)
val OnErrorDark = Color(0xFF0D1117)

// AMOLED Black theme colors (Feature #220)
// Pure black background for AMOLED screens to save battery and reduce eye strain
val PrimaryAmoled = Color(0xFF58A6FF)        // GitHub blue (same as dark)
val PrimaryVariantAmoled = Color(0xFF1F6FEB)
val SecondaryAmoled = Color(0xFFBC8CFF)      // Purple accent
val SecondaryVariantAmoled = Color(0xFF9E7FCC)
val BackgroundAmoled = Color(0xFF000000)     // Pure black
val SurfaceAmoled = Color(0xFF0A0A0A)        // Near black for surfaces
val SurfaceVariantAmoled = Color(0xFF121212) // Slightly lighter for elevated surfaces
val ErrorAmoled = Color(0xFFFF7B72)          // GitHub red
val OnPrimaryAmoled = Color(0xFF000000)
val OnSecondaryAmoled = Color(0xFF000000)
val OnBackgroundAmoled = Color(0xFFE6EDF3)   // Slightly brighter text for contrast
val OnSurfaceAmoled = Color(0xFFE6EDF3)
val OnSurfaceVariantAmoled = Color(0xFF8B949E)
val OnErrorAmoled = Color(0xFF000000)
val OutlineAmoled = Color(0xFF30363D)        // Border color

// Additional semantic colors
val Success = Color(0xFF2DA44E)           // GitHub green
val SuccessDark = Color(0xFF3FB950)
val SuccessAmoled = Color(0xFF3FB950)     // Same as dark for AMOLED
val Warning = Color(0xFFBF8700)           // GitHub yellow
val WarningDark = Color(0xFFD29922)
val WarningAmoled = Color(0xFFD29922)     // Same as dark for AMOLED
val Info = Color(0xFF218BFF)              // GitHub blue
val InfoDark = Color(0xFF58A6FF)
val InfoAmoled = Color(0xFF58A6FF)        // Same as dark for AMOLED
