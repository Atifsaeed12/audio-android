package com.rulecough.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rulecough.app.MainViewModel
import com.rulecough.app.data.HistoryEntry
import com.rulecough.app.ui.theme.RiskHigh
import com.rulecough.app.ui.theme.RiskLow
import com.rulecough.app.ui.theme.RiskModerate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun riskColor(level: String): Color = when (level.lowercase()) {
    "high" -> RiskHigh
    "moderate" -> RiskModerate
    else -> RiskLow
}

@Composable
fun HistoryScreen(vm: MainViewModel) {
    val items = vm.history
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("History", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.weight(1f))
            if (items.isNotEmpty()) {
                Row(
                    Modifier.clip(RoundedCornerShape(10.dp))
                        .clickable { vm.clearHistory() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.size(4.dp))
                    Text("Clear", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Your past screenings", color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))

        if (items.isEmpty()) {
            Column(
                Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.History, contentDescription = null,
                    modifier = Modifier.size(52.dp),
                    tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(12.dp))
                Text("No screenings yet", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text("Recordings you analyze will appear here.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(items) { e -> HistoryRow(e) { vm.openHistory(e) } }
            }
        }
    }
}

@Composable
private fun HistoryRow(e: HistoryEntry, onClick: () -> Unit) {
    val df = remember0()
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(riskColor(e.riskLevel)),
            contentAlignment = Alignment.Center
        ) {
            Text("${(e.confidence * 100).toInt()}", color = Color.White,
                fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(e.prediction, fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium)
            Text(
                "${e.riskLevel} risk · ${if (e.onDevice) "on-device" else "server"} · " +
                    df.format(Date(e.timestamp)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun remember0(): SimpleDateFormat =
    androidx.compose.runtime.remember { SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()) }
