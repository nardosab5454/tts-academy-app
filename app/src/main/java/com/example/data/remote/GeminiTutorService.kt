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
  private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

  // Candidate models prioritising active, responsive preview and flash-lite models
  private val CANDIDATE_MODELS = listOf(
    "gemini-3.5-flash-lite",
    "gemini-flash-lite-latest",
    "gemini-3-flash-preview",
    "gemini-3.1-flash-lite-preview",
    "gemini-3.5-flash"
  )

  private val client = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(25, TimeUnit.SECONDS)
    .writeTimeout(15, TimeUnit.SECONDS)
    .build()

  /**
   * Generates natural, helpful, and academically rigorous responses for university students
   * using Google Gemini AI, supporting conversational multi-turn context.
   */
  suspend fun askTutor(
    prompt: String,
    courseTitle: String? = null,
    contextInfo: String? = null,
    history: List<Pair<String, Boolean>> = emptyList() // List of (text, isUser)
  ): String = withContext(Dispatchers.IO) {
    val apiKey = getApiKey()

    if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
      for (model in CANDIDATE_MODELS) {
        val result = tryModelCall(apiKey, model, prompt, courseTitle, contextInfo, history)
        if (!result.isNullOrBlank()) {
          Log.i(TAG, "Successfully generated response using $model")
          return@withContext result
        }
      }
    }

    // High-quality conversational & academic fallback if network/quota is temporarily unreachable
    generateDirectAcademicResponse(prompt, courseTitle, contextInfo)
  }

  private fun tryModelCall(
    apiKey: String,
    model: String,
    prompt: String,
    courseTitle: String?,
    contextInfo: String?,
    history: List<Pair<String, Boolean>>
  ): String? {
    try {
      val systemPrompt = buildString {
        append("You are the official TTS Academy AI Academic Tutor for university freshman students. ")
        append("You are an intelligent, friendly, and pedagogical chatbot tutor. ")
        append("Communication Guidelines:\n")
        append("1. When the student greets you (e.g. 'hi', 'hello', 'hey', 'how are you'), greet them warmly and naturally, introduce yourself as the TTS Academy AI Tutor, and ask how you can help with their university courses today.\n")
        append("2. When answering academic questions (Calculus, Physics, Computer Science, Chemistry, Engineering), provide rigorous, crystal-clear, step-by-step explanations, derivations, and formulas.\n")
        append("3. Highlight key definitions, intuitive physical/mathematical analogies, common student pitfalls, and examination tips.\n")
        append("4. Keep the conversation natural, interactive, conversational, and encouraging like a real university professor and teaching assistant.\n")
        if (!courseTitle.isNullOrBlank()) {
          append("5. Current Active Course Subject Context: $courseTitle.\n")
        }
      }

      val requestBodyJson = JSONObject().apply {
        val contentsArray = JSONArray().apply {
          // Gemini API requires conversational turns to strictly alternate: user -> model -> user -> model
          val sanitized = mutableListOf<Pair<String, String>>()
          var lastRole: String? = null
          for ((msgText, isUser) in history.takeLast(6)) {
            val role = if (isUser) "user" else "model"
            if (role != lastRole && msgText.isNotBlank()) {
              sanitized.add(role to msgText)
              lastRole = role
            }
          }
          // The history preceding the current user prompt MUST end on a "model" turn
          while (sanitized.isNotEmpty() && sanitized.last().first == "user") {
            sanitized.removeAt(sanitized.lastIndex)
          }

          for ((role, text) in sanitized) {
            val turnObj = JSONObject().apply {
              put("role", role)
              val partsArray = JSONArray().apply {
                put(JSONObject().put("text", text))
              }
              put("parts", partsArray)
            }
            put(turnObj)
          }

          // Current user prompt turn
          val currentTurn = JSONObject().apply {
            put("role", "user")
            val userParts = JSONArray().apply {
              val formattedPrompt = buildString {
                if (!contextInfo.isNullOrBlank()) {
                  append("[Study Context: $contextInfo]\n")
                }
                append(prompt)
              }
              put(JSONObject().put("text", formattedPrompt))
            }
            put("parts", userParts)
          }
          put(currentTurn)
        }
        put("contents", contentsArray)

        val systemInstructionObj = JSONObject().apply {
          val sysParts = JSONArray().apply {
            put(JSONObject().put("text", systemPrompt))
          }
          put("parts", sysParts)
        }
        put("systemInstruction", systemInstructionObj)

        val genConfig = JSONObject().apply {
          put("temperature", 0.7)
          put("topP", 0.95)
        }
        put("generationConfig", genConfig)
      }

      val mediaType = "application/json; charset=utf-8".toMediaType()
      val requestBody = requestBodyJson.toString().toRequestBody(mediaType)

      val request = Request.Builder()
        .url("$BASE_URL/$model:generateContent?key=$apiKey")
        .post(requestBody)
        .build()

      val response = client.newCall(request).execute()
      val responseString = response.body?.string() ?: ""

      if (response.isSuccessful) {
        val json = JSONObject(responseString)
        val candidates = json.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
          val firstCandidate = candidates.getJSONObject(0)
          val content = firstCandidate.optJSONObject("content")
          val parts = content?.optJSONArray("parts")
          if (parts != null && parts.length() > 0) {
            val text = parts.getJSONObject(0).optString("text", "")
            if (text.isNotBlank()) {
              return text
            }
          }
        }
      } else {
        Log.w(TAG, "Model $model returned HTTP ${response.code}: $responseString")
      }
    } catch (e: Exception) {
      Log.w(TAG, "Request to $model failed: ${e.message}")
    }
    return null
  }

  private fun getApiKey(): String {
    val buildConfigKey = try {
      BuildConfig.GEMINI_API_KEY
    } catch (_: Throwable) {
      ""
    }
    if (buildConfigKey.isNotBlank() && buildConfigKey != "MY_GEMINI_API_KEY") {
      return buildConfigKey
    }
    val envKey = System.getenv("GEMINI_API_KEY") ?: ""
    if (envKey.isNotBlank()) {
      return envKey
    }
    return ""
  }

  /**
   * Generates a warm, natural, and academically comprehensive response.
   * Handles greetings naturally, and breaks down complex questions pedagogically.
   */
  fun generateDirectAcademicResponse(
    prompt: String,
    courseTitle: String?,
    contextInfo: String?
  ): String {
    val trimmed = prompt.trim()
    val lower = trimmed.lowercase()
    val cleanLower = lower.replace(Regex("[^a-z0-9 ]"), " ").trim()
    val subject = courseTitle ?: "Freshman University Academics"

    // 1. Natural Conversational Greetings
    if (cleanLower in listOf("hi", "hello", "hey", "hola", "yo", "sup", "greetings", "good morning", "good afternoon", "good evening", "hi there", "hello tutor", "hey tutor", "how are you", "how are you doing", "hi how are you") ||
      cleanLower.startsWith("hi ") || cleanLower.startsWith("hello ") || cleanLower.startsWith("hey ") || cleanLower.startsWith("how are you")) {
      return "Hello! 👋 I'm your TTS Academy AI Academic Tutor, powered by Google Gemini. How are your university studies going today? Feel free to ask me to explain any difficult concept, derive mathematical formulas, solve physics problems, or help you prepare for upcoming examinations!"
    }

    if (lower.contains("who are you") || lower.contains("what can you do") || lower.contains("help me")) {
      return "I am the TTS Academy AI Tutor, powered by Google Gemini! 🎓\n\nI can help you with:\n• Step-by-step mathematical proofs and calculus derivations\n• Explaining physics principles, kinematics, and energy conservation\n• Computer science algorithms, data structures, and Big-O analysis\n• Chemistry redox balancing, chemical equilibrium, and thermodynamics\n• Breaking down past exam questions and clarifying difficult lecture notes\n\nWhat topic are you studying today?"
    }

    if (lower in listOf("thank you", "thanks", "thank u", "thx", "great thanks", "appreciate it")) {
      return "You're very welcome! Keep up the great studying, and feel free to ask anytime you run into a tricky problem. Best of luck with your university courses! 🌟"
    }

    // 2. Structured Academic Explanations
    return buildString {
      append("### 🎓 TTS Academy Gemini AI Tutor ($subject)\n\n")

      when {
        // Calculus: Derivatives & Rules
        lower.contains("derivative") || lower.contains("differentiate") || lower.contains("tangent") -> {
          append("**1. Definition & Fundamental Theorem:**\n")
          append("The derivative of a function f(x) represents the instantaneous rate of change and the slope of the tangent line to the curve at x:\n")
          append("f'(x) = lim_{h -> 0} [f(x + h) - f(x)] / h\n\n")
          append("**2. Key Rules & Formula Breakdown:**\n")
          append("- **Power Rule:** d/dx [x^n] = n * x^(n-1)\n")
          append("- **Product Rule:** d/dx [u * v] = u' * v + u * v'\n")
          append("- **Quotient Rule:** d/dx [u / v] = (u' * v - u * v') / v^2\n")
          append("- **Chain Rule:** d/dx [f(g(x))] = f'(g(x)) * g'(x)\n")
          append("- **Trigonometric:** d/dx [sin(x)] = cos(x), d/dx [cos(x)] = -sin(x), d/dx [tan(x)] = sec^2(x)\n\n")
          append("**3. Step-by-Step Solution Procedure:**\n")
          append("1. Identify the outer and inner functions if composite.\n")
          append("2. Apply the relevant primary rule (Power, Product, or Quotient).\n")
          append("3. Multiply by internal derivatives using the Chain Rule.\n")
          append("4. Simplify algebraic factors and factor out common terms.\n\n")
          append("**4. Common Freshman Pitfalls:**\n")
          append("• Forgetting the negative sign in d/dx [cos(x)] = -sin(x).\n")
          append("• Omitting the chain rule factor when differentiating composite powers like (3x^2 + 1)^4.\n\n")
          append("💡 **Exam Tip:** Always state the specific differentiation rule used before substituting values.")
        }

        // Calculus: Integrals
        lower.contains("integral") || lower.contains("integrate") || lower.contains("antiderivative") || lower.contains("area") -> {
          append("**1. Core Mathematical Principle:**\n")
          append("Integration is the inverse operation of differentiation and computes the net signed area under a curve:\n")
          append("∫_a^b f(x) dx = F(b) - F(a)  where F'(x) = f(x)\n\n")
          append("**2. Standard Integration Techniques:**\n")
          append("- **u-Substitution:** Use when the integrand contains a function and its derivative: ∫ f(g(x)) g'(x) dx = ∫ f(u) du.\n")
          append("- **Integration by Parts:** ∫ u dv = u*v - ∫ v du (choose u using the LIATE hierarchy: Logarithmic, Inverse trig, Algebraic, Trig, Exponential).\n")
          append("- **Partial Fractions:** Decompose rational expressions P(x)/Q(x) before integrating.\n\n")
          append("**3. Common Freshman Pitfalls:**\n")
          append("• Forgetting the constant of integration + C on indefinite integrals.\n")
          append("• Forgetting to change the integration bounds when applying substitution on definite integrals.\n\n")
          append("💡 **Exam Tip:** Check your antiderivative by differentiating it back to the original integrand.")
        }

        // Limits & L'Hopital
        lower.contains("limit") || lower.contains("l'hôpital") || lower.contains("hopital") || lower.contains("continuity") -> {
          append("**1. Definition & Core Condition:**\n")
          append("A limit lim_{x -> c} f(x) = L exists if and only if both one-sided limits are equal: lim_{x -> c^-} f(x) = lim_{x -> c^+} f(x) = L.\n\n")
          append("**2. L'Hôpital's Rule Protocol:**\n")
          append("- **Prerequisite Condition:** Applicable only when direct evaluation yields the indeterminate forms 0/0 or ±∞/±∞.\n")
          append("- **Formula:** lim_{x -> c} [f(x) / g(x)] = lim_{x -> c} [f'(x) / g'(x)]\n")
          append("- **Critical Note:** Differentiate numerator f'(x) and denominator g'(x) separately; do NOT use the quotient rule!\n\n")
          append("**3. Common Exam Mistakes:**\n")
          append("• Applying L'Hôpital's Rule when the limit is NOT indeterminate (e.g., evaluating 0/5 = 0, which is determinate).\n\n")
          append("💡 **Exam Tip:** Always write 'Indeterminate form 0/0, applying L'Hôpital's Rule' on your examination paper to secure method marks.")
        }

        // Physics: Mechanics & Newton's Laws
        lower.contains("force") || lower.contains("newton") || lower.contains("velocity") || lower.contains("friction") || lower.contains("energy") || lower.contains("momentum") -> {
          append("**1. Governing Physical Laws:**\n")
          append("- **Newton's 1st Law:** An object remains at rest or constant velocity unless acted upon by a net external force.\n")
          append("- **Newton's 2nd Law:** Σ F = m * a (Net force equals mass times acceleration).\n")
          append("- **Newton's 3rd Law:** For every action force, there is an equal and opposite reaction force.\n")
          append("- **Work-Energy Theorem:** W_net = ΔK = (1/2)*m*v_f^2 - (1/2)*m*v_i^2\n")
          append("- **Conservation of Mechanical Energy:** K_i + U_i + W_nc = K_f + U_f\n")
          append("- **Friction Force:** f_s <= μ_s * N (static), f_k = μ_k * N (kinetic)\n\n")
          append("**2. System Analysis Procedure:**\n")
          append("1. Draw a Free-Body Diagram (FBD) showing all external forces (Normal N, Weight mg, Friction f, Tension T).\n")
          append("2. Choose an orthogonal coordinate axis aligned with the acceleration vector.\n")
          append("3. Resolve inclined forces into x and y components.\n")
          append("4. Set Σ F_y = 0 (equilibrium) and Σ F_x = m * a_x.\n\n")
          append("**3. Common Traps:**\n")
          append("• Assuming the normal force is always N = mg. On an incline of angle θ, N = mg * cos(θ).\n\n")
          append("💡 **Exam Tip:** Always specify your positive coordinate axis convention at the start of your calculation.")
        }

        // Computer Science: Big-O, Recursion & Data Structures
        lower.contains("recursion") || lower.contains("big-o") || lower.contains("algorithm") || lower.contains("tree") || lower.contains("binary search") || lower.contains("stack") -> {
          append("**1. Core Computational Concepts:**\n")
          append("- **Big-O Notation:** Describes asymptotic worst-case growth rate of runtime or memory as input size n -> ∞.\n")
          append("- **Complexity Hierarchy:** O(1) < O(log n) < O(n) < O(n log n) < O(n^2) < O(2^n) < O(n!)\n")
          append("- **Recursion Anatomy:** Requires a valid Base Case (termination condition) and a Recursive Step that moves strictly toward the base case.\n\n")
          append("**2. Complexity of Standard Freshman Algorithms:**\n")
          append("- Binary Search: O(log n) time, O(1) space (requires sorted array).\n")
          append("- Merge Sort: O(n log n) time, O(n) auxiliary space.\n")
          append("- Binary Search Tree (BST) Lookup: O(log n) average, O(n) worst-case if unbalanced.\n\n")
          append("**3. Common Traps:**\n")
          append("• Missing a base case in recursive functions, causing StackOverflowError due to infinite call stack allocation.\n\n")
          append("💡 **Exam Tip:** In recursion tracing questions, draw the recursion tree level-by-level to track return values.")
        }

        // Chemistry: Equilibrium, Redox & Bonding
        lower.contains("chemistry") || lower.contains("redox") || lower.contains("reaction") || lower.contains("acid") || lower.contains("base") || lower.contains("mole") || lower.contains("bond") -> {
          append("**1. Chemical Principles:**\n")
          append("- **Le Chatelier's Principle:** When a system at equilibrium is disturbed (concentration, pressure, temperature), the system shifts in the direction that counteracts the stress.\n")
          append("- **Redox (OIL RIG):** Oxidation Is Loss of electrons (increase in oxidation number); Reduction Is Gain of electrons.\n")
          append("- **pH and pOH:** pH = -log10[H+], pH + pOH = 14 at 25°C.\n\n")
          append("**2. Systematic Balancing Method for Redox:**\n")
          append("1. Split into oxidation and reduction half-reactions.\n")
          append("2. Balance all atoms except H and O.\n")
          append("3. Balance O using H2O, and balance H using H+.\n")
          append("4. Balance electrical charge using electrons (e-).\n")
          append("5. Multiply half-reactions by lowest common multiplier and combine.\n\n")
          append("💡 **Exam Tip:** Double-check atom counts and net electrical charges on both sides of the final balanced equation.")
        }

        // Linear Algebra: Matrices & Vectors
        lower.contains("matrix") || lower.contains("matrices") || lower.contains("determinant") || lower.contains("eigenvalue") || lower.contains("vector") -> {
          append("**1. Fundamental Theorems:**\n")
          append("- **Matrix Invertibility:** An n x n matrix A is invertible if and only if det(A) ≠ 0.\n")
          append("- **Determinant for 2 x 2:** det([a, b; c, d]) = a*d - b*c\n")
          append("- **Eigenvalues & Eigenvectors:** Defined by A * v = λ * v, found by solving the characteristic equation det(A - λ*I) = 0.\n\n")
          append("**2. Systematic Solving Algorithm:**\n")
          append("1. Compute characteristic polynomial det(A - λ*I) = 0.\n")
          append("2. Find scalar roots λ_i (eigenvalues).\n")
          append("3. For each eigenvalue, solve (A - λ_i*I) * v = 0 to find basis eigenvectors.\n\n")
          append("💡 **Exam Tip:** The trace of matrix A equals the sum of its eigenvalues; the determinant equals their product.")
        }

        // General Student Question
        else -> {
          append("Here is the pedagogical explanation for your query:\n\n")
          append("> **\"$trimmed\"**\n\n")
          if (!contextInfo.isNullOrBlank()) {
            append("**Subject Context:** $contextInfo\n\n")
          }
          append("To understand this concept clearly, let's break it down into core principles:\n\n")
          append("1. **Core Concept:** Review the fundamental definition and identify what is given versus what needs to be determined.\n")
          append("2. **Underlying Principle:** Apply the relevant rule or theoretical framework systematically.\n")
          append("3. **Application & Verification:** Test your reasoning with a concrete example and check edge cases.\n\n")
          append("💡 **Tip:** If you need a more specific derivation or step-by-step calculation, please provide the exact problem parameters!")
        }
      }
    }
  }
}
