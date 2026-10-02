package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainUiState
import com.example.ui.MainViewModel
import com.example.ui.components.CompatibilityGauge
import com.example.ui.components.ConvertedFileItem
import com.example.ui.components.ModFileItem
import com.example.ui.theme.*

@Composable
fun ConverterScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // ZIP picker launcher
    val pickZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.loadZipFromUri(uri)
        }
    }

    // Export ZIP launcher
    val exportZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            try {
                val outputStream = context.contentResolver.openOutputStream(uri)
                if (outputStream != null) {
                    viewModel.exportConvertedZip(outputStream)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- PRESET SAMPLE MODS & IMPORT BUTTON ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = IsaacSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, IsaacBorder),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (uiState.isArabic) "اختر مود PC للاختبار أو استورد ملف ZIP" else "Select Sample Mod or Import PC ZIP",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = IsaacBoneWhite
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.loadedModMetadata?.name?.contains("Neko") == true,
                            onClick = { viewModel.loadSampleMod(0) },
                            label = { Text("1. Neko Isaac (Assets)") },
                            leadingIcon = { Icon(Icons.Default.Pets, null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IsaacSurfaceVariant,
                                selectedLabelColor = IsaacTearBlue
                            ),
                            modifier = Modifier.testTag("sample_mod_1")
                        )

                        FilterChip(
                            selected = uiState.loadedModMetadata?.name?.contains("Brimstone") == true,
                            onClick = { viewModel.loadSampleMod(1) },
                            label = { Text("2. Brimstone Horns (Lua)") },
                            leadingIcon = { Icon(Icons.Default.Whatshot, null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IsaacSurfaceVariant,
                                selectedLabelColor = IsaacBloodRed
                            ),
                            modifier = Modifier.testTag("sample_mod_2")
                        )

                        FilterChip(
                            selected = uiState.loadedModMetadata?.name?.contains("Fallen") == true,
                            onClick = { viewModel.loadSampleMod(2) },
                            label = { Text("3. Tainted Fallen (Char)") },
                            leadingIcon = { Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IsaacSurfaceVariant,
                                selectedLabelColor = IsaacGold
                            ),
                            modifier = Modifier.testTag("sample_mod_3")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { pickZipLauncher.launch("application/zip") },
                        colors = ButtonDefaults.buttonColors(containerColor = IsaacSurfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, IsaacTearBlue.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pick_zip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderZip,
                            contentDescription = null,
                            tint = IsaacTearBlue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.isArabic) "فتح ملف مود مضغوط (Pick PC Mod .ZIP)" else "Browse PC Mod ZIP Archive",
                            color = IsaacBoneWhite,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // --- LOADED MOD DETAILS & ANALYSIS ---
        val metadata = uiState.loadedModMetadata
        val analysis = uiState.modAnalysis
        if (metadata != null && analysis != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = IsaacSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, IsaacBorder),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = metadata.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IsaacGold
                                )
                                Text(
                                    text = "v${metadata.version} • ${analysis.files.size} ${if (uiState.isArabic) "ملف في أرشيف المود" else "files in mod"}",
                                    fontSize = 12.sp,
                                    color = IsaacTextMuted
                                )
                            }

                            if (analysis.luaFiles.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(IsaacBloodRed.copy(alpha = 0.15f))
                                        .border(1.dp, IsaacBloodRed.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${analysis.luaFiles.size} Lua Scripts",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = IsaacBloodRed
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(IsaacSuccess.copy(alpha = 0.15f))
                                        .border(1.dp, IsaacSuccess.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (uiState.isArabic) "مود نقي (No Lua)" else "Pure Assets",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = IsaacSuccess
                                    )
                                }
                            }
                        }

                        if (metadata.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = metadata.description,
                                fontSize = 12.sp,
                                color = IsaacBoneWhite.copy(alpha = 0.8f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Warnings / Issues detected
                        if (analysis.issuesDetected.isNotEmpty()) {
                            Text(
                                text = if (uiState.isArabic) "ملاحظات الفحص حسب شروط بورت أندرويد:" else "Port Compatibility Checklist:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = IsaacWarning
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            analysis.issuesDetected.forEach { issue ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = IsaacWarning,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = issue,
                                        fontSize = 11.sp,
                                        color = IsaacBoneWhite.copy(alpha = 0.9f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Compatibility gauge
                        CompatibilityGauge(
                            score = analysis.initialCompatibilityScore,
                            isArabic = uiState.isArabic
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Start Conversion Button
                        Button(
                            onClick = { viewModel.startConversion() },
                            enabled = !uiState.isConverting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IsaacTearBlue,
                                contentColor = IsaacSurface
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("start_conversion_button")
                        ) {
                            if (uiState.isConverting) {
                                CircularProgressIndicator(
                                    color = IsaacSurface,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = uiState.conversionStage,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (uiState.isArabic) "تحويل المود لبورت أندرويد بالذكاء الاصطناعي" else "Port Mod to Android with Gemini AI",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // Original Files Preview Collapsible
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = IsaacSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, IsaacBorder),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (uiState.isArabic) "ملفات المود الأصلية (PC Structure)" else "Original PC Files",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = IsaacBoneWhite
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            analysis.files.forEach { file ->
                                ModFileItem(file = file, isArabic = uiState.isArabic)
                            }
                        }
                    }
                }
            }
        }

        // --- CONVERSION RESULTS ---
        val report = uiState.conversionReport
        if (report != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = IsaacSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, IsaacSuccess.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = IsaacSuccess,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (uiState.isArabic) "اكتمل التحويل بنجاح!" else "Conversion Completed!",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IsaacSuccess
                                )
                            }

                            Text(
                                text = "${report.compatibilityScore}% Compatible",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = IsaacGold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // AI Summary
                        Text(
                            text = if (uiState.isArabic) report.summaryAr else report.summaryEn,
                            fontSize = 13.sp,
                            color = IsaacBoneWhite
                        )

                        if (report.removedScripts.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (uiState.isArabic) "ملفات اللوا التي تم استبعادها (البورت لا يشغل سكربتات):" else "Stripped Lua Scripts (Port runs no scripts):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = IsaacBloodRed
                            )
                            report.removedScripts.forEach { script ->
                                Text(
                                    text = " • $script",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = IsaacBloodRed.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Export ZIP button
                        Button(
                            onClick = {
                                val cleanName = report.modName.replace("[^a-zA-Z0-9_]".toRegex(), "_")
                                exportZipLauncher.launch("[Android_Port]_$cleanName.zip")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IsaacSuccess),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("export_zip_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = IsaacSurface)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uiState.isArabic) "تصدير حزمة أندرويد (.ZIP جاهز للعبة)" else "Export Converted Android ZIP",
                                color = IsaacSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Converted Files Section & Interactive Preview
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = IsaacSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, IsaacBorder),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (uiState.isArabic) "الملفات الناتجة في حزمة أندرويد (انقر للمعاينة):" else "Output Files in Android Resources (Click to inspect):",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = IsaacBoneWhite
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            report.convertedFiles.forEach { file ->
                                ConvertedFileItem(
                                    file = file,
                                    isSelected = uiState.previewFile?.targetPath == file.targetPath,
                                    isArabic = uiState.isArabic,
                                    onClick = { viewModel.selectFilePreview(file) }
                                )
                            }
                        }

                        // Preview Box if a file with text is selected
                        val preview = uiState.previewFile
                        if (preview?.textContent != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(IsaacBackground)
                                    .border(1.dp, IsaacBorder, RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = preview.targetPath,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = IsaacGold
                                        )
                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("code", preview.textContent))
                                                Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy",
                                                tint = IsaacTearBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = preview.textContent,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = IsaacBoneWhite.copy(alpha = 0.9f),
                                        maxLines = 14
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Direct Installation Guide Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = IsaacSurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, IsaacGold.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Place, null, tint = IsaacGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uiState.isArabic) "طريقة تثبيت المود على هاتف أندرويد" else "How to Install on Android Port",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = IsaacGold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (uiState.isArabic)
                                "قم بفك ضغط ملف الـ ZIP المُصدر داخل مجلد resources الخاص باللعبة على مسار:"
                            else
                                "Extract the exported ZIP contents into the game's resources directory at:",
                            fontSize = 12.sp,
                            color = IsaacBoneWhite
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Path box with copy button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(IsaacBackground)
                                .border(1.dp, IsaacBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = report.androidTargetPath,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = IsaacTearBlue,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("path", report.androidTargetPath))
                                    Toast.makeText(context, if (uiState.isArabic) "تم نسخ المسار!" else "Path copied!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("copy_path_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Path",
                                    tint = IsaacGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val tips = if (uiState.isArabic) report.tipsAr else report.tipsEn
                        tips.forEach { tip ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    null,
                                    tint = IsaacSuccess,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tip,
                                    fontSize = 11.sp,
                                    color = IsaacBoneWhite.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
