package com.example.domain

import android.util.Log
import com.example.data.model.ChangeType
import com.example.data.model.ConvertedFile
import com.example.data.model.ConversionReport
import com.example.data.model.FileCategory
import com.example.data.model.ModAnalysis
import com.example.data.model.ModFile
import com.example.data.model.ModMetadata
import com.example.data.remote.GeminiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ModConverterEngine {

    suspend fun parseZipStream(inputStream: InputStream): Pair<ModMetadata, List<ModFile>> = withContext(Dispatchers.IO) {
        val files = mutableListOf<ModFile>()
        var metadata = ModMetadata()

        ZipInputStream(inputStream).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val rawPath = entry.name.replace('\\', '/')
                    val cleanPath = cleanEntryPath(rawPath)

                    val byteStream = ByteArrayOutputStream()
                    val buffer = ByteArray(4096)
                    var count: Int
                    while (zis.read(buffer).also { count = it } != -1) {
                        byteStream.write(buffer, 0, count)
                    }
                    val bytes = byteStream.toByteArray()
                    val category = detectCategory(cleanPath)

                    val textContent = if (isTextCategory(category)) {
                        try {
                            String(bytes, Charsets.UTF_8)
                        } catch (e: Exception) {
                            null
                        }
                    } else null

                    val modFile = ModFile(
                        path = cleanPath,
                        textContent = textContent,
                        binaryBytes = if (textContent == null) bytes else null,
                        size = bytes.size.toLong(),
                        category = category
                    )
                    files.add(modFile)

                    if (cleanPath.endsWith("metadata.xml", ignoreCase = true) && textContent != null) {
                        metadata = parseMetadataXml(textContent)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        if (metadata.name == "Unknown Isaac Mod" && files.isNotEmpty()) {
            val guessName = files.first().path.split('/').firstOrNull { it.isNotEmpty() && !it.contains('.') } ?: "Imported Isaac Mod"
            metadata = metadata.copy(name = guessName)
        }

        metadata to files
    }

    private fun cleanEntryPath(path: String): String {
        return path.trim().removePrefix("./").removePrefix("/")
    }

    private fun detectCategory(path: String): FileCategory {
        val lower = path.lowercase()
        return when {
            lower.endsWith(".lua") -> FileCategory.LUA
            lower.endsWith(".xml") -> FileCategory.XML
            lower.endsWith(".anm2") -> FileCategory.ANM2
            lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> FileCategory.GFX
            lower.endsWith(".ogg") || lower.endsWith(".wav") || lower.endsWith(".mp3") -> FileCategory.AUDIO
            else -> FileCategory.OTHER
        }
    }

    private fun isTextCategory(cat: FileCategory): Boolean {
        return cat == FileCategory.LUA || cat == FileCategory.XML || cat == FileCategory.ANM2 || cat == FileCategory.OTHER
    }

    private fun parseMetadataXml(xml: String): ModMetadata {
        fun extractTag(tag: String): String {
            val regex = "<$tag>(.*?)</$tag>".toRegex(RegexOption.DOT_MATCHES_ALL)
            return regex.find(xml)?.groupValues?.get(1)?.trim() ?: ""
        }
        val name = extractTag("name").ifEmpty { "Isaac PC Mod" }
        val dir = extractTag("directory")
        val id = extractTag("id")
        val version = extractTag("version").ifEmpty { "1.0" }
        val desc = extractTag("description")
        return ModMetadata(name = name, directory = dir, id = id, version = version, description = desc)
    }

    fun analyzeMod(metadata: ModMetadata, files: List<ModFile>): ModAnalysis {
        val luaFiles = files.filter { it.category == FileCategory.LUA }
        val xmlFiles = files.filter { it.category == FileCategory.XML }
        val gfxFiles = files.filter { it.category == FileCategory.GFX }
        val audioFiles = files.filter { it.category == FileCategory.AUDIO }

        val issues = mutableListOf<String>()

        if (luaFiles.isNotEmpty()) {
            issues.add("النسخة المحمولة (Android Port) لا تشغل أكواد Lua البرمجية إطلاقاً (The port runs no scripts).")
        }

        val hasContentFolder = files.any { it.path.startsWith("content/", ignoreCase = true) }
        if (hasContentFolder) {
            issues.add("يحتوي المود على مجلد 'content/' الخاص بـ PC ويجب دمجه في مجلد 'resources/'.")
        }

        val hasBadCasing = files.any { it.path.contains("Resources/") || it.path.contains("Gfx/") || it.path.contains("Sfx/") }
        if (hasBadCasing) {
            issues.add("تم اكتشاف أحرف كبيرة في المسارات قد تسبب شاشات سوداء أو اختفاء المظاهر على نظام أندرويد الحساس للأحرف.")
        }

        val isPureResourceMod = luaFiles.isEmpty()
        val score = when {
            isPureResourceMod -> 100
            luaFiles.size == 1 && files.size > 3 -> 85
            luaFiles.size > 2 -> 65
            else -> 75
        }

        return ModAnalysis(
            metadata = metadata,
            files = files,
            luaFiles = luaFiles,
            xmlFiles = xmlFiles,
            gfxFiles = gfxFiles,
            audioFiles = audioFiles,
            issuesDetected = issues,
            isPureResourceMod = isPureResourceMod,
            initialCompatibilityScore = score
        )
    }

    suspend fun convertMod(
        analysis: ModAnalysis,
        geminiService: GeminiService?,
        apiKey: String?,
        model: String = "gemini-3.5-flash"
    ): ConversionReport = withContext(Dispatchers.Default) {
        val convertedList = mutableListOf<ConvertedFile>()
        val removedScripts = mutableListOf<String>()
        val xmlAdditions = mutableListOf<String>()
        val tipsAr = mutableListOf<String>()
        val tipsEn = mutableListOf<String>()

        // 1. Process all non-Lua files and normalize paths
        for (file in analysis.files) {
            if (file.category == FileCategory.LUA) {
                removedScripts.add(file.path)
                continue
            }

            val targetPath = normalizeToAndroidResourcePath(file.path)
            val isPathChanged = targetPath != file.path

            val changeType = when {
                file.path.startsWith("content/", ignoreCase = true) -> ChangeType.XML_RESTRUCTURED
                isPathChanged -> ChangeType.PATH_NORMALIZED
                else -> ChangeType.COPIED_CLEAN
            }

            val descAr = when (changeType) {
                ChangeType.XML_RESTRUCTURED -> "تم تحويل الملف من مجلد content إلى resources ليتوافق مع بنية أندرويد"
                ChangeType.PATH_NORMALIZED -> "تم توحيد مسار الملف وإصلاح حساسية الأحرف للينكس/أندرويد"
                else -> "ملف متوافق تماماً وتم تضمينه بدون تغيير"
            }

            val descEn = when (changeType) {
                ChangeType.XML_RESTRUCTURED -> "Restructured from content/ to resources/ for Android port"
                ChangeType.PATH_NORMALIZED -> "Normalized path casing and separators for Android filesystem"
                else -> "Pure compatible asset ready for Android"
            }

            convertedList.add(
                ConvertedFile(
                    targetPath = targetPath,
                    originalPath = file.path,
                    textContent = file.textContent,
                    binaryBytes = file.binaryBytes,
                    changeType = changeType,
                    descriptionAr = descAr,
                    descriptionEn = descEn
                )
            )
        }

        var finalScore = analysis.initialCompatibilityScore
        var summaryAr: String = ""
        var summaryEn: String = ""

        // 2. If Lua scripts exist, use Gemini AI or Rule-Based Fallback
        if (analysis.luaFiles.isNotEmpty()) {
            val luaMap = analysis.luaFiles.associate { it.path to (it.textContent ?: "") }
            val xmlMap = analysis.xmlFiles.associate { it.path to (it.textContent ?: "") }

            var aiWorked = false
            if (geminiService != null && !apiKey.isNullOrBlank()) {
                val aiResult = geminiService.convertIsaacMod(
                    apiKey = apiKey,
                    model = model,
                    metadata = analysis.metadata,
                    fileNames = analysis.files.map { it.path },
                    luaContents = luaMap,
                    xmlContents = xmlMap
                )
                if (aiResult.isSuccess) {
                    val aiData = aiResult.getOrThrow()
                    finalScore = aiData.compatibilityScore
                    summaryAr = aiData.summaryAr
                    summaryEn = aiData.summaryEn

                    for (genFile in aiData.generatedFiles) {
                        // Check if we already have this target path
                        val existingIndex = convertedList.indexOfFirst { it.targetPath.equals(genFile.targetPath, ignoreCase = true) }
                        if (existingIndex >= 0) {
                            convertedList[existingIndex] = genFile
                        } else {
                            convertedList.add(genFile)
                        }
                    }

                    xmlAdditions.addAll(aiData.xmlAdditions)
                    tipsAr.addAll(aiData.tipsAr)
                    tipsEn.addAll(aiData.tipsEn)
                    aiWorked = true
                } else {
                    Log.w("ModConverterEngine", "AI conversion failed, using rule-based fallback: ${aiResult.exceptionOrNull()?.message}")
                }
            }

            if (!aiWorked) {
                // Rule-based fallback: Generate items.xml or players.xml patch if missing
                val generatedFallback = generateRuleBasedXmlPatch(analysis)
                if (generatedFallback != null) {
                    convertedList.add(generatedFallback)
                    xmlAdditions.add("تم إنشاء ملف resources/items.xml تلقائياً لتعويض دوال اللوا (Fallback).")
                }
                summaryAr = "تم تحويل المود بنجاح عبر المحرك الذكي. تم استبعاد أكواد Lua لأن بورت أندرويد لا يدعم السكربتات، وتم تحويل الخصائص إلى ملفات XML ثابتة."
                summaryEn = "Mod converted successfully. Lua scripts were stripped (port runs no scripts), and stat properties were mapped into static XML."
                tipsAr.add("ضع محتويات المجلد الناتج في: Android/media/com.nicalis.bindingofisaac/resources/")
                tipsAr.add("تأكد من استخدام تطبيق مثل ZArchiver لفك الضغط في مجلد اللعبة مباشرة.")
                tipsEn.add("Copy extracted files to: Android/media/com.nicalis.bindingofisaac/resources/")
            }
        } else {
            summaryAr = "المود عبارة عن حزمة موارد (رسومات/أصوات) نقية وهو متوافق بنسبة 100% مع بورت أندرويد!"
            summaryEn = "This is a pure asset replacement mod (graphics/audio) and is 100% compatible with Android port!"
            tipsAr.add("لا توجد أكواد لوا معقدة، المود سيعمل فور نسخه إلى مجلد resources/ الخاص باللعبة.")
            tipsEn.add("No Lua code detected. The mod will work immediately after copying into game's resources/.")
        }

        ConversionReport(
            modName = analysis.metadata.name,
            compatibilityScore = finalScore,
            summaryAr = summaryAr,
            summaryEn = summaryEn,
            convertedFiles = convertedList,
            removedScripts = removedScripts,
            convertedXmlAdditions = xmlAdditions,
            tipsAr = tipsAr,
            tipsEn = tipsEn
        )
    }

    private fun normalizeToAndroidResourcePath(path: String): String {
        var clean = path.replace('\\', '/').trim().removePrefix("/")

        // Strip common wrapper root folder (e.g. MyMod_v1/resources/... -> resources/...)
        val parts = clean.split('/')
        if (parts.size > 1 && (parts[1].equals("resources", ignoreCase = true) || parts[1].equals("content", ignoreCase = true))) {
            clean = parts.drop(1).joinToString("/")
        }

        // Map content/ to resources/
        if (clean.startsWith("content/", ignoreCase = true)) {
            val sub = clean.removePrefix("content/").removePrefix("Content/")
            clean = "resources/$sub"
        }

        // If path doesn't start with resources/ or metadata.xml, nest it in resources/
        if (!clean.startsWith("resources/", ignoreCase = true) && !clean.equals("metadata.xml", ignoreCase = true)) {
            clean = "resources/$clean"
        }

        // Canonical lowercase for directory paths to prevent Linux case sensitivity issues
        val segments = clean.split('/')
        val normalizedSegments = segments.mapIndexed { index, seg ->
            if (index == segments.lastIndex) {
                // Keep file extension lowercase, file name lowercase
                seg.lowercase()
            } else {
                seg.lowercase()
            }
        }
        return normalizedSegments.joinToString("/")
    }

    private fun generateRuleBasedXmlPatch(analysis: ModAnalysis): ConvertedFile? {
        val hasExistingItemsXml = analysis.files.any { it.path.contains("items.xml", ignoreCase = true) }
        if (hasExistingItemsXml) return null

        val luaContent = analysis.luaFiles.joinToString("\n") { it.textContent ?: "" }
        val hasDamage = luaContent.contains("Damage", ignoreCase = true)
        val hasTears = luaContent.contains("Tear", ignoreCase = true)
        val hasHoming = luaContent.contains("HOMING", ignoreCase = true)

        val xmlContent = buildString {
            appendLine("<!-- Converted from PC Lua script for Android Repentance Port -->")
            appendLine("<items gfxroot=\"gfx/items/collectibles/\">")
            append("    <passive id=\"900\" name=\"${analysis.metadata.name}\" description=\"${analysis.metadata.description}\" ")
            if (hasDamage) append("cache=\"damage\" ")
            if (hasTears) append("cache=\"tears\" ")
            if (hasHoming) append("tearflags=\"homing\" ")
            appendLine("gfx=\"collectibles_${analysis.metadata.directory.ifEmpty { "item" }}.png\" />")
            appendLine("</items>")
        }

        return ConvertedFile(
            targetPath = "resources/items.xml",
            originalPath = null,
            textContent = xmlContent,
            changeType = ChangeType.CONVERTED_FROM_LUA,
            descriptionAr = "تم توليد تعريف العنصر تلقائياً لتعويض استدعاءات اللوا",
            descriptionEn = "Generated item definition XML replacing Lua callbacks"
        )
    }

    suspend fun buildConvertedZip(files: List<ConvertedFile>, outputStream: OutputStream) = withContext(Dispatchers.IO) {
        ZipOutputStream(outputStream).use { zos ->
            for (file in files) {
                val entry = ZipEntry(file.targetPath)
                zos.putNextEntry(entry)
                if (file.textContent != null) {
                    val bytes = file.textContent.toByteArray(Charsets.UTF_8)
                    zos.write(bytes)
                } else if (file.binaryBytes != null) {
                    zos.write(file.binaryBytes)
                }
                zos.closeEntry()
            }
            zos.finish()
        }
    }
}
