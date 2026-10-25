package com.example.data.local

import com.example.data.local.model.AccessCodeEntity
import com.example.data.local.model.AnnouncementEntity
import com.example.data.local.model.CourseEntity
import com.example.data.local.model.NoteEntity
import com.example.data.local.model.PastExamEntity
import com.example.data.local.model.QuestionEntity
import com.example.data.local.model.TopicEntity
import com.example.data.local.model.UserEntity

object SeedData {

  fun getDefaultUsers(): List<UserEntity> = listOf(
    UserEntity(
      id = "admin_1",
      name = "System Administrator",
      email = "spikenarrd@gmail.com",
      password = "Spike@6776",
      studentId = "SYS-ADMIN-01",
      department = "Academic Affairs",
      role = "ADMIN",
      avatarUrl = "avatar_admin",
      streakDays = 12,
      lastStudyDate = "2026-10-12",
      questionsAnsweredCount = 0,
      correctAnswersCount = 0,
      isNotificationsEnabled = true,
      studyGoalMinutesPerDay = 30,
      isPremium = true
    ),
    UserEntity(
      id = "admin_abebe",
      name = "Abebe Wotte (Admin)",
      email = "abebewottee@gmail.com",
      password = "Admin@123",
      studentId = "SYS-ADMIN-02",
      department = "Academic Affairs",
      role = "ADMIN",
      avatarUrl = "avatar_admin",
      streakDays = 15,
      lastStudyDate = "2026-10-12",
      questionsAnsweredCount = 0,
      correctAnswersCount = 0,
      isNotificationsEnabled = true,
      studyGoalMinutesPerDay = 30,
      isPremium = true
    )
  )

  fun getCourses(): List<CourseEntity> = listOf(
    CourseEntity(
      id = "MATH_101",
      code = "MATH 101",
      title = "Calculus I",
      description = "Limits, derivatives, differentiation techniques, optimization, and fundamental theorem of calculus.",
      category = "STEM",
      iconName = "calculate",
      totalQuestions = 18,
      totalNotes = 5,
      isPublished = true,
      orderIndex = 1
    ),
    CourseEntity(
      id = "PHYS_101",
      code = "PHYS 101",
      title = "General Physics I",
      description = "Classical mechanics, 1D/2D kinematics, Newton's laws, energy conservation, and linear momentum.",
      category = "STEM",
      iconName = "science",
      totalQuestions = 15,
      totalNotes = 4,
      isPublished = true,
      orderIndex = 2
    ),
    CourseEntity(
      id = "CHEM_101",
      code = "CHEM 101",
      title = "General Chemistry",
      description = "Atomic theory, periodic trends, chemical bonding, molecular geometry, stoichiometry, and thermochemistry.",
      category = "STEM",
      iconName = "biotech",
      totalQuestions = 14,
      totalNotes = 4,
      isPublished = true,
      orderIndex = 3
    ),
    CourseEntity(
      id = "CS_101",
      code = "CS 101",
      title = "Intro to Computer Science",
      description = "Algorithmic thinking, control structures, functions, memory models, and data representation.",
      category = "Computing",
      iconName = "terminal",
      totalQuestions = 16,
      totalNotes = 4,
      isPublished = true,
      orderIndex = 4
    ),
    CourseEntity(
      id = "ENG_101",
      code = "ENG 101",
      title = "Academic Writing & Comm.",
      description = "Rhetorical strategies, formal academic essays, synthesis of literature, citations, and critical argumentation.",
      category = "Humanities",
      iconName = "menu_book",
      totalQuestions = 12,
      totalNotes = 3,
      isPublished = true,
      orderIndex = 5
    ),
    CourseEntity(
      id = "PHIL_101",
      code = "PHIL 101",
      title = "Critical Thinking & Logic",
      description = "Deductive validity, inductive inference, formal syllogisms, informal fallacies, and cognitive heuristics.",
      category = "Humanities",
      iconName = "psychology",
      totalQuestions = 12,
      totalNotes = 3,
      isPublished = true,
      orderIndex = 6
    ),
    CourseEntity(
      id = "MATH_102",
      code = "MATH 102",
      title = "Linear Algebra",
      description = "Vector spaces, matrices, systems of linear equations, determinants, eigenvalues, and transformations.",
      category = "STEM",
      iconName = "grid_view",
      totalQuestions = 14,
      totalNotes = 4,
      isPublished = true,
      orderIndex = 7
    ),
    CourseEntity(
      id = "BIO_101",
      code = "BIO 101",
      title = "General Biology",
      description = "Cellular architecture, bioenergetics, DNA replication, molecular genetics, and ecological principles.",
      category = "STEM",
      iconName = "eco",
      totalQuestions = 12,
      totalNotes = 3,
      isPublished = true,
      orderIndex = 8
    ),
    CourseEntity(
      id = "ENGR_101",
      code = "ENGR 101",
      title = "Engineering Statics",
      description = "Force systems, moments, equilibrium of rigid bodies, truss analysis, and friction.",
      category = "Engineering",
      iconName = "construction",
      totalQuestions = 12,
      totalNotes = 3,
      isPublished = true,
      orderIndex = 9
    ),
    CourseEntity(
      id = "ECON_101",
      code = "ECON 101",
      title = "Principles of Microeconomics",
      description = "Scarcity, supply & demand dynamics, elasticity, consumer choice, market structures, and public goods.",
      category = "Social Sciences",
      iconName = "trending_up",
      totalQuestions = 14,
      totalNotes = 4,
      isPublished = true,
      orderIndex = 10
    ),
    CourseEntity(
      id = "STAT_101",
      code = "STAT 101",
      title = "Probability & Statistics",
      description = "Descriptive statistics, combinatorial probability, random variables, normal distributions, and inference.",
      category = "STEM",
      iconName = "bar_chart",
      totalQuestions = 14,
      totalNotes = 4,
      isPublished = true,
      orderIndex = 11
    ),
    CourseEntity(
      id = "CIV_101",
      code = "CIV 101",
      title = "Civics, Ethics & Human Rights",
      description = "Constitutional governance, rule of law, institutional democracy, universal human rights, and civic virtue.",
      category = "Social Sciences",
      iconName = "gavel",
      totalQuestions = 12,
      totalNotes = 3,
      isPublished = true,
      orderIndex = 12
    )
  )

