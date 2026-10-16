package com.smsmurilo.telecom.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smsmurilo.telecom.domain.InvoiceStatus
import com.smsmurilo.telecom.domain.PlanUsage
import com.smsmurilo.telecom.mvi.HomeIntent
import com.smsmurilo.telecom.mvi.HomeState
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val brl = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))
private val dayMonth = DateTimeFormatter.ofPattern("dd/MM")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(state: HomeState, onIntent: (HomeIntent) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Meu Plano") }) }) { padding ->
        if (state.loading && state.usage == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        if (state.error != null && state.usage == null) {
            Column(Modifier.fillMaxSize().padding(padding), Arrangement.Center, Alignment.CenterHorizontally) {
                Text(state.error)
                Button(onClick = { onIntent(HomeIntent.Load) }) { Text("Tentar novamente") }
            }
            return@Scaffold
        }
        LazyColumn(Modifier.padding(padding), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.usage?.let { item { UsageCard(it) } }
            item { Text("Pacotes extras", style = MaterialTheme.typography.titleMedium) }
            items(state.packages, key = { it.id }) { pkg ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(pkg.name)
                            Text(brl.format(pkg.price), style = MaterialTheme.typography.bodySmall)
                        }
                        if (state.buyingPackageId == pkg.id) {
                            CircularProgressIndicator(Modifier.size(24.dp))
                        } else {
                            OutlinedButton(onClick = { onIntent(HomeIntent.BuyPackage(pkg.id)) }, enabled = state.buyingPackageId == null) {
                                Text("Contratar")
                            }
                        }
                    }
                }
            }
            item { Text("Faturas", style = MaterialTheme.typography.titleMedium) }
            items(state.invoices, key = { it.id }) { invoice ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${invoice.reference.lowercase().replaceFirstChar { it.uppercase() }} • ${brl.format(invoice.amount)}")
                            Text(
                                when (invoice.status) {
                                    InvoiceStatus.OPEN -> "Vence em ${invoice.dueDate.format(dayMonth)}"
                                    InvoiceStatus.PAID -> "Paga"
                                    InvoiceStatus.OVERDUE -> "Vencida"
                                },
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (invoice.status != InvoiceStatus.PAID) {
                            TextButton(onClick = { onIntent(HomeIntent.CopyBarcode(invoice)) }) { Text("Copiar código") }
                        }
                    }
                }
            }
            item {
                Text("Avisar quando atingir ${state.alertThreshold}% da franquia", style = MaterialTheme.typography.titleSmall)
                Slider(
                    value = state.alertThreshold.toFloat(),
                    onValueChange = { onIntent(HomeIntent.ChangeThreshold(it.toInt())) },
                    valueRange = 50f..95f,
                    steps = 8,
                )
            }
        }
    }
}

@Composable
private fun UsageCard(usage: PlanUsage) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(usage.planName, style = MaterialTheme.typography.titleLarge)
            Text(usage.msisdn, style = MaterialTheme.typography.bodySmall)
            LinearProgressIndicator(
                progress = { usage.usedPercent / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = if (usage.usedPercent >= 80) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
            Text("Restam %.1f GB de %.0f GB • renova em %s".format(
                usage.dataLeftMb / 1024.0, usage.dataTotalMb / 1024.0, usage.cycleEnd.format(dayMonth),
            ))
            if (usage.minutesUnlimited) Text("Ligações ilimitadas para qualquer operadora", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Preview
@Composable
private fun UsagePreview() = MaterialTheme {
    UsageCard(PlanUsage("Controle 25GB", "(82) 99999-0000", 25600, 21504, true, LocalDate.now()))
}
