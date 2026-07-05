package com.sunitha.fittrack.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunitha.fittrack.ui.theme.GreenPrimary
import com.sunitha.fittrack.ui.theme.OrangeAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(vm: ImportViewModel, onBack: () -> Unit) {
    val state   by vm.state.collectAsState()
    val context = LocalContext.current

    // Navigate back automatically after Done
    LaunchedEffect(state) {
        if (state is ImportState.Done) {
            kotlinx.coroutines.delay(2_500)
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Historical Data", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        vm.reset()
                        onBack()
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(20.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            when (val s = state) {
                is ImportState.Idle     -> IdleContent(onFetch = { vm.fetchAndPreview() })
                is ImportState.Fetching -> LoadingContent("Fetching data from Google Sheets…")
                is ImportState.Preview  -> PreviewContent(
                    preview = s.summary,
                    onConfirm = { vm.confirmImport(s.batch, context) },
                    onCancel  = { vm.reset() }
                )
                is ImportState.Importing -> LoadingContent("Importing records into database…")
                is ImportState.Done      -> DoneContent(message = s.message)
                is ImportState.Error     -> ErrorContent(message = s.message, onRetry = { vm.fetchAndPreview() }, onCancel = { vm.reset() })
            }
        }
    }
}

// ── Idle ─────────────────────────────────────────────────────────────────────

@Composable
private fun IdleContent(onFetch: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = GreenPrimary.copy(0.1f),
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.CloudDownload, null, tint = GreenPrimary, modifier = Modifier.size(40.dp))
            }
        }

        Text(
            "One-Time Data Import",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Text(
            "This will fetch your historical workout data from Google Sheets and import it into FitTrack. " +
            "Existing records are skipped automatically.\n\n" +
            "You'll see a preview before anything is written.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(0.6f),
            lineHeight = 21.sp
        )

        Button(
            onClick  = onFetch,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
            shape    = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Filled.FileDownload, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Fetch & Preview", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

// ── Loading ───────────────────────────────────────────────────────────────────

@Composable
private fun LoadingContent(label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.padding(top = 60.dp)
    ) {
        CircularProgressIndicator(color = GreenPrimary, strokeWidth = 3.dp, modifier = Modifier.size(56.dp))
        Text(label, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Composable
private fun PreviewContent(
    preview: ImportPreview,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        Text("Import Preview", fontWeight = FontWeight.Bold, fontSize = 18.sp)

        // Summary counts card
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(16.dp),
            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PreviewRow("Total CSV rows",        preview.totalCsvRows.toString(),       MaterialTheme.colorScheme.onSurface)
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(0.1f))
                PreviewRow("Skipped — invalid",     preview.skippedInvalid.toString(),     OrangeAccent)
                PreviewRow("Skipped — duplicates",  preview.skippedDuplicates.toString(),  OrangeAccent)
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(0.1f))
                PreviewRow("Workouts to import",    preview.workoutsToImport.toString(),   GreenPrimary)
                PreviewRow("Rest days to import",   preview.restDaysToImport.toString(),   GreenPrimary)
                PreviewRow("Cardio sessions",       preview.cardioToImport.toString(),     GreenPrimary)
                PreviewRow("Food entries",          preview.foodToImport.toString(),        GreenPrimary)
                PreviewRow("Step logs",             preview.stepsToImport.toString(),       GreenPrimary)
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(0.1f))
                PreviewRow("Total records",         preview.totalToImport.toString(),      MaterialTheme.colorScheme.primary, bold = true)
            }
        }

        if (preview.totalToImport == 0) {
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(12.dp),
                colors    = CardDefaults.cardColors(containerColor = OrangeAccent.copy(0.1f))
            ) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, null, tint = OrangeAccent, modifier = Modifier.size(20.dp))
                    Text("Nothing new to import — all rows are already in the database.", fontSize = 13.sp)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick  = onCancel,
                modifier = Modifier.weight(1f).height(50.dp),
                shape    = RoundedCornerShape(12.dp)
            ) { Text("Cancel") }

            Button(
                onClick  = onConfirm,
                enabled  = preview.totalToImport > 0,
                modifier = Modifier.weight(1f).height(50.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Text("Import ${preview.totalToImport} Records", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PreviewRow(label: String, value: String, valueColor: Color, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(if (bold) 1f else 0.7f), fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal)
        Text(value, fontSize = 14.sp, color = valueColor, fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold)
    }
}

// ── Done ──────────────────────────────────────────────────────────────────────

@Composable
private fun DoneContent(message: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.padding(top = 40.dp)
    ) {
        Surface(shape = RoundedCornerShape(20.dp), color = GreenPrimary.copy(0.1f), modifier = Modifier.size(80.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.CheckCircle, null, tint = GreenPrimary, modifier = Modifier.size(44.dp))
            }
        }
        Text("Import Complete!", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(message, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f), lineHeight = 21.sp)
        Text("Returning to settings…", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
    }
}

// ── Error ─────────────────────────────────────────────────────────────────────

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit, onCancel: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.padding(top = 40.dp)
    ) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.error.copy(0.1f), modifier = Modifier.size(80.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(44.dp))
            }
        }
        Text("Import Failed", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(message, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f), lineHeight = 21.sp)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick  = onCancel,
                modifier = Modifier.weight(1f).height(50.dp),
                shape    = RoundedCornerShape(12.dp)
            ) { Text("Cancel") }

            Button(
                onClick  = onRetry,
                modifier = Modifier.weight(1f).height(50.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Retry", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