  fun getTopics(): List<TopicEntity> = listOf(
    // MATH 101 Topics
    TopicEntity("T_MATH_1", "MATH_101", "Limits & Continuity", "Evaluating limits algebraically and epsilon-delta concepts", 1),
    TopicEntity("T_MATH_2", "MATH_101", "Derivatives & Chain Rule", "Rates of change, product rule, quotient rule, chain rule", 2),
    TopicEntity("T_MATH_3", "MATH_101", "Applications of Derivatives", "Critical points, optimization problems, and Mean Value Theorem", 3),
    TopicEntity("T_MATH_4", "MATH_101", "Integrals & Fundamental Theorem", "Riemann sums, antiderivatives, and evaluation of definite integrals", 4),

    // PHYS 101 Topics
    TopicEntity("T_PHYS_1", "PHYS_101", "1D & 2D Kinematics", "Displacement, velocity, constant acceleration, projectile motion", 1),
    TopicEntity("T_PHYS_2", "PHYS_101", "Newton's Laws & Friction", "Free-body diagrams, normal force, static and kinetic friction", 2),
    TopicEntity("T_PHYS_3", "PHYS_101", "Work, Energy & Momentum", "Work-energy theorem, conservative forces, and impulse", 3),

    // CHEM 101 Topics
    TopicEntity("T_CHEM_1", "CHEM_101", "Atomic Structure & Bonding", "Electron configurations, periodic trends, Lewis structures", 1),
    TopicEntity("T_CHEM_2", "CHEM_101", "Stoichiometry & Solutions", "Molar calculations, limiting reagents, and aqueous reactions", 2),
    TopicEntity("T_CHEM_3", "CHEM_101", "Thermodynamics & Equilibrium", "Enthalpy, entropy, Gibbs free energy, and Le Chatelier's principle", 3),

    // CS 101 Topics
    TopicEntity("T_CS_1", "CS_101", "Algorithms & Big-O Notation", "Time complexity, searching, and sorting fundamentals", 1),
    TopicEntity("T_CS_2", "CS_101", "Variables, Types & Flow", "Booleans, loops, conditionals, and logical evaluation", 2),
    TopicEntity("T_CS_3", "CS_101", "Functions & Recursion", "Call stack, parameter passing, and recursive base cases", 3),

    // ENG 101 Topics
    TopicEntity("T_ENG_1", "ENG_101", "Thesis & Paragraph Cohesion", "Crafting arguable thesis statements and topic sentences", 1),
    TopicEntity("T_ENG_2", "ENG_101", "Academic Citations & Ethics", "Avoiding plagiarism, APA/MLA synthesis, quoting rules", 2),

    // PHIL 101 Topics
    TopicEntity("T_PHIL_1", "PHIL_101", "Arguments & Validity", "Premises, conclusions, deductive validity vs inductive strength", 1),
    TopicEntity("T_PHIL_2", "PHIL_101", "Logical Fallacies", "Ad hominem, straw man, begging the question, false dilemma", 2),

    // MATH 102 Topics
    TopicEntity("T_MATH2_1", "MATH_102", "Systems of Linear Equations", "Gaussian elimination, augmented matrices, row echelon form", 1),
    TopicEntity("T_MATH2_2", "MATH_102", "Matrix Inverses & Determinants", "Matrix multiplication, inverse formula, and Cramer's rule", 2),

    // BIO 101 Topics
    TopicEntity("T_BIO_1", "BIO_101", "Cell Biology & Membranes", "Organelles, lipid bilayer, active and passive transport", 1),
    TopicEntity("T_BIO_2", "BIO_101", "DNA & Protein Synthesis", "Transcription, translation, codon table, and mutations", 2),

    // ENGR 101 Topics
    TopicEntity("T_ENGR_1", "ENGR_101", "Particle & Rigid Body Equilibrium", "Resultant forces, moments, and 2D equilibrium conditions", 1),
    TopicEntity("T_ENGR_2", "ENGR_101", "Trusses & Method of Joints", "Two-force members, zero-force members, internal axial forces", 2),

    // ECON 101 Topics
    TopicEntity("T_ECON_1", "ECON_101", "Supply, Demand & Equilibrium", "Market equilibrium, shifts vs movements, price controls", 1),
    TopicEntity("T_ECON_2", "ECON_101", "Elasticity & Market Structures", "Price elasticity of demand, perfect competition, and monopoly", 2),

    // STAT 101 Topics
    TopicEntity("T_STAT_1", "STAT_101", "Probability Fundamentals", "Sample spaces, union, intersection, conditional probability", 1),
    TopicEntity("T_STAT_2", "STAT_101", "Normal Distribution & Z-Scores", "Standard normal distribution, empirical rule, hypothesis tests", 2),

    // CIV 101 Topics
    TopicEntity("T_CIV_1", "CIV_101", "Constitutionalism & Democracy", "Separation of powers, checks and balances, rule of law", 1),
    TopicEntity("T_CIV_2", "CIV_101", "Human Rights & Civic Ethics", "Universal human rights covenants, citizen obligations, ethics", 2)
  )

