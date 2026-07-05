package com.sunitha.fittrack.ui.ai

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.sunitha.fittrack.data.db.entities.AiInsight
import com.sunitha.fittrack.ui.theme.GreenDark
import com.sunitha.fittrack.ui.theme.GreenPrimary
import com.sunitha.fittrack.ui.theme.OrangeAccent
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiInsightsScreen(vm: AiInsightsViewModel) {
    val insights         by vm.insights.collectAsState()
    val isGenerating     by vm.isGenerating.collectAsState()
    val error            by vm.error.collectAsState()
    val actionChips      by vm.actionChips.collectAsState()
    val selectedChip     by vm.selectedChip.collectAsState()
    val followUpAnswer   by vm.followUpAnswer.collectAsState()
    val isFollowUpLoading by vm.isFollowUpLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Coach", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Column(modifier = Modifier.fillMaxSize().padding(pad)) {
            if (error != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    color    = MaterialTheme.colorScheme.errorContainer,
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(error!!, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = vm::clearError) { Icon(Icons.Filled.Close, null) }
                    }
                }
            }

            WeeklyInsightsTab(
                vm                = vm,
                insights          = insights,
                isLoading         = isGenerating,
                actionChips       = actionChips,
                selectedChip      = selectedChip,
                followUpAnswer    = followUpAnswer,
                isFollowUpLoading = isFollowUpLoading
            )
        }
    }
}

// ── Weekly Insights Tab ────────────────────────────────────────────────────────

@Composable
private fun WeeklyInsightsTab(
    vm: AiInsightsViewModel,
    insights: List<AiInsight>,
    isLoading: Boolean,
    actionChips: List<String>,
    selectedChip: String?,
    followUpAnswer: String?,
    isFollowUpLoading: Boolean
) {
    val weeklyInsights = remember(insights) {
        insights.filter { it.type == "WEEKLY_SUMMARY" }.take(5)
    }
    val latest  = weeklyInsights.firstOrNull()
    val history = if (weeklyInsights.size > 1) weeklyInsights.drop(1) else emptyList()
    val hasInsights = weeklyInsights.isNotEmpty()

    LazyColumn(
        contentPadding     = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header / generate card
        item {
            GenerateCard(
                isLoading   = isLoading,
                hasInsights = hasInsights,
                onGenerate  = vm::generateWeeklySummary
            )
        }

        // Latest insight — fully expanded with chips
        if (latest != null) {
            item {
                LatestInsightCard(
                    insight           = latest,
                    actionChips       = actionChips,
                    selectedChip      = selectedChip,
                    followUpAnswer    = followUpAnswer,
                    isFollowUpLoading = isFollowUpLoading,
                    onChipClick       = vm::askFollowUp,
                    onClearFollowUp   = vm::clearFollowUp
                )
            }
        }

        // History — collapsed cards
        if (history.isNotEmpty()) {
            item {
                Text(
                    "Previous Insights",
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 13.sp,
                    color      = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                )
            }
            items(history, key = { it.id }) { insight ->
                CollapsibleInsightCard(insight)
            }
        }

        // Empty state
        if (!hasInsights && !isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 30.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Insights, null, modifier = Modifier.size(52.dp), tint = GreenPrimary.copy(0.3f))
                        Text("No insights yet", color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                        Text(
                            "Tap the button above to generate your first analysis",
                            fontSize = 12.sp,
                            color    = MaterialTheme.colorScheme.onSurface.copy(0.4f)
                        )
                    }
                }
            }
        }
    }
}

// ── Biometric helper ───────────────────────────────────────────────────────────

@Composable
private fun rememberBiometricAuth(
    title: String = "Confirm AI Generation",
    subtitle: String = "Fingerprint required to use AI credits",
    onSuccess: () -> Unit
): () -> Unit {
    val context  = LocalContext.current
    val activity = context as? FragmentActivity

    return remember(onSuccess) {
        {
            if (activity == null) {
                onSuccess()
                return@remember
            }
            val manager = BiometricManager.from(context)
            val canAuth = manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
                onSuccess()
                return@remember
            }
            val prompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(context),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onSuccess()
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                            errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                            Toast.makeText(context, "Auth error: $errString", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText("Cancel")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build()
            prompt.authenticate(info)
        }
    }
}

// ── Markdown bold renderer ─────────────────────────────────────────────────────

