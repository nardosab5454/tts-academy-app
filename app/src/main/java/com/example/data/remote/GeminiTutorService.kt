package com.example.data.remote

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withTimeout

enum class TutorMode(
  val modelName: String,
  val label: String,
  val shortName: String,
  val description: String,
  val temperature: Float,
  val systemRolePrompt: String
) {
  HYDROGEN(
    modelName = "gemini-4-hydrogen",
    label = "General • Hydrogen",
    shortName = "General",
    description = "Balanced real-time tutor for everyday study, concepts & exam prep",
    temperature = 0.65f,
    systemRolePrompt = "You are the TTS Academy Academic Tutor (General Mode). Provide clear, well-structured, accurate university freshman explanations with step-by-step derivations, intuitive examples, and practical exam tips."
  ),
  FLASH_LITE(
    modelName = "gemini-3.1-flash-lite",
    label = "Fast • Flash Lite",
    shortName = "Fast",
    description = "Ultra-fast responses for quick definitions, flashcards & sanity checks",
    temperature = 0.5f,
    systemRolePrompt = "You are the TTS Academy Rapid Study Coach (Fast Mode). Give crisp, concise, high-signal answers focused on core formulas, key definitions, and quick takeaways so the student can revise quickly."
  ),
  ARGON(
    modelName = "gemini-4-argon",
    label = "Complex • Argon",
    shortName = "Interactive",
    description = "Complex interactive problem solving, Socratic hints & mistake diagnosis",
    temperature = 0.6f,
    systemRolePrompt = "You are the TTS Academy Interactive STEM Coach (Interactive Mode). Walk through complex multi-step problems interactively, diagnose why specific MCQ distractors are wrong, check assumptions, and suggest a targeted next check question."
  ),
  CARBON(
    modelName = "gemini-4-carbon",
    label = "Deep • Carbon",
    shortName = "Deep",
    description = "Deep mathematical proofs, formal derivations & thorough synthesis",
    temperature = 0.45f,
    systemRolePrompt = "You are the TTS Academy Honors Professor (Deep Reasoning Mode). Deliver rigorous, formal step-by-step proofs, complete derivations from first principles, boundary-condition checks, and high-level conceptual synthesis."
  )
}

data class TutorContextPayload(
  val studentName: String = "Student",
  val department: String = "Freshman University",
  val dailyGoalMinutes: Int = 45,
  val streakDays: Int = 1,
  val questionsAnswered: Int = 0,
  val overallAccuracyPercent: Int = 0,
  val weakTopicsSummary: String = "",
  val activeCourseCode: String? = null,
  val activeCourseTitle: String? = null,
  val activeCourseCategory: String? = null,
  val activeCourseDescription: String? = null,
  val courseTopicsSummary: String = "",
  val courseNotesSummary: String = "",
  val coursePastExamsSummary: String = "",
  val pinnedContextTitle: String? = null,
  val pinnedContextSnippet: String? = null,
  val includeAppContext: Boolean = true
)

object GeminiTutorService {
  private const val TAG = "GeminiTutorService"
  private const val INFERENCE_TIMEOUT_MS = 60_000L

  private val firebaseAi by lazy {
    Firebase.ai(backend = GenerativeBackend.googleAI())
  }

