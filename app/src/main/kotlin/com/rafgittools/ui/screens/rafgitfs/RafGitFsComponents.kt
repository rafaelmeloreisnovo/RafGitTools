package com.rafgittools.ui.screens.rafgitfs

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun RafGitFsReadOnlyBadge(modifier: Modifier = Modifier) {
    AssistChip(
        modifier = modifier.semantics {
            contentDescription = "RafGitFS read-only mode. Remote mutation is unavailable here."
        },
        onClick = {},
        enabled = false,
        label = { Text("Read only") },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
    )
}

@Composable
fun RafGitFsStatusBanner(
    status: RafGitFsUiStatus,
    modifier: Modifier = Modifier
) {
    if (status.evidence == RafGitFsUiEvidence.IDLE) return
    val icon = when (status.evidence) {
        RafGitFsUiEvidence.OBSERVED,
        RafGitFsUiEvidence.NOT_MODIFIED -> Icons.Default.CheckCircle
        RafGitFsUiEvidence.LOADING -> Icons.Default.HourglassEmpty
        RafGitFsUiEvidence.TOKEN_VAZIO,
        RafGitFsUiEvidence.RATE_LIMITED -> Icons.Default.CloudOff
        RafGitFsUiEvidence.ERROR -> Icons.Default.Error
        RafGitFsUiEvidence.IDLE -> Icons.Default.Info
    }
    val announcement = buildString {
        append(status.evidence.name.replace('_', ' '))
        append(". ")
        append(status.title)
        status.detail?.takeIf { it.isNotBlank() }?.let {
            append(". ")
            append(it)
        }
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = announcement
            }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(status.title, style = MaterialTheme.typography.titleSmall)
                status.detail?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun RafGitFsBreadcrumbBar(
    path: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .semantics {
                contentDescription = "Repository path: ${path.ifBlank { "root" }}"
            },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RafGitFsUiPaths.breadcrumbs(path).forEachIndexed { index, item ->
            if (index > 0) Text("/", color = MaterialTheme.colorScheme.onSurfaceVariant)
            AssistChip(
                modifier = Modifier.semantics {
                    contentDescription = "Navigate to ${item.path.ifBlank { "repository root" }}"
                },
                onClick = { onNavigate(item.path) },
                label = {
                    Text(
                        item.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}

@Composable
fun RafGitFsEmptyState(
    title: String,
    detail: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(32.dp)
            .semantics { contentDescription = "$title. $detail" },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Info, contentDescription = null)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
