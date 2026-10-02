package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChangeType
import com.example.data.model.ConvertedFile
import com.example.data.model.FileCategory
import com.example.data.model.ModFile
import com.example.ui.theme.*

@Composable
fun ModFileItem(
    file: ModFile,
    isArabic: Boolean,
    modifier: Modifier = Modifier
) {
    val (icon, iconTint, badgeText, badgeColor) = when (file.category) {
        FileCategory.LUA -> Quadruple(Icons.Default.Code, IsaacBloodRed, if (isArabic) "لوا (غير مدعوم بالبورت)" else "LUA (Not Executable)", IsaacBloodRed)
        FileCategory.XML -> Quadruple(Icons.Default.Description, IsaacGold, "XML", IsaacGold)
        FileCategory.ANM2 -> Quadruple(Icons.Default.Description, IsaacTearBlue, "ANM2", IsaacTearBlue)
        FileCategory.GFX -> Quadruple(Icons.Default.Image, IsaacSuccess, "GFX", IsaacSuccess)
        FileCategory.AUDIO -> Quadruple(Icons.Default.MusicNote, IsaacSoulBlue, "AUDIO", IsaacSoulBlue)
        FileCategory.OTHER -> Quadruple(Icons.Default.Description, IsaacTextMuted, "FILE", IsaacTextMuted)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(IsaacSurface)
            .border(1.dp, IsaacBorder.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = file.path,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = IsaacBoneWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(badgeColor.copy(alpha = 0.15f))
                .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = badgeText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = badgeColor
            )
        }
    }
}

@Composable
fun ConvertedFileItem(
    file: ConvertedFile,
    isSelected: Boolean,
    isArabic: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (typeText, typeColor) = when (file.changeType) {
        ChangeType.CONVERTED_FROM_LUA -> (if (isArabic) "تم التوليد من LUA" else "AI Generated") to IsaacSoulBlue
        ChangeType.XML_RESTRUCTURED -> (if (isArabic) "نُقل إلى resources/" else "Restructured") to IsaacGold
        ChangeType.PATH_NORMALIZED -> (if (isArabic) "مسار موحّد" else "Normalized") to IsaacTearBlue
        ChangeType.COPIED_CLEAN -> (if (isArabic) "متوافق" else "Clean") to IsaacSuccess
        ChangeType.REMOVED_INCOMPATIBLE -> (if (isArabic) "مستبعد" else "Stripped") to IsaacBloodRed
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) IsaacSurfaceVariant else IsaacSurface)
            .border(
                1.dp,
                if (isSelected) IsaacTearBlue else IsaacBorder.copy(alpha = 0.6f),
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.targetPath,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) IsaacTearBlue else IsaacBoneWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (isArabic) file.descriptionAr else file.descriptionEn,
                fontSize = 10.sp,
                color = IsaacTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(typeColor.copy(alpha = 0.15f))
                .border(1.dp, typeColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text(
                text = typeText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = typeColor
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
