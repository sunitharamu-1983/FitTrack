package com.sunitha.fittrack.ui.workout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunitha.fittrack.data.db.entities.WorkoutSession
import com.sunitha.fittrack.data.db.entities.WorkoutSet
import com.sunitha.fittrack.data.local.ExerciseTemplate
import com.sunitha.fittrack.ui.theme.GreenPrimary
import com.sunitha.fittrack.ui.theme.OrangeAccent
import java.util.*

@Composable
fun WorkoutScreen(vm: WorkoutViewModel) {
    val step              by vm.step.collectAsState()
    val muscleGroup       by vm.selectedMuscleGroup.collectAsState()
    val selectedExercises by vm.selectedExercises.collectAsState()

    when (step) {
        WorkoutStep.HISTORY          -> HistoryTab(vm)
        WorkoutStep.SELECT_MUSCLE    -> MuscleGroupSelector(vm)
        WorkoutStep.SELECT_EXERCISES -> ExerciseSelector(vm, muscleGroup ?: "", selectedExercises)
        WorkoutStep.LOG_WORKOUT      -> LogWorkout(vm)
    }
}

// ── Workouts Tab ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryTab(vm: WorkoutViewModel) {
    val historyItems by vm.historyItems.collectAsState()
    val allSets      by vm.allSets.collectAsState()
    val stepsByDay   by vm.stepsByDay.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workouts", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick        = vm::showMuscleGroupSelector,
                icon           = { Icon(Icons.Filled.Add, null) },
                text           = { Text("Log Workout") },
                containerColor = GreenPrimary,
                contentColor   = Color.White
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        if (historyItems.isEmpty()) {
            androidx.compose.foundation.layout.Box(
                modifier          = Modifier.fillMaxSize().padding(pad),
                contentAlignment  = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.FitnessCenter, null, modifier = Modifier.size(72.dp), tint = GreenPrimary.copy(0.3f))
                    Text("No activity yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                    Text("Tap Log Workout to get started", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                }
            }
        } else {
            val dayGroups = remember(historyItems) {
                historyItems.groupBy { item ->
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
                modifier            = Modifier.fillMaxSize().padding(pad),
                contentPadding      = PaddingValues(16.dp),
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
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }
}

// ── Today Workout Card ────────────────────────────────────────────────────────

@Composable
private fun TodayWorkoutCard(
    session: WorkoutSession,
    sets: List<WorkoutSet>,
    cardioSessionIds: Set<Long> = emptySet(),
    onDelete: () -> Unit,
    onEditSets: (List<WorkoutSet>) -> Unit,
    onEditMuscleGroup: (String) -> Unit = {}
) {
    val byExercise = sets.groupBy { it.exerciseName }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditSheet    by remember { mutableStateOf(false) }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier         = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(GreenPrimary.copy(0.12f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.FitnessCenter, null, tint = GreenPrimary, modifier = Modifier.size(22.dp)) }
                Column(modifier = Modifier.weight(1f)) {
                    Text(session.muscleGroup, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("${sets.size} sets logged today", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                }
                IconButton(onClick = { showEditSheet = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Edit, null, tint = GreenPrimary.copy(0.6f), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.35f), modifier = Modifier.size(18.dp))
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
            byExercise.forEach { (exerciseName, exerciseSets) ->
                val exIsCardio = exerciseSets.firstOrNull()?.sessionId in cardioSessionIds
                ExerciseLogRow(exerciseName, exerciseSets, isCardio = exIsCardio)
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

@Composable
private fun ExerciseLogRow(exerciseName: String, sets: List<WorkoutSet>, isCardio: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(exerciseName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = GreenPrimary)
        sets.forEach { s ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(4.dp), color = GreenPrimary.copy(0.08f)) {
                    Text(
                        "Set ${s.setNumber}",
                        fontSize  = 11.sp,
                        color     = GreenPrimary,
                        modifier  = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                val detail = if (isCardio) {
                    "${s.reps} min"
                } else {
                    val weightText = if (s.weightKg > 0f) "${"%.1f".format(s.weightKg)} kg" else "BW"
                    "${s.reps} reps × $weightText"
                }
                Text(detail, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
            }
        }
    }
}

// ── Muscle Group Selector ──────────────────────────────────────────────────────

private val muscleGroups = listOf(
    Triple("Chest & Triceps",  Icons.Filled.FitnessCenter, Color(0xFF1565C0)),
    Triple("Back & Biceps",    Icons.Filled.FitnessCenter, Color(0xFF2E7D32)),
    Triple("Legs",             Icons.Filled.DirectionsRun, Color(0xFF6A1B9A)),
    Triple("Glutes",           Icons.Filled.FitnessCenter, Color(0xFFAD1457)),
    Triple("Shoulders",        Icons.Filled.FitnessCenter, Color(0xFFE65100)),
    Triple("Full Body",        Icons.Filled.FlashOn,       Color(0xFFC62828)),
    Triple("Abs",              Icons.Filled.FitnessCenter, Color(0xFF00838F)),
    Triple("Cardio",           Icons.Filled.DirectionsRun, Color(0xFF558B2F))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MuscleGroupSelector(vm: WorkoutViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose Muscle Group", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = vm::goBack) { Icon(Icons.Filled.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(muscleGroups) { (name, icon, color) ->
                MuscleGroupCard(name, icon, color, onClick = { vm.selectMuscleGroup(name) })
            }
        }
    }
}

@Composable
private fun MuscleGroupCard(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(50.dp).clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            }
            Text(name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.4f))
        }
    }
}

