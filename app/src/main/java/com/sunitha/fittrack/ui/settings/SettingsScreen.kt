package com.sunitha.fittrack.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunitha.fittrack.data.datastore.ThemeMode
import com.sunitha.fittrack.ui.theme.GreenPrimary
import com.sunitha.fittrack.ui.theme.OrangeAccent
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel, onBack: () -> Unit, onNavigateToImport: () -> Unit, onEditProfile: () -> Unit) {
    val context     = LocalContext.current
    val state       by vm.state.collectAsState()
    val backupFiles by vm.backupFiles.collectAsState()
    val themeMode   by vm.themeMode.collectAsState()
    val importDone  = remember { isImportDone(context) }

    LaunchedEffect(Unit) { vm.loadBackupFiles(context) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { vm.restoreFromUri(it, context) }
    }

    val snackbarHost = remember { SnackbarHostState() }
    LaunchedEffect(state) {
        if (state is BackupRestoreState.Success || state is BackupRestoreState.Error) {
            val msg = when (state) {
                is BackupRestoreState.Success -> (state as BackupRestoreState.Success).message
                is BackupRestoreState.Error   -> (state as BackupRestoreState.Error).message
                else -> ""
            }
            snackbarHost.showSnackbar(msg)
            vm.clearState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsActionCard(
                icon        = Icons.Filled.ManageAccounts,
                iconColor   = MaterialTheme.colorScheme.primary,
                title       = "My Profile",
                subtitle    = "Update your name, age, gender, height, fitness goal and activity level. Macro goals recalculate automatically.",
                buttonLabel = "Edit Profile",
                buttonColor = MaterialTheme.colorScheme.primary,
                loading     = false,
                onClick     = onEditProfile
            )

            AppearanceCard(themeMode = themeMode, onSelect = vm::setThemeMode)

            if (!importDone) {
                SettingsActionCard(
                    icon        = Icons.Filled.History,
                    iconColor   = MaterialTheme.colorScheme.tertiary,
                    title       = "Import Historical Data",
                    subtitle    = "One-time import of your workout history from Google Sheets. Disappears after a successful import.",
                    buttonLabel = "Import from Sheets",
                    buttonColor = MaterialTheme.colorScheme.tertiary,
                    loading     = false,
                    onClick     = onNavigateToImport
                )
            }

            Text(
                "Data Management",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
            )

            // Backup card
            SettingsActionCard(
                icon        = Icons.Filled.CloudUpload,
                iconColor   = GreenPrimary,
                title       = "Backup Data",
                subtitle    = "Saves all your data inside the app's folder on your phone. No permissions needed.",
                buttonLabel = "Backup Now",
                buttonColor = GreenPrimary,
                loading     = state is BackupRestoreState.Loading,
                onClick     = { vm.backup(context) }
            )

            // Restore card — shows saved backups + file picker fallback
            RestoreCard(
                backupFiles = backupFiles,
                loading     = state is BackupRestoreState.Loading,
                onRestore   = { vm.restoreFromFile(it) },
                onDelete    = { vm.deleteBackupFile(it, context) },
                onPickFile  = { filePicker.launch(arrayOf("application/json", "*/*")) }
            )

            // Info card
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.4f), modifier = Modifier.size(18.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Backup file location", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        Text(
                            "Files are saved to Android/data/com.sunitha.fittrack/files/Backups/ " +
                            "on your phone. You can also find and copy them using a file manager.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.55f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Appearance Card ────────────────────────────────────────────────────────────

@Composable
private fun AppearanceCard(themeMode: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val options = listOf(
        Triple(ThemeMode.SYSTEM, "System", Icons.Filled.BrightnessAuto),
        Triple(ThemeMode.LIGHT,  "Light",  Icons.Filled.LightMode),
        Triple(ThemeMode.DARK,   "Dark",   Icons.Filled.DarkMode)
    )
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primary.copy(0.1f)) {
                    Icon(Icons.Filled.Palette, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp).size(24.dp))
                }
                Column {
                    Text("Appearance", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text("Choose how FitTrack looks.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.55f))
                }
            }
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, (mode, label, icon) ->
                    SegmentedButton(
                        selected = themeMode == mode,
                        onClick  = { onSelect(mode) },
                        shape    = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        icon     = { Icon(icon, null, modifier = Modifier.size(16.dp)) },
                        colors   = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primary.copy(0.12f),
                            activeContentColor   = MaterialTheme.colorScheme.primary
                        )
                    ) { Text(label, fontSize = 13.sp) }
                }
            }
        }
    }
}

