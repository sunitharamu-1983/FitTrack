package com.sunitha.fittrack.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunitha.fittrack.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// ---------- Screen ----------

private val RestPurpleH = Color(0xFF6A1B9A)
private val WalkBlueH   = Color(0xFF0277BD)
private val PeriodRoseH = Color(0xFFC2185B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: HomeViewModel, onNavigate: (String) -> Unit = {}, onOpenSettings: () -> Unit = {}) {
    val today = remember {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
    }

    val consumedCalories by vm.consumedCalories.collectAsState()
    val consumedProtein  by vm.consumedProtein.collectAsState()
    val consumedCarbs    by vm.consumedCarbs.collectAsState()
    val consumedFat      by vm.consumedFat.collectAsState()
    val consumedFiber    by vm.consumedFiber.collectAsState()
    val lastWorkout             by vm.lastWorkout.collectAsState()
    val recentWorkoutSessions   by vm.recentWorkoutSessions.collectAsState()
    val lastRestDay      by vm.lastRestDay.collectAsState()
    val lastWalkDay      by vm.lastWalkDay.collectAsState()
    val lastPeriodDay    by vm.lastPeriodDay.collectAsState()
    val isRefreshing     by vm.isRefreshing.collectAsState()
    val todaySteps       by vm.todaySteps.collectAsState()
    val streak           by vm.workoutStreak.collectAsState()
    val macros           by vm.macros.collectAsState()
    val userName         by vm.userName.collectAsState()
    val todayPeriodDay   by vm.todayPeriodDay.collectAsState()
    val todayRestDay     by vm.todayRestDay.collectAsState()
    val todayWalkDay     by vm.todayWalkDay.collectAsState()
    var showStepDialog   by remember { mutableStateOf(false) }
    var showRestDialog   by remember { mutableStateOf(false) }
    var showPeriodDialog by remember { mutableStateOf(false) }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh    = { vm.refresh() },
        modifier     = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier            = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { GreetingHeader(date = today, streak = streak, name = userName, isPeriodDay = todayPeriodDay != null, onSettings = onOpenSettings) }
            item { StepsCard(steps = todaySteps, goal = macros.stepGoal, onUpdate = { showStepDialog = true }) }
            item { RecentWorkoutCard(recentWorkoutSessions, lastRestDay, lastWalkDay, lastPeriodDay, onNavigate) }
            item {
                DayToggleRow(
                    isRestDay      = todayRestDay != null,
                    isWalkDay      = todayWalkDay != null,
                    isPeriodDay    = todayPeriodDay != null,
                    onMarkRest     = { showRestDialog = true },
                    onRemoveRest   = { vm.removeRestDay() },
                    onMarkWalk     = { vm.markWalkDay() },
                    onRemoveWalk   = { vm.removeWalkDay() },
                    onMarkPeriod   = { showPeriodDialog = true },
                    onRemovePeriod = { vm.removePeriodDay() }
                )
            }
            item { CalorieCard(consumed = consumedCalories, goal = macros.calories) }
            item {
                MacrosCard(
                    consumedProtein = consumedProtein, proteinGoal = macros.proteinG.toInt(),
                    consumedCarbs   = consumedCarbs,   carbsGoal   = macros.carbsG.toInt(),
                    consumedFat     = consumedFat,     fatGoal     = macros.fatG.toInt(),
                    consumedFiber   = consumedFiber,   fiberGoal   = macros.fiberG.toInt(),
                    onViewAll = { onNavigate("macro_history") }
                )
            }
        }
    }

    if (showStepDialog) {
        StepUpdateDialog(
            current   = todaySteps,
            onConfirm = { vm.logSteps(it); showStepDialog = false },
            onDismiss = { showStepDialog = false }
        )
    }
    if (showRestDialog) {
        RestDayNoteDialog(
            onConfirm = { note -> vm.markRestDay(note); showRestDialog = false },
            onDismiss = { showRestDialog = false }
        )
    }
    if (showPeriodDialog) {
        PeriodDayNoteDialog(
            onConfirm = { note -> vm.markPeriodDay(note); showPeriodDialog = false },
            onDismiss = { showPeriodDialog = false }
        )
    }
}

// ---------- Greeting Header ----------

