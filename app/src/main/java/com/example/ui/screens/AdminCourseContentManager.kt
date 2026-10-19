package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.model.CourseEntity
import com.example.data.local.model.NoteEntity
import com.example.data.local.model.PastExamEntity
import com.example.data.local.model.QuestionEntity
import com.example.ui.theme.AcademicBluePrimary
import com.example.ui.theme.AcademicCobalt
import com.example.ui.theme.AcademicGold
import com.example.ui.theme.SuccessGreen
import java.util.UUID

/**
 * Universal Course Selector Dropdown/Bar for Admin Panel.
 * Allows administrators to switch between all courses (including newly added courses) instantly.
 */
@Composable
fun AdminCourseSelector(
  courses: List<CourseEntity>,
  selectedCourseId: String,
  onCourseSelected: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var isExpanded by remember { mutableStateOf(false) }
  val currentCourse = courses.firstOrNull { it.id == selectedCourseId } ?: courses.firstOrNull()

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(34.dp)
              .clip(CircleShape)
              .background(AcademicBluePrimary),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.School, contentDescription = null, tint = AcademicGold, modifier = Modifier.size(18.dp))
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text("TARGET COURSE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AcademicCobalt)
            Text(
              text = if (currentCourse != null) "${currentCourse.code} • ${currentCourse.title}" else "Select a Course",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        Box {
          OutlinedButton(
            onClick = { isExpanded = true },
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Text("Switch Course", style = MaterialTheme.typography.labelMedium)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
          }

          DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
          ) {
            courses.forEach { course ->
              DropdownMenuItem(
                text = {
                  Column {
                    Text("${course.code} - ${course.title}", fontWeight = FontWeight.Bold)
                    Text("${course.category} • ${course.totalQuestions} Questions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                },
                onClick = {
                  onCourseSelected(course.id)
                  isExpanded = false
                },
                leadingIcon = {
                  if (course.id == selectedCourseId) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen)
                  }
                }
              )
            }
          }
        }
      }

      // Horizontal quick-tap chips for the courses
      if (courses.isNotEmpty()) {
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(courses) { course ->
            val isSelected = course.id == selectedCourseId
            Surface(
              onClick = { onCourseSelected(course.id) },
              shape = RoundedCornerShape(8.dp),
              color = if (isSelected) AcademicBluePrimary else MaterialTheme.colorScheme.surface,
              border = BorderStroke(1.dp, if (isSelected) AcademicBluePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
              Text(
                text = course.code,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Dedicated Admin Tab to Add and Manage Past Year Exam Questions.
 */
@Composable
fun AdminPastExamTab(
  courses: List<CourseEntity>,
  questions: List<QuestionEntity>,
  initialCourseId: String = "",
  onSaveQuestion: (QuestionEntity) -> Unit,
  onSavePastExam: (PastExamEntity) -> Unit,
  onDeleteQuestion: (String) -> Unit
) {
  var selectedCourseId by remember(initialCourseId, courses) {
    mutableStateOf(if (initialCourseId.isNotBlank()) initialCourseId else courses.firstOrNull()?.id ?: "")
  }

  var examYear by remember { mutableStateOf("2016 E.C. (2024)") }
  var semester by remember { mutableStateOf("Semester 1") }
  var examType by remember { mutableStateOf("FINAL") }
  var examTitle by remember { mutableStateOf("Freshman Final Examination with Official Solutions") }

  var questionText by remember { mutableStateOf("") }
  var optA by remember { mutableStateOf("") }
  var optB by remember { mutableStateOf("") }
  var optC by remember { mutableStateOf("") }
  var optD by remember { mutableStateOf("") }
  var correctChoice by remember { mutableStateOf("A") }
  var generalExplanation by remember { mutableStateOf("") }
  var expA by remember { mutableStateOf("") }
  var expB by remember { mutableStateOf("") }
  var expC by remember { mutableStateOf("") }
  var expD by remember { mutableStateOf("") }

  var editingQuestion by remember { mutableStateOf<QuestionEntity?>(null) }
  var showSuccessMsg by remember { mutableStateOf(false) }

  val coursePastQuestions = questions.filter { it.courseId == selectedCourseId && it.isPastExam }
  val selectedCourse = courses.firstOrNull { it.id == selectedCourseId }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Text(
        text = "Add Past Year Exam Question",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
      Text(
        text = "Add authenticated Ethiopian university past exam questions, solutions, and year references.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    // 1. Course Selector
    item {
      AdminCourseSelector(
        courses = courses,
        selectedCourseId = selectedCourseId,
        onCourseSelected = { selectedCourseId = it }
      )
    }

    // 2. Exam Metadata (Year, Semester, Type)
    item {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Exam Period & Classification", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = examYear,
              onValueChange = { examYear = it },
              label = { Text("Academic Year") },
              modifier = Modifier.weight(1f),
              singleLine = true
            )
            OutlinedTextField(
              value = semester,
              onValueChange = { semester = it },
              label = { Text("Semester") },
              modifier = Modifier.weight(1f),
              singleLine = true
            )
          }

          // Year Quick Chips
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("2016 E.C. (2024)", "2015 E.C.", "2014 E.C.", "2023", "2022").forEach { yr ->
              Surface(
                onClick = { examYear = yr },
                shape = RoundedCornerShape(6.dp),
                color = if (examYear == yr) AcademicCobalt.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, if (examYear == yr) AcademicCobalt else Color.Transparent)
              ) {
                Text(
                  text = yr,
                  style = MaterialTheme.typography.labelSmall,
                  color = if (examYear == yr) AcademicCobalt else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Exam Type:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            listOf("MIDTERM", "FINAL", "NATIONAL").forEach { type ->
              Surface(
                onClick = { examType = type },
                shape = RoundedCornerShape(6.dp),
                color = if (examType == type) AcademicBluePrimary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.clickable { examType = type }
              ) {
                Text(
                  text = type,
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = if (examType == type) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
              }
            }
          }

          OutlinedTextField(
            value = examTitle,
            onValueChange = { examTitle = it },
            label = { Text("Exam Paper Title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
        }
      }
    }

    // 3. Question Content
    item {
      OutlinedTextField(
        value = questionText,
        onValueChange = { questionText = it },
        label = { Text("Question Statement") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3
      )
    }

    item { OutlinedTextField(value = optA, onValueChange = { optA = it }, label = { Text("Choice (A)") }, modifier = Modifier.fillMaxWidth()) }
    item { OutlinedTextField(value = optB, onValueChange = { optB = it }, label = { Text("Choice (B)") }, modifier = Modifier.fillMaxWidth()) }
    item { OutlinedTextField(value = optC, onValueChange = { optC = it }, label = { Text("Choice (C)") }, modifier = Modifier.fillMaxWidth()) }
    item { OutlinedTextField(value = optD, onValueChange = { optD = it }, label = { Text("Choice (D)") }, modifier = Modifier.fillMaxWidth()) }

    // Correct choice selector
    item {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Correct Answer:", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(10.dp))
        listOf("A", "B", "C", "D").forEach { choice ->
          Surface(
            modifier = Modifier
              .padding(horizontal = 4.dp)
              .clickable { correctChoice = choice },
            color = if (correctChoice == choice) SuccessGreen else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp)
          ) {
            Text(
              text = choice,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
              color = if (correctChoice == choice) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }

    item {
      OutlinedTextField(
        value = generalExplanation,
        onValueChange = { generalExplanation = it },
        label = { Text("Step-by-Step Solution / Official Rationale") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3
      )
    }

    item { OutlinedTextField(value = expA, onValueChange = { expA = it }, label = { Text("Explanation for A (Optional)") }, modifier = Modifier.fillMaxWidth()) }
    item { OutlinedTextField(value = expB, onValueChange = { expB = it }, label = { Text("Explanation for B (Optional)") }, modifier = Modifier.fillMaxWidth()) }
    item { OutlinedTextField(value = expC, onValueChange = { expC = it }, label = { Text("Explanation for C (Optional)") }, modifier = Modifier.fillMaxWidth()) }
    item { OutlinedTextField(value = expD, onValueChange = { expD = it }, label = { Text("Explanation for D (Optional)") }, modifier = Modifier.fillMaxWidth()) }

    // Save button
    item {
      Button(
        onClick = {
          if (questionText.isNotBlank() && optA.isNotBlank() && optB.isNotBlank()) {
            val qId = "PQ_" + UUID.randomUUID().toString().take(6)
            val newQ = QuestionEntity(
              id = qId,
              courseId = selectedCourseId,
              topicId = "T_EXAM",
              questionText = questionText.trim(),
              optionA = optA.trim(),
              optionB = optB.trim(),
              optionC = optC.trim(),
              optionD = optD.trim(),
              correctAnswer = correctChoice,
              explanationGeneral = generalExplanation.trim().ifBlank { "Choice ($correctChoice) is verified correct based on curriculum standards." },
              explanationA = expA.trim().ifBlank { if (correctChoice == "A") "Correct solution." else "Incorrect choice." },
              explanationB = expB.trim().ifBlank { if (correctChoice == "B") "Correct solution." else "Incorrect choice." },
              explanationC = expC.trim().ifBlank { if (correctChoice == "C") "Correct solution." else "Incorrect choice." },
              explanationD = expD.trim().ifBlank { if (correctChoice == "D") "Correct solution." else "Incorrect choice." },
              difficulty = "MEDIUM",
              isPastExam = true,
              examYear = examYear.trim(),
              examType = examType.trim(),
              isVerified = true,
              isPublished = true
            )
            onSaveQuestion(newQ)

            // Also ensure corresponding PastExamEntity exists
            val examId = "EXAM_${selectedCourseId}_${examYear.take(4)}_${examType}"
            val examEntity = PastExamEntity(
              id = examId,
              courseId = selectedCourseId,
              academicYear = examYear.trim(),
              semester = semester.trim(),
              examType = examType.trim(),
              title = examTitle.trim().ifBlank { "${selectedCourse?.code ?: "Course"} $examType Examination $examYear" },
              instructions = "All questions are multiple choice. Choose the single best answer and review official step-by-step reasoning.",
              totalQuestions = coursePastQuestions.size + 1,
              durationMinutes = 60,
              isDigitized = true,
              isVerified = true,
              documentSummary = "Official Ethiopian University freshman past exam questions with detailed mathematical and conceptual step-by-step solutions.",
              isPublished = true
            )
            onSavePastExam(examEntity)

            // Clear inputs
            questionText = ""
            optA = ""
            optB = ""
            optC = ""
            optD = ""
            generalExplanation = ""
            expA = ""
            expB = ""
            expC = ""
            expD = ""
            showSuccessMsg = true
          }
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AcademicBluePrimary)
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Save & Publish Past Year Question", fontWeight = FontWeight.Bold)
      }
    }

    // 4. Existing Past Year Questions for Course
    item {
      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider()
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Existing Past Year Questions (${coursePastQuestions.size})",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )
        Text(
          text = selectedCourse?.code ?: "",
          style = MaterialTheme.typography.labelSmall,
          color = AcademicCobalt
        )
      }
    }

    if (coursePastQuestions.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "No past year questions added yet for ${selectedCourse?.title ?: "this course"}. Use the form above to add the first past exam question.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
          )
        }
      }
    } else {
      items(coursePastQuestions) { q ->
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  color = AcademicCobalt.copy(alpha = 0.15f),
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Text(
                    text = "${q.examYear ?: "Past Exam"} • ${q.examType ?: "FINAL"}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = AcademicCobalt,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                  color = SuccessGreen.copy(alpha = 0.15f),
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Text(
                    text = "Answer: (${q.correctAnswer})",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = SuccessGreen,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }

              Row {
                IconButton(onClick = { editingQuestion = q }) {
                  Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { onDeleteQuestion(q.id) }) {
                  Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
              }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(text = q.questionText, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "A) ${q.optionA}   B) ${q.optionB}\nC) ${q.optionC}   D) ${q.optionD}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (q.explanationGeneral.isNotBlank()) {
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Solution: ${q.explanationGeneral}",
                style = MaterialTheme.typography.labelSmall,
                color = AcademicCobalt
              )
            }
          }
        }
      }
    }
  }

  // Edit Question Dialog
  if (editingQuestion != null) {
    EditQuestionDialog(
      question = editingQuestion!!,
      onDismiss = { editingQuestion = null },
      onSave = { updatedQ ->
        onSaveQuestion(updatedQ)
        editingQuestion = null
      }
    )
  }
}

/**
 * Manage Course Content Tab:
 * Customize, edit, and delete questions and notes inside any course.
 */
@Composable
fun AdminManageContentTab(
  courses: List<CourseEntity>,
  questions: List<QuestionEntity>,
  notes: List<NoteEntity>,
  initialCourseId: String = "",
  onSaveQuestion: (QuestionEntity) -> Unit,
  onDeleteQuestion: (String) -> Unit,
  onSaveNote: (NoteEntity) -> Unit,
  onDeleteNote: (String) -> Unit,
  onSwitchToAddQuestion: (String) -> Unit,
  onSwitchToAddNote: (String) -> Unit
) {
  var selectedCourseId by remember(initialCourseId, courses) {
    mutableStateOf(if (initialCourseId.isNotBlank()) initialCourseId else courses.firstOrNull()?.id ?: "")
  }

  var selectedSubTab by remember { mutableIntStateOf(0) }
  var editingQuestion by remember { mutableStateOf<QuestionEntity?>(null) }
  var editingNote by remember { mutableStateOf<NoteEntity?>(null) }

  val courseQuestions = questions.filter { it.courseId == selectedCourseId }
  val courseNotes = notes.filter { it.courseId == selectedCourseId }
  val currentCourse = courses.firstOrNull { it.id == selectedCourseId }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Text(
        text = "Customize Course Questions & Notes",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
      Text(
        text = "Select any newly added course to view, edit, or customize its questions and lecture notes.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    // Course Selector
    item {
      AdminCourseSelector(
        courses = courses,
        selectedCourseId = selectedCourseId,
        onCourseSelected = { selectedCourseId = it }
      )
    }

    // Quick Actions Bar for this course
    item {
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Button(
            onClick = { onSwitchToAddQuestion(selectedCourseId) },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = AcademicBluePrimary)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("+ Add Question", style = MaterialTheme.typography.labelMedium)
          }

          OutlinedButton(
            onClick = { onSwitchToAddNote(selectedCourseId) },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("+ Add Note", style = MaterialTheme.typography.labelMedium)
          }
        }
      }
    }

    // Sub Tabs: Questions vs Notes
    item {
      TabRow(selectedTabIndex = selectedSubTab) {
        Tab(
          selected = selectedSubTab == 0,
          onClick = { selectedSubTab = 0 },
          text = { Text("Questions (${courseQuestions.size})", fontWeight = FontWeight.Bold) }
        )
        Tab(
          selected = selectedSubTab == 1,
          onClick = { selectedSubTab = 1 },
          text = { Text("Revision Notes (${courseNotes.size})", fontWeight = FontWeight.Bold) }
        )
      }
    }

    // Questions List
    if (selectedSubTab == 0) {
      if (courseQuestions.isEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "No questions found in ${currentCourse?.code ?: "this course"}.",
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Click '+ Add Question' above to add the first question to this course.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      } else {
        items(courseQuestions) { q ->
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Surface(
                    color = if (q.isPastExam) AcademicCobalt.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                  ) {
                    Text(
                      text = if (q.isPastExam) "PAST EXAM (${q.examYear ?: ""})" else "PRACTICE Q",
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                      color = if (q.isPastExam) AcademicCobalt else MaterialTheme.colorScheme.primary,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                  Spacer(modifier = Modifier.width(6.dp))
                  Surface(
                    color = SuccessGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                  ) {
                    Text(
                      text = "Correct: (${q.correctAnswer})",
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                      color = SuccessGreen,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }

                Row {
                  IconButton(onClick = { editingQuestion = q }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Question", tint = MaterialTheme.colorScheme.primary)
                  }
                  IconButton(onClick = { onDeleteQuestion(q.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                  }
                }
              }

              Spacer(modifier = Modifier.height(6.dp))
              Text(text = q.questionText, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "A) ${q.optionA}\nB) ${q.optionB}\nC) ${q.optionC}\nD) ${q.optionD}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              if (q.explanationGeneral.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Explanation: ${q.explanationGeneral}",
                  style = MaterialTheme.typography.bodySmall,
                  color = AcademicCobalt
                )
              }
            }
          }
        }
      }
    } else {
      // Notes List
      if (courseNotes.isEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "No notes created yet for ${currentCourse?.code ?: "this course"}.",
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Click '+ Add Note' above to create revision summaries and formulas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      } else {
        items(courseNotes) { note ->
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(text = note.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Row {
                  IconButton(onClick = { editingNote = note }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Note", tint = MaterialTheme.colorScheme.primary)
                  }
                  IconButton(onClick = { onDeleteNote(note.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                  }
                }
              }

              if (note.summary.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = note.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              if (note.keyFormulas.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                  color = AcademicGold.copy(alpha = 0.15f),
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Text(
                    text = "Key Formulas: ${note.keyFormulas}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = AcademicBluePrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  // Dialogs
  if (editingQuestion != null) {
    EditQuestionDialog(
      question = editingQuestion!!,
      onDismiss = { editingQuestion = null },
      onSave = { updatedQ ->
        onSaveQuestion(updatedQ)
        editingQuestion = null
      }
    )
  }

  if (editingNote != null) {
    EditNoteDialog(
      note = editingNote!!,
      onDismiss = { editingNote = null },
      onSave = { updatedNote ->
        onSaveNote(updatedNote)
        editingNote = null
      }
    )
  }
}

/**
 * Edit / Customize Question Dialog for Administrators
 */
@Composable
fun EditQuestionDialog(
  question: QuestionEntity,
  onDismiss: () -> Unit,
  onSave: (QuestionEntity) -> Unit
) {
  var questionText by remember { mutableStateOf(question.questionText) }
  var optA by remember { mutableStateOf(question.optionA) }
  var optB by remember { mutableStateOf(question.optionB) }
  var optC by remember { mutableStateOf(question.optionC) }
  var optD by remember { mutableStateOf(question.optionD) }
  var correctChoice by remember { mutableStateOf(question.correctAnswer) }
  var explanation by remember { mutableStateOf(question.explanationGeneral) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Customize / Edit Question") },
    text = {
      LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
          OutlinedTextField(
            value = questionText,
            onValueChange = { questionText = it },
            label = { Text("Question Text") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
          )
        }
        item { OutlinedTextField(value = optA, onValueChange = { optA = it }, label = { Text("Option A") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = optB, onValueChange = { optB = it }, label = { Text("Option B") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = optC, onValueChange = { optC = it }, label = { Text("Option C") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = optD, onValueChange = { optD = it }, label = { Text("Option D") }, modifier = Modifier.fillMaxWidth()) }
        item {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Correct Choice:", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            listOf("A", "B", "C", "D").forEach { choice ->
              Surface(
                modifier = Modifier
                  .padding(horizontal = 4.dp)
                  .clickable { correctChoice = choice },
                color = if (correctChoice == choice) SuccessGreen else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = choice,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  color = if (correctChoice == choice) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
        item {
          OutlinedTextField(
            value = explanation,
            onValueChange = { explanation = it },
            label = { Text("Explanation") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(
            question.copy(
              questionText = questionText.trim(),
              optionA = optA.trim(),
              optionB = optB.trim(),
              optionC = optC.trim(),
              optionD = optD.trim(),
              correctAnswer = correctChoice,
              explanationGeneral = explanation.trim()
            )
          )
        }
      ) {
        Text("Save Changes")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

/**
 * Edit / Customize Revision Note Dialog for Administrators
 */
@Composable
fun EditNoteDialog(
  note: NoteEntity,
  onDismiss: () -> Unit,
  onSave: (NoteEntity) -> Unit
) {
  var title by remember { mutableStateOf(note.title) }
  var summary by remember { mutableStateOf(note.summary) }
  var formulas by remember { mutableStateOf(note.keyFormulas) }
  var markdownContent by remember { mutableStateOf(note.contentMarkdown) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Customize / Edit Revision Note") },
    text = {
      LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = summary, onValueChange = { summary = it }, label = { Text("Summary") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(value = formulas, onValueChange = { formulas = it }, label = { Text("Key Formulas") }, modifier = Modifier.fillMaxWidth()) }
        item {
          OutlinedTextField(
            value = markdownContent,
            onValueChange = { markdownContent = it },
            label = { Text("Content / Markdown Body") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 5
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(
            note.copy(
              title = title.trim(),
              summary = summary.trim(),
              keyFormulas = formulas.trim(),
              contentMarkdown = markdownContent.trim()
            )
          )
        }
      ) {
        Text("Save Changes")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}