// ── Exercise Selector ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseSelector(
    vm: WorkoutViewModel,
    muscleGroup: String,
    selected: List<ExerciseTemplate>
) {
    val exercises    by vm.availableExercises.collectAsState()
    val aiSuggestions by vm.aiSuggestions.collectAsState()
    val isLoadingAi  by vm.isLoadingAi.collectAsState()
    val aiError      by vm.aiError.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(muscleGroup, fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = vm::goBack) { Icon(Icons.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            if (selected.isNotEmpty()) {
                Surface(shadowElevation = 8.dp) {
                    Button(
                        onClick = vm::startWorkout,
                        modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Start Workout (${selected.size} exercises)", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Custom exercise first
            item {
                var showCustomDialog by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick  = { showCustomDialog = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = OrangeAccent)
                ) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add Custom Exercise")
                }
                if (showCustomDialog) {
                    CustomExerciseDialog(
                        onConfirm = { name, sets, reps, weight ->
                            vm.addCustomExercise(name, sets, reps, weight)
                            showCustomDialog = false
                        },
                        onDismiss = { showCustomDialog = false }
                    )
                }
            }

            // AI button
            item {
                OutlinedButton(
                    onClick = vm::loadAiSuggestions,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoadingAi,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary)
                ) {
                    if (isLoadingAi) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = GreenPrimary, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("AI is thinking…")
                    } else {
                        Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Ask AI to suggest exercises")
                    }
                }
            }

            // AI error
            if (aiError != null) item {
                Text(aiError!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            // AI suggestions
            if (aiSuggestions.isNotEmpty()) {
                item {
                    Text("AI Suggestions", fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                        color = GreenPrimary, modifier = Modifier.padding(top = 8.dp))
                }
                items(aiSuggestions) { ai ->
                    val alreadySelected = selected.any { it.name == ai.name }
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (!alreadySelected) vm.addAiExercise(ai)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (alreadySelected) GreenPrimary.copy(0.08f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        border = if (alreadySelected) BorderStroke(1.dp, GreenPrimary) else null,
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AutoAwesome, null, tint = GreenPrimary, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(ai.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                if (alreadySelected)
                                    Icon(Icons.Filled.CheckCircle, null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
                            }
                            Text("${ai.sets} sets × ${ai.reps}  ·  ${ai.weight}",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                            if (ai.tip.isNotBlank())
                                Text(ai.tip, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                                    modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
                item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            }

            // Standard exercise list
            item {
                Text("All Exercises", fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
            }
            items(exercises) { exercise ->
                val isSelected = selected.any { it.name == exercise.name }
                ExerciseCheckCard(exercise, isSelected) { vm.toggleExercise(exercise) }
            }
        }
    }
}

// ── Custom Exercise Dialog ─────────────────────────────────────────────────────

@Composable
private fun CustomExerciseDialog(onConfirm: (String, Int, String, String) -> Unit, onDismiss: () -> Unit) {
    var name   by remember { mutableStateOf("") }
    var sets   by remember { mutableStateOf("3") }
    var reps   by remember { mutableStateOf("10") }
    var weight by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Exercise name") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(12.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value           = sets,
                        onValueChange   = { sets = it.filter { c -> c.isDigit() } },
                        label           = { Text("Sets") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine      = true,
                        modifier        = Modifier.weight(1f),
                        shape           = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value           = reps,
                        onValueChange   = { reps = it.filter { c -> c.isDigit() } },
                        label           = { Text("Reps") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine      = true,
                        modifier        = Modifier.weight(1f),
                        shape           = RoundedCornerShape(12.dp)
                    )
                }
                // Weight with +/- stepper and editable text field
                Text("Weight (kg)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val w = weight.toFloatOrNull() ?: 0f
                    FilledTonalIconButton(
                        onClick  = {
                            val v = (w - 0.5f).coerceAtLeast(0f)
                            weight = if (v == 0f) "" else "%.1f".format(v)
                        },
                        modifier = Modifier.size(40.dp),
                        colors   = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = GreenPrimary.copy(0.12f), contentColor = GreenPrimary)
                    ) { Icon(Icons.Filled.Remove, null) }
                    OutlinedTextField(
                        value           = weight,
                        onValueChange   = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                        placeholder     = { Text("0.0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine      = true,
                        modifier        = Modifier.weight(1f),
                        shape           = RoundedCornerShape(12.dp),
                        textStyle       = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
                    )
                    FilledTonalIconButton(
                        onClick  = {
                            val v = (w + 0.5f).coerceAtMost(200f)
                            weight = "%.1f".format(v)
                        },
                        modifier = Modifier.size(40.dp),
                        colors   = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = GreenPrimary.copy(0.12f), contentColor = GreenPrimary)
                    ) { Icon(Icons.Filled.Add, null) }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name, sets.toIntOrNull() ?: 3, reps.ifBlank { "10" }, weight) },
                enabled = name.isNotBlank()
            ) {
                Text("Add", color = GreenPrimary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ExerciseCheckCard(
    exercise: ExerciseTemplate,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onToggle() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) GreenPrimary.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = GreenPrimary)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                if (exercise.muscleGroup == "Cardio") {
                    Text("Duration: ${exercise.defaultReps}",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.55f))
                } else {
                    Text("${exercise.defaultSets} sets × ${exercise.defaultReps}  ·  ${exercise.suggestedWeight}",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.55f))
                }
            }
        }
    }
}

// ── Log Workout ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogWorkout(vm: WorkoutViewModel) {
    val activeExercises by vm.activeExercises.collectAsState()
    val isSaving        by vm.isSaving.collectAsState()
    val muscleGroup     by vm.selectedMuscleGroup.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(muscleGroup ?: "Workout", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = vm::goBack) { Icon(Icons.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = vm::finishWorkout,
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    else { Icon(Icons.Filled.Done, null); Spacer(Modifier.width(8.dp)); Text("Finish Workout", fontWeight = FontWeight.SemiBold) }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(activeExercises) { ae ->
                ActiveExerciseCard(ae = ae, vm = vm)
            }
        }
    }
}