// ── Restore Card ───────────────────────────────────────────────────────────────

@Composable
private fun RestoreCard(
    backupFiles: List<File>,
    loading: Boolean,
    onRestore: (File) -> Unit,
    onDelete: (File) -> Unit,
    onPickFile: () -> Unit
) {
    var confirmFile by remember { mutableStateOf<File?>(null) }
    var deleteFile  by remember { mutableStateOf<File?>(null) }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = OrangeAccent.copy(0.1f)) {
                    Icon(Icons.Filled.CloudDownload, null, tint = OrangeAccent, modifier = Modifier.padding(10.dp).size(24.dp))
                }
                Column {
                    Text("Restore Data", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(
                        if (backupFiles.isEmpty()) "No backups found in the app folder yet."
                        else "Tap a backup below to restore it.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.55f),
                        lineHeight = 17.sp
                    )
                }
            }

            if (backupFiles.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(0.08f))
                backupFiles.forEach { file ->
                    BackupFileRow(
                        file      = file,
                        loading   = loading,
                        onRestore = { confirmFile = file },
                        onDelete  = { deleteFile  = file }
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(0.08f))
            }

            TextButton(
                onClick  = onPickFile,
                enabled  = !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.FolderOpen, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Pick backup from another location")
            }
        }
    }

    // Restore confirm dialog
    confirmFile?.let { file ->
        AlertDialog(
            onDismissRequest = { confirmFile = null },
            title = { Text("Restore Backup?", fontWeight = FontWeight.Bold) },
            text  = { Text("This will merge ${file.name} into your current data. Existing records with matching IDs will be overwritten.") },
            confirmButton = {
                Button(
                    onClick = { onRestore(file); confirmFile = null },
                    colors  = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
                ) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { confirmFile = null }) { Text("Cancel") } },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Delete confirm dialog
    deleteFile?.let { file ->
        AlertDialog(
            onDismissRequest = { deleteFile = null },
            title = { Text("Delete Backup?", fontWeight = FontWeight.Bold) },
            text  = { Text("${file.name} will be permanently deleted.") },
            confirmButton = {
                Button(
                    onClick = { onDelete(file); deleteFile = null },
                    colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteFile = null }) { Text("Cancel") } },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun BackupFileRow(file: File, loading: Boolean, onRestore: () -> Unit, onDelete: () -> Unit) {
    val sdf  = remember { SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault()) }
    val date = remember(file) { sdf.format(Date(file.lastModified())) }
    val size = remember(file) { "%.1f KB".format(file.length() / 1024f) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Filled.Description, null, tint = OrangeAccent.copy(0.7f), modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(file.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text("$date · $size", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
        }
        TextButton(onClick = onRestore, enabled = !loading, contentPadding = PaddingValues(horizontal = 8.dp)) {
            Text("Restore", color = OrangeAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.3f), modifier = Modifier.size(16.dp))
        }
    }
}

// ── Generic Settings Action Card ──────────────────────────────────────────────

@Composable
private fun SettingsActionCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    buttonLabel: String,
    buttonColor: Color,
    loading: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = iconColor.copy(0.1f)) {
                    Icon(icon, null, tint = iconColor, modifier = Modifier.padding(10.dp).size(24.dp))
                }
                Column {
                    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.55f), lineHeight = 17.sp)
                }
            }
            Button(
                onClick  = onClick,
                enabled  = !loading,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = buttonColor),
                shape    = RoundedCornerShape(12.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(buttonLabel, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
