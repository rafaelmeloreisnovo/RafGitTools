package com.rafgittools.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rafgittools.data.github.GithubOrganizationInventory

data class GithubOwnerOption(
    val login: String,
    val type: String,
    val repositoryCount: Int,
    val countObserved: Boolean = true
)

/**
 * Selects only owner identities observed in authenticated repository inventory.
 * The picker does not infer GitHub organization membership or write permission.
 */
@Composable
fun GithubOwnerPicker(
    options: List<GithubOwnerOption>,
    selectedOwner: String?,
    inventoryComplete: Boolean,
    organizationStatus: String,
    onSelect: (String?) -> Unit,
    onLookupOrganization: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var typedOrg by remember { mutableStateOf("") }
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
                            Text("${option.login} · ${if (option.type == "Organization") "Organização" else "Conta"} " +
                                if (option.countObserved) "(${option.repositoryCount})" else "(consultar)")
                        },
                        onClick = { expanded = false; onSelect(option.login) }
                    )
                }
                HorizontalDivider()
                OutlinedTextField(
                    value = typedOrg,
                    onValueChange = { typedOrg = it.take(39) },
                    label = { Text("Login da organização") },
                    singleLine = true,
                    modifier = Modifier.width(260.dp)
                )
                DropdownMenuItem(
                    text = { Text("Consultar repositórios da organização") },
                    enabled = GithubOrganizationInventory.isValidLogin(typedOrg.trim()),
                    onClick = {
                        expanded = false
                        onLookupOrganization(typedOrg.trim())
                    }
                )
            }
        }
        if (organizationStatus != "TOKEN_VAZIO_NOT_REQUESTED") {
            Text(
                when (organizationStatus) {
                    "ORG_LOADING" -> "Consultando repositórios da organização..."
                    "ORG_AUTHORIZED_RESULTS_COMPLETE" ->
                        "Listagem acessível ao PAT concluída (não comprova acesso a todos os privados)."
                    "ORG_PAGE_LIMIT_PARTIAL" -> "Organização: limite de páginas atingido; lista parcial."
                    "ORG_PARTIAL_OR_PERMISSION_BLOCKED" ->
                        "Organização: acesso/permissão incompleto. Verifique autorização do PAT e SSO."
                    "TOKEN_VAZIO_GITHUB_AUTH_REQUIRED" ->
                        "Faça login no GitHub antes de consultar a organização."
                    "INVALID_ORGANIZATION_LOGIN" -> "Login da organização inválido."
                    "USER_REPOSITORY_LIST_UNAVAILABLE" ->
                        "Listagem global indisponível; consulte sua organização diretamente."
                    else -> "Estado da organização: TOKEN_VAZIO"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