  fun getQuestions(): List<QuestionEntity> = listOf(
    // MATH 101 Questions
    QuestionEntity(
      id = "Q_MATH_1",
      courseId = "MATH_101",
      topicId = "T_MATH_1",
      questionText = "Evaluate the limit: lim (x -> 3) [(x² - 9) / (x - 3)].",
      optionA = "0",
      optionB = "3",
      optionC = "6",
      optionD = "Undefined / Does not exist",
      correctAnswer = "C",
      explanationGeneral = "Factoring the numerator gives (x - 3)(x + 3). Cancelling the common factor (x - 3) yields lim (x -> 3) [x + 3] = 3 + 3 = 6.",
      explanationA = "Incorrect. 0 would occur if the numerator vanished without factoring out the indeterminate 0/0 form.",
      explanationB = "Incorrect. 3 is the limit point itself, not the value of the evaluated expression.",
      explanationC = "Correct! (x² - 9) factors into (x - 3)(x + 3). Cancelling (x - 3) leaves (x + 3), which equals 6 as x approaches 3.",
      explanationD = "Incorrect. While direct substitution yields 0/0 (indeterminate), algebraic simplification resolves the limit to 6.",
      difficulty = "EASY",
      isPastExam = true,
      examYear = "2024",
      examType = "MIDTERM"
    ),
    QuestionEntity(
      id = "Q_MATH_2",
      courseId = "MATH_101",
      topicId = "T_MATH_2",
      questionText = "What is the derivative of f(x) = sin(x² + 3x) with respect to x?",
      optionA = "cos(x² + 3x)",
      optionB = "(2x + 3) cos(x² + 3x)",
      optionC = "(2x + 3) sin(x² + 3x)",
      optionD = "-(2x + 3) cos(x² + 3x)",
      correctAnswer = "B",
      explanationGeneral = "By the chain rule, d/dx [f(g(x))] = f'(g(x)) * g'(x). Here f(u) = sin(u) so f'(u) = cos(u), and g(x) = x² + 3x so g'(x) = 2x + 3.",
      explanationA = "Incorrect. This omits the derivative of the inner function (2x + 3) required by the Chain Rule.",
      explanationB = "Correct! The derivative of outer function sin(u) is cos(u), multiplied by the inner derivative d/dx (x² + 3x) = (2x + 3).",
      explanationC = "Incorrect. The derivative of sine is cosine, not sine.",
      explanationD = "Incorrect. The derivative of sin(u) is positive cos(u); the negative sign applies to the derivative of cos(u).",
      difficulty = "MEDIUM",
      isPastExam = true,
      examYear = "2024",
      examType = "MIDTERM"
    ),
    QuestionEntity(
      id = "Q_MATH_3",
      courseId = "MATH_101",
      topicId = "T_MATH_3",
      questionText = "A critical point of a differentiable function f(x) occurs at x = c whenever:",
      optionA = "f(c) = 0",
      optionB = "f''(c) = 0",
      optionC = "f'(c) = 0 or f'(c) is undefined",
      optionD = "f(c) is a global maximum",
      correctAnswer = "C",
      explanationGeneral = "By definition, a critical number of a function f is a number c in the domain of f such that either f'(c) = 0 or f'(c) does not exist.",
      explanationA = "Incorrect. f(c) = 0 is an x-intercept (root) of the function, not necessarily a critical point.",
      explanationB = "Incorrect. f''(c) = 0 relates to potential inflection points, not critical points.",
      explanationC = "Correct! Critical points are defined precisely where the first derivative equals zero or fails to exist in the function domain.",
      explanationD = "Incorrect. While local extrema occur at critical points, a point being a critical point does not guarantee it is a global maximum.",
      difficulty = "EASY",
      isPastExam = false
    ),
    QuestionEntity(
      id = "Q_MATH_4",
      courseId = "MATH_101",
      topicId = "T_MATH_4",
      questionText = "Evaluate the definite integral ∫ from 0 to 2 of (3x² - 4x + 1) dx.",
      optionA = "2",
      optionB = "4",
      optionC = "6",
      optionD = "8",
      correctAnswer = "A",
      explanationGeneral = "Antiderivative F(x) = x³ - 2x² + x. Evaluating F(2) - F(0): (2³ - 2(2²) + 2) - 0 = (8 - 8 + 2) = 2.",
      explanationA = "Correct! Antiderivative is x³ - 2x² + x. At x = 2: 8 - 8 + 2 = 2; at x = 0: 0. Difference is 2.",
      explanationB = "Incorrect. Check your integration of 4x: ∫ 4x dx = 2x², not 4x.",
      explanationC = "Incorrect. This usually results from an arithmetic error when evaluating x³.",
      explanationD = "Incorrect. Recalculate F(2) using the Power Rule for integrals.",
      difficulty = "MEDIUM",
      isPastExam = true,
      examYear = "2023",
      examType = "FINAL"
    ),

    // PHYS 101 Questions
    QuestionEntity(
      id = "Q_PHYS_1",
      courseId = "PHYS_101",
      topicId = "T_PHYS_1",
      questionText = "A ball is launched vertically upward at 20 m/s. Neglecting air resistance (g = 9.8 m/s²), what is its velocity at the peak of its trajectory?",
      optionA = "20 m/s",
      optionB = "9.8 m/s",
      optionC = "0 m/s",
      optionD = "-20 m/s",
      correctAnswer = "C",
      explanationGeneral = "At the highest point of vertical projectile motion, the instantaneous vertical velocity is 0 m/s as the object momentarily stops before reversing direction.",
      explanationA = "Incorrect. 20 m/s is the initial launch velocity.",
      explanationB = "Incorrect. 9.8 m/s² is the magnitude of gravitational acceleration acting downwards, not the velocity.",
      explanationC = "Correct! At maximum height, the object stops rising and has not yet begun falling, making instantaneous velocity zero.",
      explanationD = "Incorrect. -20 m/s is the return velocity when it lands back at the initial height.",
      difficulty = "EASY",
      isPastExam = true,
      examYear = "2024",
      examType = "MIDTERM"
    ),
    QuestionEntity(
      id = "Q_PHYS_2",
      courseId = "PHYS_101",
      topicId = "T_PHYS_2",
      questionText = "An object of mass 5 kg rests on a frictionless horizontal table. If a constant horizontal force of 15 N is applied, what is its acceleration?",
      optionA = "75 m/s²",
      optionB = "3.0 m/s²",
      optionC = "0.33 m/s²",
      optionD = "10 m/s²",
      correctAnswer = "B",
      explanationGeneral = "According to Newton's Second Law, ΣF = ma. Rearranging for acceleration gives a = F / m = 15 N / 5 kg = 3.0 m/s².",
      explanationA = "Incorrect. 75 comes from multiplying force by mass (F * m) rather than dividing.",
      explanationB = "Correct! a = F / m = 15 N / 5 kg = 3.0 m/s².",
      explanationC = "Incorrect. 0.33 comes from inverting the ratio (m / F).",
      explanationD = "Incorrect. Recalculate using Newton's Second Law a = F / m.",
      difficulty = "EASY",
      isPastExam = true,
      examYear = "2024",
      examType = "MIDTERM"
    ),
    QuestionEntity(
      id = "Q_PHYS_3",
      courseId = "PHYS_101",
      topicId = "T_PHYS_3",
      questionText = "In an isolated system with no external forces, which of the following quantities is ALWAYS conserved in both elastic and inelastic collisions?",
      optionA = "Kinetic energy only",
      optionB = "Total mechanical energy only",
      optionC = "Linear momentum only",
      optionD = "Both kinetic energy and linear momentum",
      correctAnswer = "C",
      explanationGeneral = "Linear momentum is conserved in all isolated collisions by Newton's Third Law. Kinetic energy is only conserved in perfectly elastic collisions (in inelastic collisions, some is converted to thermal energy or deformation).",
      explanationA = "Incorrect. Kinetic energy is converted to internal energy or sound in inelastic collisions.",
      explanationB = "Incorrect. Mechanical energy changes form when thermal/sound energy is generated.",
      explanationC = "Correct! Conservation of total linear momentum holds universally for all isolated collisions.",
      explanationD = "Incorrect. Kinetic energy is NOT conserved in inelastic collisions.",
      difficulty = "MEDIUM",
      isPastExam = true,
      examYear = "2023",
      examType = "FINAL"
    ),

    // CHEM 101 Questions
    QuestionEntity(
      id = "Q_CHEM_1",
      courseId = "CHEM_101",
      topicId = "T_CHEM_1",
      questionText = "Which set of quantum numbers (n, l, ml, ms) is NOT allowed for an electron in an atom?",
      optionA = "n = 2, l = 1, ml = 0, ms = +1/2",
      optionB = "n = 3, l = 2, ml = -2, ms = -1/2",
      optionC = "n = 2, l = 2, ml = 0, ms = +1/2",
      optionD = "n = 1, l = 0, ml = 0, ms = -1/2",
      correctAnswer = "C",
      explanationGeneral = "The orbital angular momentum quantum number l can only take integer values from 0 to n - 1. When n = 2, the allowed values of l are 0 and 1. l cannot equal 2 (there is no 2d subshell).",
      explanationA = "Incorrect. This represents a valid 2p electron.",
      explanationB = "Incorrect. This represents a valid 3d electron.",
      explanationC = "Correct! For principal quantum number n = 2, l must be less than n (l can only be 0 or 1). l = 2 is invalid.",
      explanationD = "Incorrect. This represents a valid 1s electron.",
      difficulty = "MEDIUM",
      isPastExam = true,
      examYear = "2024",
      examType = "MIDTERM"
    ),
    QuestionEntity(
      id = "Q_CHEM_2",
      courseId = "CHEM_101",
      topicId = "T_CHEM_2",
      questionText = "According to VSEPR theory, what is the molecular geometry of methane (CH4)?",
      optionA = "Square planar",
      optionB = "Tetrahedral",
      optionC = "Trigonal pyramidal",
      optionD = "Bent",
      correctAnswer = "B",
      explanationGeneral = "Carbon has 4 valence electrons forming 4 single bonds with hydrogen and 0 lone pairs (steric number 4). The electron-pair geometry and molecular geometry are both tetrahedral with 109.5° bond angles.",
      explanationA = "Incorrect. Square planar occurs with steric number 6 (e.g., XeF4 with two lone pairs).",
      explanationB = "Correct! With 4 bonding pairs and zero lone pairs, the valence electron pairs minimize repulsion in a tetrahedral geometry.",
      explanationC = "Incorrect. Trigonal pyramidal geometry occurs when there are 3 bonds and 1 lone pair (e.g., NH3).",
      explanationD = "Incorrect. Bent geometry occurs with 2 bonds and lone pairs (e.g., H2O).",
      difficulty = "EASY",
      isPastExam = false
    ),

    // CS 101 Questions
    QuestionEntity(
      id = "Q_CS_1",
      courseId = "CS_101",
      topicId = "T_CS_1",
      questionText = "What is the worst-case time complexity of binary search on a sorted array of size n?",
      optionA = "O(1)",
      optionB = "O(log n)",
      optionC = "O(n)",
      optionD = "O(n log n)",
      correctAnswer = "B",
      explanationGeneral = "Binary search divides the search space in half with every comparison. Halving n times yields log2(n) steps, so the time complexity is O(log n).",
      explanationA = "Incorrect. O(1) is the best-case complexity when the target element happens to be at the exact midpoint on the first check.",
      explanationB = "Correct! Binary search repeatedly halves the active search partition, requiring at most ⌊log2(n)⌋ + 1 comparisons.",
      explanationC = "Incorrect. O(n) is the worst-case time complexity of linear search, not binary search.",
      explanationD = "Incorrect. O(n log n) is typical for optimal comparison sorts such as merge sort, not searching.",
      difficulty = "EASY",
      isPastExam = true,
      examYear = "2024",
      examType = "FINAL"
    ),
    QuestionEntity(
      id = "Q_CS_2",
      courseId = "CS_101",
      topicId = "T_CS_2",
      questionText = "In Python or Java, what is the output of evaluating the expression (True and False or not False)?",
      optionA = "True",
      optionB = "False",
      optionC = "Syntax Error",
      optionD = "Null",
      correctAnswer = "A",
      explanationGeneral = "Operator precedence applies: 'not' takes highest precedence, followed by 'and', followed by 'or'. So not False = True. (True and False) = False. Then False or True = True.",
      explanationA = "Correct! Step 1: not False evaluates to True. Step 2: (True and False) evaluates to False. Step 3: False or True evaluates to True.",
      explanationB = "Incorrect. Because the right side of the 'or' evaluates to True (not False), the entire disjunction evaluates to True.",
      explanationC = "Incorrect. This is a valid boolean expression.",
      explanationD = "Incorrect. Boolean operators always return a boolean boolean primitive value.",
      difficulty = "MEDIUM",
      isPastExam = false
    ),

    // CS 101 - Q3
    QuestionEntity(
      id = "Q_CS_3",
      courseId = "CS_101",
      topicId = "T_CS_3",
      questionText = "What happens if a recursive function does not include a base case?",
      optionA = "It executes in O(1) time.",
      optionB = "It causes infinite recursion resulting in a StackOverflowError.",
      optionC = "The compiler converts it into an iterative loop automatically.",
      optionD = "It returns null immediately.",
      correctAnswer = "B",
      explanationGeneral = "Every recursive call allocates a stack frame in memory. Without a terminating base case, calls continue indefinitely until stack memory is exhausted, throwing a Stack Overflow error.",
      explanationA = "Incorrect. It will not execute in O(1) constant time.",
      explanationB = "Correct! Without a terminating condition (base case), the call stack grows uncontrollably until stack memory is exhausted.",
      explanationC = "Incorrect. Most compilers will not safely vectorize or eliminate non-terminating recursion.",
      explanationD = "Incorrect. Execution cannot gracefully return without reaching a termination clause.",
      difficulty = "EASY",
      isPastExam = true,
      examYear = "2023",
      examType = "MIDTERM"
    ),

    // PHIL 101 Questions
    QuestionEntity(
      id = "Q_PHIL_1",
      courseId = "PHIL_101",
      topicId = "T_PHIL_1",
      questionText = "In formal logic, a deductive argument is said to be 'Sound' if and only if:",
      optionA = "Its conclusion is emotionally compelling.",
      optionB = "It has at least three supporting premises.",
      optionC = "It is valid and all of its premises are true.",
      optionD = "It cannot be refuted by counterexamples.",
      correctAnswer = "C",
      explanationGeneral = "Soundness requires two criteria: (1) the argument structure must be deductively valid (if premises are true, conclusion must follow), and (2) all premises must actually be true in reality.",
      explanationA = "Incorrect. Soundness is an objective structural and factual property, not emotional appeal.",
      explanationB = "Incorrect. The number of premises is irrelevant to logical soundness.",
      explanationC = "Correct! Soundness = Deductive Validity + True Premises. This guarantees a true conclusion.",
      explanationD = "Incorrect. Counterexamples test validity, but soundness additionally requires factual premise truth.",
      difficulty = "MEDIUM",
      isPastExam = true,
      examYear = "2024",
      examType = "FINAL"
    ),
    QuestionEntity(
      id = "Q_PHIL_2",
      courseId = "PHIL_101",
      topicId = "T_PHIL_2",
      questionText = "Attacking an opponent's character or personal background instead of addressing their substantive argument is known as which fallacy?",
      optionA = "Straw Man fallacy",
      optionB = "Ad Hominem fallacy",
      optionC = "Post hoc ergo propter hoc",
      optionD = "False Dilemma",
      correctAnswer = "B",
      explanationGeneral = "Ad Hominem (Latin for 'to the person') occurs when an arguer bypasses the evidence and attacks the speaker's personal traits, motives, or identity instead.",
      explanationA = "Incorrect. Straw Man involves distorting or exaggerating someone's position to make it easier to attack.",
      explanationB = "Correct! Ad Hominem is an informal fallacy of relevance where personal attacks substitute for valid argumentation.",
      explanationC = "Incorrect. Post hoc assumes causation merely because one event followed another chronologically.",
      explanationD = "Incorrect. False Dilemma artificially restricts complex choices to only two extreme options.",
      difficulty = "EASY",
      isPastExam = false
    ),

    // MATH 102 Linear Algebra
    QuestionEntity(
      id = "Q_MATH2_1",
      courseId = "MATH_102",
      topicId = "T_MATH2_1",
      questionText = "A system of n linear equations with n unknowns has a unique solution if and only if the coefficient matrix A:",
      optionA = "Has determinant det(A) = 0",
      optionB = "Has determinant det(A) ≠ 0",
      optionC = "Has all zero diagonal entries",
      optionD = "Is symmetric",
      correctAnswer = "B",
      explanationGeneral = "By the Invertible Matrix Theorem, a square matrix A is invertible (non-singular) if and only if det(A) ≠ 0. Invertibility guarantees a unique solution x = A⁻¹b.",
      explanationA = "Incorrect. If det(A) = 0, the matrix is singular and the system has either zero solutions or infinitely many solutions.",
      explanationB = "Correct! A non-zero determinant guarantees that matrix A is invertible, ensuring exactly one unique solution exists.",
      explanationC = "Incorrect. Diagonal entries can be non-zero or zero as long as the determinant is non-zero after row reduction.",
      explanationD = "Incorrect. Symmetry (A = Aᵀ) is neither necessary nor sufficient for invertibility.",
      difficulty = "MEDIUM",
      isPastExam = true,
      examYear = "2024",
      examType = "MIDTERM"
    ),

    // BIO 101 Questions
    QuestionEntity(
      id = "Q_BIO_1",
      courseId = "BIO_101",
      topicId = "T_BIO_1",
      questionText = "Which cellular organelle is responsible for generating the majority of ATP through oxidative phosphorylation?",
      optionA = "Ribosome",
      optionB = "Endoplasmic Reticulum",
      optionC = "Mitochondrion",
      optionD = "Golgi Apparatus",
      correctAnswer = "C",
      explanationGeneral = "The mitochondrion houses the Krebs cycle in its matrix and the electron transport chain across its inner cristae membrane, generating over 30 ATP molecules per glucose molecule.",
      explanationA = "Incorrect. Ribosomes synthesize proteins from amino acids using mRNA templates.",
      explanationB = "Incorrect. The ER is involved in protein folding (rough ER) and lipid synthesis/detoxification (smooth ER).",
      explanationC = "Correct! The mitochondrion is the metabolic powerhouse conducting oxidative phosphorylation and chemiosmosis.",
      explanationD = "Incorrect. The Golgi apparatus modifies, sorts, and packages macromolecules for secretion.",
      difficulty = "EASY",
      isPastExam = true,
      examYear = "2023",
      examType = "MIDTERM"
    ),

    // ENGR 101 Questions
    QuestionEntity(
      id = "Q_ENGR_1",
      courseId = "ENGR_101",
      topicId = "T_ENGR_1",
      questionText = "For a 2D coplanar rigid body to be in complete static equilibrium, which conditions must simultaneously be satisfied?",
      optionA = "ΣFx = 0 only",
      optionB = "ΣFx = 0 and ΣFy = 0 only",
      optionC = "ΣFx = 0, ΣFy = 0, and ΣM_O = 0 about any point O",
      optionD = "ΣM_O = 0 only",
      correctAnswer = "C",
      explanationGeneral = "Complete 2D equilibrium requires both translational equilibrium in orthogonal axes (ΣFx = 0, ΣFy = 0) and rotational equilibrium against moments (ΣM = 0).",
      explanationA = "Incorrect. This only prevents horizontal translation.",
      explanationB = "Incorrect. Zero net force prevents translation but allows rotational angular acceleration.",
      explanationC = "Correct! Static equilibrium requires no net force in x and y (preventing translation) and no net moment about any axis (preventing rotation).",
      explanationD = "Incorrect. Zero moment alone does not prevent translation.",
      difficulty = "MEDIUM",
      isPastExam = true,
      examYear = "2024",
      examType = "FINAL"
    ),

    // ECON 101 Questions
    QuestionEntity(
      id = "Q_ECON_1",
      courseId = "ECON_101",
      topicId = "T_ECON_1",
      questionText = "When the government imposes a binding price ceiling on apartment rentals below the market equilibrium price, what is the immediate economic result?",
      optionA = "A surplus of rental housing",
      optionB = "A shortage of rental housing",
      optionC = "An increase in landlord investments",
      optionD = "A downward shift in demand",
      correctAnswer = "B",
      explanationGeneral = "A price ceiling set below equilibrium keeps prices artificially low. At this lower price, quantity demanded (Qd) increases while quantity supplied (Qs) decreases, creating a shortage (Qd > Qs).",
      explanationA = "Incorrect. A surplus occurs when price is held above equilibrium (such as a price floor).",
      explanationB = "Correct! Lower prices boost tenant demand while reducing landlord supply, causing excess demand or shortage.",
      explanationC = "Incorrect. Landlords have less financial incentive to maintain or build new rental housing.",
      explanationD = "Incorrect. Demand itself does not shift; the quantity demanded along the curve increases.",
      difficulty = "EASY",
      isPastExam = true,
      examYear = "2024",
      examType = "MIDTERM"
    ),

    // STAT 101 Questions
    QuestionEntity(
      id = "Q_STAT_1",
      courseId = "STAT_101",
      topicId = "T_STAT_1",
      questionText = "If event A and event B are mutually exclusive (disjoint) with P(A) = 0.4 and P(B) = 0.3, what is P(A and B)?",
      optionA = "0.12",
      optionB = "0.70",
      optionC = "0.0",
      optionD = "0.10",
      correctAnswer = "C",
      explanationGeneral = "Mutually exclusive events cannot occur simultaneously by definition. Therefore, their intersection has zero probability: P(A ∩ B) = 0.",
      explanationA = "Incorrect. 0.12 is P(A) * P(B), which applies if the events are independent, NOT mutually exclusive.",
      explanationB = "Incorrect. 0.70 is P(A or B) = P(A) + P(B).",
      explanationC = "Correct! Mutually exclusive events have an empty intersection: P(A and B) = 0.",
      explanationD = "Incorrect. Mutual exclusivity means joint occurrence is impossible.",
      difficulty = "EASY",
      isPastExam = true,
      examYear = "2024",
      examType = "MIDTERM"
    ),

    // CIV 101 Questions
    QuestionEntity(
      id = "Q_CIV_1",
      courseId = "CIV_101",
      topicId = "T_CIV_1",
      questionText = "The principle of 'Separation of Powers' in democratic governance distributes authority among which three branches?",
      optionA = "Military, Police, and Civil Service",
      optionB = "Legislative, Executive, and Judicial",
      optionC = "Local Government, State Government, and Federal Government",
      optionD = "Media, Academia, and Religious Bodies",
      correctAnswer = "B",
      explanationGeneral = "Formulated by Montesquieu, separation of powers divides governance into Legislative (law-making), Executive (law enforcement), and Judicial (law interpretation) to prevent tyranny.",
      explanationA = "Incorrect. The military and police are enforcement arms of the executive branch.",
      explanationB = "Correct! The three classic branches are Legislative (Parliament/Congress), Executive (President/Prime Minister), and Judicial (Courts).",
      explanationC = "Incorrect. This describes federalism (levels of government), not separation of powers.",
      explanationD = "Incorrect. While media is often called the fourth estate, it is not a constitutional branch of government.",
      difficulty = "EASY",
      isPastExam = true,
      examYear = "2023",
      examType = "FINAL"
    )
  )

