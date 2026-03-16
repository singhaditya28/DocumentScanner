package com.kaushalvasava.apps.documentscanner.ui.screen

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.kaushalvasava.apps.documentscanner.MainActivity
import com.kaushalvasava.apps.documentscanner.R
import com.kaushalvasava.apps.documentscanner.network.Template
import com.kaushalvasava.apps.documentscanner.network.UploadedDocument
import com.kaushalvasava.apps.documentscanner.ui.viewmodel.ScannerViewModel
import com.kaushalvasava.apps.documentscanner.ui.viewmodel.UploadUiState
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    viewModel: ScannerViewModel,
    userName: String?,
    onNavigateToSettings: () -> Unit,
    onSessionExpired: () -> Unit
) {
    val activity = LocalContext.current as MainActivity
    val uploadState by viewModel.uploadState.collectAsState()
    val lastUpload by viewModel.lastUpload.collectAsState()
    val recentDocs by viewModel.recentDocuments.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val creditsRemaining by viewModel.creditsRemaining.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val savedTemplateId by viewModel.templateId.collectAsState("")
    val snackbarHostState = remember { SnackbarHostState() }

    // One-time onboarding tooltip
    var showTooltip by remember { mutableStateOf(true) }
    // C: Template picker bottom-sheet state
    var showTemplatePicker by remember { mutableStateOf(false) }
    var pendingUploadData by remember {
        mutableStateOf<Triple<android.net.Uri?, android.net.Uri?, Int>?>(null)
    }
    // I: Reprocess state
    var reprocessDoc by remember { mutableStateOf<UploadedDocument?>(null) }

    val options = remember {
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF
            )
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
    }
    val scanner = remember { GmsDocumentScanning.getClient(options) }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            if (scanningResult != null) {
                val pages = scanningResult.pages ?: emptyList()
                val pageCount = pages.size
                val jpegUri = if (pageCount == 1) pages.firstOrNull()?.imageUri else null
                val pdfUri = scanningResult.pdf?.uri
                val templateFromSettings = savedTemplateId
                showTooltip = false

                if (templates.isNotEmpty() && templateFromSettings.isNullOrBlank()) {
                    // C: Show template picker if no default set
                    pendingUploadData = Triple(jpegUri, pdfUri, pageCount)
                    showTemplatePicker = true
                } else {
                    // Upload immediately with default (or null if none)
                    viewModel.upload(
                        jpegUri = jpegUri,
                        pdfUri = pdfUri,
                        pageCount = pageCount,
                        selectedTemplateId = templateFromSettings?.takeIf { it.isNotBlank() }
                    )
                }
            }
        }
    }

    // Snackbar + session expiry handling
    LaunchedEffect(uploadState) {
        when (val state = uploadState) {
            is UploadUiState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetUploadState()
            }
            is UploadUiState.Error -> {
                if (state.isSessionExpired) {
                    onSessionExpired()
                } else {
                    snackbarHostState.showSnackbar(state.message)
                }
                viewModel.resetUploadState()
            }
            is UploadUiState.NotConfigured -> {
                snackbarHostState.showSnackbar("Please sign in again to upload documents.")
                viewModel.resetUploadState()
                onSessionExpired()
            }
            else -> {}
        }
    }

    // Pulsing FAB animation (B)
    val infiniteTransition = rememberInfiniteTransition(label = "fab_pulse")
    val fabScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fab_scale"
    )

    // C: Template picker bottom-sheet
    if (showTemplatePicker) {
        TemplatePicker(
            templates = templates,
            onTemplateSelected = { templateId ->
                showTemplatePicker = false
                val (jpeg, pdf, count) = pendingUploadData ?: return@TemplatePicker
                pendingUploadData = null
                viewModel.upload(
                    jpegUri = jpeg,
                    pdfUri = pdf,
                    pageCount = count,
                    selectedTemplateId = templateId
                )
            },
            onDismiss = {
                showTemplatePicker = false
                pendingUploadData = null
                // Removed bypass — user MUST pick a template
            }
        )
    }

    // I: Reprocess sheet
    if (reprocessDoc != null && templates.isNotEmpty()) {
        ReprocessPicker(
            doc = reprocessDoc!!,
            templates = templates,
            onTemplateSelected = { templateId ->
                viewModel.reprocessDocument(reprocessDoc!!.docId, templateId)
                reprocessDoc = null
            },
            onDismiss = { reprocessDoc = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.mediumTopAppBarColors(),
                actions = {
                    // D: Credits chip in top bar
                    creditsRemaining?.let { credits ->
                        val tint = when {
                            credits == 0 -> MaterialTheme.colorScheme.error
                            credits <= 5 -> Color(0xFFE65100)
                            else -> MaterialTheme.colorScheme.secondary
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = tint.copy(alpha = 0.12f),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = "$credits credits",
                                color = tint,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            val isUploading = uploadState is UploadUiState.Uploading
            ExtendedFloatingActionButton(
                onClick = {
                    if (!isUploading) {
                        scanner.getStartScanIntent(activity)
                            .addOnSuccessListener { intentSender ->
                                scannerLauncher.launch(
                                    IntentSenderRequest.Builder(intentSender).build()
                                )
                            }
                            .addOnFailureListener {
                                Log.d("HomeScreen", "Scanner error: ${it.message}")
                            }
                    }
                },
                modifier = Modifier.scale(if (isUploading) 1f else fabScale),
                text = { Text(if (isUploading) "Uploading…" else stringResource(R.string.scan)) },
                icon = { Icon(painterResource(R.drawable.ic_camera), contentDescription = null) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        val pullRefreshState = rememberPullRefreshState(
            refreshing = isRefreshing,
            onRefresh = { viewModel.refresh() }
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pullRefresh(pullRefreshState)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
            ) {
            // ── Greeting ────────────────────────────────────────────────────
            if (!userName.isNullOrBlank()) {
                item {
                    Text(
                        text = "Hi, $userName 👋",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // ── Uploading progress card ───────────────────────────────────
            if (uploadState is UploadUiState.Uploading) {
                item {
                    val pc = (uploadState as UploadUiState.Uploading).pageCount
                    val label = if (pc == 1) "1 page" else "$pc pages"
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text(
                                "Uploading $label…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // ── Low credits warning (D) ───────────────────────────────────
            creditsRemaining?.let { credits ->
                if (credits == 0) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Text(
                                "⚠️ No credits remaining — please contact your admin to top up.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                } else if (credits <= 5) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                        ) {
                            Text(
                                "⚠️ Only $credits credit${if (credits == 1) "" else "s"} remaining.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE65100),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            // ── Dashboard Stats ─────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(
                        "Dashboard",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Overview of your processed documents",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                val processedCount = recentDocs.count { it.status == "processed" || it.status == "exported" }
                val pendingCount = recentDocs.count { it.status == "pending" || it.status == "processing" || it.status == "queued" }
                val activeTemplates = templates.size
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Total Processed Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Processed",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                processedCount.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    // Pending Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Pending",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                pendingCount.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    
                    // Templates Active Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Templates",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                activeTemplates.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            // ── Last upload card (E + F + G) ─────────────────────────────
            lastUpload?.let { info ->
                item {
                    val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault())
                        .format(Date(info.timestamp))
                    val docLabel = if (info.pageCount == 1) "1 page" else "${info.pageCount} pages"
                    val typeLabel = if (info.type == "image") "JPEG" else "PDF"
                    val docIdLabel = if (info.docId > 0) "  ·  Doc #${info.docId}" else ""
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Last upload",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                                // E: Live status badge
                                DocStatusChip(info.docStatus)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "$docLabel · $typeLabel · $timeStr$docIdLabel",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // ── Onboarding tooltip (shown until user scans) ──────────────
            if (showTooltip && recentDocs.isEmpty()) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Getting started",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Tap the Scan button below to upload a document.\n" +
                                "• Single page → uploads as JPEG\n" +
                                "• Multiple pages → uploads as PDF\n" +
                                "You can pick a template during the upload.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }

            // ── Recent documents (A + B + I) ─────────────────────────────
            if (recentDocs.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                Text(
                    "Recent Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            items(recentDocs) { doc ->
                    RecentDocumentRow(
                        doc = doc,
                        onReprocess = if (templates.isNotEmpty() && doc.status == "failed") {
                            { reprocessDoc = doc }
                        } else null
                    )
                }
            }

            // ── Empty state ──────────────────────────────────────────────
            if (recentDocs.isEmpty() && uploadState is UploadUiState.Idle && !showTooltip) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_camera),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No Documents Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tap Scan to upload and process your first document.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            }
        }
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = MaterialTheme.colorScheme.primary
        )
    }
}
}

// ── Document status chip (B) ─────────────────────────────────────────────────

@Composable
fun DocStatusChip(status: String) {
    val (label, color) = when (status) {
        "processed", "ready_for_export", "exported" -> "Processed" to Color(0xFF2E7D32)
        "processing", "queued" -> "Processing" to Color(0xFF1565C0)
        "pending" -> "Pending" to Color(0xFFF57F17)
        "failed", "rejected" -> "Failed" to Color(0xFFC62828)
        else -> status.replaceFirstChar { it.uppercase() } to Color(0xFF546E7A)
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

// ── Recent document row (A + B + G + I) ─────────────────────────────────────

@Composable
fun RecentDocumentRow(
    doc: UploadedDocument,
    onReprocess: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        doc.originalFilename,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    // G: Duplicate badge
                    if (doc.isDuplicate) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFFFF3E0)
                        ) {
                            Text(
                                "Duplicate",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE65100),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (doc.templateName != null) {
                        Text(
                            doc.templateName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("·", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (doc.docId > 0) {
                        Text(
                            "#${doc.docId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DocStatusChip(doc.status)
                // I: Reprocess button for failed docs
                if (onReprocess != null) {
                    TextButton(
                        onClick = onReprocess,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("Retry", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

// ── C: Template picker bottom-sheet ─────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatePicker(
    templates: List<Template>,
    onTemplateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                "Choose a template",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            Text(
                "The template tells the AI what to extract from your document.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(12.dp))
            templates.forEach { template ->
                ListItem(
                    headlineContent = { Text(template.name, fontWeight = FontWeight.Medium) },
                    supportingContent = template.description?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
                    modifier = Modifier.clickable { onTemplateSelected(template.templateId.toString()) }
                )
                Divider(modifier = Modifier.padding(horizontal = 24.dp))
            }
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "A template is required.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

// ── I: Reprocess picker ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReprocessPicker(
    doc: UploadedDocument,
    templates: List<Template>,
    onTemplateSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = "Reprocess \"${doc.originalFilename}\"",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            Text(
                "Choose a template to retry OCR extraction.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(12.dp))
            templates.forEach { template ->
                ListItem(
                    headlineContent = { Text(template.name, fontWeight = FontWeight.Medium) },
                    modifier = Modifier.clickable { onTemplateSelected(template.templateId) }
                )
                Divider(modifier = Modifier.padding(horizontal = 24.dp))
            }
        }
    }
}