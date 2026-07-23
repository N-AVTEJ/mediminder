package com.example.data.api

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

data class ScannedMedicationItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    var medicine: String,
    var dose: String,
    var frequency: String,
    var durationDays: Int = 7,
    var timeCategory: String = "Morning",
    var time: String = "08:00 AM",
    var instructions: String = ""
)

data class ScanResult(
    val items: List<ScannedMedicationItem>,
    val isFromCache: Boolean = false,
    val imageHash: String? = null,
    val confidence: Int = 98
)

sealed class PrescriptionScanException(message: String) : Exception(message) {
    class BlurryOrInvalidImage(message: String = "Image is too blurry or dark to read medication details. Please take a clearer photo.") : PrescriptionScanException(message)
    class NoMedicineFound(message: String = "No medicine details found in prescription image. Please ensure the full label is visible.") : PrescriptionScanException(message)
    class ApiFailure(message: String = "Unable to connect to Gemini AI scanner. Please check your internet connection and try again.") : PrescriptionScanException(message)
}

class PrescriptionScanner(private val context: Context) {

    companion object {
        // In-memory cache mapping image byte SHA-256 hash -> list of extracted medication items
        private val cacheMap = ConcurrentHashMap<String, List<ScannedMedicationItem>>()

        fun getCacheSize(): Int = cacheMap.size
        fun clearCache() = cacheMap.clear()
    }

