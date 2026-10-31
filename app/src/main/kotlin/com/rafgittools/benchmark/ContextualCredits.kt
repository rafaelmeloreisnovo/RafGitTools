package com.rafgittools.benchmark

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Contextual credits apply to exact screen/component use, not a generic
 * assertion that all referenced projects are linked in the running app.
 * Any unresolved provenance must keep the info control active.
 */
enum class CreditOrigin { PROJECT_AUTHORIAL, EXTERNAL_API, UNKNOWN_REVIEW }
data class ScreenCredit(
    val component: String,
    val origin: CreditOrigin,
    val usage: String,
    val publisher: String,
    val license: String,
    val source: String,
    val localPointer: String
)
object RafBenchmarkCredits {
    val benchmarkScreen = listOf(
        ScreenCredit("Raf Benchmark kernel", CreditOrigin.PROJECT_AUTHORIAL,
            "Algoritmo de teste local, dados sintéticos e receipts.",
            "RAFAELIA / RafGitTools contributors", "RafGitTools project license",
            "https://github.com/rafaelmeloreisnovo/RafGitTools",
            "app/src/main/kotlin/com/rafgittools/benchmark/"),
        ScreenCredit("Jetpack Compose / Material3", CreditOrigin.EXTERNAL_API,
            "Renderização e controles desta tela.",
            "Android Open Source Project / AndroidX contributors", "Apache-2.0 (AndroidX)",
            "https://github.com/androidx/androidx",
            "app/src/main/kotlin/com/rafgittools/RafBenchmarkActivity.kt"),
        ScreenCredit("Kotlin standard library", CreditOrigin.EXTERNAL_API,
            "Compilação e estruturas Kotlin utilizadas pela aplicação.",
            "JetBrains and Kotlin contributors", "Apache-2.0",
            "https://github.com/JetBrains/kotlin",
            "app/build.gradle")
    )
}

/** Dark info glyph for external use; subdued/disabled only for verified authorial-only scopes. */
@Composable
fun ContextualCreditsInfo(credits: List<ScreenCredit>, modifier: Modifier = Modifier) {
    var showAbout by remember { mutableStateOf(false) }
    val externalOrUnknown = credits.any { it.origin != CreditOrigin.PROJECT_AUTHORIAL }
    TextButton(
        modifier = modifier.semantics {
            contentDescription = if (externalOrUnknown) "Sobre e créditos desta tela"
                else "Somente componentes autorais; sem créditos externos"
        },
        enabled = externalOrUnknown,
        onClick = { showAbout = true },
        colors = ButtonDefaults.textButtonColors(
            contentColor = Color.Black,
            disabledContentColor = Color.Gray
        )
    ) { Text("ⓘ", style = MaterialTheme.typography.titleLarge) }

    if (showAbout) AlertDialog(
        onDismissRequest = { showAbout = false },
        title = { Text("Sobre · componentes desta tela") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                credits.forEach { credit ->
                    Text(credit.component, style = MaterialTheme.typography.titleSmall)
                    Text("Uso: " + credit.usage)
                    Text("Origem: " + credit.origin.name)
                    Text("Autor / mantenedor: " + credit.publisher)
                    Text("Licença declarada: " + credit.license)
                    Text("Fonte: " + credit.source)
                    Text("Ponto do código: " + credit.localPointer)
                    Text(" ", modifier = Modifier.padding(4.dp))
                }
                Text("Inventário adicional: THIRD_PARTY_LICENSES.md e docs/LICENSE_INFO.md. " +
                    "Uso de API não equivale a cópia de código. Em caso de procedência não verificada, manter UNKNOWN_REVIEW.")
            }
        },
        confirmButton = {
            TextButton(onClick = { showAbout = false }) { Text("Fechar") }
        }
    )
}
