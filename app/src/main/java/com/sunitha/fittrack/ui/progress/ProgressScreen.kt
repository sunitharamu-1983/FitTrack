package com.sunitha.fittrack.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunitha.fittrack.data.db.entities.WeightEntry
import com.sunitha.fittrack.ui.theme.GreenDark
import com.sunitha.fittrack.ui.theme.GreenLight
import com.sunitha.fittrack.ui.theme.GreenPrimary
import com.sunitha.fittrack.ui.theme.OrangeAccent
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(vm: ProgressViewModel) {
    val entries          by vm.weightEntries.collectAsState()
    val latestEntry      by vm.latestWeight.collectAsState()
    val showDialog       by vm.showLogDialog.collectAsState()
    val weightInput      by vm.newWeightInput.collectAsState()
    val macroTrends      by vm.macroTrends.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progress", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = vm::openLogDialog,
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Log Weight") },
                containerColor = GreenPrimary,
                contentColor = Color.White
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { CurrentWeightCard(latestEntry, vm.goalWeight, vm.weeklyChange()) }
            item { MacroTrendsCard(macroTrends) }
            if (entries.isNotEmpty()) {
                item { Text("Weight History", fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
                items(entries.sortedByDescending { it.dateMillis }.take(30)) { entry ->
                    WeightHistoryRow(entry, onDelete = { vm.deleteEntry(entry) })
                }
            }
        }
    }

    if (showDialog) {
        LogWeightDialog(
            value     = weightInput,
            onChange  = vm::setWeightInput,
            onConfirm = vm::logWeight,
            onDismiss = vm::closeLogDialog
        )
    }
}

// ── Current Weight Card ────────────────────────────────────────────────────────

@Composable
private fun CurrentWeightCard(latest: WeightEntry?, goal: Float, weeklyChange: Float?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(GreenDark, GreenPrimary)))
                .padding(20.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Current Weight", color = Color.White.copy(0.8f), fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            latest?.weightKg?.let { "$it" } ?: "--",
                            color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold
                        )
                        Text(" kg", color = Color.White.copy(0.8f), fontSize = 16.sp, modifier = Modifier.padding(bottom = 6.dp))
                    }
                    if (weeklyChange != null) {
                        val isLoss = weeklyChange < 0
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                if (isLoss) Icons.Filled.TrendingDown else Icons.Filled.TrendingUp,
                                null, tint = if (isLoss) GreenLight else OrangeAccent, modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "${if (weeklyChange < 0) "" else "+"}${"%.1f".format(weeklyChange)} kg this week",
                                color = Color.White.copy(0.9f), fontSize = 13.sp, fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Goal", color = Color.White.copy(0.7f), fontSize = 12.sp)
                    Text("$goal kg", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    if (latest != null) {
                        val diff = latest.weightKg - goal
                        Text("${"%.1f".format(diff)} kg to go", color = Color.White.copy(0.7f), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ── Macro Trends Card (7-day) ──────────────────────────────────────────────────

@Composable
private fun MacroTrendsCard(trends: List<DayMacros>) {
    if (trends.isEmpty()) return

    val proteinColor = Color(0xFF1565C0)
    val carbsColor   = OrangeAccent
    val fatColor     = Color(0xFFC62828)

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("7-Day Macro Trend", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MacroLegendDot("Protein", proteinColor)
                MacroLegendDot("Carbs", carbsColor)
                MacroLegendDot("Fat", fatColor)
            }
            Spacer(Modifier.height(10.dp))

            val maxG = trends.maxOfOrNull { maxOf(it.proteinG, it.carbsG, it.fatG) }?.coerceAtLeast(1f) ?: 1f

            Canvas(
                modifier = Modifier.fillMaxWidth().height(120.dp).padding(horizontal = 4.dp)
            ) {
                if (trends.size < 2) return@Canvas

                val xStep = size.width / (trends.size - 1)
                val padV  = 8.dp.toPx()

                fun xOf(i: Int) = i * xStep
                fun yOf(g: Float) = size.height - padV - (g / maxG) * (size.height - 2 * padV)

                listOf(
                    trends.map { it.proteinG } to proteinColor,
                    trends.map { it.carbsG }   to carbsColor,
                    trends.map { it.fatG }      to fatColor
                ).forEach { (values, color) ->
                    val path = Path().apply {
                        values.forEachIndexed { i, g ->
                            if (i == 0) moveTo(xOf(0), yOf(g)) else lineTo(xOf(i), yOf(g))
                        }
                    }
                    drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                    values.forEachIndexed { i, g ->
                        drawCircle(Color.White, radius = 4.dp.toPx(), center = Offset(xOf(i), yOf(g)))
                        drawCircle(color,       radius = 2.5.dp.toPx(), center = Offset(xOf(i), yOf(g)))
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                trends.forEach { day ->
                    Text(day.dateLabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                }
            }
        }
    }
}

@Composable
private fun MacroLegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(color))
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.65f))
    }
}

// ── History Row ────────────────────────────────────────────────────────────────

@Composable
private fun WeightHistoryRow(entry: WeightEntry, onDelete: () -> Unit) {
    val sdf = remember { SimpleDateFormat("EEE, MMM d · h:mm a", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(GreenPrimary.copy(0.1f)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.MonitorWeight, null, tint = GreenPrimary, modifier = Modifier.size(18.dp)) }
            Column(modifier = Modifier.weight(1f)) {
                Text("${entry.weightKg} kg", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(sdf.format(Date(entry.dateMillis)), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.3f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ── Log Weight Dialog ──────────────────────────────────────────────────────────

@Composable
private fun LogWeightDialog(value: String, onChange: (String) -> Unit, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Weight", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onChange,
                label = { Text("Weight (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = value.toFloatOrNull() != null,
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        shape = RoundedCornerShape(20.dp)
    )
}
