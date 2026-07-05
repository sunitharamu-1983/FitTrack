package com.sunitha.fittrack.ui.nutrition

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunitha.fittrack.ai.ClaudeApiService
import com.sunitha.fittrack.data.db.entities.FoodEntry
import com.sunitha.fittrack.data.local.IndianFood
import com.sunitha.fittrack.data.local.IndianFoodData
import com.sunitha.fittrack.ui.theme.GreenPrimary
import com.sunitha.fittrack.ui.theme.OrangeAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(vm: NutritionViewModel) {
    val entries       by vm.todayEntries.collectAsState()
    val calories      by vm.totalCalories.collectAsState()
    val protein       by vm.totalProtein.collectAsState()
    val carbs         by vm.totalCarbs.collectAsState()
    val fat           by vm.totalFat.collectAsState()
    val fiber         by vm.totalFiber.collectAsState()
    val showSheet     by vm.showAddSheet.collectAsState()
    val pendingFood   by vm.pendingFood.collectAsState()
    val mealType      by vm.selectedMealType.collectAsState()
    val searchQuery   by vm.searchQuery.collectAsState()
    val searchResults by vm.searchResults.collectAsState()
    val editingEntry  by vm.editingEntry.collectAsState()
    val macros        by vm.macros.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nutrition", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { vm.openAddSheet() },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Log Food") },
                containerColor = OrangeAccent,
                contentColor = Color.White
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Summary card
            item {
                DailySummaryCard(
                    calories    = calories,
                    protein     = protein,
                    carbs       = carbs,
                    fat         = fat,
                    fiber       = fiber,
                    calorieGoal = macros.calories,
                    proteinGoal = macros.proteinG,
                    carbsGoal   = macros.carbsG,
                    fatGoal     = macros.fatG,
                    fiberGoal   = macros.fiberG
                )
            }

            // Group by meal type
            vm.mealTypes.forEach { meal ->
                val mealEntries = entries.filter { it.mealType == meal }
                if (mealEntries.isNotEmpty()) {
                    item { MealSectionHeader(meal, mealEntries.sumOf { it.calories }) }
                    items(mealEntries) { entry ->
                        FoodEntryRow(
                            entry    = entry,
                            onEdit   = { vm.startEditing(entry) },
                            onDelete = { vm.deleteEntry(entry) }
                        )
                    }
                }
            }

            if (entries.isEmpty()) item {
                Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Restaurant, null, modifier = Modifier.size(60.dp), tint = OrangeAccent.copy(0.3f))
                        Spacer(Modifier.height(8.dp))
                        Text("No meals logged today", color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                        Text("Tap + Log Food to start", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                    }
                }
            }
        }
    }

    // Bottom sheet to add food
    if (showSheet) {
        AddFoodSheet(
            vm            = vm,
            pendingFood   = pendingFood,
            selectedMeal  = mealType,
            searchQuery   = searchQuery,
            searchResults = searchResults,
            onDismiss     = vm::closeAddSheet
        )
    }

    // Bottom sheet to edit existing entry
    editingEntry?.let { entry ->
        EditFoodSheet(entry = entry, mealTypes = vm.mealTypes, onDismiss = vm::stopEditing) {
            foodName, servingDesc, meal, servings, cal, prot, carbs2, fat2, fiber2 ->
            vm.saveEdit(entry, foodName, servingDesc, meal, servings, cal, prot, carbs2, fat2, fiber2)
        }
    }
}

// ── Daily Summary Card ─────────────────────────────────────────────────────────

@Composable
private fun DailySummaryCard(
    calories: Int, protein: Float, carbs: Float, fat: Float, fiber: Float,
    calorieGoal: Int, proteinGoal: Float, carbsGoal: Float, fatGoal: Float, fiberGoal: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFFBF360C), OrangeAccent)))
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Today's Nutrition", color = Color.White.copy(0.8f), fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "$calories",
                                color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                " / $calorieGoal kcal",
                                color = Color.White.copy(0.75f), fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 5.dp)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Restaurant, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroChip("Protein", "${"%.0f".format(protein)}g", "/${proteinGoal.toInt()}g", Color(0xFF1565C0), if (proteinGoal > 0f) protein / proteinGoal else 0f)
                MacroChip("Carbs", "${"%.0f".format(carbs)}g", "/${carbsGoal.toInt()}g", OrangeAccent, if (carbsGoal > 0f) carbs / carbsGoal else 0f)
                MacroChip("Fat", "${"%.0f".format(fat)}g", "/${fatGoal.toInt()}g", Color(0xFFC62828), if (fatGoal > 0f) fat / fatGoal else 0f)
                MacroChip("Fiber", "${"%.0f".format(fiber)}g", "/${fiberGoal.toInt()}g", GreenPrimary, if (fiberGoal > 0f) fiber / fiberGoal else 0f)
            }
        }
    }
}

@Composable
private fun MacroChip(label: String, value: String, goal: String, color: Color, fraction: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
        Text(goal, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier.width(56.dp).height(4.dp).clip(CircleShape),
            color = color,
            trackColor = color.copy(0.15f),
            strokeCap = StrokeCap.Round
        )
    }
}