  fun getNotes(): List<NoteEntity> = listOf(
    NoteEntity(
      id = "NOTE_MATH_1",
      courseId = "MATH_101",
      topicId = "T_MATH_1",
      title = "Essential Limit Laws & L'Hopital's Rule",
      summary = "Comprehensive summary of limit evaluation techniques, squeeze theorem, and indeterminate forms.",
      contentMarkdown = """
# Calculus I: Limits & Continuity Quick Revision

## 1. Core Limit Rules
For functions where lim (x -> a) f(x) = L and lim (x -> a) g(x) = M:
• Sum/Difference: lim [f(x) ± g(x)] = L ± M
• Product: lim [f(x) · g(x)] = L · M
• Quotient: lim [f(x) / g(x)] = L / M (provided M ≠ 0)

## 2. Indeterminate Forms & L'Hopital's Rule
When evaluating lim (x -> a) [f(x) / g(x)] yields 0/0 or ±∞/±∞:
  lim (x -> a) [f(x) / g(x)] = lim (x -> a) [f'(x) / g'(x)]

*Warning:* Differentiate numerator and denominator separately; do NOT use the quotient rule!

## 3. The Squeeze (Sandwich) Theorem
If g(x) ≤ f(x) ≤ h(x) near a, and:
  lim (x -> a) g(x) = lim (x -> a) h(x) = L
Then lim (x -> a) f(x) = L must hold.
      """.trimIndent(),
      keyFormulas = "lim (f/g) = lim (f'/g') [L'Hopital]; sin(x)/x -> 1 as x -> 0",
      readTimeMinutes = 4
    ),
    NoteEntity(
      id = "NOTE_MATH_2",
      courseId = "MATH_101",
      topicId = "T_MATH_2",
      title = "Differentiation Rules Mastery",
      summary = "Product rule, quotient rule, chain rule, and derivatives of trigonometric and exponential functions.",
      contentMarkdown = """
# Calculus I: Differentiation Rules

## Key Derivative Formulas
1. Power Rule: d/dx [x^n] = n · x^(n-1)
2. Product Rule: d/dx [u · v] = u'v + uv'
3. Quotient Rule: d/dx [u / v] = (u'v - uv') / v²
4. Chain Rule: d/dx [f(g(x))] = f'(g(x)) · g'(x)

## Trigonometric Derivatives
• d/dx [sin x] = cos x
• d/dx [cos x] = -sin x
• d/dx [tan x] = sec² x
• d/dx [sec x] = sec x · tan x
      """.trimIndent(),
      keyFormulas = "(uv)' = u'v + uv'; (u/v)' = (u'v - uv')/v²; d/dx[sin x] = cos x",
      readTimeMinutes = 5
    ),
    NoteEntity(
      id = "NOTE_PHYS_1",
      courseId = "PHYS_101",
      topicId = "T_PHYS_1",
      title = "Kinematics Equations (SUVAT)",
      summary = "Constant acceleration motion equations in 1D and 2D projectile breakdown.",
      contentMarkdown = """
# Physics I: 1D & 2D Kinematics

## 1. Fundamental SUVAT Equations (Constant Acceleration)
• v = v0 + at
• Δx = v0·t + (1/2)a·t²
• v² = v0² + 2a·Δx
• Δx = ((v0 + v) / 2) · t

## 2. Projectile Motion Components
For launch angle θ and initial speed v0:
• Horizontal (ax = 0): vx = v0·cos(θ) [constant], x(t) = v0·cos(θ)·t
• Vertical (ay = -g): vy(t) = v0·sin(θ) - gt
• Time to peak: t_peak = (v0·sin(θ)) / g
• Max Height: H = (v0²·sin²(θ)) / (2g)
      """.trimIndent(),
      keyFormulas = "v = v0 + at; Δx = v0·t + 0.5a·t²; v² = v0² + 2a·Δx",
      readTimeMinutes = 4
    ),
    NoteEntity(
      id = "NOTE_CHEM_1",
      courseId = "CHEM_101",
      topicId = "T_CHEM_1",
      title = "Chemical Bonding & Molecular Shapes",
      summary = "Lewis electron dot structures, VSEPR geometries, bond angles, and hybridization.",
      contentMarkdown = """
# Chemistry: VSEPR & Bonding Summary

## Electron Domains & Geometries
• Steric 2 (2 bonds, 0 lone pairs): Linear (180°, sp)
• Steric 3 (3 bonds, 0 lone pairs): Trigonal Planar (120°, sp²)
• Steric 3 (2 bonds, 1 lone pair): Bent (<120°, sp²)
• Steric 4 (4 bonds, 0 lone pairs): Tetrahedral (109.5°, sp³)
• Steric 4 (3 bonds, 1 lone pair): Trigonal Pyramidal (107°, sp³)
• Steric 4 (2 bonds, 2 lone pairs): Bent (104.5°, sp³)

## Electronegativity Differences
• ΔEN < 0.4: Nonpolar Covalent
• 0.4 ≤ ΔEN < 1.7: Polar Covalent
• ΔEN ≥ 1.7: Ionic
      """.trimIndent(),
      keyFormulas = "Steric Number = Bonded Atoms + Lone Pairs; Formal Charge = Valence - Nonbonding - 0.5(Bonding)",
      readTimeMinutes = 6
    ),
    NoteEntity(
      id = "NOTE_CS_1",
      courseId = "CS_101",
      topicId = "T_CS_1",
      title = "Asymptotic Notation & Time Complexity",
      summary = "Big-O, Big-Omega, Big-Theta, and algorithm complexity comparison guide.",
      contentMarkdown = """
# Computer Science: Big-O Complexity Chart

## Growth Hierarchy (Fastest to Slowest)
O(1) < O(log n) < O(n) < O(n log n) < O(n²) < O(2^n) < O(n!)

## Common Data Structure Operations
• Array Lookup: O(1) by index, O(n) by value search
• Linked List Insertion (Head): O(1)
• Binary Search: O(log n) [requires sorted array]
• Merge Sort / Quick Sort (Average): O(n log n)
• Bubble Sort / Insertion Sort: O(n²) worst case
      """.trimIndent(),
      keyFormulas = "O(1) < O(log n) < O(n) < O(n log n) < O(n²)",
      readTimeMinutes = 4
    )
  )