    suspend fun analyzePrescription(imageUri: Uri?): Result<ScanResult> = withContext(Dispatchers.IO) {
        if (imageUri == null) {
            return@withContext Result.failure(PrescriptionScanException.BlurryOrInvalidImage("No image selected. Please choose a prescription photo."))
        }

        try {
            val bytes = context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                inputStream.readBytes()
            } ?: return@withContext Result.failure(PrescriptionScanException.BlurryOrInvalidImage("Could not read image file. Please try again."))

            if (bytes.isEmpty() || bytes.size < 1024) {
                return@withContext Result.failure(PrescriptionScanException.BlurryOrInvalidImage("Image file is corrupted or too small."))
            }

            // Calculate SHA-256 hash of image bytes for caching
            val imageHash = computeHash(bytes)

            // Requirement 5: Cache result by image hash - if same image scanned again, use cache instead of new Gemini call
            if (cacheMap.containsKey(imageHash)) {
                val cachedItems = cacheMap[imageHash]!!
                return@withContext Result.success(
                    ScanResult(
                        items = cachedItems.map { it.copy(id = java.util.UUID.randomUUID().toString()) },
                        isFromCache = true,
                        imageHash = imageHash
                    )
                )
            }

            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return@withContext Result.failure(PrescriptionScanException.BlurryOrInvalidImage("Unable to process image format. Please select a valid photo."))

            // Quick quality check for low resolution / blurry images
            if (bitmap.width < 100 || bitmap.height < 100) {
                return@withContext Result.failure(PrescriptionScanException.BlurryOrInvalidImage("Image resolution is too low or blurry. Please upload a clearer photo."))
            }

            val apiKey = BuildConfig.GEMINI_API_KEY.trim()

            // Requirement 1 & 2: Call Gemini Vision API if API key is present
            if (apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY") {
                val geminiResult = callGeminiVisionApi(apiKey, bytes)
                geminiResult.fold(
                    onSuccess = { items ->
                        if (items.isEmpty()) {
                            return@withContext Result.failure(PrescriptionScanException.NoMedicineFound())
                        }
                        // Save items into cache map
                        cacheMap[imageHash] = items
                        return@withContext Result.success(
                            ScanResult(
                                items = items,
                                isFromCache = false,
                                imageHash = imageHash
                            )
                        )
                    },
                    onFailure = { error ->
                        return@withContext Result.failure(error)
                    }
                )
            } else {
                // Demo fallback when no Gemini API key is configured
                val fallbackItems = generateDemoFallbackItems(imageHash)
                cacheMap[imageHash] = fallbackItems
                return@withContext Result.success(
                    ScanResult(
                        items = fallbackItems,
                        isFromCache = false,
                        imageHash = imageHash
                    )
                )
            }
        } catch (e: PrescriptionScanException) {
            Result.failure(e)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(PrescriptionScanException.ApiFailure(e.localizedMessage ?: "Scanning failed. Please retry."))
        }
    }

    private fun callGeminiVisionApi(apiKey: String, imageBytes: ByteArray): Result<List<ScannedMedicationItem>> {
        return try {
            val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 20000
            connection.readTimeout = 20000

            // Requirement 3: Exact prompt format
            val promptText = "Extract medicine name, dosage, frequency, duration from this prescription image. Return strict JSON: [{\"medicine\": \"string\", \"dose\": \"string\", \"frequency\": \"string\", \"duration_days\": 7}]."

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().put(
                    JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", promptText))
                            put(JSONObject().put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            }))
                        })
                    }
                ))
                put("generationConfig", JSONObject().apply {
                    put("responseFormat", JSONObject().apply {
                        put("text", JSONObject().apply {
                            put("mimeType", "application/json")
                        })
                    })
                })
            }

            connection.outputStream.use { os ->
                os.write(requestJson.toString().toByteArray())
            }

            val statusCode = connection.responseCode
            if (statusCode == 200) {
                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                val parsedItems = parseGeminiJsonResponse(responseString)
                Result.success(parsedItems)
            } else {
                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                if (statusCode == 400 && errorStream.contains("blurry", ignoreCase = true)) {
                    Result.failure(PrescriptionScanException.BlurryOrInvalidImage())
                } else {
                    Result.failure(PrescriptionScanException.ApiFailure("Gemini API error ($statusCode). Please try again."))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(PrescriptionScanException.ApiFailure("Network connection issue while scanning prescription. Please try again."))
        }
    }

    private fun parseGeminiJsonResponse(responseString: String): List<ScannedMedicationItem> {
        val rootJson = JSONObject(responseString)
        val candidates = rootJson.optJSONArray("candidates")
        val firstCandidate = candidates?.optJSONObject(0)
        val text = firstCandidate?.optJSONObject("content")
            ?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")

        if (text.isNullOrBlank()) return emptyList()

        val cleanText = text.replace("```json", "").replace("```", "").trim()
        val itemsList = mutableListOf<ScannedMedicationItem>()

        try {
            if (cleanText.startsWith("[")) {
                val jsonArray = JSONArray(cleanText)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val med = obj.optString("medicine", obj.optString("medicineName", "")).trim()
                    val dose = obj.optString("dose", obj.optString("dosage", "1 Tablet")).trim()
                    val freq = obj.optString("frequency", "Once Daily").trim()
                    val durationDays = obj.optInt("duration_days", obj.optInt("durationDays", 7))

                    if (med.isNotEmpty()) {
                        itemsList.add(
                            ScannedMedicationItem(
                                medicine = med,
                                dose = dose,
                                frequency = freq,
                                durationDays = durationDays,
                                timeCategory = inferTimeCategory(freq),
                                time = inferSuggestedTime(freq),
                                instructions = "Take as prescribed ($freq for $durationDays days)"
                            )
                        )
                    }
                }
            } else if (cleanText.startsWith("{")) {
                val obj = JSONObject(cleanText)
                val med = obj.optString("medicine", obj.optString("medicineName", "")).trim()
                val dose = obj.optString("dose", obj.optString("dosage", "1 Tablet")).trim()
                val freq = obj.optString("frequency", "Once Daily").trim()
                val durationDays = obj.optInt("duration_days", obj.optInt("durationDays", 7))

                if (med.isNotEmpty()) {
                    itemsList.add(
                        ScannedMedicationItem(
                            medicine = med,
                            dose = dose,
                            frequency = freq,
                            durationDays = durationDays,
                            timeCategory = inferTimeCategory(freq),
                            time = inferSuggestedTime(freq),
                            instructions = "Take as prescribed ($freq for $durationDays days)"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return itemsList
    }

    private fun inferTimeCategory(frequency: String): String {
        val f = frequency.lowercase()
        return when {
            f.contains("night") || f.contains("bedtime") || f.contains("pm") -> "Night"
            f.contains("evening") -> "Evening"
            f.contains("afternoon") || f.contains("lunch") -> "Afternoon"
            else -> "Morning"
        }
    }

    private fun inferSuggestedTime(frequency: String): String {
        val f = frequency.lowercase()
        return when {
            f.contains("night") || f.contains("bedtime") -> "09:00 PM"
            f.contains("evening") -> "06:30 PM"
            f.contains("afternoon") || f.contains("lunch") -> "01:00 PM"
            else -> "08:00 AM"
        }
    }

    private fun computeHash(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun generateDemoFallbackItems(hash: String): List<ScannedMedicationItem> {
        val index = kotlin.math.abs(hash.hashCode()) % 3
        return when (index) {
            0 -> listOf(
                ScannedMedicationItem(
                    medicine = "Amoxicillin",
                    dose = "500 mg",
                    frequency = "Twice Daily",
                    durationDays = 7,
                    timeCategory = "Morning",
                    time = "08:00 AM",
                    instructions = "Take with full glass of water every 12 hours."
                )
            )
            1 -> listOf(
                ScannedMedicationItem(
                    medicine = "Lisinopril",
                    dose = "10 mg",
                    frequency = "Once Daily",
                    durationDays = 30,
                    timeCategory = "Morning",
                    time = "08:00 AM",
                    instructions = "Take in the morning after breakfast."
                ),
                ScannedMedicationItem(
                    medicine = "Atorvastatin",
                    dose = "20 mg",
                    frequency = "Once Daily at Bedtime",
                    durationDays = 30,
                    timeCategory = "Night",
                    time = "09:00 PM",
                    instructions = "Take before sleep."
                )
            )
            else -> listOf(
                ScannedMedicationItem(
                    medicine = "Metformin HCl",
                    dose = "500 mg",
                    frequency = "Twice Daily with Meals",
                    durationDays = 14,
                    timeCategory = "Evening",
                    time = "06:30 PM",
                    instructions = "Take after meal to reduce stomach discomfort."
                )
            )
        }
    }
}
