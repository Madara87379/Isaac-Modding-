package com.example.data.model

enum class FileCategory {
    LUA,
    XML,
    ANM2,
    GFX,
    AUDIO,
    OTHER
}

enum class ChangeType {
    COPIED_CLEAN,
    PATH_NORMALIZED,
    CONVERTED_FROM_LUA,
    XML_RESTRUCTURED,
    REMOVED_INCOMPATIBLE
}

data class ModFile(
    val path: String,
    val textContent: String? = null,
    val binaryBytes: ByteArray? = null,
    val size: Long = 0,
    val category: FileCategory = FileCategory.OTHER
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ModFile) return false
        return path == other.path
    }

    override fun hashCode(): Int = path.hashCode()
}

data class ModMetadata(
    val name: String = "Unknown Isaac Mod",
    val directory: String = "",
    val id: String = "",
    val version: String = "1.0",
    val description: String = ""
)

data class ModAnalysis(
    val metadata: ModMetadata,
    val files: List<ModFile>,
    val luaFiles: List<ModFile>,
    val xmlFiles: List<ModFile>,
    val gfxFiles: List<ModFile>,
    val audioFiles: List<ModFile>,
    val issuesDetected: List<String>,
    val isPureResourceMod: Boolean,
    val initialCompatibilityScore: Int
)

data class ConvertedFile(
    val targetPath: String,
    val originalPath: String?,
    val textContent: String? = null,
    val binaryBytes: ByteArray? = null,
    val changeType: ChangeType,
    val descriptionAr: String,
    val descriptionEn: String
)

data class ConversionReport(
    val modName: String,
    val compatibilityScore: Int,
    val summaryAr: String,
    val summaryEn: String,
    val convertedFiles: List<ConvertedFile>,
    val removedScripts: List<String>,
    val convertedXmlAdditions: List<String>,
    val tipsAr: List<String>,
    val tipsEn: List<String>,
    val androidTargetPath: String = "Android/media/com.nicalis.bindingofisaac/resources/"
)

data class GeminiMessage(
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
