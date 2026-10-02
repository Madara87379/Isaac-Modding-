package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun CompatibilityGauge(
    score: Int,
    isArabic: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(durationMillis = 800),
        label = "compat_score"
    )

    val (badgeColor, statusTitle) = when {
        score >= 90 -> IsaacSuccess to if (isArabic) "توافق مثالي (موارد نقية)" else "Full Compatibility (Pure Assets)"
        score >= 70 -> IsaacSoulBlue to if (isArabic) "توافق مرتفع (تحويل Lua لـ XML)" else "High Compatibility (Lua to XML)"
        score >= 50 -> IsaacWarning to if (isArabic) "توافق جزئي (أكواد معقدة)" else "Partial (Complex Scripts)"
        else -> IsaacBloodRed to if (isArabic) "يتطلب تعديل يدوي" else "Needs Manual Overhaul"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(IsaacSurfaceVariant)
            .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (score >= 70) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = "Status",
                        tint = badgeColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusTitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = IsaacBoneWhite
                    )
                }

                Text(
                    text = "$score%",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = badgeColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = badgeColor,
                trackColor = IsaacBackground
            )
        }
    }
}
