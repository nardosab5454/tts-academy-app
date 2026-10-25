package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.model.CourseEntity
import com.example.data.local.model.PastExamEntity
import com.example.ui.components.DetailTopBar
import com.example.ui.theme.AcademicBlueDark
import com.example.ui.theme.AcademicBluePrimary
import com.example.ui.theme.AcademicCobalt
import com.example.ui.theme.AcademicGold
import com.example.ui.theme.SuccessGreen

@Composable
fun PastExamsScreen(
  pastExams: List<PastExamEntity>,
  courses: List<CourseEntity>,
  onExamClick: (PastExamEntity) -> Unit,
  onDownloadExam: ((PastExamEntity) -> Unit)? = null,
  isUnlocked: Boolean = true,
  onUnlockClick: () -> Unit = {},
  isExamDownloaded: ((PastExamEntity) -> Boolean)? = null,
  onRefreshCloud: (() -> Unit)? = null,
  isRefreshing: Boolean = false,
  modifier: Modifier = Modifier
) {
  var selectedUniversity by remember { mutableStateOf("All Universities") }
  var selectedCourseId by remember { mutableStateOf("ALL") }
  var selectedYear by remember { mutableStateOf("All") }
  var selectedExamType by remember { mutableStateOf("ALL") }

  val courseMap = remember(courses) { courses.associateBy { it.id } }

  // Extract all distinct universities from past exams + standard Ethiopian institutions
  val availableUniversities = remember(pastExams) {
    val fromExams = pastExams.map { it.university }.filter { it.isNotBlank() }.distinct()
    val standardUnis = listOf(
      "Addis Ababa University (AAU)",
      "ASTU (Adama)",
      "AASTU",
      "Hawassa Univ.",
      "Bahir Dar Univ.",
      "Jimma Univ."
    )
    (listOf("All Universities") + (fromExams + standardUnis).distinct())
  }

  // Filter exams by university first, then inside university by course/subject, year, and type
  val examsInSelectedUniversity = remember(pastExams, selectedUniversity) {
    if (selectedUniversity == "All Universities") {
      pastExams
    } else {
      pastExams.filter { it.university == selectedUniversity }
    }
  }

  // Available courses with exams inside the selected university
  val availableCourseIds = remember(examsInSelectedUniversity) {
    examsInSelectedUniversity.map { it.courseId }.distinct()
  }

  val filteredExams = remember(examsInSelectedUniversity, selectedCourseId, selectedYear, selectedExamType) {
    examsInSelectedUniversity.filter { exam ->
      val matchesCourse = selectedCourseId == "ALL" || exam.courseId == selectedCourseId
      val matchesYear = selectedYear == "All" || exam.academicYear == selectedYear
      val matchesType = selectedExamType == "ALL" || exam.examType == selectedExamType
      matchesCourse && matchesYear && matchesType
    }
  }

  val years = listOf("All", "2024/2025", "2023/2024")

  Column(
    modifier = modifier
      .fillMaxSize()
      .testTag("past_exams_screen")
  ) {
    // 1. Header Banner
    Surface(
      color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.School,
              contentDescription = null,
              tint = AcademicCobalt,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Past-Year Examination Archive",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
          }
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Browse official university exam papers by institution, then by subject.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        if (onRefreshCloud != null) {
          Spacer(modifier = Modifier.width(8.dp))
          IconButton(
            onClick = onRefreshCloud,
            enabled = !isRefreshing,
            modifier = Modifier.testTag("past_exams_refresh_button")
          ) {
            if (isRefreshing) {
              CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
              )
            } else {
              Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Sync Exams from Cloud",
                tint = MaterialTheme.colorScheme.primary
              )
            }
          }
        }
      }
    }

    // 2. Lock Banner if unauthenticated or locked
    if (!isUnlocked) {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Exams Archive Locked",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Enter your admin 6-digit access code to unlock all midterm and final papers.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = onUnlockClick,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AcademicCobalt)
          ) {
            Text("Unlock", fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // 3. UNIVERSITY SECTION (MANDATORY REQUIREMENT)
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 10.dp, bottom = 4.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.School,
            contentDescription = null,
            tint = AcademicCobalt,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Select University / Institution",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
        }
        if (selectedUniversity != "All Universities" || selectedCourseId != "ALL" || selectedYear != "All") {
          Text(
            text = "Reset Filters",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = AcademicCobalt,
            modifier = Modifier
              .clickable {
                selectedUniversity = "All Universities"
                selectedCourseId = "ALL"
                selectedYear = "All"
                selectedExamType = "ALL"
              }
              .padding(4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // University Horizontal Selector Cards
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(availableUniversities) { uni ->
          val isSelected = selectedUniversity == uni
          val examCount = if (uni == "All Universities") {
            pastExams.size
          } else {
            pastExams.count { it.university == uni }
          }

          Surface(
            onClick = {
              selectedUniversity = uni
              selectedCourseId = "ALL" // Reset subject selection when university changes
            },
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) AcademicCobalt else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(
              width = if (isSelected) 1.5.dp else 1.dp,
              color = if (isSelected) AcademicCobalt else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            ),
            modifier = Modifier.testTag("uni_tab_${uni.take(10).replace(" ", "_")}")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.School,
                contentDescription = null,
                tint = if (isSelected) AcademicGold else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Column {
                Text(
                  text = uni,
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                  ),
                  color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "$examCount ${if (examCount == 1) "paper" else "papers"}",
                  style = MaterialTheme.typography.labelSmall,
                  color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }
    }

    // 4. SUBJECT / COURSE SECTION (INSIDE UNIVERSITY)
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 10.dp, bottom = 4.dp)
    ) {
      Text(
        text = if (selectedUniversity == "All Universities") {
          "Filter by Subject / Course"
        } else {
          "Subjects in ${selectedUniversity.take(24)}..."
        },
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
      )

      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // "All Subjects" chip
        item {
          FilterChip(
            selected = selectedCourseId == "ALL",
            onClick = { selectedCourseId = "ALL" },
            label = {
              Text("All Subjects (${examsInSelectedUniversity.size})")
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
          )
        }

        // Available Courses
        items(courses) { course ->
          val countInUni = examsInSelectedUniversity.count { it.courseId == course.id }
          val isAvailable = countInUni > 0 || selectedUniversity == "All Universities"
          if (isAvailable) {
            FilterChip(
              selected = selectedCourseId == course.id,
              onClick = { selectedCourseId = course.id },
              label = {
                Text("${course.code} ($countInUni)")
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
              )
            )
          }
        }
      }
    }

    // 5. SECONDARY FILTERS: ACADEMIC YEAR & EXAM TYPE
    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      items(years) { year ->
        FilterChip(
          selected = selectedYear == year,
          onClick = { selectedYear = year },
          label = { Text(year) }
        )
      }

      item {
        FilterChip(
          selected = selectedExamType == "ALL",
          onClick = { selectedExamType = "ALL" },
          label = { Text("All Types") }
        )
      }
      item {
        FilterChip(
          selected = selectedExamType == "MIDTERM",
          onClick = { selectedExamType = "MIDTERM" },
          label = { Text("Midterm") }
        )
      }
      item {
        FilterChip(
          selected = selectedExamType == "FINAL",
          onClick = { selectedExamType = "FINAL" },
          label = { Text("Final") }
        )
      }
    }

    // 6. EXAM RESULTS LIST
    if (filteredExams.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(24.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Default.School,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp)
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "No past-year exams found for this selection.",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Try switching university or resetting the subject filter.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedButton(
            onClick = {
              selectedUniversity = "All Universities"
              selectedCourseId = "ALL"
              selectedYear = "All"
              selectedExamType = "ALL"
            }
          ) {
            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Show All Exams")
          }
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(filteredExams) { exam ->
          val course = courseMap[exam.courseId]
          val downloaded = isExamDownloaded?.invoke(exam) == true
          PastExamCard(
            exam = exam,
            courseCode = course?.code ?: "COURSE",
            isUnlocked = isUnlocked,
            isDownloaded = downloaded,
            onClick = { onExamClick(exam) },
            onDownload = onDownloadExam?.let { { it(exam) } }
          )
        }
      }
    }
  }
}