  fun getPastExams(): List<PastExamEntity> = listOf(
    PastExamEntity(
      id = "EXAM_MATH_2024_MID_AAU",
      courseId = "MATH_101",
      university = "Addis Ababa University (AAU)",
      academicYear = "2024/2025",
      semester = "Semester I",
      examType = "MIDTERM",
      title = "AAU MATH 101 Midterm Examination 2024",
      instructions = "Time allowed: 90 Minutes. Answer all digitized questions. Calculators allowed. Official verified solutions included.",
      totalQuestions = 4,
      durationMinutes = 90,
      isDigitized = true,
      isVerified = true,
      documentSummary = "Verified by AAU Department of Mathematics. Covers Limits, Continuity, Derivative Rules, and Tangent Lines."
    ),
    PastExamEntity(
      id = "EXAM_MATH_2023_FIN_AAU",
      courseId = "MATH_101",
      university = "Addis Ababa University (AAU)",
      academicYear = "2023/2024",
      semester = "Semester II",
      examType = "FINAL",
      title = "AAU MATH 101 Final Comprehensive Examination 2023",
      instructions = "Comprehensive end-of-semester paper covering differential and integral calculus.",
      totalQuestions = 4,
      durationMinutes = 120,
      isDigitized = true,
      isVerified = true,
      documentSummary = "Full semester paper. Verified answer key provided by AAU Examination Board."
    ),
    PastExamEntity(
      id = "EXAM_PHYS_2024_MID_AAU",
      courseId = "PHYS_101",
      university = "Addis Ababa University (AAU)",
      academicYear = "2024/2025",
      semester = "Semester I",
      examType = "MIDTERM",
      title = "AAU PHYS 101 Midterm Examination 2024",
      instructions = "Mechanics and Kinematics official paper. Gravitational acceleration g = 9.8 m/s².",
      totalQuestions = 3,
      durationMinutes = 75,
      isDigitized = true,
      isVerified = true,
      documentSummary = "Official AAU Freshman Engineering & Physics joint midterm exam paper."
    ),
    PastExamEntity(
      id = "EXAM_CS_2024_FIN_AAU",
      courseId = "CS_101",
      university = "Addis Ababa University (AAU)",
      academicYear = "2024/2025",
      semester = "Semester I",
      examType = "FINAL",
      title = "AAU CS 101 Final Theory & Logic Examination 2024",
      instructions = "Includes questions on algorithm complexity, recursion, control flow, and data types.",
      totalQuestions = 3,
      durationMinutes = 90,
      isDigitized = true,
      isVerified = true,
      documentSummary = "Digitized multiple-choice section with verified grading rubric."
    ),
    PastExamEntity(
      id = "EXAM_CHEM_2024_DOC_AAU",
      courseId = "CHEM_101",
      university = "Addis Ababa University (AAU)",
      academicYear = "2024/2025",
      semester = "Semester I",
      examType = "FINAL",
      title = "AAU CHEM 101 Official Past Paper Document 2024",
      instructions = "Official examination document provided by AAU Department of Chemistry for revision study.",
      totalQuestions = 25,
      durationMinutes = 120,
      isDigitized = false,
      isVerified = true,
      documentSummary = "Certified past exam script. Includes Periodic Table appendix and standard reduction potential chart."
    ),
    PastExamEntity(
      id = "EXAM_MATH_2024_FIN_ASTU",
      courseId = "MATH_101",
      university = "ASTU (Adama)",
      academicYear = "2024/2025",
      semester = "Semester I",
      examType = "FINAL",
      title = "ASTU Calculus I Applied Engineering Final 2024",
      instructions = "Applied mathematics for freshman engineers. Detailed integrals, optimization, and vectors.",
      totalQuestions = 4,
      durationMinutes = 120,
      isDigitized = true,
      isVerified = true,
      documentSummary = "Official ASTU Applied Science & Engineering Examination Board archive."
    ),
    PastExamEntity(
      id = "EXAM_PHYS_2024_MID_ASTU",
      courseId = "PHYS_101",
      university = "ASTU (Adama)",
      academicYear = "2024/2025",
      semester = "Semester I",
      examType = "MIDTERM",
      title = "ASTU General Physics Midterm Exam 2024",
      instructions = "Applied physics focusing on 2D motion, friction, work-energy theorem, and rotational dynamics.",
      totalQuestions = 3,
      durationMinutes = 80,
      isDigitized = true,
      isVerified = true,
      documentSummary = "ASTU School of Applied Natural Sciences verified midterm."
    ),
    PastExamEntity(
      id = "EXAM_CS_2023_FIN_ASTU",
      courseId = "CS_101",
      university = "ASTU (Adama)",
      academicYear = "2023/2024",
      semester = "Semester II",
      examType = "FINAL",
      title = "ASTU Programming Fundamentals Comprehensive Final 2023",
      instructions = "Programming logic, conditional statements, loops, functions, and memory addresses.",
      totalQuestions = 3,
      durationMinutes = 90,
      isDigitized = true,
      isVerified = true,
      documentSummary = "ASTU Department of Computer Science & Engineering past paper."
    ),
    PastExamEntity(
      id = "EXAM_MATH_2024_MID_AASTU",
      courseId = "MATH_101",
      university = "AASTU",
      academicYear = "2024/2025",
      semester = "Semester I",
      examType = "MIDTERM",
      title = "AASTU Calculus I Midterm Assessment 2024",
      instructions = "Standard freshman STEM paper on differential calculus and curve sketching.",
      totalQuestions = 4,
      durationMinutes = 90,
      isDigitized = true,
      isVerified = true,
      documentSummary = "AASTU Center of Excellence in Engineering freshman exam."
    ),
    PastExamEntity(
      id = "EXAM_PHYS_2024_FIN_AASTU",
      courseId = "PHYS_101",
      university = "AASTU",
      academicYear = "2024/2025",
      semester = "Semester I",
      examType = "FINAL",
      title = "AASTU General Physics I Final Exam 2024",
      instructions = "End-of-term comprehensive examination including harmonic motion and waves.",
      totalQuestions = 3,
      durationMinutes = 110,
      isDigitized = true,
      isVerified = true,
      documentSummary = "AASTU College of Applied Sciences verified final paper."
    ),
    PastExamEntity(
      id = "EXAM_CHEM_2024_FIN_HWU",
      courseId = "CHEM_101",
      university = "Hawassa Univ.",
      academicYear = "2024/2025",
      semester = "Semester I",
      examType = "FINAL",
      title = "Hawassa Univ. General Chemistry Final 2024",
      instructions = "Atomic structure, chemical bonding, gas laws, and equilibrium problems.",
      totalQuestions = 25,
      durationMinutes = 100,
      isDigitized = false,
      isVerified = true,
      documentSummary = "Hawassa University Natural & Computational Sciences past exam paper."
    ),
    PastExamEntity(
      id = "EXAM_MATH_2023_MID_HWU",
      courseId = "MATH_101",
      university = "Hawassa Univ.",
      academicYear = "2023/2024",
      semester = "Semester I",
      examType = "MIDTERM",
      title = "Hawassa Univ. Calculus I Midterm Exam 2023",
      instructions = "Calculus concepts covering limit theorems, derivatives, and related rates.",
      totalQuestions = 4,
      durationMinutes = 85,
      isDigitized = true,
      isVerified = true,
      documentSummary = "Verified midterm with department answer guide."
    ),
    PastExamEntity(
      id = "EXAM_PHYS_2024_FIN_BDU",
      courseId = "PHYS_101",
      university = "Bahir Dar Univ.",
      academicYear = "2024/2025",
      semester = "Semester I",
      examType = "FINAL",
      title = "Bahir Dar Univ. General Physics Final 2024",
      instructions = "Comprehensive paper covering mechanics, thermodynamics, and simple harmonic oscillators.",
      totalQuestions = 3,
      durationMinutes = 120,
      isDigitized = true,
      isVerified = true,
      documentSummary = "Bahir Dar Institute of Technology (BiT) verified exam script."
    ),
    PastExamEntity(
      id = "EXAM_CS_2024_MID_BDU",
      courseId = "CS_101",
      university = "Bahir Dar Univ.",
      academicYear = "2024/2025",
      semester = "Semester I",
      examType = "MIDTERM",
      title = "Bahir Dar Univ. Intro to Computing Midterm 2024",
      instructions = "Binary arithmetic, algorithm flowcharts, and pseudo-code tracing.",
      totalQuestions = 3,
      durationMinutes = 75,
      isDigitized = true,
      isVerified = true,
      documentSummary = "BiT Computing faculty official midterm questions."
    ),
    PastExamEntity(
      id = "EXAM_MATH_2024_FIN_JU",
      courseId = "MATH_101",
      university = "Jimma Univ.",
      academicYear = "2024/2025",
      semester = "Semester I",
      examType = "FINAL",
      title = "Jimma Univ. Freshman Calculus I Final 2024",
      instructions = "Integration techniques, fundamental theorem of calculus, and area between curves.",
      totalQuestions = 4,
      durationMinutes = 120,
      isDigitized = true,
      isVerified = true,
      documentSummary = "Jimma Institute of Technology (JiT) official exam."
    ),
    PastExamEntity(
      id = "EXAM_CHEM_2024_MID_JU",
      courseId = "CHEM_101",
      university = "Jimma Univ.",
      academicYear = "2024/2025",
      semester = "Semester I",
      examType = "MIDTERM",
      title = "Jimma Univ. General Chemistry Midterm 2024",
      instructions = "Stoichiometry, redox reactions, solution concentrations, and thermochemistry.",
      totalQuestions = 20,
      durationMinutes = 90,
      isDigitized = false,
      isVerified = true,
      documentSummary = "Jimma University College of Natural Sciences past test paper."
    )
  )

