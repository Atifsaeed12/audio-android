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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rulecough.app.ConnStatus
import com.rulecough.app.MainViewModel
import com.rulecough.app.ui.theme.RiskHigh
import com.rulecough.app.ui.theme.RiskLow
import com.rulecough.app.ui.theme.RiskModerate

@Composable
fun SettingsScreen(vm: MainViewModel) {
    var url by remember { mutableStateOf(vm.serverUrl) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Connect the app to the machine running your model server.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(22.dp))
        // ---- appearance / night mode ----
        Text(
            "APPEARANCE",
            fontSize = 11.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (key, label) ->
                val active = vm.themeMode == key
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(11.dp))
                        .background(
                            if (active) MaterialTheme.colorScheme.primary else Color.Transparent
                        )
                        .clickable { vm.setThemeMode(key) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (active) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        // ---- inference mode: on-device vs server ----
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Run on-device (offline)", fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium)
                Text(
                    if (vm.onDevice)
                        (if (vm.onDeviceModelAvailable) "Using the bundled .tflite model — no server needed."
                         else "Model not bundled yet — add rule_cough.tflite to assets.")
                    else "Audio is sent to your server for the full multi-view model.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = vm.onDevice, onCheckedChange = { vm.setOnDevice(it) })
        }

        Spacer(Modifier.height(22.dp))
        Text(
            "SERVER URL",
            fontSize = 11.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            placeholder = { Text("http://192.168.1.20:8000") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(13.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { vm.updateServerUrl(url); vm.testConnection() }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Save & test", color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(Modifier.height(12.dp))
        ConnBadge(vm.connStatus)

        Spacer(Modifier.height(26.dp))
        Text(
            "MODEL",
            fontSize = 11.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Kv("Architecture", "Multi-view fusion")
        Kv("Input", "16 kHz · 5 s mono WAV")
        Kv("Inference", "on the server (FastAPI)")
        Kv("Uncertainty", "Monte-Carlo dropout")

        Spacer(Modifier.height(24.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant).padding(14.dp)
        ) {
            Text(
                "For research and education only. RULe-Cough is a screening aid, " +
                    "not a medical device, and must not be used for clinical decisions.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ConnBadge(status: ConnStatus) {
    val (color, label) = when (status) {
        ConnStatus.OK -> RiskLow to "Connected · model ready"
        ConnStatus.NO_MODEL -> RiskModerate to "Server up, but no model loaded"
        ConnStatus.FAIL -> RiskHigh to "Could not reach the server"
        ConnStatus.CHECKING -> MaterialTheme.colorScheme.primary to "Checking…"
        ConnStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant to "Not tested yet"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Spacer(Modifier.size(8.dp))
        Text(label, color = color, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
    }
}

@Composable
private fun Kv(key: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(key, color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium)
        Text(value, fontFamily = FontFamily.Monospace, fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline))
}