@Composable
private fun PastExamCard(
  exam: PastExamEntity,
  courseCode: String,
  isUnlocked: Boolean = true,
  isDownloaded: Boolean = false,
  onClick: () -> Unit,
  onDownload: (() -> Unit)? = null
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(2.dp),
    border = if (!isUnlocked) BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)) else null,
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // Top Tag Row: University Badge + Course Code + Session
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // University Badge with School Icon
        Surface(
          color = AcademicCobalt.copy(alpha = 0.12f),
          border = BorderStroke(1.dp, AcademicCobalt.copy(alpha = 0.35f)),
          shape = RoundedCornerShape(6.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.School,
              contentDescription = null,
              tint = AcademicCobalt,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = exam.university,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = AcademicCobalt,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        if (isDownloaded) {
          Surface(
            color = SuccessGreen.copy(alpha = 0.15f),
            shape = RoundedCornerShape(4.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Verified, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(10.dp))
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = "Offline Ready",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = SuccessGreen
              )
            }
          }
        } else if (!isUnlocked) {
          Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(6.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Lock, contentDescription = "Locked", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(12.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Locked",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.error
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Secondary Tag: Course Code & Year/Type
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer,
          shape = RoundedCornerShape(4.dp)
        ) {
          Text(
            text = courseCode,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }

        Surface(
          color = MaterialTheme.colorScheme.secondaryContainer,
          shape = RoundedCornerShape(4.dp)
        ) {
          Text(
            text = "${exam.academicYear} • ${exam.examType}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }

        if (exam.isVerified && !isDownloaded && isUnlocked) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Verified,
              contentDescription = "Verified",
              tint = SuccessGreen,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "Verified",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = SuccessGreen
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = exam.title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = exam.instructions,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(4.dp))
            Text("${exam.durationMinutes} min", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }

          Surface(
            color = if (exam.isDigitized) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(4.dp)
          ) {
            Text(
              text = if (exam.isDigitized) "Digitized Quiz" else "Official Document",
              style = MaterialTheme.typography.labelSmall,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (onDownload != null) {
            IconButton(
              onClick = onDownload,
              modifier = Modifier.size(32.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Download PDF",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
              )
            }
            Spacer(modifier = Modifier.width(4.dp))
          }
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }
  }
}

@Composable
fun PastExamDetailScreen(
  exam: PastExamEntity,
  course: CourseEntity?,
  onBackClick: () -> Unit,
  onStartExam: (PastExamEntity, CourseEntity) -> Unit,
  onDownloadPaper: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .testTag("past_exam_detail_screen")
  ) {
    DetailTopBar(
      title = exam.title,
      subtitle = "${exam.university} • ${course?.title ?: "Course"} • ${exam.academicYear}",
      onBackClick = onBackClick
    )

    LazyColumn(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Info Card
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(2.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "EXAMINATION SPECIFICATIONS",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
              )
              if (exam.isVerified) {
                Surface(
                  color = SuccessGreen.copy(alpha = 0.15f),
                  shape = RoundedCornerShape(4.dp)
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Verified Answer Key", style = MaterialTheme.typography.labelSmall, color = SuccessGreen)
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Prominent University Row
            Surface(
              color = AcademicCobalt.copy(alpha = 0.1f),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.School,
                  contentDescription = null,
                  tint = AcademicCobalt,
                  modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = "University / Institution",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Text(
                    text = exam.university,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = AcademicCobalt
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Course: ${course?.title ?: "Freshman Course"} (${course?.code ?: "COURSE"})", style = MaterialTheme.typography.bodyMedium)
            Text("Academic Year: ${exam.academicYear}", style = MaterialTheme.typography.bodyMedium)
            Text("Examination Type: ${exam.examType} (${exam.semester})", style = MaterialTheme.typography.bodyMedium)
            Text("Allowed Time: ${exam.durationMinutes} Minutes", style = MaterialTheme.typography.bodyMedium)
            Text("Digitized Status: ${if (exam.isDigitized) "Interactive inside app" else "Archival PDF paper"}", style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))

            Text(
              text = "Official Instructions for Candidates:",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = exam.instructions,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (exam.documentSummary.isNotBlank()) {
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = exam.documentSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }
        }
      }

      // Examination Document / Digitized Questions Section
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Text(
              text = "Paper Overview & Syllabus Coverage",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "• Section A: Multiple-Choice Conceptual Questions\n• Section B: Analytical and Problem-Solving Exercises\n• All questions preserved in their original university examination wording.\n• Verified solutions prepared by university subject examiners.",
              style = MaterialTheme.typography.bodySmall.copy(lineHeight = 22.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }

    // Action Bottom Bar
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 4.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedButton(
          onClick = onDownloadPaper,
          modifier = Modifier.weight(1f)
        ) {
          Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Download PDF")
        }

        Button(
          onClick = {
            if (course != null) {
              onStartExam(exam, course)
            }
          },
          enabled = course != null,
          modifier = Modifier.weight(1f),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Start Test")
        }
      }
    }
  }
}
