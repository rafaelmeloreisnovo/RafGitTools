package com.rafgittools.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * GitHub-inspired status colors for consistency with GitHub UI
 * These colors are used for issues, PRs, and other GitHub-related status indicators
 */
object GitHubColors {
    // Issue/PR state colors
    val OpenGreen = Color(0xFF238636)    // Open issues/PRs
    val ClosedRed = Color(0xFFCF222E)    // Closed issues/PRs (not merged)
    val MergedPurple = Color(0xFF8957E5) // Merged PRs, closed issues
    val DraftGray = Color(0xFF6E7781)    // Draft PRs
    
    // Label colors with transparency for chip backgrounds
    val LabelBackground = Color.Gray.copy(alpha = 0.3f)
    
    // Branch colors
    val CurrentBranch = Color(0xFF238636)
    val RemoteBranch = Color(0xFF0969DA)
}
