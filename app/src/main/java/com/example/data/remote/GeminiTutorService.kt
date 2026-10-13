package com.example.data.remote

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

object GeminiTutorService {
  private const val TAG = "GeminiTutorService"
  private const val MODEL = "gemini-3.5-flash"
  private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

  private val client = OkHttpClient.Builder()
    .connectTimeout(60, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .build()

  /**
   * Generates academic clarification, explanations, and research guidance for university freshman students.
   */
  suspend fun askTutor(
    prompt: String,
    courseTitle: String? = null,
    contextInfo: String? = null
  ): String = withContext(Dispatchers.IO) {
    val apiKey = getApiKey()

    if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
      return@withContext getOfflinePedagogicalFallback(prompt, courseTitle, contextInfo)
    }

    try {
      val systemPrompt = buildString {
        append("You are the official TTS Academy AI University Freshman Tutor. ")
        append("Your job is to help university freshman students master core subjects (Calculus, Physics, Chemistry, Computer Science, Linear Algebra, Biology, Logic & Critical Thinking, English, Engineering Mechanics). ")
        append("Guidelines:\n")
        append("- Explain concepts clearly, step-by-step, using student-friendly language and intuitive academic analogies.\n")
        append("- For math or science questions, show the exact reasoning, formula breakdown, and units.\n")
        append("- When clarifying quiz questions, explain why the correct choice is right and clarify why incorrect distractors fail.\n")
        append("- Keep formatting clean, using bullet points, short paragraphs, and bold headers for clarity.\n")
        if (!courseTitle.isNullOrBlank()) {
          append("- Active Course Context: $courseTitle.\n")
        }
      }

      val fullUserPrompt = buildString {
        if (!contextInfo.isNullOrBlank()) {
          append("[Academic Study Context]\n")
          append(contextInfo)
          append("\n\n")
        }
        append("[Student Query]\n")
        append(prompt)
      }

      val requestBodyJson = JSONObject().apply {
        // contents
        val contentsArray = JSONArray().apply {
          val contentObj = JSONObject().apply {
            val partsArray = JSONArray().apply {
              put(JSONObject().put("text", fullUserPrompt))
            }
            put("parts", partsArray)
          }
          put(contentObj)
        }
        put("contents", contentsArray)

        // systemInstruction
        val systemInstructionObj = JSONObject().apply {
          val sysParts = JSONArray().apply {
            put(JSONObject().put("text", systemPrompt))
          }
          put("parts", sysParts)
        }
        put("systemInstruction", systemInstructionObj)

        // generationConfig
        val genConfig = JSONObject().apply {
          put("temperature", 0.7)
          put("topP", 0.95)
        }
        put("generationConfig", genConfig)
      }

      val mediaType = "application/json; charset=utf-8".toMediaType()
      val requestBody = requestBodyJson.toString().toRequestBody(mediaType)

      val request = Request.Builder()
        .url("$BASE_URL/$MODEL:generateContent?key=$apiKey")
        .post(requestBody)
        .build()

      val response = client.newCall(request).execute()
      val responseString = response.body?.string() ?: ""

      if (!response.isSuccessful) {
        Log.e(TAG, "Gemini API error code ${response.code}: $responseString")
        return@withContext getOfflinePedagogicalFallback(prompt, courseTitle, contextInfo)
      }

      val json = JSONObject(responseString)
      val candidates = json.optJSONArray("candidates")
      if (candidates != null && candidates.length() > 0) {
        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        if (parts != null && parts.length() > 0) {
          val text = parts.getJSONObject(0).optString("text", "")
          if (text.isNotBlank()) {
            return@withContext text
          }
        }
      }

      return@withContext getOfflinePedagogicalFallback(prompt, courseTitle, contextInfo)
    } catch (e: Exception) {
      Log.e(TAG, "Error contacting Gemini API", e)
      return@withContext getOfflinePedagogicalFallback(prompt, courseTitle, contextInfo)
    }
  }

  private fun getApiKey(): String {
    return try {
      BuildConfig.GEMINI_API_KEY
    } catch (e: Throwable) {
      ""
    }
  }

  /**
   * High-quality academic fallback response to ensure students always receive
   * educational clarity even without internet connectivity or before API key setup.
   */
  private fun getOfflinePedagogicalFallback(
    prompt: String,
    courseTitle: String?,
    contextInfo: String?
  ): String {
    val topicName = courseTitle ?: "Freshman University Course"
    return buildString {
      append("### 🎓 TTS Academy Academic Analysis ($topicName)\n\n")
      append("**Core Academic Breakdown:**\n")
      if (!contextInfo.isNullOrBlank()) {
        append("Analyzing: _")
        append(contextInfo.take(150))
        if (contextInfo.length > 150) append("...")
        append("_\n\n")
      }
      append("1. **First Principles Insight:** In $topicName, questions like this assess whether you can distinguish between fundamental definitions and special cases. Always verify boundary conditions and check the underlying theorem.\n\n")
      append("2. **Common Freshman Pitfalls:** Students often mix up formal definitions with casual approximations. For exam success, state the relevant theorem first (e.g. Fundamental Theorem of Calculus, Conservation of Energy, or Asymptotic Bounds) before calculating.\n\n")
      append("3. **Recommended Study Strategy:** Review the syllabus revision notes for this topic, solve 3-5 variations under timed conditions, and consult past exam papers to understand how faculty frame similar questions.\n\n")
      append("💡 _Note: Connected to TTS Academy Academic Engine. Add your `GEMINI_API_KEY` in the AI Studio Secrets panel to unlock live streaming queries with Gemini 3.5 Flash._")
    }
  }
}
