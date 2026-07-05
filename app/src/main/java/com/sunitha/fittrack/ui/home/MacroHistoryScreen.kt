package com.sunitha.fittrack.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunitha.fittrack.data.datastore.MacroGoals
import com.sunitha.fittrack.ui.nutrition.AddFoodSheet
import com.sunitha.fittrack.ui.nutrition.EditFoodSheet
import com.sunitha.fittrack.ui.nutrition.FoodEntryRow
import com.sunitha.fittrack.ui.nutrition.NutritionViewModel
import com.sunitha.fittrack.ui.theme.GreenPrimary
import com.sunitha.fittrack.ui.theme.OrangeAccent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ColBlue    = Color(0xFF1565C0)
private val ColOrange  = OrangeAccent
private val ColRed     = Color(0xFFC62828)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroHistoryScreen(vm: MacroHistoryViewModel, nutritionVm: NutritionViewModel, onBack: () -> Unit) {
    val days  by vm.days.collectAsState()
    val goals by vm.goals.collectAsState()

    var expandedDay by remember { mutableStateOf<Long?>(null) }
    val dayEntries       by nutritionVm.todayEntries.collectAsState()
    val showAddSheet     by nutritionVm.showAddSheet.collectAsState()
    val pendingFood      by nutritionVm.pendingFood.collectAsState()
    val selectedMealType by nutritionVm.selectedMealType.collectAsState()
    val searchQuery      by nutritionVm.searchQuery.collectAsState()
    val searchResults    by nutritionVm.searchResults.collectAsState()
    val editingEntry     by nutritionVm.editingEntry.collectAsState()

    fun toggleDay(dayMillis: Long) {
        expandedDay = if (expandedDay == dayMillis) null else dayMillis
        nutritionVm.selectDay(dayMillis)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Macro History", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (days.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No food logged in the last 30 days.",
                    color    = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier        = Modifier.fillMaxSize().padding(padding),
                contentPadding  = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Goals row
                item {
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
                        colors    = CardDefaults.cardColors(containerColor = GreenPrimary.copy(0.08f)),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Goals →", modifier = Modifier.weight(1.7f), fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(0.5f), fontWeight = FontWeight.Medium)
                            GoalCell("${goals.calories}", "kcal", GreenPrimary)
                            GoalCell("${goals.proteinG.toInt()}g", "prot", ColBlue)
                            GoalCell("${goals.carbsG.toInt()}g", "carb", ColOrange)
                            GoalCell("${goals.fatG.toInt()}g", "fat",  ColRed)
                        }
                    }
                }

                // Column header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Date", modifier = Modifier.weight(1.7f), fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                        HeaderCell("Kcal")
                        HeaderCell("Prot")
                        HeaderCell("Carbs")
                        HeaderCell("Fat")
                    }
                }

                // Data rows
                itemsIndexed(days) { index, day ->
                    val bg = if (index % 2 == 0) MaterialTheme.colorScheme.surface
                             else MaterialTheme.colorScheme.surfaceVariant.copy(0.4f)
                    val isLast = index == days.lastIndex
                    val isExpanded = expandedDay == day.dateMillis
                    val shape  = if (isLast && !isExpanded) RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp) else RoundedCornerShape(0.dp)

                    Surface(
                        shape = shape,
                        color = bg,
                        modifier = Modifier.fillMaxWidth().clickable { toggleDay(day.dateMillis) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text     = dayLabel(day.dateMillis),
                                modifier = Modifier.weight(1.7f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color    = MaterialTheme.colorScheme.onSurface
                            )
                            ValueCell(day.calories.toString(), goals.calories.toFloat(), day.calories.toFloat(), GreenPrimary)
                            ValueCell("${day.proteinG.toInt()}g", goals.proteinG, day.proteinG, ColBlue)
                            ValueCell("${day.carbsG.toInt()}g",   goals.carbsG,   day.carbsG,   ColOrange)
                            ValueCell("${day.fatG.toInt()}g",     goals.fatG,     day.fatG,     ColRed)
                            Icon(
                                if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (isExpanded) {
                        Surface(color = bg, modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                dayEntries.forEach { entry ->
                                    FoodEntryRow(
                                        entry    = entry,
                                        onEdit   = { nutritionVm.startEditing(entry) },
                                        onDelete = { nutritionVm.deleteEntry(entry) }
                                    )
                                }
                                TextButton(
                                    onClick = { nutritionVm.openAddSheet() },
                                    colors  = ButtonDefaults.textButtonColors(contentColor = OrangeAccent)
                                ) {
                                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Add food for this day", fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    if (!isLast || isExpanded) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddFoodSheet(
            vm            = nutritionVm,
            pendingFood   = pendingFood,
            selectedMeal  = selectedMealType,
            searchQuery   = searchQuery,
            searchResults = searchResults,
            onDismiss     = nutritionVm::closeAddSheet
        )
    }

    editingEntry?.let { entry ->
        EditFoodSheet(entry = entry, mealTypes = nutritionVm.mealTypes, onDismiss = nutritionVm::stopEditing) {
            foodName, servingDesc, meal, servings, cal, prot, carbs2, fat2, fiber2 ->
            nutritionVm.saveEdit(entry, foodName, servingDesc, meal, servings, cal, prot, carbs2, fat2, fiber2)
        }
    }
}

@Composable
private fun RowScope.GoalCell(value: String, label: String, color: Color) {
    Column(
        modifier           = Modifier.weight(1f),
        horizontalAlignment = Alignment.End
    ) {
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color, textAlign = TextAlign.End)
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.4f), textAlign = TextAlign.End)
    }
}

@Composable
private fun RowScope.HeaderCell(text: String) {
    Text(
        text      = text,
        modifier  = Modifier.weight(1f),
        fontSize  = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color     = MaterialTheme.colorScheme.onSurface.copy(0.6f),
        textAlign = TextAlign.End
    )
}

@Composable
private fun RowScope.ValueCell(text: String, goal: Float, actual: Float, color: Color) {
    val pct   = if (goal > 0f) actual / goal else 0f
    val alpha = when {
        actual == 0f -> 0.3f
        pct >= 0.8f  -> 1f
        pct >= 0.5f  -> 0.75f
        else         -> 0.5f
    }
    Text(
        text      = text,
        modifier  = Modifier.weight(1f),
        fontSize  = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color     = color.copy(alpha = alpha),
        textAlign = TextAlign.End
    )
}

private fun dayLabel(millis: Long): String =
    SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(millis))
