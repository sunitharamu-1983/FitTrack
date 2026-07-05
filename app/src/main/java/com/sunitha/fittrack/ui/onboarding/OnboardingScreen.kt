package com.sunitha.fittrack.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunitha.fittrack.data.datastore.*
import com.sunitha.fittrack.ui.theme.GreenPrimary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(vm: OnboardingViewModel, onDone: () -> Unit) {
    val saved by vm.saved.collectAsState()
    LaunchedEffect(saved) { if (saved) onDone() }

    var showDatePicker by remember { mutableStateOf(false) }
    val thirtyYearsAgo = remember {
        Calendar.getInstance().apply { add(Calendar.YEAR, -30) }.timeInMillis
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = if (vm.dobMillis > 0L) vm.dobMillis else thirtyYearsAgo,
        yearRange = 1940..Calendar.getInstance().get(Calendar.YEAR) - 10
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { vm.dobMillis = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(32.dp))

            // Header
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Set up your profile", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(
                    "FitTrack uses this to calculate your personalised macro goals and give you smarter AI insights.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.6f),
                    lineHeight = 20.sp
                )
            }

            // Name
            OutlinedTextField(
                value         = vm.name,
                onValueChange = { vm.name = it },
                label         = { Text("Your name") },
                leadingIcon   = { Icon(Icons.Filled.Person, null) },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                shape         = MaterialTheme.shapes.medium
            )

            // Date of birth
            val dobLabel = if (vm.dobMillis > 0L)
                SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(vm.dobMillis))
            else
                "Tap to select"

            OutlinedCard(
                onClick  = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape    = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Cake, null, tint = GreenPrimary)
                    Column {
                        Text("Date of birth", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                        Text(dobLabel, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    }
                }
            }

            // Gender
            ProfileSection("Gender") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Gender.FEMALE            to "Female",
                        Gender.MALE              to "Male",
                        Gender.PREFER_NOT_TO_SAY to "Other"
                    ).forEach { (g, label) ->
                        FilterChip(
                            selected = vm.gender == g,
                            onClick  = { vm.gender = g },
                            label    = { Text(label, fontSize = 13.sp) }
                        )
                    }
                }
            }

            // Height
            OutlinedTextField(
                value         = vm.heightCmText,
                onValueChange = { vm.heightCmText = it.filter { c -> c.isDigit() } },
                label         = { Text("Height") },
                leadingIcon   = { Icon(Icons.Filled.Height, null) },
                suffix        = { Text("cm") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                shape         = MaterialTheme.shapes.medium
            )

            // Fitness goal
            ProfileSection("Fitness goal") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        FitnessGoal.CUT      to "Lose weight",
                        FitnessGoal.MAINTAIN to "Maintain",
                        FitnessGoal.BULK     to "Build muscle"
                    ).forEach { (g, label) ->
                        FilterChip(
                            selected = vm.goal == g,
                            onClick  = { vm.goal = g },
                            label    = { Text(label, fontSize = 13.sp) }
                        )
                    }
                }
            }

            // Activity level
            ProfileSection("Activity level") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        ActivityLevel.SEDENTARY to Pair("Sedentary",          "Desk job, little or no exercise"),
                        ActivityLevel.MODERATE  to Pair("Moderately active",  "Exercise 3–5 days/week"),
                        ActivityLevel.ACTIVE    to Pair("Very active",        "Hard exercise 6–7 days/week")
                    ).forEach { (level, text) ->
                        val (label, desc) = text
                        OutlinedCard(
                            onClick  = { vm.activityLevel = level },
                            modifier = Modifier.fillMaxWidth(),
                            shape    = MaterialTheme.shapes.medium,
                            border   = if (vm.activityLevel == level)
                                BorderStroke(2.dp, GreenPrimary)
                            else
                                CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier              = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = vm.activityLevel == level,
                                    onClick  = { vm.activityLevel = level }
                                )
                                Column {
                                    Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(desc,  fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.55f))
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick  = vm::save,
                enabled  = vm.isValid,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape    = MaterialTheme.shapes.medium
            ) {
                Text("Save Profile", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProfileSection(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
        content()
    }
}
