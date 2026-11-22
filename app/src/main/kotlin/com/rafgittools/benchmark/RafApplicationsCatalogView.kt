package com.rafgittools.benchmark

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Navigate session references without implying executable kernels or scientific
 * validation. Producer routing is read-only; no cross-repository dispatch.
 */
@Composable
fun RafApplicationsCatalogView() {
    var selected by remember { mutableStateOf("G") }
    val group = RafApplicationsCatalog.groups.first { it.id == selected }
    OutlinedCard {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Aplicações Rafael IA · ATLAS K001–K090",
                style = MaterialTheme.typography.titleMedium)
            Text("90 rotas de conhecimento, não 90 aplicativos executáveis. " +
                "Apenas o benchmark local autorizado nesta tela executa.",
                style = MaterialTheme.typography.bodySmall)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RafApplicationsCatalog.groups.forEach { item ->
                    FilterChip(
                        selected = item.id == selected,
                        onClick = { selected = item.id },
                        label = { Text(item.id + " · " + (item.last - item.first + 1)) }
                    )
                }
            }
            Text(group.title + " · autoridade: " + group.producer,
                style = MaterialTheme.typography.titleSmall)
            RafApplicationsCatalog.forGroup(selected).forEach { route ->
                Text(
                    route.id + " · " + route.title +
                        " · " + route.stage.name,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text("SOURCE ≠ ARTIFACT ≠ EXECUTION ≠ EVIDENCE ≠ CLAIM. " +
                "Rotas sem execução = CATALOG_ONLY · TOKEN_VAZIO_DEVICE.",
                style = MaterialTheme.typography.labelSmall)
        }
    }
}
