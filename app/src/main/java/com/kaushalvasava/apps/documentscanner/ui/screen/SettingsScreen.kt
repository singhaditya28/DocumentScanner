package com.kaushalvasava.apps.documentscanner.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kaushalvasava.apps.documentscanner.data.SettingsDataStore
import com.kaushalvasava.apps.documentscanner.data.dataStore
import com.kaushalvasava.apps.documentscanner.ui.theme.AppTheme
import com.kaushalvasava.apps.documentscanner.ui.viewmodel.AuthViewModel
import com.kaushalvasava.apps.documentscanner.ui.viewmodel.ScannerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    scannerViewModel: ScannerViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onThemeChange: (AppTheme) -> Unit = {}
) {
    val context = LocalContext.current
    val dataStore = remember { SettingsDataStore(context) }
    val scope = rememberCoroutineScope()

    val savedTemplateId by scannerViewModel.templateId.collectAsState(initial = "")
    val savedAutoProcess by scannerViewModel.autoProcess.collectAsState(initial = false)
    val savedThemeKey by dataStore.appTheme.collectAsState(initial = null)
    val templates by scannerViewModel.templates.collectAsState()

    var templateId by remember(savedTemplateId) { mutableStateOf(savedTemplateId ?: "") }
    var autoProcess by remember(savedAutoProcess) { mutableStateOf(savedAutoProcess) }
    var currentTheme by remember(savedThemeKey) {
        mutableStateOf(AppTheme.fromWebKey(savedThemeKey))
    }

    var templateDropdownExpanded by remember { mutableStateOf(false) }
    var themeDropdownExpanded by remember { mutableStateOf(false) }
    val selectedTemplate = templates.firstOrNull { it.templateId.toString() == templateId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {

            // ── Theme selector ──────────────────────────────────────────────
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = themeDropdownExpanded,
                onExpandedChange = { themeDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = "${currentTheme.emoji} ${currentTheme.label.removePrefix("${currentTheme.emoji} ")}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Theme") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeDropdownExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = themeDropdownExpanded,
                    onDismissRequest = { themeDropdownExpanded = false }
                ) {
                    AppTheme.entries.forEach { theme ->
                        DropdownMenuItem(
                            text = { Text(theme.label) },
                            onClick = {
                                currentTheme = theme
                                themeDropdownExpanded = false
                                scope.launch { dataStore.saveTheme(theme.webKey) }
                                onThemeChange(theme)  // update live in parent
                            },
                            trailingIcon = {
                                if (theme == currentTheme) {
                                    Text("✓", color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        )
                    }
                }
            }
            Text(
                "Matches the themes available on the web dashboard.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(24.dp))

            // ── Upload defaults ──────────────────────────────────────────────
            Text(
                text = "Upload Defaults",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            // H: Template picker — dropdown if templates loaded, text field as fallback
            if (templates.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = templateDropdownExpanded,
                    onExpandedChange = { templateDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedTemplate?.name ?: if (templateId.isBlank()) "None (auto)" else "ID $templateId",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Default Template") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = templateDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = templateDropdownExpanded,
                        onDismissRequest = { templateDropdownExpanded = false }
                    ) {
                        // "None" option
                        DropdownMenuItem(
                            text = { Text("None — upload without template") },
                            onClick = {
                                templateId = ""
                                templateDropdownExpanded = false
                            }
                        )
                        templates.forEach { template ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(template.name, style = MaterialTheme.typography.bodyMedium)
                                        if (!template.description.isNullOrBlank()) {
                                            Text(
                                                template.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    templateId = template.templateId.toString()
                                    templateDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            } else {
                // Fallback: manual ID entry (when templates haven't loaded)
                OutlinedTextField(
                    value = templateId,
                    onValueChange = { templateId = it },
                    label = { Text("Template ID (optional)") },
                    placeholder = { Text("e.g. 3") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "This template will be pre-selected when you scan. You can still change it per upload.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto Process", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Trigger OCR immediately after upload",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (templateId.isBlank()) {
                        Text(
                            "Requires a default template to be selected above",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                Switch(
                    checked = autoProcess && templateId.isNotBlank(),
                    onCheckedChange = { autoProcess = it },
                    enabled = templateId.isNotBlank()
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    scannerViewModel.saveSettings(templateId.ifBlank { null }, autoProcess)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { Text("Save Settings") }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    authViewModel.logout()
                    onLogout()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) { Text("Log Out") }
        }
    }
}