@Composable
private fun GreetingHeader(date: String, streak: Int, name: String, isPeriodDay: Boolean, onSettings: () -> Unit) {
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 0..11  -> "Good morning"
            in 12..16 -> "Good afternoon"
            else      -> "Good evening"
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(colors = listOf(RoseDark, RosePrimary)))
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Text("$greeting, $name!", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text(date, color = Color.White.copy(alpha = 0.82f), fontSize = 13.sp)
                if (isPeriodDay) {
                    Spacer(Modifier.height(4.dp))
                    Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(0.2f)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.WaterDrop, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Text("Period Day", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.18f)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(Icons.Filled.LocalFireDepartment, null, tint = Color(0xFFFFCC80), modifier = Modifier.size(18.dp))
                        Text("$streak day streak", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
                IconButton(onClick = onSettings, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White.copy(0.8f))
                }
            }
        }
    }
}

// ---------- Calorie Ring Card ----------

@Composable
private fun CalorieCard(consumed: Int, goal: Int) {
    val progress = (consumed.toFloat() / goal).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress, animationSpec = tween(1200), label = "calorie_ring"
    )
    val remaining = (goal - consumed).coerceAtLeast(0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Today's Calories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                CalorieRing(progress = animatedProgress, consumed = consumed)
                Column(
                    modifier = Modifier.weight(1f).padding(start = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CalorieStat("Goal",      "$goal kcal",      MaterialTheme.colorScheme.onSurface.copy(0.55f))
                    CalorieStat("Consumed",  "$consumed kcal",  RosePrimary)
                    CalorieStat("Remaining", "$remaining kcal", OrangeAccent)
                }
            }
        }
    }
}

@Composable
private fun CalorieRing(progress: Float, consumed: Int) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(140.dp)) {
            val strokeWidth = 16.dp.toPx()
            val radius      = (size.minDimension / 2f) - strokeWidth / 2f
            val arcTopLeft  = Offset(center.x - radius, center.y - radius)
            val arcSize     = Size(radius * 2, radius * 2)
            val startAngle  = 135f; val sweep = 270f

            drawArc(trackColor, startAngle, sweep, false, arcTopLeft, arcSize, style = Stroke(strokeWidth, cap = StrokeCap.Round))
            drawArc(
                Brush.sweepGradient(listOf(RoseLight, RosePrimary, RoseDark), center),
                startAngle, sweep * progress, false, arcTopLeft, arcSize,
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$consumed", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            Text("kcal", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        }
    }
}

@Composable
private fun CalorieStat(label: String, value: String, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.45f))
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

// ---------- Macros Card ----------

private data class MacroData(val name: String, val current: Int, val goal: Int, val unit: String, val color: Color)

@Composable
private fun MacrosCard(
    consumedProtein: Float, proteinGoal: Int,
    consumedCarbs: Float, carbsGoal: Int,
    consumedFat: Float, fatGoal: Int,
    consumedFiber: Float, fiberGoal: Int,
    onViewAll: () -> Unit = {}
) {
    val macros = listOf(
        MacroData("Protein", consumedProtein.toInt(), proteinGoal, "g", Color(0xFF1565C0)),
        MacroData("Carbs",   consumedCarbs.toInt(),   carbsGoal,   "g", OrangeAccent),
        MacroData("Fat",     consumedFat.toInt(),      fatGoal,     "g", Color(0xFFC62828)),
        MacroData("Fiber",   consumedFiber.toInt(),    fiberGoal,   "g", GreenPrimary)
    )
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Macros", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onViewAll) { Text("View All", color = RosePrimary, fontSize = 13.sp) }
            }
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    MacroItem(macros[0]); MacroItem(macros[1])
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    MacroItem(macros[2]); MacroItem(macros[3])
                }
            }
        }
    }
}

@Composable
private fun MacroItem(macro: MacroData) {
    val progress = (macro.current.toFloat() / macro.goal).coerceIn(0f, 1f)
    val animProg by animateFloatAsState(progress, tween(1200), label = "${macro.name}_prog")
    val displayPct = if (macro.goal > 0) macro.current * 100 / macro.goal else 0
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.width(88.dp)) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(progress = { animProg }, modifier = Modifier.size(72.dp), strokeWidth = 6.dp,
                color = macro.color, trackColor = macro.color.copy(0.15f), strokeCap = StrokeCap.Round)
            Text("$displayPct%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = macro.color)
        }
        Text(macro.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        Text("${macro.current}/${macro.goal}${macro.unit}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
    }
}

// ---------- Recent Workout Card ----------

