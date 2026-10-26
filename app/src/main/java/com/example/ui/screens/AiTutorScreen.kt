package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.ui.viewmodel.TutorMessage
import kotlinx.coroutines.launch

@Composable
fun AiTutorScreen(
  courses: List<CourseEntity>,
  initialContext: String? = null,
  initialPrompt: String? = null,
  aiResponse: String?,
  isLoading: Boolean,
  conversation: List<TutorMessage> = emptyList(),
  onAskGemini: (prompt: String, courseTitle: String?, context: String?) -> Unit,
  onClearConversation: () -> Unit = {},
  onBackClick: () -> Unit,
  onShowMessage: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedCourse by remember { mutableStateOf<CourseEntity?>(courses.firstOrNull()) }
  var userQuery by remember { mutableStateOf(initialPrompt ?: "") }
  val clipboardManager = LocalClipboardManager.current
  val listState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()

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

  // Auto-scroll to bottom whenever a new message is added or loading finishes
  LaunchedEffect(conversation.size, isLoading, aiResponse) {
    if (conversation.isNotEmpty() || isLoading) {
      coroutineScope.launch {
        listState.animateScrollToItem(listState.layoutInfo.totalItemsCount.coerceAtLeast(1) - 1)
      }
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .testTag("ai_tutor_screen")
  ) {
    DetailTopBar(
      title = "Gemini AI Tutor",
      subtitle = "Live Academic Clarification & Research",
      onBackClick = onBackClick
    )

    LazyColumn(
      state = listState,
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
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
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .size(38.dp)
                      .clip(CircleShape)
                      .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.Default.AutoAwesome,
                      contentDescription = null,
                      tint = AcademicGold,
                      modifier = Modifier.size(22.dp)
                    )
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  Column {
                    Text(
                      text = "TTS Academy Live Gemini Tutor",
                      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                      color = Color.White
                    )
                    Text(
                      text = "Multi-Turn Live AI • gemini-4-argon & gemini-3.8-flash",
                      style = MaterialTheme.typography.labelSmall,
                      color = AcademicGold
                    )
                  }
                }

                if (conversation.isNotEmpty()) {
                  IconButton(onClick = onClearConversation) {
                    Icon(
                      imageVector = Icons.Default.DeleteSweep,
                      contentDescription = "Clear Chat",
                      tint = Color.White.copy(alpha = 0.8f)
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = "Ask any university freshman question for instant pedagogical clarification, step-by-step mathematical proofs, and examination strategies.",
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = Color.White.copy(alpha = 0.9f)
              )
            }
          }
        }
      }

      // Course Subject Selection Row
      item {
        Column {
          Text(
            text = "Active Course Subject:",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(6.dp))
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
            text = "Suggested Freshman Questions:",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
          )
          Spacer(modifier = Modifier.height(6.dp))
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            suggestedPrompts.forEach { promptText ->
              Surface(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    userQuery = ""
                    onAskGemini(promptText, selectedCourse?.title, initialContext)
                  },
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
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

      // Render welcome prompt or conversation messages
      if (conversation.isEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.AutoAwesome,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Live Gemini Academic Tutor Ready",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.primary
                )
              }
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "Ask any university question! Gemini provides rigorous step-by-step mathematical proofs, physics formulas, algorithms, and exam strategies. You can ask follow-up questions to continue our multi-turn tutoring session.",
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      items(conversation) { msg ->
        if (msg.isUser) {
          // User Question Bubble
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            Surface(
              shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
              color = MaterialTheme.colorScheme.primaryContainer,
              modifier = Modifier.fillMaxWidth(0.88f)
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top
              ) {
                Icon(
                  imageVector = Icons.Default.Person,
                  contentDescription = "Student",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = msg.text,
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = MaterialTheme.colorScheme.onPrimaryContainer
                )
              }
            }
          }
        } else {
          // Gemini Tutor Response Card
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
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
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Gemini AI Tutor Response",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                  )
                }

                IconButton(
                  onClick = {
                    clipboardManager.setText(AnnotatedString(msg.text))
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
                text = msg.text,
                style = MaterialTheme.typography.bodyMedium.copy(
                  lineHeight = 23.sp,
                  fontSize = 14.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      }

      // Loading Indicator Card
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
                .padding(20.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.5.dp
              )
              Spacer(modifier = Modifier.width(14.dp))
              Text(
                text = "Gemini AI is analyzing academic principles...",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
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
          placeholder = { Text("Ask Gemini tutor any question...") },
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
            val queryToSend = userQuery.trim()
            if (queryToSend.isNotBlank() && !isLoading) {
              userQuery = "" // Clear input immediately
              onAskGemini(queryToSend, selectedCourse?.title, initialContext)
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