  fun getAnnouncements(): List<AnnouncementEntity> = listOf(
    AnnouncementEntity(
      id = "ANN_1",
      title = "Freshman Midterm Examinations Schedule",
      message = "The 2026/2027 Semester I Midterm Examination timetable is now released. Calculus I and General Physics are scheduled for Monday 9:00 AM.",
      category = "EXAM",
      targetCourseId = "MATH_101",
      targetCourseTitle = "Calculus I",
      isUrgent = true
    ),
    AnnouncementEntity(
      id = "ANN_2",
      title = "New Revision Notes Uploaded: CS 101 & Linear Algebra",
      message = "Concise summary notes for Big-O Asymptotics and Gaussian Elimination have been published in the revision center.",
      category = "NOTE",
      targetCourseId = "CS_101",
      targetCourseTitle = "Intro to Computer Science",
      isUrgent = false
    ),
    AnnouncementEntity(
      id = "ANN_3",
      title = "Academic Tutoring Clinics Open 24/7",
      message = "Freshman peer tutoring and faculty clinic sessions are available in the science learning commons. Practice quizzes before attending!",
      category = "ALERT",
      targetCourseId = null,
      targetCourseTitle = null,
      isUrgent = false
    )
  )

  fun getAccessCodes(): List<AccessCodeEntity> {
    val now = System.currentTimeMillis()
    val validWindow = 365L * 24 * 60 * 60 * 1000L
    return listOf(
      AccessCodeEntity(
        code = "789123",
        createdAt = now,
        expiresAt = now + validWindow,
        createdByAdminId = "admin_1",
        isRedeemed = false,
        note = "Permanent Starter Key (Calculus & STEM)"
      ),
      AccessCodeEntity(
        code = "892104",
        createdAt = now,
        expiresAt = now + validWindow,
        createdByAdminId = "admin_1",
        isRedeemed = false,
        note = "Permanent 6-Digit Numeric Key"
      ),
      AccessCodeEntity(
        code = "654321",
        createdAt = now,
        expiresAt = now + validWindow,
        createdByAdminId = "admin_1",
        isRedeemed = false,
        note = "Freshman Scholar Key"
      ),
      AccessCodeEntity(
        code = "991244",
        createdAt = now,
        expiresAt = now + validWindow,
        createdByAdminId = "admin_1",
        isRedeemed = false,
        note = "University Student Pass"
      )
    )
  }
}