@Composable
private fun ActiveExerciseCard(ae: ActiveExercise, vm: WorkoutViewModel) {
    val isCardio = ae.template.muscleGroup == "Cardio"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(GreenPrimary.copy(0.12f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.FitnessCenter, null, tint = GreenPrimary, modifier = Modifier.size(18.dp)) }
                Spacer(Modifier.width(10.dp))
                Text(ae.template.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))

            // Header row
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Set", modifier = Modifier.width(36.dp), fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f), fontWeight = FontWeight.Medium)
                Text(
                    if (isCardio) "Duration (min)" else "Reps",
                    modifier = Modifier.weight(1f), fontSize = 12.sp, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f), fontWeight = FontWeight.Medium)
                if (!isCardio) {
                    Text("Weight kg  −/+", modifier = Modifier.weight(1.2f), fontSize = 12.sp, textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.5f), fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.width(32.dp))
            }
            Spacer(Modifier.height(6.dp))

            ae.sets.forEachIndexed { idx, setLog ->
                SetRow(
                    setLog         = setLog,
                    isCardio       = isCardio,
                    onRepsChange   = { vm.updateReps(ae.template.name, idx, it) },
                    onWeightChange = { vm.updateWeight(ae.template.name, idx, it) },
                    onRemove       = { if (ae.sets.size > 1) vm.removeSet(ae.template.name, idx) }
                )
            }

            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = { vm.addSet(ae.template.name) },
                colors  = ButtonDefaults.textButtonColors(contentColor = GreenPrimary)
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Set", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SetRow(
    setLog: SetLog,
    isCardio: Boolean,
    onRepsChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(GreenPrimary.copy(0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text("${setLog.setNumber}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
        }
        Spacer(Modifier.width(8.dp))

        // Reps / Duration field
        OutlinedTextField(
            value         = setLog.reps,
            onValueChange = { v ->
                if (isCardio) {
                    val num = v.filter { it.isDigit() }
                    val capped = num.toIntOrNull()?.coerceAtMost(30)?.toString() ?: num
                    onRepsChange(capped)
                } else {
                    onRepsChange(v)
                }
            },
            modifier        = Modifier.weight(1f).height(52.dp),
            placeholder     = { Text(if (isCardio) "5–30" else "12", fontSize = 13.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine      = true,
            textStyle       = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 14.sp),
            shape           = RoundedCornerShape(8.dp)
        )

        // Weight stepper — hidden for cardio
        if (!isCardio) {
            Spacer(Modifier.width(6.dp))
            WeightStepper(
                weightStr     = setLog.weightKg,
                onWeightChange = onWeightChange,
                modifier      = Modifier.weight(1.2f)
            )
        }

        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Close, null,
                tint     = MaterialTheme.colorScheme.onSurface.copy(0.4f),
                modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun WeightStepper(weightStr: String, onWeightChange: (String) -> Unit, modifier: Modifier) {
    val weight = weightStr.toFloatOrNull() ?: 0f
    Row(
        modifier              = modifier,
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        FilledTonalIconButton(
            onClick  = {
                val v = (weight - 0.5f).coerceAtLeast(0f)
                onWeightChange(if (v == 0f) "" else "%.1f".format(v))
            },
            modifier = Modifier.size(30.dp),
            colors   = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = GreenPrimary.copy(0.12f), contentColor = GreenPrimary)
        ) { Icon(Icons.Filled.Remove, null, modifier = Modifier.size(14.dp)) }

        OutlinedTextField(
            value         = weightStr,
            onValueChange = { v ->
                val filtered = v.filter { it.isDigit() || it == '.' }
                onWeightChange(filtered)
            },
            modifier      = Modifier.weight(1f).height(52.dp),
            placeholder   = { Text("kg", fontSize = 12.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine    = true,
            textStyle     = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 14.sp),
            shape         = RoundedCornerShape(8.dp)
        )

        FilledTonalIconButton(
            onClick  = {
                val v = (weight + 0.5f).coerceAtMost(200f)
                onWeightChange("%.1f".format(v))
            },
            modifier = Modifier.size(30.dp),
            colors   = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = GreenPrimary.copy(0.12f), contentColor = GreenPrimary)
        ) { Icon(Icons.Filled.Add, null, modifier = Modifier.size(14.dp)) }
    }
}

// ── Shared workout edit/delete composables ────────────────────────────────────

@Composable
fun DeleteWorkoutConfirmDialog(muscleGroup: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("Delete workout?") },
        text    = { Text("Remove the $muscleGroup session? This cannot be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private class EditSetRow(
    val originalId: Long,
    val exerciseName: String,
    val sessionId: Long,
    val reps: androidx.compose.runtime.MutableState<String>,
    val weight: androidx.compose.runtime.MutableState<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWorkoutSheet(
    session: WorkoutSession,
    sets: List<WorkoutSet>,
    cardioSessionIds: Set<Long> = emptySet(),
    onDismiss: () -> Unit,
    onSave: (newMuscleGroup: String, List<WorkoutSet>) -> Unit
) {
    val editRows = remember(sets) {
        androidx.compose.runtime.snapshots.SnapshotStateList<EditSetRow>().also { list ->
            sets.forEach { s ->
                list.add(EditSetRow(
                    originalId   = s.id,
                    exerciseName = s.exerciseName,
                    sessionId    = s.sessionId,
                    reps         = mutableStateOf(s.reps.toString()),
                    weight       = mutableStateOf(if (s.weightKg > 0f) "%.1f".format(s.weightKg) else "")
                ))
            }
        }
    }
    val editExerciseNames = remember(sets) {
        sets.map { it.exerciseName }.distinct()
            .associateWith { mutableStateOf(it) }
            .toMutableMap()
    }
    var muscleGroupName  by remember { mutableStateOf(session.muscleGroup) }
    var expandedExercise by remember { mutableStateOf(editRows.firstOrNull()?.exerciseName) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Edit Workout", fontWeight = FontWeight.Bold, fontSize = 18.sp)

            OutlinedTextField(
                value         = muscleGroupName,
                onValueChange = { muscleGroupName = it },
                label         = { Text("Workout Name") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(10.dp)
            )

            val byExercise = editRows.groupBy { it.exerciseName }

            if (byExercise.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
                Text("Tap an exercise to expand and edit",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.45f))
            }

            byExercise.forEach { (exerciseName, exerciseRows) ->
                val nameState = editExerciseNames.getOrPut(exerciseName) { mutableStateOf(exerciseName) }
                val isExpanded = expandedExercise == exerciseName
                val isCardioExercise = if (cardioSessionIds.isEmpty()) session.muscleGroup == "Cardio"
                                       else exerciseRows.firstOrNull()?.sessionId in cardioSessionIds

                Card(
                    shape  = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isExpanded) GreenPrimary.copy(0.06f)
                                         else MaterialTheme.colorScheme.surfaceVariant.copy(0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier            = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier              = Modifier.fillMaxWidth().clickable {
                                expandedExercise = if (isExpanded) null else exerciseName
                            },
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                exerciseName,
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 14.sp,
                                color      = GreenPrimary,
                                modifier   = Modifier.weight(1f)
                            )
                            Surface(shape = RoundedCornerShape(4.dp), color = GreenPrimary.copy(0.08f)) {
                                Text(
                                    "${exerciseRows.size} sets",
                                    fontSize = 11.sp,
                                    color    = GreenPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Icon(
                                if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                null,
                                tint     = GreenPrimary.copy(0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (isExpanded) {
                            OutlinedTextField(
                                value         = nameState.value,
                                onValueChange = { nameState.value = it },
                                label         = { Text("Exercise Name") },
                                singleLine    = true,
                                modifier      = Modifier.fillMaxWidth(),
                                shape         = RoundedCornerShape(10.dp)
                            )

                            exerciseRows.forEachIndexed { rowIdx, row ->
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = GreenPrimary.copy(0.08f)) {
                                        Text(
                                            "Set ${rowIdx + 1}",
                                            fontSize = 12.sp,
                                            color    = GreenPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    OutlinedTextField(
                                        value           = row.reps.value,
                                        onValueChange   = { row.reps.value = it.filter { c -> c.isDigit() } },
                                        label           = { Text(if (isCardioExercise) "Duration (min)" else "Reps") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine      = true,
                                        modifier        = Modifier.weight(1f),
                                        shape           = RoundedCornerShape(10.dp)
                                    )
                                    if (!isCardioExercise) {
                                        OutlinedTextField(
                                            value           = row.weight.value,
                                            onValueChange   = { row.weight.value = it.filter { c -> c.isDigit() || c == '.' } },
                                            label           = { Text("kg") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine      = true,
                                            modifier        = Modifier.weight(1f),
                                            shape           = RoundedCornerShape(10.dp)
                                        )
                                    }
                                    // Delete set (only if more than 1 set remains for this exercise)
                                    if (exerciseRows.size > 1) {
                                        IconButton(
                                            onClick  = { editRows.remove(row) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Delete, null,
                                                tint     = MaterialTheme.colorScheme.onSurface.copy(0.35f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Add Set button
                            TextButton(
                                onClick = {
                                    val sessionId = exerciseRows.firstOrNull()?.sessionId ?: 0L
                                    editRows.add(EditSetRow(
                                        originalId   = 0L,
                                        exerciseName = exerciseName,
                                        sessionId    = sessionId,
                                        reps         = mutableStateOf(""),
                                        weight       = mutableStateOf("")
                                    ))
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = GreenPrimary)
                            ) {
                                Icon(Icons.Filled.Add, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add Set", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val updated = editRows
                        .groupBy { it.exerciseName }
                        .flatMap { (exName, exRows) ->
                            val newName = editExerciseNames[exName]?.value?.trim()?.ifBlank { exName } ?: exName
                            exRows.mapIndexed { idx, row ->
                                if (row.originalId != 0L) {
                                    sets.first { it.id == row.originalId }.copy(
                                        exerciseName = newName,
                                        setNumber    = idx + 1,
                                        reps         = row.reps.value.toIntOrNull() ?: 0,
                                        weightKg     = row.weight.value.toFloatOrNull() ?: 0f
                                    )
                                } else {
                                    WorkoutSet(
                                        id           = 0L,
                                        sessionId    = row.sessionId,
                                        exerciseName = newName,
                                        setNumber    = idx + 1,
                                        reps         = row.reps.value.toIntOrNull() ?: 0,
                                        weightKg     = row.weight.value.toFloatOrNull() ?: 0f
                                    )
                                }
                            }
                        }
                    onSave(muscleGroupName.trim().ifBlank { session.muscleGroup }, updated)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape    = RoundedCornerShape(14.dp)
            ) { Text("Save Changes", fontWeight = FontWeight.SemiBold) }
        }
    }
}
