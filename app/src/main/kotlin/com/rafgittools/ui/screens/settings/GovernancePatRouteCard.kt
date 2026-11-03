package com.rafgittools.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Documentation-only PAT capability route inside the existing governance screen.
 *
 * Never retrieves, handles or persists credential values. These are static
 * contract labels, NOT live provider permission or secret-existence checks.
 * Provider mutations remain owned by RepositoryGovernanceViewModel and the
 * separately protected, human-approved GitHub administration workflow.
 */
@Composable
internal fun GovernancePatRouteCard(
    repository: String?,
    canAudit: Boolean,
    canOpenProvider: Boolean,
    onAudit: () -> Unit,
    onOpenProvider: () -> Unit
) {
    val documentedRoutes = listOf(
        "PAT_ACTIONS" to "Leitura/teste limitado a SHA exato; sem administracao.",
        "PAT_AGENTS" to "Preflight de identidade; escrita e acesso administrativo nao comprovados.",
        "PAT_CODESPACES" to "Operacoes de Codespaces nao autorizadas sem readback de permissoes.",
        "PAT_DEPENDABOT" to "Consultas/atualizacoes Dependabot exigem escopo comprovado.",
        "PAT_ENV" to "Rota administrativa manual e protegida; permissao depende do GitHub.",
        "PAT_ENVIRONMENTS" to "Identificador distinto de PAT_ENV; escopo e permissoes TOKEN_VAZIO."
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text("PATs do GitHub · capacidades e protecoes", style = MaterialTheme.typography.titleMedium)
            Text(
                "Catalogo documentado, nao leitura atual de secrets ou de permissoes. " +
                    "Nenhum valor de PAT e coletado, mostrado, copiado ou gravado.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Repositorio: " + (repository ?: "TOKEN_VAZIO — selecione um repositorio"),
                style = MaterialTheme.typography.bodyMedium
            )
            documentedRoutes.forEach { (id, route) ->
                Text(id, style = MaterialTheme.typography.labelLarge)
                Text(
                    route,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "Fonte local: AGENTS.md → docs/governance/PAT_CAPABILITY_ROUTING_V1.md. " +
                    "PAT_ENV != PAT_ENVIRONMENTS. Nomes de secrets nao provam acesso.",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Mudancas de protecao: selecionar alvo exato, auditar o provedor, " +
                    "revisar dry-run e rollback, obter autorizacao humana, aplicar " +
                    "somente na rota administrativa existente e verificar readback. " +
                    "FAIL / UNKNOWN / TOKEN_VAZIO => HOLD. Sem fallback entre PATs.",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(
                onClick = onAudit,
                enabled = canAudit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Auditar protecoes no GitHub")
            }
            OutlinedButton(
                onClick = onOpenProvider,
                enabled = canOpenProvider,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Abrir configuracoes do provedor")
            }
        }
    }
}
