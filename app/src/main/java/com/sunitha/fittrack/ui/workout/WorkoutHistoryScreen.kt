package com.sunitha.fittrack.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunitha.fittrack.data.db.entities.WorkoutSession
import com.sunitha.fittrack.data.db.entities.WorkoutSet
import com.sunitha.fittrack.ui.theme.GreenPrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val RestPurple  = Color(0xFF6A1B9A)
private val WalkBlue    = Color(0xFF0277BD)
private val PeriodRose  = Color(0xFFC2185B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHistoryScreen(vm: WorkoutHistoryViewModel, onBack: () -> Unit) {
    val items      by vm.historyItems.collectAsState()
    val allSets    by vm.allSets.collectAsState()
    val stepsByDay by vm.stepsByDay.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Filled.FitnessCenter, null,
                        modifier = Modifier.size(72.dp),
                        tint = GreenPrimary.copy(alpha = 0.3f)
                    )
                    Text(
                        "No activity logged yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                    )
                    Text(
                        "Your workouts, rest days and walks will appear here",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.4f)
                    )
                }
            }
        } else {
            val dayGroups = remember(items) {
                items.groupBy { item ->
                    val millis = when (item) {
                        is HistoryItem.Workout   -> item.session.dateMillis
                        is HistoryItem.RestDay   -> item.entry.dateMillis
                        is HistoryItem.WalkDay   -> item.entry.dateMillis
                        is HistoryItem.PeriodDay -> item.entry.dateMillis
                    }
                    dayKey(millis)
                }.entries.sortedByDescending { (_, dayItems) ->
                    when (val first = dayItems.first()) {
                        is HistoryItem.Workout   -> first.session.dateMillis
                        is HistoryItem.RestDay   -> first.entry.dateMillis
                        is HistoryItem.WalkDay   -> first.entry.dateMillis
                        is HistoryItem.PeriodDay -> first.entry.dateMillis
                    }
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(dayGroups, key = { it.key }) { (_, dayItems) ->
                    DayHistoryCard(
                        dayItems          = dayItems,
                        allSets           = allSets,
                        stepsByDay        = stepsByDay,
                        onDeleteSessions  = { vm.deleteSessions(it) },
                        onEditSets        = { vm.upsertWorkoutSets(it) },
                        onEditMuscleGroup = { ids, name -> vm.updateSessionsMuscleGroup(ids, name) }
                    )
                }
            }
        }
    }
}

// ── Combined day card ─────────────────────────────────────────────────────────

