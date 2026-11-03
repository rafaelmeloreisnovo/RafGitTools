package com.rafgittools.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class GithubOwnerOption(val login: String, val type: String, val repositoryCount: Int)

/**
 * Selects only owner identities observed in authenticated repository inventory.
 * The picker does not infer GitHub organization membership or write permission.
 */
@Composable
fun GithubOwnerPicker(
    options: List<GithubOwnerOption>,
    selectedOwner: String?,
    inventoryComplete: Boolean,
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Conta / organização GitHub", style = MaterialTheme.typography.labelLarge)
        Row {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selectedOwner ?: "Todas as contas e organizações")
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Selecionar organização")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("Todas — somente navegação") },
                    onClick = { expanded = false; onSelect(null) }
                )
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text("${option.login} · ${if (option.type == "Organization") "Organização" else "Conta"} (${option.repositoryCount})")
                        },
                        onClick = { expanded = false; onSelect(option.login) }
                    )
                }
            }
        }
        if (!inventoryComplete) {
            Text(
                "Inventário GitHub incompleto: organizações ausentes não podem ser presumidas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        if (selectedOwner != null && options.none { it.login.equals(selectedOwner, ignoreCase = true) }) {
            Text(
                "Escopo não encontrado no inventário atual. Nenhum repositório será selecionado.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