  /**
   * Streams real-time, context-aware multi-turn chat chunks from Firebase AI Logic (`Firebase.ai`).
   * Maintains conversation history and injects live student, course, note, and problem context
   * into the model's `systemInstruction`.
   */
  fun streamTutorResponse(
    prompt: String,
    mode: TutorMode,
    contextPayload: TutorContextPayload,
    history: List<Pair<String, Boolean>> // Pair(text, isUser)
  ): Flow<String> = flow {
    val cleanPrompt = prompt.trim()
    require(cleanPrompt.isNotEmpty()) { "Prompt cannot be empty." }

    val systemText = buildSystemInstruction(mode, contextPayload)
    val model = firebaseAi.generativeModel(
      modelName = mode.modelName,
      generationConfig = generationConfig {
        temperature = mode.temperature
        topP = 0.95f
        maxOutputTokens = 2048
      },
      systemInstruction = content { text(systemText) }
    )

    val chatHistory = buildSanitizedHistory(history)
    val chat = model.startChat(history = chatHistory)

    val contextualUserTurn = buildString {
      if (contextPayload.includeAppContext && !contextPayload.pinnedContextSnippet.isNullOrBlank()) {
        append("[Focused Academic Item: ${contextPayload.pinnedContextTitle ?: "Study Reference"}]\n")
        append(contextPayload.pinnedContextSnippet.trim())
        append("\n\n")
      }
      append(cleanPrompt)
    }

    val accumulated = StringBuilder()
    try {
      withTimeout(INFERENCE_TIMEOUT_MS) {
        chat.sendMessageStream(contextualUserTurn).collect { chunk ->
          val delta = chunk.text.orEmpty()
          if (delta.isNotEmpty()) {
            accumulated.append(delta)
            emit(accumulated.toString())
          }
        }
      }
      if (accumulated.isBlank()) {
        throw IllegalStateException("Gemini (${mode.modelName}) returned an empty response. Please try again.")
      }
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "Streaming inference timed out for ${mode.modelName}", e)
      throw IllegalStateException("Response timed out after 60 seconds. Check your connection or try Fast mode.", e)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Firebase AI chat error (${mode.modelName})", e)
      throw e
    }
  }.flowOn(Dispatchers.IO)

  internal fun buildSystemInstruction(
    mode: TutorMode,
    ctx: TutorContextPayload
  ): String = buildString {
    appendLine(mode.systemRolePrompt)
    appendLine()
    appendLine("Core Behavioral Rules:")
    appendLine("- Answer directly and accurately based on verified academic principles and the student's live TTS Academy context.")
    appendLine("- Keep mathematical formulas and step-by-step derivations crystal clear in readable plain text / Markdown.")
    appendLine("- Respect prior conversation turns seamlessly for multi-turn follow-up questions.")
    appendLine("- Do NOT invent fake course facts; if something is outside the provided syllabus summary, state standard university knowledge clearly.")

    if (ctx.includeAppContext) {
      appendLine()
      appendLine("=== LIVE STUDENT CONTEXT ===")
      appendLine("Student: ${ctx.studentName} | Department: ${ctx.department}")
      appendLine("Study Progress: ${ctx.questionsAnswered} questions answered, ${ctx.overallAccuracyPercent}% accuracy, ${ctx.streakDays}-day streak, Daily Goal: ${ctx.dailyGoalMinutes} mins/day.")
      if (ctx.weakTopicsSummary.isNotBlank()) {
        appendLine("Topics Needing Practice (Weak Areas): ${ctx.weakTopicsSummary}")
      }

      if (!ctx.activeCourseTitle.isNullOrBlank()) {
        appendLine()
        appendLine("=== ACTIVE COURSE SYLLABUS & CONTENT ===")
        appendLine("Course: ${ctx.activeCourseCode.orEmpty()} - ${ctx.activeCourseTitle} (${ctx.activeCourseCategory.orEmpty()})")
        if (!ctx.activeCourseDescription.isNullOrBlank()) {
          appendLine("Course Overview: ${ctx.activeCourseDescription}")
        }
        if (ctx.courseTopicsSummary.isNotBlank()) {
          appendLine("Chapters / Sections: ${ctx.courseTopicsSummary}")
        }
        if (ctx.courseNotesSummary.isNotBlank()) {
          appendLine("Published Lecture Notes & Formulas:")
          appendLine(ctx.courseNotesSummary)
        }
        if (ctx.coursePastExamsSummary.isNotBlank()) {
          appendLine("Available Past Exams: ${ctx.coursePastExamsSummary}")
        }
      }
    }
  }

  internal fun buildSanitizedHistory(history: List<Pair<String, Boolean>>): List<Content> {
    val sanitized = mutableListOf<Pair<String, String>>()
    var lastRole: String? = null
    for ((msgText, isUser) in history.takeLast(12)) {
      val clean = msgText.trim()
      if (clean.isBlank()) continue
      val role = if (isUser) "user" else "model"
      if (role != lastRole) {
        sanitized.add(role to clean)
        lastRole = role
      }
    }
    // History must start with a user turn and end on a model turn before the new user prompt
    while (sanitized.isNotEmpty() && sanitized.first().first != "user") {
      sanitized.removeAt(0)
    }
    while (sanitized.isNotEmpty() && sanitized.last().first == "user") {
      sanitized.removeAt(sanitized.lastIndex)
    }
    return sanitized.map { (role, text) ->
      content(role = role) { text(text) }
    }
  }
}
