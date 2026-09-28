package com.rafgittools.ui.screens.setupwizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rafgittools.setupwizard.SetupWizardCatalog
import com.rafgittools.setupwizard.SetupWizardCustodyLedger
import com.rafgittools.setupwizard.SetupWizardPreferences
import com.rafgittools.setupwizard.WizardDecision
import com.rafgittools.setupwizard.WizardRisk

@Composable
fun SetupWizardScreen(
    preferences: SetupWizardPreferences,
    ledger: SetupWizardCustodyLedger,
    onClose: () -> Unit
) {
    val steps = SetupWizardCatalog.steps
    var index by rememberSaveable { mutableIntStateOf(0) }
    val decisions = remember {
        mutableStateMapOf<String, WizardDecision>().apply { putAll(preferences.allDecisions()) }
    }
    val step = steps[index]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Configuração clara e revisável",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Passo ${index + 1} de ${steps.size}",
            style = MaterialTheme.typography.titleMedium
        )
        LinearProgressIndicator(
            progress = (index + 1).toFloat() / steps.size.toFloat(),
            modifier = Modifier.fillMaxWidth()
        )

        Text(text = step.title, style = MaterialTheme.typography.headlineSmall)
        Text(text = step.summary, style = MaterialTheme.typography.bodyLarge)

        SectionCard("O que esta opção possibilita", step.enables)
        SectionCard("Riscos e limites", step.risks)
        SectionCard("Dados envolvidos", step.dataTouched)
        SectionCard("Rollback", listOf(step.rollback))
        SectionCard("Regra Zero Trust", listOf(step.zeroTrustRule))

        Text(
            text = "Nível de risco: ${riskLabel(step.risk)}",
            style = MaterialTheme.typography.titleMedium
        )

        decisions[step.id]?.let {
            Text(
                text = "Sua decisão atual: ${decisionLabel(it)}",
                style = MaterialTheme.typography.titleMedium
            )
        }

        Text(
            text = "Sua escolha registra intenção e configuração. Ela não executa automaticamente uma operação privilegiada.",
            style = MaterialTheme.typography.bodyLarge
        )

        Button(
            onClick = {
                record(step.id, WizardDecision.AGREE, preferences, ledger, decisions)
                if (index < steps.lastIndex) index += 1
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Concordo e quero seguir")
        }

        OutlinedButton(
            onClick = {
                record(step.id, WizardDecision.DISAGREE, preferences, ledger, decisions)
                if (index < steps.lastIndex) index += 1
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Não concordo")
        }

        OutlinedButton(
            onClick = {
                record(step.id, WizardDecision.LATER, preferences, ledger, decisions)
                if (index < steps.lastIndex) index += 1
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Decidir depois")
        }

        OutlinedButton(
            onClick = { if (index > 0) index -= 1 },
            enabled = index > 0,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Voltar")
        }

        OutlinedButton(
            onClick = {
                ledger.append("WIZARD", null, "REVIEW")
                index = 0
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Revisar do início")
        }

        if (index == steps.lastIndex) {
            val agreed = decisions.values.count { it == WizardDecision.AGREE }
            val disagreed = decisions.values.count { it == WizardDecision.DISAGREE }
            val later = decisions.values.count { it == WizardDecision.LATER }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Resumo das suas escolhas", style = MaterialTheme.typography.titleLarge)
                    Text("Concordei: $agreed", style = MaterialTheme.typography.bodyLarge)
                    Text("Discordei: $disagreed", style = MaterialTheme.typography.bodyLarge)
                    Text("Decidir depois: $later", style = MaterialTheme.typography.bodyLarge)
                }
            }

            val allStepsDecided = steps.all { decisions.containsKey(it.id) }

            if (!allStepsDecided) {
                Text(
                    text = "Antes de concluir, registre uma escolha em cada passo: concordar, não concordar ou decidir depois.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Button(
                onClick = {
                    preferences.setCompleted(true)
                    ledger.append("WIZARD", null, "COMPLETE")
                    onClose()
                },
                enabled = allStepsDecided,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salvar e concluir configuração")
            }

            OutlinedButton(
                onClick = {
                    preferences.resetLocalDecisions()
                    decisions.clear()
                    ledger.append("WIZARD", null, "LOCAL_ROLLBACK")
                    index = 0
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reverter somente decisões locais")
            }

            Text(
                text = "Importante: esta reversão limpa somente preferências locais do wizard. Mudanças já realizadas em GitHub, módulos ou outros provedores exigem o rollback próprio de cada capacidade.",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text("Sair sem executar ações externas")
        }
    }
}

@Composable
private fun SectionCard(title: String, items: List<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            items.forEach { Text(text = "• $it", style = MaterialTheme.typography.bodyLarge) }
        }
    }
}

private fun record(
    stepId: String,
    decision: WizardDecision,
    preferences: SetupWizardPreferences,
    ledger: SetupWizardCustodyLedger,
    decisions: MutableMap<String, WizardDecision>
) {
    preferences.setDecision(stepId, decision)
    decisions[stepId] = decision
    ledger.append(stepId, decision, "DECISION")
}

private fun decisionLabel(decision: WizardDecision): String = when (decision) {
    WizardDecision.AGREE -> "CONCORDO"
    WizardDecision.DISAGREE -> "DISCORDO"
    WizardDecision.LATER -> "DECIDIR DEPOIS"
}

private fun riskLabel(risk: WizardRisk): String = when (risk) {
    WizardRisk.LOW -> "BAIXO"
    WizardRisk.MEDIUM -> "MÉDIO"
    WizardRisk.HIGH -> "ALTO"
}