private fun parseMarkdownBold(text: String): AnnotatedString = buildAnnotatedString {
    val pattern = Regex("\\*\\*(.*?)\\*\\*")
    var lastEnd = 0
    pattern.findAll(text).forEach { match ->
        if (match.range.first > lastEnd) append(text.substring(lastEnd, match.range.first))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(match.groupValues[1]) }
        lastEnd = match.range.last + 1
    }
    if (lastEnd < text.length) append(text.substring(lastEnd))
}

// ── Generate / Regenerate Card ─────────────────────────────────────────────────

@Composable
private fun GenerateCard(isLoading: Boolean, hasInsights: Boolean, onGenerate: () -> Unit) {
    val guardedGenerate = rememberBiometricAuth(
        title    = if (hasInsights) "Regenerate Insights" else "Generate Insights",
        subtitle = "Use fingerprint to confirm — this uses AI credits",
        onSuccess = onGenerate
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(Color(0xFF1A237E), GreenDark)), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.AutoAwesome, null, tint = Color(0xFFFFD54F), modifier = Modifier.size(22.dp))
                Text("Claude AI Coach", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Get personalised insights based on your workouts, food, and weight data from the past 7 days.",
                color = Color.White.copy(0.82f), fontSize = 13.sp, lineHeight = 20.sp
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick  = guardedGenerate,
                enabled  = !isLoading,
                colors   = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = GreenDark, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Claude is analysing…", color = GreenDark, fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(
                        if (hasInsights) Icons.Filled.Refresh else Icons.Filled.AutoAwesome,
                        null, tint = GreenDark, modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (hasInsights) "Regenerate Insights" else "Generate Weekly Insights",
                        color = GreenDark, fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ── Latest Insight Card (expanded + chips + follow-up) ────────────────────────

@Composable
private fun LatestInsightCard(
    insight: AiInsight,
    actionChips: List<String>,
    selectedChip: String?,
    followUpAnswer: String?,
    isFollowUpLoading: Boolean,
    onChipClick: (String) -> Unit,
    onClearFollowUp: () -> Unit
) {
    val sdf       = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val clipboard = LocalClipboardManager.current
    val context   = LocalContext.current

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Header row
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.AutoAwesome, null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                Text("Latest Insight", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = GreenPrimary)
                Spacer(Modifier.weight(1f))
                Text(
                    sdf.format(Date(insight.generatedAtMillis)),
                    fontSize = 11.sp,
                    color    = MaterialTheme.colorScheme.onSurface.copy(0.4f)
                )
                IconButton(
                    onClick  = {
                        clipboard.setText(AnnotatedString(insight.content))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Filled.ContentCopy, null,
                        tint     = MaterialTheme.colorScheme.onSurface.copy(0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Insight text
            Text(parseMarkdownBold(insight.content), fontSize = 14.sp, lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurface)

            // Action chips
            if (actionChips.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(0.08f))
                Text(
                    "Explore further",
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color      = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(actionChips) { chip ->
                        val isSelected = selectedChip == chip
                        FilterChip(
                            selected = isSelected,
                            onClick  = {
                                if (isSelected) onClearFollowUp() else onChipClick(chip)
                            },
                            label    = { Text(chip, fontSize = 12.sp) },
                            leadingIcon = if (isSelected) ({
                                Icon(Icons.Filled.Close, null, modifier = Modifier.size(14.dp))
                            }) else null,
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GreenPrimary.copy(0.15f),
                                selectedLabelColor     = GreenPrimary
                            )
                        )
                    }
                }

                // Follow-up answer
                AnimatedVisibility(
                    visible = selectedChip != null,
                    enter   = expandVertically(),
                    exit    = shrinkVertically()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(12.dp),
                        color    = GreenPrimary.copy(0.07f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.QuestionAnswer, null, tint = GreenPrimary, modifier = Modifier.size(14.dp))
                                Text(selectedChip ?: "", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GreenPrimary)
                            }
                            if (isFollowUpLoading) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = GreenPrimary)
                                    Text("Claude is thinking…", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                                }
                            } else if (followUpAnswer != null) {
                                Text(followUpAnswer, fontSize = 13.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Collapsible Older Insight Card ─────────────────────────────────────────────

@Composable
private fun CollapsibleInsightCard(insight: AiInsight) {
    val sdf       = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val clipboard = LocalClipboardManager.current
    val context   = LocalContext.current
    var expanded  by remember { mutableStateOf(false) }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header — always visible
            Row(
                verticalAlignment   = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.History, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.35f), modifier = Modifier.size(14.dp))
                Text(
                    sdf.format(Date(insight.generatedAtMillis)),
                    fontSize   = 12.sp,
                    color      = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                    modifier   = Modifier.weight(1f)
                )
                if (expanded) {
                    IconButton(
                        onClick  = {
                            clipboard.setText(AnnotatedString(insight.content))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Filled.ContentCopy, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.35f), modifier = Modifier.size(14.dp))
                    }
                }
                IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(26.dp)) {
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        null,
                        tint     = MaterialTheme.colorScheme.onSurface.copy(0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Preview (collapsed)
            if (!expanded) {
                Spacer(Modifier.height(4.dp))
                Text(
                    insight.content,
                    fontSize  = 13.sp,
                    lineHeight = 19.sp,
                    maxLines  = 2,
                    overflow  = TextOverflow.Ellipsis,
                    color     = MaterialTheme.colorScheme.onSurface.copy(0.65f)
                )
            }

            // Full content (expanded)
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Text(parseMarkdownBold(insight.content), fontSize = 14.sp, lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

// ── Rest Day Tab ───────────────────────────────────────────────────────────────

@Composable
private fun RestDayTab(vm: AiInsightsViewModel, symptoms: String, restResult: String?, isLoading: Boolean) {
    val guardedRecommend = rememberBiometricAuth(
        title    = "Rest Day Planner",
        subtitle = "Use fingerprint to confirm — this uses AI credits",
        onSuccess = vm::generateRestDayAdvice
    )
    LazyColumn(
        contentPadding     = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(20.dp),
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.SelfImprovement, null, tint = OrangeAccent, modifier = Modifier.size(22.dp))
                        Text("Rest Day Planner", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Claude analyses your recent workout intensity and recovery to recommend the optimal plan for today.",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f), lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value         = symptoms,
                        onValueChange = vm::setSymptoms,
                        label         = { Text("Any symptoms? (optional)") },
                        placeholder   = { Text("e.g. sore legs, fatigue, tight shoulders…") },
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(12.dp),
                        maxLines      = 3
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick  = guardedRecommend,
                        enabled  = !isLoading,
                        colors   = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                        shape    = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Analysing recovery…")
                        } else {
                            Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Get Today's Recommendation", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        if (restResult != null) {
            item { RestDayResultCard(restResult) }
        }
    }
}

// ── Rest Day Result Card ───────────────────────────────────────────────────────

@Composable
private fun RestDayResultCard(result: String) {
    val clipboard = LocalClipboardManager.current
    val context   = LocalContext.current
    val lines = result.split("\n").filter { it.isNotBlank() }
    val recommendation = lines.firstOrNull { it.startsWith("RECOMMENDATION:") }
        ?.removePrefix("RECOMMENDATION:")?.trim() ?: ""
    val reason = lines.firstOrNull { it.startsWith("REASON:") }
        ?.removePrefix("REASON:")?.trim() ?: result

    val (bgColor, iconColor) = when {
        recommendation.contains("Full Rest", ignoreCase = true) -> Pair(Color(0xFFE3F2FD), Color(0xFF1565C0))
        recommendation.contains("Active",    ignoreCase = true) -> Pair(Color(0xFFF3E5F5), Color(0xFF7B1FA2))
        else                                                     -> Pair(Color(0xFFE8F5E9), GreenDark)
    }
    val icon = when {
        recommendation.contains("Full Rest", ignoreCase = true) -> Icons.Filled.Hotel
        recommendation.contains("Active",    ignoreCase = true) -> Icons.Filled.DirectionsWalk
        else                                                     -> Icons.Filled.FitnessCenter
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment   = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(28.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Today's Recommendation", fontSize = 12.sp, color = iconColor.copy(0.7f))
                    Text(recommendation.ifBlank { "See below" }, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = iconColor)
                }
                IconButton(
                    onClick  = {
                        clipboard.setText(AnnotatedString(result))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Filled.ContentCopy, null, tint = iconColor.copy(0.5f), modifier = Modifier.size(16.dp))
                }
            }
            if (reason.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(reason, fontSize = 14.sp, lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
