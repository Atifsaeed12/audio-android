package com.rulecough.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rulecough.app.MainViewModel
import com.rulecough.app.UiState
import com.rulecough.app.ui.theme.Viridis0
import com.rulecough.app.ui.theme.Viridis2
import com.rulecough.app.ui.theme.Viridis3
import com.rulecough.app.ui.theme.Viridis4
import com.rulecough.app.ui.theme.Viridis6

@Composable
fun RecordScreen(
    vm: MainViewModel,
    hasMic: Boolean,
    onRequestMic: () -> Unit,
    onPickFile: () -> Unit
) {
    when (val s = vm.uiState) {
        is UiState.Recording -> RecordingView(seconds = s.seconds, onStop = { vm.stopRecording() })
        is UiState.Analyzing -> AnalyzingView()
        is UiState.Error -> ErrorView(message = s.message, onRetry = { vm.reset() })
        else -> IdleView(onRecord = onRequestMic, onPickFile = onPickFile)
    }
}

@Composable
private fun IdleView(onRecord: () -> Unit, onPickFile: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val ring = Brush.sweepGradient(
            listOf(Viridis0, Viridis2, Viridis3, Viridis4, Viridis6, Viridis0)
        )
        Box(
            Modifier.size(200.dp).clip(CircleShape).background(ring).clickable { onRecord() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(162.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Mic, contentDescription = "Record",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(46.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "TAP TO RECORD",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        Text(
            "Record about 5 seconds of coughing in a quiet room, phone ~20 cm away.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth(0.8f)
        )
        Spacer(Modifier.height(22.dp))
        Row(
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                .clickable { onPickFile() }
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text("Upload an audio file", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun RecordingView(seconds: Int, onStop: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "● RECORDING",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "0:0$seconds",
            fontSize = 52.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(" / 0:05", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))
        Box(
            Modifier.size(70.dp).clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.error).clickable { onStop() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Stop, contentDescription = "Stop",
                tint = MaterialTheme.colorScheme.onError, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Tap to stop early, or it stops automatically at 5s.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun AnalyzingView() {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(22.dp))
        Text("Analyzing cough acoustics", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            "Denoising · multi-view features · model + uncertainty",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Something went wrong", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Text(
            message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(22.dp))
        Box(
            Modifier.clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primary)
                .clickable { onRetry() }
                .padding(horizontal = 26.dp, vertical = 13.dp)
        ) {
            Text("Try again", color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelLarge)
        }
    }
}