@Composable
internal fun DayHistoryCard(
    dayItems: List<HistoryItem>,
    allSets: List<WorkoutSet>,
    stepsByDay: Map<Long, Int>,
    onDeleteSessions: (List<Long>) -> Unit,
    onEditSets: (List<WorkoutSet>) -> Unit,
    onEditMuscleGroup: (List<Long>, String) -> Unit
) {
    val workout   = dayItems.filterIsInstance<HistoryItem.Workout>().firstOrNull()
    val restDay   = dayItems.filterIsInstance<HistoryItem.RestDay>().firstOrNull()
    val walkDay   = dayItems.filterIsInstance<HistoryItem.WalkDay>().firstOrNull()
    val periodDay = dayItems.filterIsInstance<HistoryItem.PeriodDay>().firstOrNull()

    val sets             = workout?.let { allSets.filter { s -> s.sessionId in it.allSessionIds } } ?: emptyList()
    val cardioSessionIds = workout?.allSessions?.filter { it.muscleGroup == "Cardio" }?.map { it.id }?.toSet() ?: emptySet()
    val byExercise       = sets.groupBy { it.exerciseName }
    val dateMillis       = workout?.session?.dateMillis ?: restDay?.entry?.dateMillis
                          ?: walkDay?.entry?.dateMillis ?: periodDay?.entry?.dateMillis ?: return
    val daySteps         = stepsByDay[startOfDay(dateMillis)] ?: 0

    var expanded         by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditSheet    by remember { mutableStateOf(false) }
    val accentColor = when {
        workout != null  -> GreenPrimary
        restDay != null  -> RestPurple
        walkDay != null  -> WalkBlue
        else             -> PeriodRose
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(accentColor.copy(alpha = 0.7f)))
            Column(modifier = Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // ── Workout section ──────────────────────────────────────────────
            if (workout != null) {
                Row(
                    modifier              = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ActivityIcon(icon = Icons.Filled.FitnessCenter, color = GreenPrimary)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(workout.session.muscleGroup, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            buildString {
                                append(formatDate(dateMillis))
                                if (daySteps > 0) append("  ·  $daySteps steps")
                            },
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatPill("${workout.session.totalSets} sets", GreenPrimary.copy(0.7f))
                            StatPill("${byExercise.size} exercises", GreenPrimary.copy(0.5f))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                        IconButton(onClick = { showEditSheet = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Edit, null, tint = GreenPrimary.copy(0.6f), modifier = Modifier.size(17.dp))
                        }
                        IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.35f), modifier = Modifier.size(17.dp))
                        }
                        Icon(
                            if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            null, tint = GreenPrimary.copy(0.6f), modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (expanded && byExercise.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
                    byExercise.forEach { (exerciseName, exerciseSets) ->
                        val isCardio = exerciseSets.firstOrNull()?.sessionId in cardioSessionIds
                        Column(modifier = Modifier.padding(bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(exerciseName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = GreenPrimary)
                            exerciseSets.forEach { s ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = RoundedCornerShape(4.dp), color = GreenPrimary.copy(0.08f)) {
                                        Text("Set ${s.setNumber}", fontSize = 11.sp, color = GreenPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                    val detail = if (isCardio) "${s.reps} min"
                                    else {
                                        val wt = if (s.weightKg > 0f) "${"%.1f".format(s.weightKg)} kg" else "Bodyweight"
                                        "${s.reps} reps  ×  $wt"
                                    }
                                    Text(detail, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.75f))
                                }
                            }
                        }
                    }
                }

                if (restDay != null || walkDay != null || periodDay != null) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.25f))
                }
            }

            // ── Rest day row ─────────────────────────────────────────────────
            restDay?.let { rest ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(RestPurple.copy(0.12f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.SelfImprovement, null, tint = RestPurple, modifier = Modifier.size(20.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Rest Day", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        if (workout == null) Text(formatDate(rest.entry.dateMillis), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                        if (rest.entry.note.isNotBlank()) Text(rest.entry.note, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                    }
                    ActivityBadge("Recovery", RestPurple)
                }
            }

            // ── Walk day row ─────────────────────────────────────────────────
            walkDay?.let { walk ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(WalkBlue.copy(0.12f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.DirectionsWalk, null, tint = WalkBlue, modifier = Modifier.size(20.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Walk Day", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        if (workout == null) Text(formatDate(walk.entry.dateMillis), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                    }
                    if (walk.entry.steps > 0) StatPill("${walk.entry.steps} steps", WalkBlue)
                }
            }

            // ── Period day row ───────────────────────────────────────────────
            periodDay?.let { period ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(PeriodRose.copy(0.12f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.WaterDrop, null, tint = PeriodRose, modifier = Modifier.size(20.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Period Day", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        if (workout == null) Text(formatDate(period.entry.dateMillis), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                        if (period.entry.note.isNotBlank()) Text(period.entry.note, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                    }
                    ActivityBadge("Period", PeriodRose)
                }
            }
        }
        }
    }

    if (showDeleteDialog && workout != null) {
        DeleteWorkoutConfirmDialog(
            muscleGroup = workout.session.muscleGroup,
            onConfirm   = { onDeleteSessions(workout.allSessionIds); showDeleteDialog = false },
            onDismiss   = { showDeleteDialog = false }
        )
    }
    if (showEditSheet && workout != null) {
        EditWorkoutSheet(
            session          = workout.session,
            sets             = sets,
            cardioSessionIds = cardioSessionIds,
            onDismiss        = { showEditSheet = false },
            onSave           = { newGroup, updated ->
                onEditMuscleGroup(workout.allSessionIds, newGroup)
                onEditSets(updated)
                showEditSheet = false
            }
        )
    }
}

// ── Workout card ──────────────────────────────────────────────────────────────

internal fun startOfDay(millis: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
    }.timeInMillis

@Composable
private fun WorkoutHistoryCard(
    item: HistoryItem.Workout,
    sets: List<WorkoutSet>,
    stepsByDay: Map<Long, Int> = emptyMap(),
    onDelete: () -> Unit,
    onEditSets: (List<WorkoutSet>) -> Unit,
    onEditMuscleGroup: (String) -> Unit = {}
) {
    val session          = item.session
    val cardioSessionIds = item.allSessions.filter { it.muscleGroup == "Cardio" }.map { it.id }.toSet()
    var expanded         by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditSheet    by remember { mutableStateOf(false) }
    val byExercise       = sets.groupBy { it.exerciseName }
    val daySteps         = stepsByDay[startOfDay(session.dateMillis)] ?: 0

    Card(
        modifier  = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ActivityIcon(icon = Icons.Filled.FitnessCenter, color = GreenPrimary)

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(session.muscleGroup, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        buildString {
                            append(formatDate(session.dateMillis))
                            if (daySteps > 0) append("  ·  $daySteps steps")
                        },
                        fontSize = 12.sp,
                        color    = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatPill("${session.totalSets} sets", GreenPrimary.copy(0.7f))
                        StatPill("${byExercise.size} exercises", GreenPrimary.copy(0.5f))
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    IconButton(onClick = { showEditSheet = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Edit, null, tint = GreenPrimary.copy(0.6f), modifier = Modifier.size(17.dp))
                    }
                    IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.35f), modifier = Modifier.size(17.dp))
                    }
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        null,
                        tint     = GreenPrimary.copy(0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (expanded && byExercise.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
                Spacer(Modifier.height(10.dp))
                byExercise.forEach { (exerciseName, exerciseSets) ->
                    val isCardio = exerciseSets.firstOrNull()?.sessionId in cardioSessionIds
                    Column(
                        modifier            = Modifier.padding(bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(exerciseName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = GreenPrimary)
                        exerciseSets.forEach { s ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Surface(shape = RoundedCornerShape(4.dp), color = GreenPrimary.copy(0.08f)) {
                                    Text(
                                        "Set ${s.setNumber}",
                                        fontSize = 11.sp,
                                        color    = GreenPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                val detail = if (isCardio) "${s.reps} min"
                                else {
                                    val wt = if (s.weightKg > 0f) "${"%.1f".format(s.weightKg)} kg" else "Bodyweight"
                                    "${s.reps} reps  ×  $wt"
                                }
                                Text(detail, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.75f))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        DeleteWorkoutConfirmDialog(
            muscleGroup = session.muscleGroup,
            onConfirm   = { onDelete(); showDeleteDialog = false },
            onDismiss   = { showDeleteDialog = false }
        )
    }
    if (showEditSheet) {
        EditWorkoutSheet(
            session          = session,
            sets             = sets,
            cardioSessionIds = cardioSessionIds,
            onDismiss        = { showEditSheet = false },
            onSave           = { newGroup, updated ->
                onEditMuscleGroup(newGroup)
                onEditSets(updated)
                showEditSheet = false
            }
        )
    }
}

// ── Rest Day card ─────────────────────────────────────────────────────────────

@Composable
private fun RestDayHistoryCard(item: HistoryItem.RestDay) {
    val entry = item.entry
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ActivityIcon(icon = Icons.Filled.SelfImprovement, color = RestPurple)

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Rest Day", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    formatDate(entry.dateMillis),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                )
                if (entry.note.isNotBlank()) {
                    Text(entry.note, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.55f))
                }
            }

            ActivityBadge("Recovery", RestPurple)
        }
    }
}

// ── Walk Day card ─────────────────────────────────────────────────────────────

@Composable
private fun WalkDayHistoryCard(item: HistoryItem.WalkDay) {
    val entry = item.entry
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ActivityIcon(icon = Icons.Filled.DirectionsWalk, color = WalkBlue)

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Walk Day", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    formatDate(entry.dateMillis),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                )
                if (entry.steps > 0) {
                    StatPill("${entry.steps} steps", WalkBlue)
                }
            }

            ActivityBadge("Walk", WalkBlue)
        }
    }
}

// ── Period Day card ───────────────────────────────────────────────────────────

@Composable
private fun PeriodDayHistoryCard(item: HistoryItem.PeriodDay) {
    val entry = item.entry
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ActivityIcon(icon = Icons.Filled.WaterDrop, color = PeriodRose)

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Period Day", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    formatDate(entry.dateMillis),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                )
                if (entry.note.isNotBlank()) {
                    Text(entry.note, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.55f))
                }
            }

            ActivityBadge("Period", PeriodRose)
        }
    }
}

// ── Shared components ─────────────────────────────────────────────────────────

@Composable
private fun ActivityIcon(icon: ImageVector, color: Color) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun StatPill(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun ActivityBadge(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(millis))