@Composable
private fun RecentWorkoutCard(
    recentWorkoutSessions: List<com.sunitha.fittrack.data.db.entities.WorkoutSession>,
    restDay:    com.sunitha.fittrack.data.db.entities.RestDayEntry?,
    walkDay:    com.sunitha.fittrack.data.db.entities.StepEntry?,
    periodDay:  com.sunitha.fittrack.data.db.entities.PeriodEntry?,
    onNavigate: (String) -> Unit
) {
    val sdf         = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }
    val restColor   = Color(0xFF7B1FA2)
    val walkColor   = Color(0xFF0277BD)
    val periodColor = Color(0xFFC2185B)

    fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    // Find the most recent day that has any activity
    val mostRecentDay = listOfNotNull(
        recentWorkoutSessions.firstOrNull()?.let { startOfDay(it.dateMillis) },
        restDay?.let  { startOfDay(it.dateMillis) },
        walkDay?.let  { startOfDay(it.dateMillis) },
        periodDay?.let{ startOfDay(it.dateMillis) }
    ).maxOrNull()

    // Collect everything that happened on that day
    val dayWorkouts    = if (mostRecentDay != null) recentWorkoutSessions.filter { startOfDay(it.dateMillis) == mostRecentDay } else emptyList()
    val muscleGroups   = dayWorkouts.map { it.muscleGroup }.distinct()
    val totalSets      = dayWorkouts.sumOf { it.totalSets }
    val isRestOnDay    = restDay?.let   { startOfDay(it.dateMillis) == mostRecentDay } == true
    val isWalkOnDay    = walkDay?.let   { startOfDay(it.dateMillis) == mostRecentDay } == true
    val walkSteps      = if (isWalkOnDay) walkDay!!.steps else 0
    val isPeriodOnDay  = periodDay?.let { startOfDay(it.dateMillis) == mostRecentDay } == true

    // Build combined title: e.g. "Legs · Cardio · Walk Day"
    val titleParts = mutableListOf<String>().apply {
        addAll(muscleGroups)
        if (isRestOnDay)   add("Rest Day")
        if (isWalkOnDay)   add("Walk Day")
        if (isPeriodOnDay) add("Period Day")
    }

    // Build badge: e.g. "12 sets · 5000 steps"
    val badgeParts = mutableListOf<String>().apply {
        if (totalSets  > 0) add("$totalSets sets")
        if (walkSteps  > 0) add("$walkSteps steps")
    }
    val badge = badgeParts.joinToString(" · ").ifBlank { "Done" }

    // Primary icon and colour
    val (primaryIcon, primaryColor) = when {
        muscleGroups.isNotEmpty() -> Icons.Filled.FitnessCenter to RosePrimary
        isRestOnDay               -> Icons.Filled.SelfImprovement to restColor
        isWalkOnDay               -> Icons.Filled.DirectionsWalk to walkColor
        else                      -> Icons.Filled.WaterDrop to periodColor
    }
    val badgeColor = when {
        muscleGroups.isNotEmpty() -> RosePrimary
        isRestOnDay               -> restColor
        isWalkOnDay               -> walkColor
        else                      -> periodColor
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(GreenDark, GreenPrimary)))
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.FitnessCenter, null, tint = Color.White.copy(0.9f), modifier = Modifier.size(18.dp))
                        Text("Last Activity", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                    TextButton(
                        onClick        = { onNavigate("workout_history") },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("View All", color = Color.White.copy(0.85f), fontSize = 13.sp)
                    }
                }
            }
            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                if (mostRecentDay == null || titleParts.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(RosePrimary.copy(0.08f)), Alignment.Center) {
                            Icon(Icons.Filled.FitnessCenter, null, tint = RosePrimary.copy(0.4f), modifier = Modifier.size(24.dp))
                        }
                        Text("No activity yet — tap Log Workout to start!", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.45f))
                    }
                } else {
                    LastActivityRow(
                        icon       = primaryIcon,
                        iconColor  = primaryColor,
                        title      = titleParts.joinToString(" · "),
                        subtitle   = sdf.format(Date(mostRecentDay)),
                        badge      = badge,
                        badgeColor = badgeColor
                    )
                }
            }
        }
    }
}

