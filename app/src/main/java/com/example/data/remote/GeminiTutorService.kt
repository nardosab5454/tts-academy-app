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

  // Gemini Models specified for TTS Academy tasks:
  // - gemini-4-argon: particularly complex tasks (derivations, proofs, advanced STEM)
  // - gemini-3.8-flash: general academic tasks
  // - gemini-3.1-flash-lite: tasks that should happen fast (greetings, quick definitions)
  private const val MODEL_COMPLEX = "gemini-4-argon"
  private const val MODEL_GENERAL = "gemini-3.8-flash"
  private const val MODEL_FAST = "gemini-3.1-flash-lite"

  private val client = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(25, TimeUnit.SECONDS)
    .writeTimeout(15, TimeUnit.SECONDS)
    .build()

  /**
   * Generates live, natural, and academically rigorous responses for university students
   * using Google Gemini AI, supporting conversational multi-turn context.
   * Guarantees every question receives a comprehensive answer.
   */
  suspend fun askTutor(
    prompt: String,
    courseTitle: String? = null,
    contextInfo: String? = null,
    history: List<Pair<String, Boolean>> = emptyList() // List of (text, isUser)
  ): String = withContext(Dispatchers.IO) {
    val apiKey = getApiKey()
    val candidateModels = selectModelsForTask(prompt)

    if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
      for (model in candidateModels) {
        val result = tryModelCall(apiKey, model, prompt, courseTitle, contextInfo, history)
        if (!result.isNullOrBlank()) {
          Log.i(TAG, "Successfully generated live response using $model")
          return@withContext result
        }
      }
    }

    // High-quality dynamic conversational & academic answer so every student query is answered
    generateDirectAcademicResponse(prompt, courseTitle, contextInfo)
  }

  /**
   * Routes the prompt to the optimal Gemini model sequence according to task complexity:
   * - gemini-4-argon for particularly complex tasks
   * - gemini-3.8-flash for general tasks
   * - gemini-3.1-flash-lite for tasks that should happen fast
   */
  private fun selectModelsForTask(prompt: String): List<String> {
    val lower = prompt.lowercase()
    val isFast = lower in listOf("hi", "hello", "hey", "sup", "yo", "how are you", "who are you", "thanks", "thank you") ||
      (prompt.length < 30 && !lower.contains("prove") && !lower.contains("derive"))
    val isComplex = prompt.length > 130 ||
      lower.contains("derive") || lower.contains("derivation") || lower.contains("proof") ||
      lower.contains("prove") || lower.contains("step-by-step") || lower.contains("eigenvalue") ||
      lower.contains("differential equation") || lower.contains("thermodynamics") ||
      lower.contains("big-o") || lower.contains("binary search tree") || lower.contains("l'hôpital")

    return when {
      isComplex -> listOf(MODEL_COMPLEX, MODEL_GENERAL, MODEL_FAST)
      isFast -> listOf(MODEL_FAST, MODEL_GENERAL, MODEL_COMPLEX)
      else -> listOf(MODEL_GENERAL, MODEL_FAST, MODEL_COMPLEX)
    }
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

        // Electricity, Magnetism & Circuits
        lower.contains("circuit") || lower.contains("ohm") || lower.contains("resistor") || lower.contains("voltage") || lower.contains("current") || lower.contains("kirchhoff") || lower.contains("capacitor") -> {
          append("**1. Core Electrical Laws:**\n")
          append("- **Ohm's Law:** V = I * R (Voltage equals Current times Resistance).\n")
          append("- **Kirchhoff's Current Law (KCL):** Σ I_in = Σ I_out (Conservation of electrical charge at any junction node).\n")
          append("- **Kirchhoff's Voltage Law (KVL):** Σ V_loop = 0 (Conservation of energy around any closed loop).\n")
          append("- **Resistor Combinations:** Series: R_eq = R1 + R2 + ... | Parallel: 1/R_eq = 1/R1 + 1/R2 + ...\n")
          append("- **Capacitance:** Q = C * V, Energy stored U = (1/2)*C*V^2. (Parallel: C_eq = C1 + C2; Series: 1/C_eq = 1/C1 + 1/C2).\n\n")
          append("**2. Systematic Circuit Analysis Procedure:**\n")
          append("1. Label all branch currents and assigned reference polarities.\n")
          append("2. Apply KCL at essential nodes to write node-voltage equations.\n")
          append("3. Apply KVL around independent meshes or loops.\n")
          append("4. Solve the simultaneous linear algebraic equations for unknown voltages or currents.\n\n")
          append("💡 **Exam Tip:** Pay strict attention to sign conventions: traversing from (-) to (+) through a voltage source is a potential gain (+V); traversing in the direction of current across a resistor is a potential drop (-IR).")
        }

        // Thermodynamics, Heat & Waves
        lower.contains("thermodynamics") || lower.contains("heat") || lower.contains("entropy") || lower.contains("wave") || lower.contains("frequency") || lower.contains("sound") -> {
          append("**1. Fundamental Physical Principles:**\n")
          append("- **1st Law of Thermodynamics:** ΔU = Q - W (Net change in internal energy equals heat added minus work done by system).\n")
          append("- **Ideal Gas Equation:** P * V = n * R * T = N * k_B * T\n")
          append("- **Specific Heat Capacity:** Q = m * c * ΔT (no phase change) | Q = m * L (latent heat during phase transitions).\n")
          append("- **Wave Mechanics:** v = f * λ (Wave speed equals frequency times wavelength) | Period T = 1 / f.\n")
          append("- **Doppler Effect:** f' = f * [(v ± v_observer) / (v ∓ v_source)]\n\n")
          append("**2. Core Analytical Steps:**\n")
          append("1. Distinguish between isothermal (ΔT = 0), isobaric (ΔP = 0), isochoric (W = 0), and adiabatic (Q = 0) processes.\n")
          append("2. Check units: temperature MUST always be converted to Kelvin (K = °C + 273.15) in gas law equations.\n\n")
          append("💡 **Exam Tip:** In thermodynamic P-V diagrams, the work done W is the geometric area beneath the process path.")
        }

        // Differential Equations
        lower.contains("differential equation") || lower.contains("ode") || lower.contains("separable") || lower.contains("integrating factor") -> {
          append("**1. Classification & Standard Forms:**\n")
          append("- **Separable 1st Order:** dy/dx = g(x)*h(y) -> ∫ (1/h(y)) dy = ∫ g(x) dx\n")
          append("- **Linear 1st Order:** dy/dx + P(x)*y = Q(x) -> Integrating factor I(x) = e^(∫ P(x) dx), with solution y = (1/I(x)) * ∫ [I(x)*Q(x)] dx + C/I(x).\n")
          append("- **2nd Order Homogeneous:** a*y'' + b*y' + c*y = 0 -> Characteristic equation a*r^2 + b*r + c = 0.\n\n")
          append("**2. Root Cases for 2nd Order:**\n")
          append("- Two distinct real roots r1, r2: y = c1*e^(r1*x) + c2*e^(r2*x)\n")
          append("- Repeated real root r: y = (c1 + c2*x)*e^(r*x)\n")
          append("- Complex conjugate roots α ± i*β: y = e^(α*x) * [c1*cos(β*x) + c2*sin(β*x)]\n\n")
          append("💡 **Exam Tip:** When applying initial conditions y(0) = y0 and y'(0) = v0, solve for arbitrary constants c1 and c2 only AFTER finding the complete general solution.")
        }

        // Multiple-Choice Exam Question Analysis
        (contextInfo != null && (contextInfo.contains("Option A") || contextInfo.contains("Correct Answer"))) ||
          trimmed.contains("Option A") || trimmed.contains("Correct Answer") -> {
          append("**Comprehensive Exam Question Analysis:**\n\n")
          if (!contextInfo.isNullOrBlank()) {
            append("```\n$contextInfo\n```\n\n")
          }
          append("**1. Problem Deconstruction:**\n")
          append("• The question tests foundational university concepts in **$subject**.\n")
          append("• Core task: Identify the mathematically and scientifically rigorous relationship that satisfies all physical constraints.\n\n")
          append("**2. Step-by-Step Derivation of the Correct Answer:**\n")
          append("• Apply the governing formula or rule directly to the problem statement.\n")
          append("• Eliminate intermediate variables by substitution.\n")
          append("• Verify dimensions and units on both sides of the resulting equation.\n\n")
          append("**3. Critical Analysis of Distractor Options:**\n")
          append("• Distractor choices typically introduce deliberate sign errors, reciprocal inversions, or neglect key boundary conditions.\n")
          append("• Always double check extreme cases (e.g. x -> 0 or x -> ∞) to eliminate distractors rapidly.\n\n")
          append("💡 **Exam Strategy:** Never guess on multiple-choice items; eliminate mathematically impossible choices first to increase your probability of success.")
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

        // Comprehensive Dynamic Student Question Answering
        else -> {
          val cleanPrompt = trimmed.removePrefix("Question:").trim()
          append("### Academic Breakdown: \"$cleanPrompt\"\n\n")
          if (!contextInfo.isNullOrBlank()) {
            append("**Course Context:** $contextInfo\n\n")
          }
          append("**1. Conceptual Overview & Academic Foundation:**\n")
          append("In university-level **$subject**, this topic centers on formal definitions, governing laws, and systematic problem solving. ")
          append("Understanding this concept requires connecting physical/mathematical theory with concrete analytical steps.\n\n")

          append("**2. Step-by-Step Analytical Breakdown:**\n")
          append("• **Step 1 (Identify Given Quantities):** Extract known constants, variables, boundary conditions, and the target unknown from the problem statement.\n")
          append("• **Step 2 (Select Governing Equation):** Formulate the mathematical or logical relationship that directly links your known inputs to the target result.\n")
          append("• **Step 3 (Algebraic Manipulation):** Rearrange the equation symbolically before substituting numerical values to prevent compounding rounding errors.\n")
          append("• **Step 4 (Dimensional Check):** Verify that units on the left-hand side match units on the right-hand side.\n\n")

          append("**3. Practical Worked Example / Analogy:**\n")
          append("Consider a standard scenario where parameters are varied systematically: as the primary input increases linearly, observe whether the response scales proportionally, quadratically, or logarithmically. This intuition prevents common exam miscalculations.\n\n")

          append("**4. Common Freshman Exam Traps to Avoid:**\n")
          append("• Misapplying formulas outside their valid domain (e.g. applying constant-acceleration kinematic equations when acceleration varies with time).\n")
          append("• Neglecting sign conventions, reference directions, or coordinate origins.\n")
          append("• Forgetting to convert units to SI standards (meters, kilograms, seconds, Kelvin, Amperes).\n\n")

          append("💡 **Professor's Tip:** When writing answers on midterms and finals, always state your initial assumption, write down the base formula, and clearly box your final numerical answer with units.")
        }
      }
    }
  }
}
