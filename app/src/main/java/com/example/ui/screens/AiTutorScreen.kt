package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.model.CourseEntity
import com.example.ui.components.DetailTopBar
import com.example.ui.theme.AcademicBlueDark
import com.example.ui.theme.AcademicBluePrimary
import com.example.ui.theme.AcademicCobalt
import com.example.ui.theme.AcademicGold

@Composable
fun AiTutorScreen(
  courses: List<CourseEntity>,
  initialContext: String? = null,
  initialPrompt: String? = null,
  aiResponse: String?,
  isLoading: Boolean,
  onAskGemini: (prompt: String, courseTitle: String?, context: String?) -> Unit,
  onBackClick: () -> Unit,
  onShowMessage: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedCourse by remember { mutableStateOf<CourseEntity?>(courses.firstOrNull()) }
  var userQuery by remember { mutableStateOf(initialPrompt ?: "") }
  val clipboardManager = LocalClipboardManager.current

  val suggestedPrompts = remember(selectedCourse) {
    when (selectedCourse?.code) {
      "MATH 101" -> listOf(
        "Explain evaluating limits using L'Hôpital's Rule step-by-step",
        "How do I determine inflection points using the 2nd derivative?",
        "Derive the Fundamental Theorem of Calculus with an intuitive analogy"
      )
      "PHYS 101" -> listOf(
        "Derive conservation of mechanical energy with friction considerations",
        "Explain simple harmonic motion and period of a pendulum",
        "How do Newton's 3 laws apply to bank angles on curved tracks?"
      )
      "CS 101" -> listOf(
        "Explain recursion vs iteration with stack frame diagrams in mind",
        "How do Big-O time and space complexities work for freshman algorithms?",
        "Explain binary search tree insertion and search logic"
      )
      "CHEM 101" -> listOf(
        "How do I balance redox reactions using the half-reaction method?",
        "Explain Le Chatelier's Principle when pressure and temperature shift",
        "What is the difference between sigma and pi bonds in hybridization?"
      )
      "MATH 102" -> listOf(
        "How to find eigenvalues and eigenvectors of a 3x3 matrix?",
        "Explain Gaussian elimination vs Cramer's Rule for systems of equations",
        "What is the geometric meaning of matrix determinant?"
      )
      else -> listOf(
        "Explain core exam definitions and theorems for this course",
        "What are the top 5 most common mistakes freshman students make in this subject?",
        "Provide a comprehensive revision summary with sample problem"
      )
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .testTag("ai_tutor_screen")
  ) {
    DetailTopBar(
      title = "Gemini AI Tutor",
      subtitle = "Academic Clarification & Research Assistant",
      onBackClick = onBackClick
    )

    LazyColumn(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Hero Header Card
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(2.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(
                Brush.horizontalGradient(
                  listOf(AcademicBluePrimary, AcademicCobalt)
                )
              )
              .padding(18.dp)
          ) {
            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = AcademicGold,
                    modifier = Modifier.size(24.dp)
                  )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                  Text(
                    text = "TTS Academy AI Research Tutor",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                  )
                  Text(
                    text = "Powered by Google Gemini 3.5 Flash",
                    style = MaterialTheme.typography.labelSmall,
                    color = AcademicGold
                  )
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              Text(
                text = "Ask any university question for step-by-step mathematical reasoning, exam tips, and deep pedagogical concept clarity.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f)
              )
            }
          }
        }
      }

      // Course Selection Row
      item {
        Column {
          Text(
            text = "Select Course Subject:",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            courses.forEach { course ->
              val isSelected = selectedCourse?.id == course.id
              FilterChip(
                selected = isSelected,
                onClick = { selectedCourse = course },
                label = { Text("${course.code} - ${course.title.take(14)}") },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primary,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
              )
            }
          }
        }
      }

      // Quick Inquiry Prompt Chips
      item {
        Column {
          Text(
            text = "Quick Academic Research Prompts:",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
          )
          Spacer(modifier = Modifier.height(8.dp))
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            suggestedPrompts.forEach { promptText ->
              Surface(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    userQuery = promptText
                    onAskGemini(promptText, selectedCourse?.title, initialContext)
                  },
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = promptText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                  )
                }
              }
            }
          }
        }
      }

      // AI Response Card
      if (isLoading) {
        item {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.primary
              )
              Spacer(modifier = Modifier.width(16.dp))
              Text(
                text = "Gemini AI is analyzing academic principles...",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      } else if (!aiResponse.isNullOrBlank()) {
        item {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            elevation = CardDefaults.cardElevation(3.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(18.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Tutor Explanation & Proof",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                  )
                }

                IconButton(
                  onClick = {
                    clipboardManager.setText(AnnotatedString(aiResponse))
                    onShowMessage("Copied explanation to clipboard")
                  }
                ) {
                  Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy Response",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              Text(
                text = aiResponse,
                style = MaterialTheme.typography.bodyMedium.copy(
                  lineHeight = 24.sp,
                  fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      }
    }

    // Bottom Input Bar
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = userQuery,
          onValueChange = { userQuery = it },
          placeholder = { Text("Ask Gemini tutor for clarification or research...") },
          modifier = Modifier
            .weight(1f)
            .testTag("ai_tutor_input"),
          maxLines = 3,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary
          )
        )

        Spacer(modifier = Modifier.width(8.dp))

        Button(
          onClick = {
            if (userQuery.isNotBlank() && !isLoading) {
              onAskGemini(userQuery, selectedCourse?.title, initialContext)
            }
          },
          enabled = userQuery.isNotBlank() && !isLoading,
          shape = RoundedCornerShape(12.dp),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Send to AI",
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}
