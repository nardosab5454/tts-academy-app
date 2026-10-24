package com.example.data.ai

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GroundingSource(
  val title: String,
  val url: String
)

data class AiExplanationResult(
  val responseText: String,
  val searchQueries: List<String> = emptyList(),
  val sources: List<GroundingSource> = emptyList(),
  val isError: Boolean = false
)

object GeminiService {
  private const val TAG = "GeminiService"
  private const val MODEL_NAME = "gemini-3.5-flash"
  private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

  private val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(60, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .build()

  suspend fun askGemini(
    prompt: String,
    courseContext: String? = null,
    enableSearchGrounding: Boolean = true
  ): AiExplanationResult = withContext(Dispatchers.IO) {
    val rawKey = try { BuildConfig.GEMINI_API_KEY } catch (_: Throwable) { "" }
    val apiKey = if (rawKey.isNotBlank() && rawKey != "MY_GEMINI_API_KEY") rawKey else (System.getenv("GEMINI_API_KEY") ?: "")
    if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
      val fallback = com.example.data.remote.GeminiTutorService.generateDirectAcademicResponse(prompt, courseContext, null)
      return@withContext AiExplanationResult(
        responseText = fallback,
        isError = false
      )
    }

    try {
      val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"

      val systemPrompt = buildString {
        append("You are TTS Academy's AI Academic Tutor and Research Guide for university freshman students. ")
        append("Your goal is to provide clear, thorough, step-by-step educational explanations, derivations, ")
        append("and up-to-date research clarifications for university-level coursework. ")
        if (!courseContext.isNullOrBlank()) {
          append("Active University Course Context: $courseContext. ")
        }
        append("Use clear headings, bullet points, and plain mathematical/scientific notation where applicable.")
      }

      val requestJson = JSONObject().apply {
        // Contents
        val contentsArray = JSONArray().apply {
          val contentObj = JSONObject().apply {
            val partsArray = JSONArray().apply {
              put(JSONObject().apply { put("text", prompt) })
            }
            put("parts", partsArray)
          }
          put(contentObj)
        }
        put("contents", contentsArray)

        // System Instruction
        put("systemInstruction", JSONObject().apply {
          val partsArray = JSONArray().apply {
            put(JSONObject().apply { put("text", systemPrompt) })
          }
          put("parts", partsArray)
        })

        // Google Search Grounding Tool
        if (enableSearchGrounding) {
          val toolsArray = JSONArray().apply {
            val googleSearchTool = JSONObject().apply {
              put("googleSearch", JSONObject())
            }
            put(googleSearchTool)
          }
          put("tools", toolsArray)
        }
      }

      val mediaType = "application/json; charset=utf-8".toMediaType()
      val requestBody = requestJson.toString().toRequestBody(mediaType)

      val request = Request.Builder()
        .url(url)
        .post(requestBody)
        .build()

      val response = okHttpClient.newCall(request).execute()
      val responseBodyString = response.body?.string() ?: ""

      if (!response.isSuccessful) {
        Log.e(TAG, "Gemini API call failed code: ${response.code}, body: $responseBodyString")
        val fallback = com.example.data.remote.GeminiTutorService.generateDirectAcademicResponse(prompt, courseContext, null)
        return@withContext AiExplanationResult(
          responseText = fallback,
          isError = false
        )
      }

      val rootJson = JSONObject(responseBodyString)
      val candidates = rootJson.optJSONArray("candidates")
      if (candidates == null || candidates.length() == 0) {
        val fallback = com.example.data.remote.GeminiTutorService.generateDirectAcademicResponse(prompt, courseContext, null)
        return@withContext AiExplanationResult(
          responseText = fallback,
          isError = false
        )
      }

      val firstCandidate = candidates.getJSONObject(0)
      val content = firstCandidate.optJSONObject("content")
      val parts = content?.optJSONArray("parts")

      val textBuilder = StringBuilder()
      if (parts != null) {
        for (i in 0 until parts.length()) {
          val part = parts.getJSONObject(i)
          val t = part.optString("text")
          if (t.isNotBlank()) {
            textBuilder.append(t)
          }
        }
      }

      // Parse Grounding Metadata (Search Queries & Sources)
      val groundingMetadata = firstCandidate.optJSONObject("groundingMetadata")
      val searchQueries = mutableListOf<String>()
      val sources = mutableListOf<GroundingSource>()

      if (groundingMetadata != null) {
        val webSearchQueries = groundingMetadata.optJSONArray("webSearchQueries")
        if (webSearchQueries != null) {
          for (i in 0 until webSearchQueries.length()) {
            searchQueries.add(webSearchQueries.getString(i))
          }
        }

        val groundingChunks = groundingMetadata.optJSONArray("groundingChunks")
        if (groundingChunks != null) {
          for (i in 0 until groundingChunks.length()) {
            val chunk = groundingChunks.getJSONObject(i)
            val web = chunk.optJSONObject("web")
            if (web != null) {
              val title = web.optString("title", "Reference Source")
              val uri = web.optString("uri", "")
              if (uri.isNotBlank()) {
                sources.add(GroundingSource(title, uri))
              }
            }
          }
        }
      }

      AiExplanationResult(
        responseText = textBuilder.toString().ifBlank {
          com.example.data.remote.GeminiTutorService.generateDirectAcademicResponse(prompt, courseContext, null)
        },
        searchQueries = searchQueries,
        sources = sources.distinctBy { it.url },
        isError = false
      )
    } catch (e: Exception) {
      Log.e(TAG, "Error invoking Gemini API", e)
      val fallback = com.example.data.remote.GeminiTutorService.generateDirectAcademicResponse(prompt, courseContext, null)
      AiExplanationResult(
        responseText = fallback,
        isError = false
      )
    }
  }
}