// ── Meal Section ───────────────────────────────────────────────────────────────

@Composable
private fun MealSectionHeader(meal: String, totalCal: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(meal, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Text("$totalCal kcal", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
    }
}

@Composable
internal fun FoodEntryRow(entry: FoodEntry, onEdit: () -> Unit, onDelete: () -> Unit) {
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
                modifier = Modifier.size(38.dp).clip(CircleShape).background(OrangeAccent.copy(0.1f)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Restaurant, null, tint = OrangeAccent, modifier = Modifier.size(18.dp)) }
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.foodName, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text("${entry.servingDescription}  ·  P:${"%.0f".format(entry.proteinG)}g  C:${"%.0f".format(entry.carbsG)}g  F:${"%.0f".format(entry.fatG)}g",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
            }
            Text("${entry.calories}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = OrangeAccent)
            IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Edit, null, tint = OrangeAccent.copy(0.5f), modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.3f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ── Add Food Bottom Sheet ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddFoodSheet(
    vm: NutritionViewModel,
    pendingFood: IndianFood?,
    selectedMeal: String,
    searchQuery: String,
    searchResults: List<IndianFood>,
    onDismiss: () -> Unit
) {
    var servings by remember { mutableStateOf("1") }
    var currentMeal by remember { mutableStateOf(selectedMeal) }
    val aiLookupState by vm.aiLookupState.collectAsState()

    ModalBottomSheet(onDismissRequest = onDismiss, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {

            if (pendingFood != null) {
                // Confirm add with servings
                Text("Add Food", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))
                Text(pendingFood.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text("${pendingFood.serving}  ·  ${pendingFood.calories} kcal  ·  P:${pendingFood.proteinG}g",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                Spacer(Modifier.height(16.dp))

                // Meal type selector
                Text("Meal", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    vm.mealTypes.forEach { meal ->
                        FilterChip(
                            selected = currentMeal == meal,
                            onClick  = { currentMeal = meal },
                            label    = { Text(meal, fontSize = 12.sp) },
                            colors   = FilterChipDefaults.filterChipColors(selectedContainerColor = OrangeAccent.copy(0.15f), selectedLabelColor = OrangeAccent)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = servings,
                    onValueChange = { servings = it },
                    label = { Text("Servings") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(16.dp))

                // Preview macros
                val s = servings.toFloatOrNull() ?: 1f
                Surface(shape = RoundedCornerShape(12.dp), color = OrangeAccent.copy(0.08f)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        MacroPreview("Calories", "${(pendingFood.calories * s).toInt()} kcal")
                        MacroPreview("Protein",  "${"%.1f".format(pendingFood.proteinG * s)}g")
                        MacroPreview("Carbs",    "${"%.1f".format(pendingFood.carbsG * s)}g")
                        MacroPreview("Fat",      "${"%.1f".format(pendingFood.fatG * s)}g")
                        MacroPreview("Fiber",    "${"%.1f".format(pendingFood.fiberG * s)}g")
                    }
                }
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = { vm.addFood(pendingFood, currentMeal, servings.toFloatOrNull() ?: 1f) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Add to $currentMeal", fontWeight = FontWeight.SemiBold) }

            } else {
                // Search screen
                Text("Log Food", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = vm::setSearchQuery,
                    label = { Text("Search Indian foods…") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(12.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(searchResults.take(30)) { food ->
                        FoodSearchRow(food, onClick = { vm.openAddSheet(food) })
                    }
                }

                // AI lookup section — shown when query is long enough
                if (searchQuery.length >= 2) {
                    Spacer(Modifier.height(10.dp))
                    when (aiLookupState) {
                        is AiFoodState.Idle -> {
                            OutlinedButton(
                                onClick = { vm.lookupFoodWithAi() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, OrangeAccent.copy(alpha = 0.6f))
                            ) {
                                Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(16.dp), tint = OrangeAccent)
                                Spacer(Modifier.width(8.dp))
                                Text("Not found? Ask AI for \"$searchQuery\"", color = OrangeAccent)
                            }
                        }
                        is AiFoodState.Loading -> {
                            Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CircularProgressIndicator(color = OrangeAccent, modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                                    Text("Asking AI about \"$searchQuery\"…", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                                }
                            }
                        }
                        is AiFoodState.Found -> {
                            val found = (aiLookupState as AiFoodState.Found).result
                            AiFoodResultCard(
                                result   = found,
                                onAdd    = { vm.addAiFood(found) },
                                onDismiss = { vm.clearAiLookup() }
                            )
                        }
                        is AiFoodState.Error -> {
                            val msg = (aiLookupState as AiFoodState.Error).message
                            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer.copy(0.5f)) {
                                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.WarningAmber, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(msg, fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                                    TextButton(onClick = { vm.clearAiLookup() }) {
                                        Text("OK", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroPreview(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = OrangeAccent)
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
    }
}

@Composable
private fun FoodSearchRow(food: IndianFood, onClick: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth().clickable { onClick() },
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier              = Modifier.padding(10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier         = Modifier.size(36.dp).clip(CircleShape).background(OrangeAccent.copy(0.1f)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Restaurant, null, tint = OrangeAccent, modifier = Modifier.size(16.dp)) }
            Column(modifier = Modifier.weight(1f)) {
                Text(food.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(
                    "${food.serving}  ·  P:${food.proteinG}g  C:${food.carbsG}g  F:${"%.1f".format(food.fiberG)}g",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${food.calories}", fontWeight = FontWeight.Bold, color = OrangeAccent)
                Text("kcal", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
            }
        }
    }
}

// ── Edit Food Bottom Sheet ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditFoodSheet(
    entry: FoodEntry,
    mealTypes: List<String>,
    onDismiss: () -> Unit,
    onSave: (foodName: String, servingDesc: String, mealType: String, servings: Float, calories: Int, proteinG: Float, carbsG: Float, fatG: Float, fiberG: Float) -> Unit
) {
    var foodName    by remember { mutableStateOf(entry.foodName) }
    var servingDesc by remember { mutableStateOf(entry.servingDescription) }
    var meal        by remember { mutableStateOf(entry.mealType) }
    var quantity    by remember {
        mutableStateOf(entry.servings.let { if (it == it.toInt().toFloat()) it.toInt().toString() else it.toString() })
    }

    // Per-serving base, derived from what's currently stored — editing quantity scales from here.
    val baseServings = if (entry.servings > 0f) entry.servings else 1f
    val baseCalories = entry.calories / baseServings
    val baseProtein  = entry.proteinG / baseServings
    val baseCarbs    = entry.carbsG   / baseServings
    val baseFat      = entry.fatG     / baseServings
    val baseFiber    = entry.fiberG   / baseServings

    val qty         = quantity.toFloatOrNull()?.takeIf { it > 0f }
    val newCalories = ((qty ?: baseServings) * baseCalories).toInt()
    val newProtein  = (qty ?: baseServings) * baseProtein
    val newCarbs    = (qty ?: baseServings) * baseCarbs
    val newFat      = (qty ?: baseServings) * baseFat
    val newFiber    = (qty ?: baseServings) * baseFiber

    val isValid = foodName.isNotBlank() && qty != null

    ModalBottomSheet(onDismissRequest = onDismiss, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Edit Food Entry", fontWeight = FontWeight.Bold, fontSize = 18.sp)

            OutlinedTextField(
                value         = foodName,
                onValueChange = { foodName = it },
                label         = { Text("Food name") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value         = servingDesc,
                onValueChange = { servingDesc = it },
                label         = { Text("Serving description") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(12.dp)
            )

            Text("Meal", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                mealTypes.forEach { m ->
                    FilterChip(
                        selected = meal == m,
                        onClick  = { meal = m },
                        label    = { Text(m, fontSize = 12.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OrangeAccent.copy(0.15f),
                            selectedLabelColor     = OrangeAccent
                        )
                    )
                }
            }

            OutlinedTextField(
                value         = quantity,
                onValueChange = { quantity = it },
                label         = { Text("Quantity (servings)") },
                isError       = qty == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(12.dp)
            )

            // Macros recompute automatically from quantity — no manual macro editing.
            Surface(shape = RoundedCornerShape(12.dp), color = OrangeAccent.copy(0.08f)) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    MacroPreview("Calories", "$newCalories kcal")
                    MacroPreview("Protein",  "${"%.1f".format(newProtein)}g")
                    MacroPreview("Carbs",    "${"%.1f".format(newCarbs)}g")
                    MacroPreview("Fat",      "${"%.1f".format(newFat)}g")
                    MacroPreview("Fiber",    "${"%.1f".format(newFiber)}g")
                }
            }

            Button(
                onClick = {
                    onSave(
                        foodName, servingDesc, meal, qty ?: baseServings,
                        newCalories, newProtein, newCarbs, newFat, newFiber
                    )
                },
                enabled  = isValid,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                shape    = RoundedCornerShape(14.dp)
            ) { Text("Save Changes", fontWeight = FontWeight.SemiBold) }
        }
    }
}

// ── AI Food Result Card ────────────────────────────────────────────────────────

@Composable
private fun AiFoodResultCard(
    result: ClaudeApiService.AiFoodResult,
    onAdd: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = OrangeAccent.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoAwesome, null, tint = OrangeAccent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("AI Result", fontSize = 12.sp, color = OrangeAccent, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Close, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(result.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("${result.serving}  ·  ${result.category}",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                MacroPreview("Calories", "${result.calories} kcal")
                MacroPreview("Protein",  "${"%.1f".format(result.proteinG)}g")
                MacroPreview("Carbs",    "${"%.1f".format(result.carbsG)}g")
                MacroPreview("Fat",      "${"%.1f".format(result.fatG)}g")
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Add to Log", fontWeight = FontWeight.SemiBold) }
        }
    }
}
