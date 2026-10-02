package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainUiState
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var keyInput by remember(uiState.apiKey) { mutableStateOf(uiState.apiKey) }
    var isKeyHidden by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Gemini API Key Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = IsaacSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, IsaacBorder),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, null, tint = IsaacGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.isArabic) "مفتاح Google Gemini API" else "Google Gemini API Key",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = IsaacBoneWhite
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (uiState.isArabic)
                            "يستخدم التطبيق ذكاء جيميناي الاصطناعي لتحليل أكواد Lua وتحويلها إلى ملفات XML متوافقة مع بورت أندرويد."
                        else
                            "The app uses Gemini AI to inspect PC mod Lua scripts and decompile them into Android port compatible XML.",
                        fontSize = 12.sp,
                        color = IsaacTextMuted
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = {
                            keyInput = it
                            viewModel.updateApiKey(it)
                        },
                        label = { Text(if (uiState.isArabic) "أدخل مفتاحك الخاص (API Key)" else "Enter your Gemini API Key") },
                        visualTransformation = if (isKeyHidden) PasswordVisualTransformation() else VisualTransformation.None,
                        trailingIcon = {
                            IconButton(onClick = { isKeyHidden = !isKeyHidden }) {
                                Icon(
                                    imageVector = if (isKeyHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle visibility",
                                    tint = IsaacTextMuted
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IsaacTearBlue,
                            unfocusedBorderColor = IsaacBorder,
                            focusedTextColor = IsaacBoneWhite,
                            unfocusedTextColor = IsaacBoneWhite
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.testApiKey() },
                            enabled = !uiState.isTestingKey && keyInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = IsaacSurfaceVariant),
                            border = androidx.compose.foundation.BorderStroke(1.dp, IsaacTearBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_api_key_button")
                        ) {
                            if (uiState.isTestingKey) {
                                CircularProgressIndicator(
                                    color = IsaacTearBlue,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = if (uiState.isArabic) "فحص المفتاح" else "Test Key",
                                color = IsaacTearBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                                context.startActivity(intent)
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, IsaacBorder),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("get_key_link_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = IsaacGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.isArabic) "احصل على مفتاح مجاني" else "Get Free Key",
                                color = IsaacGold
                            )
                        }
                    }

                    // Test Status message
                    val status = uiState.keyTestStatus
                    if (status != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val isOk = uiState.isKeyValid == true
                        Text(
                            text = status,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isOk) IsaacSuccess else IsaacBloodRed
                        )
                    }
                }
            }
        }

        // Model Selector
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = IsaacSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, IsaacBorder),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (uiState.isArabic) "اختر نموذج الذكاء الاصطناعي" else "Select Gemini Model",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = IsaacBoneWhite
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ModelOption(
                        modelId = "gemini-3.5-flash",
                        modelTitle = "Gemini 3.5 Flash",
                        modelSubtitle = if (uiState.isArabic) "سريع، دقيق، ومثالي لفحص أكواد اللوا وتحويلها (مستحسن)" else "Fast, highly accurate, best for Lua script decompile (Recommended)",
                        isSelected = uiState.selectedModel == "gemini-3.5-flash",
                        onSelect = { viewModel.updateModel("gemini-3.5-flash") }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    ModelOption(
                        modelId = "gemini-3.1-pro-preview",
                        modelTitle = "Gemini 3.1 Pro Preview",
                        modelSubtitle = if (uiState.isArabic) "نموذج استنتاج عميق ومتقدم للمودات الضخمة والمعقدة" else "Advanced reasoning model for large multi-file mods",
                        isSelected = uiState.selectedModel == "gemini-3.1-pro-preview",
                        onSelect = { viewModel.updateModel("gemini-3.1-pro-preview") }
                    )
                }
            }
        }

        // App Information & Modding Guide Reference
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = IsaacSurfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, IsaacBorder),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (uiState.isArabic) "حول التطبيق والشروط" else "About & Port Modding Rules",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = IsaacGold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (uiState.isArabic) {
                            "تم بناء هذا التطبيق خصيصاً لمجتمع لاعبي The Binding of Isaac Repentance على أندرويد بالاعتماد على دليل التعديل المعتمد:\nhttps://tboi-android-modding-guide.netlify.app/\n\nيقوم المحرك بفرز الموارد وتحويل مسارات الحروف الكبيرة إلى حروف صغيرة، وحذف استدعاءات اللوا غير المتوافقة، وتوليد ملفات XML المتوافقة مع أندرويد."
                        } else {
                            "Built specifically for The Binding of Isaac Repentance Android community following the guidelines at:\nhttps://tboi-android-modding-guide.netlify.app/\n\nThe porter normalizes Linux casing, eliminates broken Lua runtime dependencies, and compiles passive XML definitions for the mobile port."
                        },
                        fontSize = 11.sp,
                        color = IsaacBoneWhite.copy(alpha = 0.85f),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelOption(
    modelId: String,
    modelTitle: String,
    modelSubtitle: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) IsaacSurfaceVariant else IsaacBackground)
            .border(
                1.dp,
                if (isSelected) IsaacTearBlue else IsaacBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onSelect)
            .padding(12.dp)
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(selectedColor = IsaacTearBlue)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = modelTitle,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) IsaacTearBlue else IsaacBoneWhite
            )
            Text(
                text = modelSubtitle,
                fontSize = 10.sp,
                color = IsaacTextMuted
            )
        }
    }
}
