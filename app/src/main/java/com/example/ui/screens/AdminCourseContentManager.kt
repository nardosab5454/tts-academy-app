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
import com.example.data.local.model.TopicEntity
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
 * Universal Section (Topic) Selector Dropdown for Admin Panel.
 * Allows administrators to assign questions and notes to specific sections within a course.
 */
@Composable
fun AdminSectionSelector(
  sections: List<TopicEntity>,
  selectedSectionId: String,
  onSectionSelected: (String) -> Unit,
  onAddNewSection: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  var isExpanded by remember { mutableStateOf(false) }
  val currentSection = sections.firstOrNull { it.id == selectedSectionId } ?: sections.firstOrNull()

  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    modifier = modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          "TARGET SECTION / TOPIC",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
          color = AcademicCobalt
        )
        Text(
          text = currentSection?.title ?: if (sections.isEmpty()) "No Sections Created Yet" else "Select Section",
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (onAddNewSection != null) {
          OutlinedButton(
            onClick = onAddNewSection,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("+ Section", style = MaterialTheme.typography.labelSmall)
          }
        }

        if (sections.isNotEmpty()) {
          Box {
            OutlinedButton(
              onClick = { isExpanded = true },
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
              Text("Choose", style = MaterialTheme.typography.labelSmall)
              Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
            }

            DropdownMenu(
              expanded = isExpanded,
              onDismissRequest = { isExpanded = false }
            ) {
              sections.forEach { section ->
                DropdownMenuItem(
                  text = {
                    Column {
                      Text(section.title, fontWeight = FontWeight.Bold)
                      if (section.description.isNotBlank()) {
                        Text(
                          section.description,
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                      }
                    }
                  },
                  onClick = {
                    onSectionSelected(section.id)
                    isExpanded = false
                  },
                  leadingIcon = {
                    if (section.id == selectedSectionId) {
                      Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen)
                    }
                  }
                )
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Manage Course Sections / Topics Dialog
 * Lists all sections within a course, allows adding, editing, and deleting sections.
 */
@Composable
fun ManageSectionsDialog(
  course: CourseEntity,
  sections: List<TopicEntity>,
  questions: List<QuestionEntity> = emptyList(),
  notes: List<NoteEntity> = emptyList(),
  onDismiss: () -> Unit,
  onSaveSection: (TopicEntity) -> Unit,
  onDeleteSection: (String) -> Unit
) {
  var editingSection by remember { mutableStateOf<TopicEntity?>(null) }
  var showCreateSectionDialog by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Manage Course Sections", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
          Text("${course.code} • ${course.title}", style = MaterialTheme.typography.bodySmall, color = AcademicCobalt)
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            "Sections (${sections.size})",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
          )
          Button(
            onClick = { showCreateSectionDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = AcademicBluePrimary),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            shape = RoundedCornerShape(8.dp)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Section", style = MaterialTheme.typography.labelSmall)
          }
        }

        if (sections.isEmpty()) {
          Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
              Text("No sections created for this course yet.", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
              Spacer(modifier = Modifier.height(4.dp))
              Text("Click '+ Add Section' above to organize this course into study sections.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .height(300.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(sections) { section ->
              val qCount = questions.count { it.topicId == section.id }
              val nCount = notes.count { it.topicId == section.id }

              Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(section.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    if (section.description.isNotBlank()) {
                      Text(
                        section.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                      )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "$qCount questions • $nCount notes",
                      style = MaterialTheme.typography.labelSmall,
                      color = AcademicCobalt
                    )
                  }

                  Row {
                    IconButton(onClick = { editingSection = section }) {
                      Icon(Icons.Default.Edit, contentDescription = "Edit Section", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { onDeleteSection(section.id) }) {
                      Icon(Icons.Default.Delete, contentDescription = "Delete Section", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                  }
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Done")
      }
    }
  )

  // Sub-dialog to create a new section
  if (showCreateSectionDialog) {
    CreateOrEditSectionDialog(
      courseId = course.id,
      existingSection = null,
      existingCount = sections.size,
      onDismiss = { showCreateSectionDialog = false },
      onSave = { newSection ->
        onSaveSection(newSection)
        showCreateSectionDialog = false
      }
    )
  }

  // Sub-dialog to edit existing section
  if (editingSection != null) {
    CreateOrEditSectionDialog(
      courseId = course.id,
      existingSection = editingSection,
      existingCount = sections.size,
      onDismiss = { editingSection = null },
      onSave = { updatedSection ->
        onSaveSection(updatedSection)
        editingSection = null
      }
    )
  }
}

/**
 * Dialog to Create or Edit a Course Section / Topic.
 */
@Composable
fun CreateOrEditSectionDialog(
  courseId: String,
  existingSection: TopicEntity? = null,
  existingCount: Int = 0,
  onDismiss: () -> Unit,
  onSave: (TopicEntity) -> Unit
) {
  var title by remember { mutableStateOf(existingSection?.title ?: "") }
  var description by remember { mutableStateOf(existingSection?.description ?: "") }
  var orderStr by remember { mutableStateOf(existingSection?.orderIndex?.toString() ?: (existingCount + 1).toString()) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (existingSection == null) "Create New Section" else "Edit Section") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Section Title (e.g. Limits & Continuity)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Section Description / Scope") },
          maxLines = 3,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = orderStr,
          onValueChange = { orderStr = it },
          label = { Text("Order Index (1, 2, 3...)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isNotBlank()) {
            val order = orderStr.toIntOrNull() ?: (existingCount + 1)
            val section = TopicEntity(
              id = existingSection?.id ?: ("T_" + UUID.randomUUID().toString().take(6).uppercase()),
              courseId = courseId,
              title = title.trim(),
              description = description.trim(),
              orderIndex = order
            )
            onSave(section)
          }
        }
      ) {
        Text("Save Section")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

/**
 * Dialog to Edit Course metadata (code, title, category, description).
 */
@Composable
fun EditCourseDialog(
  course: CourseEntity,
  onDismiss: () -> Unit,
  onSave: (CourseEntity) -> Unit
) {
  var code by remember { mutableStateOf(course.code) }
  var title by remember { mutableStateOf(course.title) }
  var desc by remember { mutableStateOf(course.description) }
  var category by remember { mutableStateOf(course.category) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Edit Course Details") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Course Code (e.g. MATH 101)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Course Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category (STEM, Computing, etc.)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, maxLines = 3, modifier = Modifier.fillMaxWidth())
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (code.isNotBlank() && title.isNotBlank()) {
            onSave(
              course.copy(
                code = code.trim(),
                title = title.trim(),
                category = category.trim(),
                description = desc.trim()
              )
            )
          }
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
  topics: List<TopicEntity> = emptyList(),
  initialCourseId: String = "",
  onSaveQuestion: (QuestionEntity) -> Unit,
  onDeleteQuestion: (String) -> Unit,
  onSaveNote: (NoteEntity) -> Unit,
  onDeleteNote: (String) -> Unit,
  onSaveTopic: (TopicEntity) -> Unit = {},
  onDeleteTopic: (String) -> Unit = {},
  onSwitchToAddQuestion: (String) -> Unit,
  onSwitchToAddNote: (String) -> Unit
) {
  var selectedCourseId by remember(initialCourseId, courses) {
    mutableStateOf(if (initialCourseId.isNotBlank()) initialCourseId else courses.firstOrNull()?.id ?: "")
  }

  var selectedSubTab by remember { mutableIntStateOf(0) }
  var selectedSectionFilter by remember { mutableStateOf("ALL") }
  var showManageSections by remember { mutableStateOf(false) }
  var editingQuestion by remember { mutableStateOf<QuestionEntity?>(null) }
  var editingNote by remember { mutableStateOf<NoteEntity?>(null) }

  val courseTopics = topics.filter { it.courseId == selectedCourseId }
  val courseQuestions = questions.filter { it.courseId == selectedCourseId }
  val courseNotes = notes.filter { it.courseId == selectedCourseId }
  val currentCourse = courses.firstOrNull { it.id == selectedCourseId }

  val filteredQuestions = if (selectedSectionFilter == "ALL") {
    courseQuestions
  } else {
    courseQuestions.filter { it.topicId == selectedSectionFilter }
  }

  val filteredNotes = if (selectedSectionFilter == "ALL") {
    courseNotes
  } else {
    courseNotes.filter { it.topicId == selectedSectionFilter }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Text(
        text = "Course & Section Content Manager",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
      Text(
        text = "Organize content hierarchically: Course → Section → Learning Resources. Changes sync globally across all devices.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    // Course Selector
    item {
      AdminCourseSelector(
        courses = courses,
        selectedCourseId = selectedCourseId,
        onCourseSelected = {
          selectedCourseId = it
          selectedSectionFilter = "ALL"
        }
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
          OutlinedButton(
            onClick = { showManageSections = true },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1.1f)
          ) {
            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Sections (${courseTopics.size})", style = MaterialTheme.typography.labelSmall)
          }

          Button(
            onClick = { onSwitchToAddQuestion(selectedCourseId) },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = AcademicBluePrimary)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("+ Question", style = MaterialTheme.typography.labelSmall)
          }

          OutlinedButton(
            onClick = { onSwitchToAddNote(selectedCourseId) },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("+ Note", style = MaterialTheme.typography.labelSmall)
          }
        }
      }
    }

    // Course Hierarchy Banner & Section Filter Chips
    if (courseTopics.isNotEmpty()) {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "FILTER BY SECTION (TOPIC)",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = AcademicCobalt
          )
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            item {
              FilterChip(
                selected = selectedSectionFilter == "ALL",
                onClick = { selectedSectionFilter = "ALL" },
                label = { Text("All (${courseQuestions.size + courseNotes.size})") }
              )
            }
            items(courseTopics) { sec ->
              val qC = courseQuestions.count { it.topicId == sec.id }
              val nC = courseNotes.count { it.topicId == sec.id }
              FilterChip(
                selected = selectedSectionFilter == sec.id,
                onClick = { selectedSectionFilter = sec.id },
                label = { Text("${sec.title} (${qC + nC})") }
              )
            }
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
          text = { Text("Questions (${filteredQuestions.size})", fontWeight = FontWeight.Bold) }
        )
        Tab(
          selected = selectedSubTab == 1,
          onClick = { selectedSubTab = 1 },
          text = { Text("Revision Notes (${filteredNotes.size})", fontWeight = FontWeight.Bold) }
        )
      }
    }

    // Questions List
    if (selectedSubTab == 0) {
      if (filteredQuestions.isEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "No questions found in this section.",
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Click '+ Question' above to add questions to this course section.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      } else {
        items(filteredQuestions) { q ->
          val secTitle = courseTopics.find { it.id == q.topicId }?.title ?: "General Section"

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
                    color = AcademicGold.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                  ) {
                    Text(
                      text = secTitle,
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                      color = AcademicBluePrimary,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                      maxLines = 1
                    )
                  }
                  Spacer(modifier = Modifier.width(6.dp))
                  Surface(
                    color = SuccessGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                  ) {
                    Text(
                      text = "Ans: (${q.correctAnswer})",
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
      if (filteredNotes.isEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "No notes created yet for this section.",
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Click '+ Note' above to create revision summaries and formulas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      } else {
        items(filteredNotes) { note ->
          val secTitle = courseTopics.find { it.id == note.topicId }?.title ?: "General Section"

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
                Column(modifier = Modifier.weight(1f)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                      color = AcademicGold.copy(alpha = 0.2f),
                      shape = RoundedCornerShape(6.dp)
                    ) {
                      Text(
                        text = secTitle,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = AcademicBluePrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(text = note.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                }

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

  // Manage Sections Dialog
  if (showManageSections && currentCourse != null) {
    ManageSectionsDialog(
      course = currentCourse,
      sections = courseTopics,
      questions = courseQuestions,
      notes = courseNotes,
      onDismiss = { showManageSections = false },
      onSaveSection = onSaveTopic,
      onDeleteSection = onDeleteTopic
    )
  }

  // Edit Question Dialog
  if (editingQuestion != null) {
    EditQuestionDialog(
      question = editingQuestion!!,
      sections = courseTopics,
      onDismiss = { editingQuestion = null },
      onSave = { updatedQ ->
        onSaveQuestion(updatedQ)
        editingQuestion = null
      }
    )
  }

  // Edit Note Dialog
  if (editingNote != null) {
    EditNoteDialog(
      note = editingNote!!,
      sections = courseTopics,
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
  sections: List<TopicEntity> = emptyList(),
  onDismiss: () -> Unit,
  onSave: (QuestionEntity) -> Unit
) {
  var questionText by remember { mutableStateOf(question.questionText) }
  var selectedTopicId by remember { mutableStateOf(question.topicId) }
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
        if (sections.isNotEmpty()) {
          item {
            AdminSectionSelector(
              sections = sections,
              selectedSectionId = selectedTopicId,
              onSectionSelected = { selectedTopicId = it }
            )
          }
        }
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
              topicId = selectedTopicId.ifBlank { question.topicId },
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
  sections: List<TopicEntity> = emptyList(),
  onDismiss: () -> Unit,
  onSave: (NoteEntity) -> Unit
) {
  var title by remember { mutableStateOf(note.title) }
  var selectedTopicId by remember { mutableStateOf(note.topicId) }
  var summary by remember { mutableStateOf(note.summary) }
  var formulas by remember { mutableStateOf(note.keyFormulas) }
  var markdownContent by remember { mutableStateOf(note.contentMarkdown) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Customize / Edit Revision Note") },
    text = {
      LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (sections.isNotEmpty()) {
          item {
            AdminSectionSelector(
              sections = sections,
              selectedSectionId = selectedTopicId,
              onSectionSelected = { selectedTopicId = it }
            )
          }
        }
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
              topicId = selectedTopicId.ifBlank { note.topicId },
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
