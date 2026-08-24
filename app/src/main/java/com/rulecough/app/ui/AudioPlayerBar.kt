package com.rulecough.app.ui

import android.media.MediaPlayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rulecough.app.ui.theme.Viridis2
import com.rulecough.app.ui.theme.Viridis4
import kotlinx.coroutines.delay
import java.io.File

/** Play/pause bar with a progress track for a local audio file. */
@Composable
fun AudioPlayerBar(path: String?, modifier: Modifier = Modifier) {
    val available = path != null && File(path).exists()
    var player by remember(path) { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember(path) { mutableStateOf(false) }
    var pos by remember(path) { mutableStateOf(0) }
    var dur by remember(path) { mutableStateOf(0) }

    DisposableEffect(path) {
        onDispose {
            try { player?.release() } catch (_: Exception) {}
            player = null
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            pos = try { player?.currentPosition ?: 0 } catch (_: Exception) { 0 }
            delay(120)
        }
    }

    val frac by animateFloatAsState(
        targetValue = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f,
        label = "progress"
    )

    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(46.dp).clip(CircleShape)
                .background(
                    if (available) Brush.linearGradient(listOf(Viridis2, Viridis4))
                    else Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.outline)
                    )
                )
                .clickable(enabled = available) {
                    val p = player ?: try {
                        val mp = MediaPlayer()
                        mp.setDataSource(path!!)
                        mp.prepare()
                        mp.setOnCompletionListener { isPlaying = false; mp.seekTo(0); pos = 0 }
                        player = mp
                        dur = mp.duration
                        mp
                    } catch (e: Exception) { null }
                    if (p != null) {
                        if (isPlaying) { p.pause(); isPlaying = false }
                        else { p.start(); isPlaying = true }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            Box(
                Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(99.dp))
                    .background(MaterialTheme.colorScheme.outline)
            )
            Box(
                Modifier.fillMaxWidth(frac).height(6.dp).clip(RoundedCornerShape(99.dp))
                    .background(Brush.horizontalGradient(listOf(Viridis2, Viridis4)))
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            if (available) fmt(if (isPlaying || pos > 0) pos else dur) else "--:--",
            fontFamily = FontFamily.Monospace, fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun fmt(ms: Int): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
