package com.sunitha.fittrack.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.sunitha.fittrack.FitTrackApp
import com.sunitha.fittrack.MainActivity
import com.sunitha.fittrack.data.StreakCalculator
import com.sunitha.fittrack.data.datastore.calculateMacros
import kotlinx.coroutines.flow.first

data class WidgetData(
    val streak: Int,
    val calories: Int,
    val calorieGoal: Int,
    val steps: Int,
    val stepGoal: Int
)

private val WidgetBg     = Color(0xFF8B1A4A)
private val WidgetAccent = Color(0xFFECA4BE)

class FitTrackWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetData(context)
        provideContent { WidgetContent(data) }
    }

    private suspend fun loadWidgetData(context: Context): WidgetData {
        val app  = context.applicationContext as FitTrackApp
        val repo = app.repository

        val sessions      = repo.getAllWorkoutSessions().first()
        val restDays      = repo.getRecentRestDays(365).first()
        val walkDays      = repo.getRecentWalkDays(365).first()
        val foodEntries   = repo.getFoodEntriesSinceFlow(System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000).first()
        val weightEntries = repo.getAllWeightEntries().first()
        val streak = StreakCalculator.calculate(sessions, restDays, walkDays, foodEntries, weightEntries)

        val profile      = app.profileStore.profile.first()
        val latestWeight = repo.getLatestWeight().first()
        val goals        = calculateMacros(profile, latestWeight?.weightKg ?: 0f)

        val todayCalories = repo.getTodayCalories().first() ?: 0
        val todaySteps    = repo.getTodaySteps().first()?.steps ?: 0

        return WidgetData(
            streak      = streak,
            calories    = todayCalories,
            calorieGoal = goals.calories,
            steps       = todaySteps,
            stepGoal    = goals.stepGoal
        )
    }
}

private fun solid(color: Color) = ColorProvider(day = color, night = color)

@Composable
private fun WidgetContent(data: WidgetData) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(solid(WidgetBg))
            .padding(16.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
            Text("FitTrack", style = TextStyle(color = solid(Color.White), fontWeight = FontWeight.Bold, fontSize = 14.sp))
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text("🔥 ${data.streak}d", style = TextStyle(color = solid(WidgetAccent), fontWeight = FontWeight.Bold, fontSize = 13.sp))
        }
        Spacer(modifier = GlanceModifier.height(12.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            StatBlock(label = "Calories", value = "${data.calories}", goal = "/ ${data.calorieGoal}")
            Spacer(modifier = GlanceModifier.width(16.dp))
            StatBlock(label = "Steps", value = "${data.steps}", goal = "/ ${data.stepGoal}")
        }
    }
}

@Composable
private fun StatBlock(label: String, value: String, goal: String) {
    Column {
        Text(value, style = TextStyle(color = solid(Color.White), fontWeight = FontWeight.Bold, fontSize = 20.sp))
        Row {
            Text(label, style = TextStyle(color = solid(WidgetAccent), fontSize = 11.sp))
            Text(" $goal", style = TextStyle(color = solid(WidgetAccent), fontSize = 11.sp))
        }
    }
}
