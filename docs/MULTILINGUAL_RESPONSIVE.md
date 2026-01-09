# Multilingual and Responsive Design Implementation

## Overview

This document describes the multilingual and responsive design features implemented in RafGitTools.

## Multilingual Support

### Supported Languages

The application now supports three languages:
1. **English** (default) - `en`
2. **Portuguese (Brazil)** - `pt-BR`
3. **Spanish** - `es`

### Implementation Details

#### Language Configuration
- **LanguageConfig.kt**: Defines the `Language` enum with supported languages
- Each language has:
  - Language code (e.g., "en", "pt", "es")
  - Locale object for Android localization
  - Display name for UI presentation

#### LocalizationManager
- Manages application localization and language changes
- Handles locale updates for different Android API levels
- Provides methods to:
  - Set application locale
  - Get current locale  
  - Get current language

#### PreferencesRepository
- Stores user language preferences using DataStore
- Provides:
  - Flow for observing language changes
  - Methods to get/set language preferences
  - Persistent storage of language selection

#### Language Selection UI
- **LanguageSelector**: Dialog component for language selection
- **LanguageFAB**: Floating action button to trigger language selector
- Features:
  - Visual indication of currently selected language
  - Smooth animations and transitions
  - Material Design 3 styling

### String Resources

All user-facing strings are externalized to XML resources:
- `values/strings.xml` - English (default)
- `values-pt-rBR/strings.xml` - Portuguese (Brazil)
- `values-es/strings.xml` - Spanish

Categories of strings:
- App information and descriptions
- Common actions (Clone, Commit, Push, Pull, etc.)
- Repository management
- Git operations
- GitHub integration
- Authentication
- Error messages
- Loading states
- Language settings

### Usage

The application automatically applies the saved language preference on startup. Users can change the language at any time using the language selector button (floating action button with globe icon).

## Responsive Design

### Window Size Classes

The application uses window size classes to adapt to different screen sizes:
- **COMPACT**: Phones in portrait (<  600dp)
- **MEDIUM**: Tablets in portrait, phones in landscape (600dp - 840dp)
- **EXPANDED**: Tablets in landscape, desktops (> 840dp)

### Responsive Components

#### ResponsiveUtils.kt

Provides utilities for building responsive layouts:

1. **getWindowSize()**: Returns the current window size class
2. **getResponsivePadding()**: Provides appropriate padding based on screen size
   - COMPACT: 16dp
   - MEDIUM: 24dp
   - EXPANDED: 32dp

3. **getResponsiveContentWidth()**: Provides maximum content width for larger screens
   - COMPACT: Full width (no max)
   - MEDIUM: 720dp
   - EXPANDED: 1200dp

### Responsive Layout Strategy

The MainActivity demonstrates the responsive layout approach:
- Content is centered on large screens
- Appropriate padding is applied based on screen size
- Maximum content width prevents text from stretching too wide on large screens
- UI components scale appropriately for different form factors

### Adaptive UI Components

All UI components are designed to be responsive:
- Language selector adapts to available space
- Buttons and text scale appropriately
- Layouts reflow based on available width
- Material Design 3 components automatically adapt

## Architecture

### Clean Architecture Layers

The implementation follows Clean Architecture principles:

1. **Presentation Layer**
   - MainActivity: Main entry point with language and responsive support
   - UI Components: LanguageSelector, LanguageFAB, ResponsiveUtils
   - Theme: Material Design 3 theming

2. **Domain Layer**
   - Language enum: Domain model for supported languages
   - LocalizationManager: Business logic for localization

3. **Data Layer**
   - PreferencesRepository: Persistent storage for user preferences
   - DataStore: Modern Android preferences storage

### Dependency Injection

Hilt is used for dependency injection:
- LocalizationManager is injected as a Singleton
- PreferencesRepository is injected as a Singleton
- All dependencies are automatically provided to MainActivity

## Future Enhancements

Potential improvements for future versions:
1. Add more languages
2. Automatic language detection based on system locale
3. Language-specific date/time formatting
4. RTL (Right-to-Left) language support
5. Per-screen responsive layouts for specific features
6. Landscape-specific layouts
7. Foldable device support
8. Desktop-specific optimizations

## Testing

To test the implementation:
1. Build and run the application
2. Click the language floating action button (globe icon)
3. Select a different language
4. The app will recreate to apply the new language
5. All text should update to the selected language
6. Test on different screen sizes (phone, tablet, landscape)
7. Verify responsive padding and content width adjustments

## Technical Notes

- Language preferences persist across app restarts
- The app recreates the activity when language changes to properly apply new resources
- Responsive utilities are composable functions that adapt based on current configuration
- All string resources support localization through Android's resource system
- DataStore provides type-safe, async access to preferences

