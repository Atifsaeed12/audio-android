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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rulecough.app.net.PredictResponse
import com.rulecough.app.ui.theme.RiskHigh
import com.rulecough.app.ui.theme.RiskLow
import com.rulecough.app.ui.theme.RiskModerate
import com.rulecough.app.ui.theme.Viridis0
import com.rulecough.app.ui.theme.Viridis2
import com.rulecough.app.ui.theme.Viridis3
import com.rulecough.app.ui.theme.Viridis4
import com.rulecough.app.ui.theme.ViridisScale

private fun riskColor(level: String): Color = when (level.lowercase()) {
    "high" -> RiskHigh
    "moderate" -> RiskModerate
    else -> RiskLow
}

@Composable
fun ResultScreen(result: PredictResponse, onAgain: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        VerdictCard(result)

        SectionLabel("Class probabilities")
        result.probabilities.forEachIndexed { i, p ->
            ProbabilityBar(
                name = p.label,
                pct = p.prob,
                color = ViridisScale[i % ViridisScale.size],
                emphasize = i == 0
            )
            Spacer(Modifier.height(8.dp))
        }

        UncertaintyRow(result)

        if (result.acousticFeatures.isNotEmpty()) {
            SectionLabel("Acoustic biomarkers")
            val feats = result.acousticFeatures
            var row = 0
            while (row < feats.size) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    FeatureCell(feats[row].name, feats[row].value, Modifier.weight(1f))
                    if (row + 1 < feats.size) {
                        FeatureCell(feats[row + 1].name, feats[row + 1].value, Modifier.weight(1f))
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(9.dp))
                row += 2
            }
        }

        Spacer(Modifier.height(6.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Text(
                result.advisory,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(20.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.horizontalGradient(listOf(Viridis2, Viridis4))
                )
                .clickable { onAgain() }
                .padding(vertical = 15.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Record again",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelLarge
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun VerdictCard(result: PredictResponse) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Viridis0, Viridis2, Viridis3)))
            .padding(18.dp)
    ) {
        Column {
            Text(
                "MOST LIKELY CONDITION",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                result.prediction,
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${(result.confidence * 100).toInt()}% confidence · " +
                    (if (result.highUncertainty) "high" else "low") + " uncertainty",
                color = Color.White.copy(alpha = 0.92f),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
        }
        Box(
            Modifier.align(Alignment.TopEnd).clip(RoundedCornerShape(999.dp))
                .background(riskColor(result.riskLevel))
                .padding(horizontal = 11.dp, vertical = 5.dp)
        ) {
            Text(
                "${result.riskLevel} risk".uppercase(),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp
            )
        }
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun ProbabilityBar(name: String, pct: Float, color: Color, emphasize: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            name,
            modifier = Modifier.width(88.dp),
            maxLines = 1,
            fontSize = 12.sp,
            color = if (emphasize) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Normal
        )
        Box(
            Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                Modifier.fillMaxWidth(pct.coerceIn(0f, 1f)).height(10.dp)
                    .clip(RoundedCornerShape(999.dp)).background(color)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "${(pct * 100).toInt()}%",
            modifier = Modifier.width(38.dp),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun UncertaintyRow(result: PredictResponse) {
    val col = if (result.highUncertainty) RiskModerate else RiskLow
    Row(
        Modifier.fillMaxWidth().padding(top = 14.dp).clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(col),
            contentAlignment = Alignment.Center
        ) {
            Text(
                String.format("%.2f", result.uncertainty),
                color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            if (result.highUncertainty)
                "High uncertainty across ${result.mcPasses} Monte-Carlo passes — clinical confirmation recommended."
            else
                "Low uncertainty across ${result.mcPasses} Monte-Carlo passes — a consistent screening signal.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FeatureCell(name: String, value: Float, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(13.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(13.dp))
            .padding(11.dp)
    ) {
        Column {
            Text(
                name.uppercase(),
                fontSize = 10.sp,
                letterSpacing = 0.4.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.height(3.dp))
            Text(
                formatValue(value),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun formatValue(v: Float): String =
    if (v >= 100f) v.toInt().toString()
    else String.format("%.2f", v)

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        fontSize = 11.sp,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 20.dp, bottom = 11.dp),
        style = MaterialTheme.typography.labelMedium
    )
}
