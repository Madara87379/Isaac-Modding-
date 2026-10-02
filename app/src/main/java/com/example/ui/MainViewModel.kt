package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ConvertedModDao
import com.example.data.local.ConvertedModEntity
import com.example.data.local.ModDatabase
import com.example.data.local.UserPreferences
import com.example.data.model.ConvertedFile
import com.example.data.model.ConversionReport
import com.example.data.model.GeminiMessage
import com.example.data.model.ModAnalysis
import com.example.data.model.ModFile
import com.example.data.model.ModMetadata
import com.example.data.remote.GeminiService
import com.example.domain.ModConverterEngine
import com.example.domain.SampleMods
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.OutputStream

enum class AppTab {
    CONVERTER,
    ASSISTANT,
    GUIDE,
    HISTORY,
    SETTINGS
}

data class MainUiState(
    val currentTab: AppTab = AppTab.CONVERTER,
    val isArabic: Boolean = true,
    val apiKey: String = "",
    val selectedModel: String = "gemini-3.5-flash",
    val isTestingKey: Boolean = false,
    val keyTestStatus: String? = null,
    val isKeyValid: Boolean? = null,

    // Converter state
    val isReadingZip: Boolean = false,
    val loadedModMetadata: ModMetadata? = null,
    val loadedFiles: List<ModFile> = emptyList(),
    val modAnalysis: ModAnalysis? = null,
    val isConverting: Boolean = false,
    val conversionStage: String = "",
    val conversionProgress: Float = 0f,
    val conversionReport: ConversionReport? = null,
    val conversionError: String? = null,
    val previewFile: ConvertedFile? = null,

    // AI Assistant state
    val assistantMessages: List<GeminiMessage> = emptyList(),
    val isAssistantThinking: Boolean = false,
    val assistantError: String? = null,

    // History state
    val historyList: List<ConvertedModEntity> = emptyList(),

    // SnackBar message
    val snackbarMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = UserPreferences(application)
    private val geminiService = GeminiService()
    private val converterEngine = ModConverterEngine()
    private val modDao: ConvertedModDao = ModDatabase.getInstance(application).convertedModDao()

    private val _uiState = MutableStateFlow(
        MainUiState(
            apiKey = prefs.getApiKey(),
            selectedModel = prefs.getModel(),
            isArabic = prefs.isArabic()
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        // Collect history
        viewModelScope.launch {
            modDao.getAllConvertedMods().collect { list ->
                _uiState.update { it.copy(historyList = list) }
            }
        }

        // Initialize with default welcome message in assistant
        val welcomeMsg = if (prefs.isArabic()) {
            "مرحباً بك في مساعد بورت The Binding of Isaac Repentance! يمكنك هنا سؤالي عن تحويل أي دالة Lua إلى ملفات XML، أو فحص رسائل الخطأ، أو الاستفسار عن شروط دليل netlify."
        } else {
            "Welcome to Isaac Repentance Android Mod Porter Assistant! Ask me anything about converting Lua callbacks to XML, fixing Android port crashes, or complying with the netlify modding guide."
        }
        _uiState.update {
            it.copy(
                assistantMessages = listOf(GeminiMessage(role = "model", content = welcomeMsg))
            )
        }

        // Load first sample mod by default so user can immediately test!
        loadSampleMod(0)
    }

    fun setTab(tab: AppTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun toggleLanguage() {
        val newLang = !_uiState.value.isArabic
        prefs.setArabic(newLang)
        _uiState.update { it.copy(isArabic = newLang) }
    }

    fun updateApiKey(key: String) {
        prefs.setApiKey(key)
        _uiState.update { it.copy(apiKey = key, keyTestStatus = null, isKeyValid = null) }
    }

    fun updateModel(model: String) {
        prefs.setModel(model)
        _uiState.update { it.copy(selectedModel = model) }
    }

    fun testApiKey() {
        val key = _uiState.value.apiKey
        if (key.isBlank()) {
            val msg = if (_uiState.value.isArabic) "يرجى إدخال مفتاح API أولاً" else "Please enter an API Key first"
            _uiState.update { it.copy(keyTestStatus = msg, isKeyValid = false) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isTestingKey = true, keyTestStatus = null) }
            val res = geminiService.testApiKey(key)
            if (res.isSuccess) {
                val okMsg = if (_uiState.value.isArabic) "تم التحقق بنجاح! المفتاح فعال وجاهز للتحويل." else "Connection successful! API key is active."
                _uiState.update { it.copy(isTestingKey = false, keyTestStatus = okMsg, isKeyValid = true) }
            } else {
                val errMsg = res.exceptionOrNull()?.message ?: "Unknown error"
                val failMsg = if (_uiState.value.isArabic) "فشل الاتصال: $errMsg" else "Connection failed: $errMsg"
                _uiState.update { it.copy(isTestingKey = false, keyTestStatus = failMsg, isKeyValid = false) }
            }
        }
    }

    fun loadSampleMod(index: Int) {
        val samples = SampleMods.getSampleModList()
        val sample = samples.getOrNull(index) ?: return
        val (metadata, files) = sample
        val analysis = converterEngine.analyzeMod(metadata, files)
        _uiState.update {
            it.copy(
                loadedModMetadata = metadata,
                loadedFiles = files,
                modAnalysis = analysis,
                conversionReport = null,
                conversionError = null,
                previewFile = null
            )
        }
    }

    fun loadZipFromUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isReadingZip = true, conversionError = null, conversionReport = null) }
            try {
                val contentResolver = getApplication<Application>().contentResolver
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _uiState.update { it.copy(isReadingZip = false, conversionError = "Failed to open selected file") }
                    return@launch
                }

                val (metadata, files) = converterEngine.parseZipStream(inputStream)
                val analysis = converterEngine.analyzeMod(metadata, files)

                _uiState.update {
                    it.copy(
                        isReadingZip = false,
                        loadedModMetadata = metadata,
                        loadedFiles = files,
                        modAnalysis = analysis,
                        snackbarMessage = if (it.isArabic) "تم قراءة ملف المود بنجاح: ${metadata.name}" else "Mod loaded: ${metadata.name}"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isReadingZip = false,
                        conversionError = "Failed to parse ZIP: ${e.message}"
                    )
                }
            }
        }
    }

    fun startConversion() {
        val analysis = _uiState.value.modAnalysis ?: return
        val apiKey = _uiState.value.apiKey
        val model = _uiState.value.selectedModel

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isConverting = true,
                    conversionProgress = 0.15f,
                    conversionStage = if (it.isArabic) "جاري فحص وتوحيد مسارات الملفات وحساسية الأحرف..." else "Normalizing paths and Linux casing...",
                    conversionError = null
                )
            }

            try {
                _uiState.update {
                    it.copy(
                        conversionProgress = 0.45f,
                        conversionStage = if (it.isArabic) "جاري تحليل أكواد اللوا ومطابقتها مع شروط دليل البورت..." else "Analyzing Lua scripts & matching port guidelines..."
                    )
                }

                val report = converterEngine.convertMod(
                    analysis = analysis,
                    geminiService = geminiService,
                    apiKey = apiKey,
                    model = model
                )

                _uiState.update {
                    it.copy(
                        conversionProgress = 0.85f,
                        conversionStage = if (it.isArabic) "جاري تجهيز بنية المجلد resources/ وحفظ التقرير..." else "Structuring resources/ and saving report..."
                    )
                }

                // Save to Room DB
                modDao.insertConvertedMod(
                    ConvertedModEntity(
                        modName = report.modName,
                        originalFileName = "${analysis.metadata.directory.ifEmpty { "mod" }}.zip",
                        compatibilityScore = report.compatibilityScore,
                        hasLuaScript = analysis.luaFiles.isNotEmpty(),
                        totalFiles = analysis.files.size,
                        convertedFilesCount = report.convertedFiles.size,
                        summaryAr = report.summaryAr,
                        summaryEn = report.summaryEn
                    )
                )

                _uiState.update {
                    it.copy(
                        isConverting = false,
                        conversionProgress = 1.0f,
                        conversionStage = "",
                        conversionReport = report,
                        previewFile = report.convertedFiles.firstOrNull { f -> f.textContent != null }
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isConverting = false,
                        conversionError = "Conversion error: ${e.message}"
                    )
                }
            }
        }
    }

    fun selectFilePreview(file: ConvertedFile) {
        _uiState.update { it.copy(previewFile = file) }
    }

    fun exportConvertedZip(outputStream: OutputStream) {
        val files = _uiState.value.conversionReport?.convertedFiles ?: return
        viewModelScope.launch {
            try {
                converterEngine.buildConvertedZip(files, outputStream)
                _uiState.update {
                    it.copy(
                        snackbarMessage = if (it.isArabic) "تم تصدير ملف ZIP المتوافق بنجاح!" else "Converted ZIP exported successfully!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(snackbarMessage = "Export failed: ${e.message}")
                }
            }
        }
    }

    fun sendAssistantPrompt(prompt: String) {
        if (prompt.isBlank()) return
        val currentMessages = _uiState.value.assistantMessages
        val newMsg = GeminiMessage(role = "user", content = prompt)
        val updatedList = currentMessages + newMsg
        val apiKey = _uiState.value.apiKey
        val model = _uiState.value.selectedModel

        _uiState.update {
            it.copy(
                assistantMessages = updatedList,
                isAssistantThinking = true,
                assistantError = null
            )
        }

        viewModelScope.launch {
            val result = geminiService.chatWithAssistant(
                apiKey = apiKey,
                model = model,
                prompt = prompt,
                history = currentMessages
            )
            if (result.isSuccess) {
                val reply = result.getOrThrow()
                _uiState.update {
                    it.copy(
                        assistantMessages = it.assistantMessages + GeminiMessage(role = "model", content = reply),
                        isAssistantThinking = false
                    )
                }
            } else {
                val err = result.exceptionOrNull()?.message ?: "Unknown error"
                val errDisplay = if (_uiState.value.isArabic) {
                    "حدث خطأ أثناء استدعاء جيميناي ($err). تأكد من إدخال مفتاح API صحيح في تبويب الإعدادات."
                } else {
                    "Error calling Gemini ($err). Please verify your API key in Settings."
                }
                _uiState.update {
                    it.copy(
                        assistantMessages = it.assistantMessages + GeminiMessage(role = "model", content = errDisplay),
                        isAssistantThinking = false
                    )
                }
            }
        }
    }

    fun clearAssistantChat() {
        val welcomeMsg = if (_uiState.value.isArabic) {
            "تمت إعادة تعيين المحادثة. كيف يمكنني مساعدتك في تعديل مودات أندرويد؟"
        } else {
            "Chat reset. How can I help you port your Isaac Android mod?"
        }
        _uiState.update {
            it.copy(assistantMessages = listOf(GeminiMessage(role = "model", content = welcomeMsg)))
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            modDao.deleteMod(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            modDao.clearAll()
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
