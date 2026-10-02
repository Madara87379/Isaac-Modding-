package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainUiState
import com.example.ui.theme.*

@Composable
fun GuideScreen(
    uiState: MainUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val guideUrl = "https://tboi-android-modding-guide.netlify.app/"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Guide Link Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = IsaacSurfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, IsaacTearBlue.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (uiState.isArabic) "دليل شروط بورت أندرويد الرسمي" else "TBOI Android Modding Guide",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = IsaacGold
                        )
                        Text(
                            text = "tboi-android-modding-guide.netlify.app",
                            fontSize = 11.sp,
                            color = IsaacTearBlue,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(guideUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error opening browser", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IsaacTearBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("open_guide_url_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = IsaacSurface, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.isArabic) "فتح" else "Open",
                            color = IsaacSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Rule 1: No Lua Scripts
        item {
            GuideRuleCard(
                icon = Icons.Default.Block,
                iconColor = IsaacBloodRed,
                ruleNumber = "1",
                title = if (uiState.isArabic) "البورت لا يدعم السكربتات (The Port Runs No Scripts)" else "The Port Runs No Scripts",
                description = if (uiState.isArabic) {
                    "على عكس نسخة الكمبيوتر عبر Steam Workshop، بورت أندرويد لا يحتوي على محرك Lua VM أو C++ Extender (مثل REPENTOGON). ولذلك فإن ملفات main.lua لن تنفذ. يقوم تطبيقنا بقراءة كود اللوا واستخراج التعديلات الإحصائية والمظاهر وتحويلها إلى ملفات XML ثابتة."
                } else {
                    "Unlike the PC Steam Workshop version, the Android port has no Lua virtual machine or C++ script extender (REPENTOGON). main.lua scripts do not run. This tool inspects Lua code and compiles passive stats and visuals into static XML replacements."
                }
            )
        }

        // Rule 2: Resource Replacement Format
        item {
            GuideRuleCard(
                icon = Icons.Default.Folder,
                iconColor = IsaacGold,
                ruleNumber = "2",
                title = if (uiState.isArabic) "صيغة استبدال الموارد فقط (Resources Format Only)" else "Format Holds Resources Only",
                description = if (uiState.isArabic) {
                    "يجب أن تكون جميع ملفات المود داخل مجلد 'resources/' مباشرة (مثل: resources/gfx, resources/sfx, resources/music). مجلد 'content/' الخاص بنظام دمج الورشة على الحاسوب لا يعمل على أندرويد ويجب نقله وتسطيحه داخل resources/."
                } else {
                    "All mod assets must be placed inside the standard 'resources/' directory (e.g. resources/gfx, resources/sfx, resources/music). PC Workshop 'content/' folders must be converted directly into root 'resources/' XML files."
                }
            )
        }

        // Rule 3: Case-Sensitivity
        item {
            GuideRuleCard(
                icon = Icons.Default.TextFormat,
                iconColor = IsaacTearBlue,
                ruleNumber = "3",
                title = if (uiState.isArabic) "حساسية حالة الأحرف لنظام لينكس (Case Sensitivity)" else "Linux / Android Case Sensitivity",
                description = if (uiState.isArabic) {
                    "نظام ملفات أندرويد (ext4/f2fs) يعتبر 'Resources/Gfx/' مختلفاً تماماً عن 'resources/gfx/'. الحروف الكبيرة في مودات ويندوز تسبب اختفاء الصور وظهور مربعات سوداء أو إغلاق اللعبة. تطبيقنا يوحد جميع الحروف للمسارات القياسية."
                } else {
                    "Android filesystem is case-sensitive, unlike Windows. Paths like 'Gfx/Characters/' will fail to load or crash the game. This tool automatically forces canonical lowercase paths for all game folders."
                }
            )
        }

        // Rule 4: XML Structure
        item {
            GuideRuleCard(
                icon = Icons.Default.Code,
                iconColor = IsaacSoulBlue,
                ruleNumber = "4",
                title = if (uiState.isArabic) "تنسيق ملفات XML والكوستيوم" else "XML & Costume Format",
                description = if (uiState.isArabic) {
                    "ملفات items.xml و players.xml و costumes2.xml هي الطريقة المتبقية لتعديل اللعبة على البورت بدون أكواد. يمكنك تغيير الهجوم، السرعة، الدموع، إعطاء الطيران، وتغيير مظهر الشخصية أو الوحوش بالكامل."
                } else {
                    "items.xml, players.xml, and costumes2.xml are the native way to customize stats, flight, heart containers, and appearances on the port without needing any executable code."
                }
            )
        }

        // Rule 5: Installation Folder
        item {
            GuideRuleCard(
                icon = Icons.Default.SdCard,
                iconColor = IsaacSuccess,
                ruleNumber = "5",
                title = if (uiState.isArabic) "مسار المودات على أندرويد (Android Media Folder)" else "Mod Directory Path on Android",
                description = if (uiState.isArabic) {
                    "يتم تثبيت المودات عادة داخل مجلد Media للعبة:\nAndroid/media/com.nicalis.bindingofisaac/resources/\nعلى أندرويد 13 و 14، استخدم تطبيق ZArchiver أو FV File Explorer للوصول إلى هذا المسار بسهولة."
                } else {
                    "Mods reside in the game's Media folder:\nAndroid/media/com.nicalis.bindingofisaac/resources/\nOn Android 13/14, use ZArchiver or FV File Explorer to extract into this folder."
                }
            )
        }
    }
}

@Composable
private fun GuideRuleCard(
    icon: ImageVector,
    iconColor: Color,
    ruleNumber: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = IsaacSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, IsaacBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(iconColor.copy(alpha = 0.15f))
                        .border(1.dp, iconColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ruleNumber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = iconColor
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = IsaacBoneWhite
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                fontSize = 12.sp,
                color = IsaacBoneWhite.copy(alpha = 0.85f),
                lineHeight = 18.sp
            )
        }
    }
}
