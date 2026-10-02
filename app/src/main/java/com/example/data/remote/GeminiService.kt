package com.example.data.remote

import android.util.Log
import com.example.data.model.ChangeType
import com.example.data.model.ConvertedFile
import com.example.data.model.GeminiMessage
import com.example.data.model.ModMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GeminiModConversionResponse(
    val compatibilityScore: Int,
    val summaryAr: String,
    val summaryEn: String,
    val generatedFiles: List<ConvertedFile>,
    val removedScripts: List<String>,
    val xmlAdditions: List<String>,
    val tipsAr: List<String>,
    val tipsEn: List<String>
)

class GeminiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun testApiKey(apiKey: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank()) return@withContext Result.failure(Exception("API Key is empty"))
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val requestBodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "Ping")
                            })
                        })
                    })
                })
            }
            val request = Request.Builder()
                .url(url)
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                val errorMsg = response.body?.string() ?: "HTTP ${response.code}"
                Result.failure(Exception("HTTP ${response.code}: $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun convertIsaacMod(
        apiKey: String,
        model: String = "gemini-3.5-flash",
        metadata: ModMetadata,
        fileNames: List<String>,
        luaContents: Map<String, String>,
        xmlContents: Map<String, String>
    ): Result<GeminiModConversionResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

            val systemPrompt = """
You are a senior game engineer and expert on modding The Binding of Isaac: Repentance, specifically specializing in porting PC mods to the unofficial/native The Binding of Isaac: Repentance Android port.

IMPORTANT RULES FROM THE ANDROID MODDING GUIDE (tboi-android-modding-guide.netlify.app):
1. **THE ANDROID PORT RUNS NO SCRIPTS**: The port has no Lua engine or REPENTOGON script loader. All Lua files (`main.lua`, helper `.lua`) will NOT execute and must be stripped or converted into static XML format.
2. **FORMAT HOLDS RESOURCES ONLY**: Mods must be structured as resource replacements in `resources/` (e.g. `resources/gfx/...`, `resources/music/...`, `resources/sfx/...`).
3. **XML CONVERSIONS**:
   - PC `content/items.xml` -> Android `resources/items.xml`
   - PC `content/players.xml` -> Android `resources/players.xml`
   - PC `content/costumes2.xml` -> Android `resources/costumes2.xml`
   - Simple Lua stat changes (damage, tears, speed, health, flying, starting items, cache flags) should be converted to attributes in `resources/items.xml` or `resources/players.xml`.
   - Strip PC-only Steam workshop attributes or mod-loader wrappers.
4. **FILE PATH CASING**: Linux / Android filesystems are strictly case-sensitive. All resource paths must be normalized to lowercase canonical paths.
5. **OUTPUT REQUIREMENT**: Respond ONLY with a valid JSON object without markdown code blocks, having this exact schema:
{
  "compatibilityScore": 85,
  "summaryAr": "ملخص التحويل للبورت العربي",
  "summaryEn": "English summary of the conversion",
  "generatedFiles": [
    {
      "targetPath": "resources/items.xml",
      "content": "xml content here",
      "descriptionAr": "وصف التعديل بالعربية",
      "descriptionEn": "Description in English"
    }
  ],
  "removedScripts": ["main.lua"],
  "xmlAdditions": ["Added stat cache flags to items.xml", "Converted player costumes to costumes2.xml"],
  "tipsAr": ["نصيحة لتشغيل المود على أندرويد 13/14"],
  "tipsEn": ["Tip for running this mod on Android port"]
}
""".trimIndent()

            val modContext = buildString {
                appendLine("Mod Name: ${metadata.name}")
                appendLine("Description: ${metadata.description}")
                appendLine("Files in mod:")
                fileNames.forEach { appendLine(" - $it") }
                appendLine()
                if (luaContents.isNotEmpty()) {
                    appendLine("=== LUA SCRIPTS (Analyze and convert mechanics to XML) ===")
                    luaContents.forEach { (path, content) ->
                        appendLine("--- FILE: $path ---")
                        appendLine(content.take(4000)) // Take reasonable length
                    }
                }
                if (xmlContents.isNotEmpty()) {
                    appendLine("=== EXISTING XML FILES ===")
                    xmlContents.forEach { (path, content) ->
                        appendLine("--- FILE: $path ---")
                        appendLine(content.take(3000))
                    }
                }
            }

            val requestBodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", systemPrompt + "\n\n" + modContext)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.2)
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "HTTP ${response.code}"
                return@withContext Result.failure(Exception("Gemini API Error (${response.code}): $errorBody"))
            }

            val responseStr = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response from Gemini"))
            val responseJson = JSONObject(responseStr)
            val candidates = responseJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val rawText = parts?.optJSONObject(0)?.optString("text") ?: ""

            // Parse response JSON
            val cleanJson = rawText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val parsed = JSONObject(cleanJson)

            val score = parsed.optInt("compatibilityScore", 80)
            val summaryAr = parsed.optString("summaryAr", "تم فحص المود وتحويله ليتوافق مع بورت أندرويد.")
            val summaryEn = parsed.optString("summaryEn", "Mod analyzed and converted for Android port.")

            val generatedFilesList = mutableListOf<ConvertedFile>()
            val genFilesArr = parsed.optJSONArray("generatedFiles")
            if (genFilesArr != null) {
                for (i in 0 until genFilesArr.length()) {
                    val obj = genFilesArr.optJSONObject(i) ?: continue
                    val path = obj.optString("targetPath", "resources/mod_patch.xml")
                    val fileContent = obj.optString("content", "")
                    val descAr = obj.optString("descriptionAr", "ملف تم إنشاؤه عبر الذكاء الاصطناعي")
                    val descEn = obj.optString("descriptionEn", "File generated by Gemini AI")
                    generatedFilesList.add(
                        ConvertedFile(
                            targetPath = path,
                            originalPath = null,
                            textContent = fileContent,
                            changeType = ChangeType.CONVERTED_FROM_LUA,
                            descriptionAr = descAr,
                            descriptionEn = descEn
                        )
                    )
                }
            }

            val removedScripts = mutableListOf<String>()
            val remArr = parsed.optJSONArray("removedScripts")
            if (remArr != null) {
                for (i in 0 until remArr.length()) {
                    removedScripts.add(remArr.optString(i))
                }
            }

            val xmlAdditions = mutableListOf<String>()
            val xmlArr = parsed.optJSONArray("xmlAdditions")
            if (xmlArr != null) {
                for (i in 0 until xmlArr.length()) {
                    xmlAdditions.add(xmlArr.optString(i))
                }
            }

            val tipsAr = mutableListOf<String>()
            val tArArr = parsed.optJSONArray("tipsAr")
            if (tArArr != null) {
                for (i in 0 until tArArr.length()) {
                    tipsAr.add(tArArr.optString(i))
                }
            }

            val tipsEn = mutableListOf<String>()
            val tEnArr = parsed.optJSONArray("tipsEn")
            if (tEnArr != null) {
                for (i in 0 until tEnArr.length()) {
                    tipsEn.add(tEnArr.optString(i))
                }
            }

            Result.success(
                GeminiModConversionResponse(
                    compatibilityScore = score,
                    summaryAr = summaryAr,
                    summaryEn = summaryEn,
                    generatedFiles = generatedFilesList,
                    removedScripts = removedScripts,
                    xmlAdditions = xmlAdditions,
                    tipsAr = tipsAr,
                    tipsEn = tipsEn
                )
            )
        } catch (e: Exception) {
            Log.e("GeminiService", "Failed to convert Isaac mod", e)
            Result.failure(e)
        }
    }

    suspend fun chatWithAssistant(
        apiKey: String,
        model: String = "gemini-3.5-flash",
        prompt: String,
        history: List<GeminiMessage>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val systemContext = """
You are IsaacPortBot, a specialist AI assistant for modding 'The Binding of Isaac: Repentance' on the Android port.
The port was created by the community and follows the rules of the official modding guide (tboi-android-modding-guide.netlify.app):
- The port runs NO LUA SCRIPTS.
- It only supports pure resource replacement in `Android/media/com.nicalis.bindingofisaac/resources/` (or equivalent data folder).
- Case sensitivity matters (Linux ext4).
- You can explain how to turn Lua callbacks into items.xml, costumes2.xml, or players.xml.
Answer clearly, concisely, and support both Arabic (العربية) and English.
""".trimIndent()

            val contentsArray = JSONArray()
            // Add system instruction first
            contentsArray.put(JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemContext) })
                })
            })

            // Add history
            for (msg in history.takeLast(6)) {
                contentsArray.put(JSONObject().apply {
                    put("role", if (msg.role == "user") "user" else "model")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", msg.content) })
                    })
                })
            }

            // Current prompt
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", prompt) })
                })
            })

            val requestBodyJson = JSONObject().apply {
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "HTTP ${response.code}"
                return@withContext Result.failure(Exception("Gemini API Error: $errorBody"))
            }

            val responseStr = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            val responseJson = JSONObject(responseStr)
            val candidates = responseJson.optJSONArray("candidates")
            val text = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text") ?: "No response"
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