@Composable
private fun LastActivityRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    badge: String,
    badgeColor: Color
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier         = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(iconColor.copy(0.12f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp)) }

        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
        }

        Surface(shape = RoundedCornerShape(8.dp), color = badgeColor.copy(0.1f)) {
            Text(
                badge,
                color      = badgeColor,
                fontSize   = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier   = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

// ---------- Steps Card ----------

@Composable
private fun StepsCard(steps: Int, goal: Int, onUpdate: () -> Unit) {
    val progress     = (steps.toFloat() / goal).coerceIn(0f, 1f)
    val animProgress by animateFloatAsState(progress, tween(1200), label = "home_steps")
    val stepsPct     = if (goal > 0) steps * 100 / goal else 0

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF0277BD), Color(0xFF039BE5))))
                .padding(20.dp)
        ) {
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.DirectionsWalk, null, tint = Color.White.copy(0.9f), modifier = Modifier.size(22.dp))
                        Text("Today's Steps", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                    TextButton(onClick = onUpdate, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("Update", color = Color.White.copy(0.85f), fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("$steps", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("/ $goal steps", fontSize = 13.sp, color = Color.White.copy(0.7f),
                        modifier = Modifier.padding(bottom = 6.dp))
                }
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress   = { animProgress },
                    modifier   = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                    color      = Color.White,
                    trackColor = Color.White.copy(0.25f),
                    strokeCap  = StrokeCap.Round
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "$stepsPct% of daily goal",
                    fontSize = 11.sp,
                    color    = Color.White.copy(0.7f)
                )
            }
        }
    }
}

// ---------- Step Update Dialog ----------

@Composable
private fun StepUpdateDialog(current: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(if (current > 0) current.toString() else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Steps") },
        text = {
            OutlinedTextField(
                value         = text,
                onValueChange = { text = it.filter { c -> c.isDigit() } },
                label         = { Text("Steps walked today") },
                leadingIcon   = { Icon(Icons.Filled.DirectionsWalk, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            TextButton(onClick = { text.toIntOrNull()?.let { onConfirm(it) } }) {
                Text("Save", color = RosePrimary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ---------- Day Toggle Row ----------

@Composable
private fun DayToggleRow(
    isRestDay: Boolean,   isWalkDay: Boolean,   isPeriodDay: Boolean,
    onMarkRest: () -> Unit,   onRemoveRest: () -> Unit,
    onMarkWalk: () -> Unit,   onRemoveWalk: () -> Unit,
    onMarkPeriod: () -> Unit, onRemovePeriod: () -> Unit
) {
    Row(
        modifier            = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DayToggleChip(
            modifier    = Modifier.weight(1f),
            icon        = Icons.Filled.SelfImprovement,
            label       = "Rest Day",
            isActive    = isRestDay,
            activeColor = RestPurpleH,
            onClick     = { if (isRestDay) onRemoveRest() else onMarkRest() }
        )
        DayToggleChip(
            modifier    = Modifier.weight(1f),
            icon        = Icons.Filled.DirectionsWalk,
            label       = "Walk Day",
            isActive    = isWalkDay,
            activeColor = WalkBlueH,
            onClick     = { if (isWalkDay) onRemoveWalk() else onMarkWalk() }
        )
        DayToggleChip(
            modifier    = Modifier.weight(1f),
            icon        = Icons.Filled.WaterDrop,
            label       = "Period Day",
            isActive    = isPeriodDay,
            activeColor = PeriodRoseH,
            onClick     = { if (isPeriodDay) onRemovePeriod() else onMarkPeriod() }
        )
    }
}

@Composable
private fun DayToggleChip(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier  = modifier.clickable { onClick() },
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (isActive) activeColor else MaterialTheme.colorScheme.surface
        ),
        border    = if (!isActive) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.5f)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 4.dp else 1.dp)
    ) {
        Column(
            modifier              = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon, null,
                tint     = if (isActive) Color.White else activeColor.copy(0.6f),
                modifier = Modifier.size(26.dp)
            )
            Text(
                label,
                fontSize   = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color      = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface.copy(0.55f),
                textAlign  = TextAlign.Center
            )
        }
    }
}

// ---------- Rest Day Note Dialog ----------

@Composable
private fun RestDayNoteDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark Rest Day") },
        text = {
            OutlinedTextField(
                value         = note,
                onValueChange = { note = it },
                label         = { Text("Note (optional)") },
                placeholder   = { Text("e.g. Recovery, felt tired…") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(note) }) {
                Text("Save", color = RestPurpleH, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ---------- Period Day Note Dialog ----------

@Composable
private fun PeriodDayNoteDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Period Day") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "This helps FitTrack give you better advice around nutrition and recovery.",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                )
                OutlinedTextField(
                    value         = note,
                    onValueChange = { note = it },
                    label         = { Text("Note (optional)") },
                    placeholder   = { Text("e.g. Cramps, Light, Heavy…") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(note) }) {
                Text("Save", color = PeriodRoseH, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    // Preview is now broken because of ViewModel dependency, usually we'd pass a mock or interface
    // For now, just a comment to avoid compilation error if needed, but we can't easily show it
    // com.sunitha.fittrack.ui.theme.FitTrackTheme { HomeScreen(...) }
}
