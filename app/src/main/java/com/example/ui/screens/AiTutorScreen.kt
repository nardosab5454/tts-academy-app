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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.model.CourseEntity
import com.example.data.remote.TutorMode
import com.example.data.repository.WeakTopicSummary
import com.example.ui.components.DetailTopBar
import com.example.ui.theme.AcademicGold
import com.example.ui.theme.GradientDeepNavy
import com.example.ui.theme.GradientTealNavy
import com.example.ui.viewmodel.AiChatUiState
import com.example.ui.viewmodel.TutorMessage
import kotlinx.coroutines.launch

@Composable
fun AiTutorScreen(
  courses: List<CourseEntity>,
  selectedCourseId: String?,
  selectedMode: TutorMode,
  includeAppContext: Boolean,
  pinnedContextTitle: String? = null,
  pinnedContextSnippet: String? = null,
  weakTopics: List<WeakTopicSummary> = emptyList(),
  aiChatUiState: AiChatUiState = AiChatUiState.Idle,
  isLoading: Boolean,
  conversation: List<TutorMessage> = emptyList(),
  onSelectCourse: (String?) -> Unit,
  onSelectMode: (TutorMode) -> Unit,
  onToggleAppContext: (Boolean) -> Unit,
  onClearPinnedContext: () -> Unit,
  onAskGemini: (prompt: String, courseTitle: String?, context: String?) -> Unit,
  onRetryLast: (fallbackToHydrogen: Boolean) -> Unit,
  onClearConversation: () -> Unit = {},
  onBackClick: () -> Unit,
  onShowMessage: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val selectedCourse = remember(courses, selectedCourseId) {
    courses.find { it.id == selectedCourseId } ?: courses.firstOrNull()
  }
  var userQuery by remember { mutableStateOf("") }
  val clipboardManager = LocalClipboardManager.current
  val listState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()

  val suggestedPrompts = remember(selectedCourse?.id, weakTopics.size) {
    val weakHint = weakTopics.firstOrNull()?.let { wt ->
      "Coach me on my weak topic: ${wt.topicTitle} (${wt.accuracyPercent}% accuracy)"
    }
    val coursePrompts = when (selectedCourse?.code) {
      "MATH 101" -> listOf(
        "Explain evaluating limits using L'Hôpital's Rule step-by-step",
        "How do I determine inflection points using the 2nd derivative?",
        "Derive the Fundamental Theorem of Calculus with an intuitive analogy"
      )
      "PHYS 101" -> listOf(
        "Derive conservation of mechanical energy with friction considerations",
        "Explain simple harmonic motion and period of a pendulum",
        "Apply Newton's laws to circular motion on banked curves"
      )
      "CS 101" -> listOf(
        "Explain recursion vs iteration with stack frame intuition",
        "Compare Big-O time & space complexity for freshman sorting algorithms",
        "Trace binary search tree insertion and lookup logic"
      )
      "CHEM 101" -> listOf(
        "How do I balance redox reactions using the half-reaction method?",
        "Explain Le Chatelier's Principle for pressure and temperature shifts",
        "Contrast sigma and pi bonds using orbital hybridization"
      )
      "MATH 102" -> listOf(
        "How do I find eigenvalues and eigenvectors of a 3x3 matrix?",
        "Compare Gaussian elimination vs Cramer's Rule for linear systems",
        "Explain the geometric meaning of a matrix determinant"
      )
      else -> listOf(
        "Summarize the core exam definitions & formulas for ${selectedCourse?.code ?: "this course"}",
        "Quiz me interactively with 3 realistic freshman exam questions",
        "What are the top mistakes students make in ${selectedCourse?.title ?: "this subject"}?"
      )
    }
    if (weakHint != null) listOf(weakHint) + coursePrompts.take(2) else coursePrompts
  }

  // Smooth auto-scroll on new messages or incoming streaming tokens
  val lastMsgLength = conversation.lastOrNull()?.text?.length ?: 0
  LaunchedEffect(conversation.size, lastMsgLength, isLoading) {
    if (conversation.isNotEmpty()) {
      coroutineScope.launch {
        val lastIdx = (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
        listState.animateScrollToItem(lastIdx)
      }
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .imePadding()
      .testTag("ai_tutor_screen")
  ) {
    DetailTopBar(
      title = "Gemini AI Tutor",
      subtitle = "${selectedMode.label} • Live Context",
      onBackClick = onBackClick
    )

    LazyColumn(
      state = listState,
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Hero Banner Card
      item(key = "hero_header") {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(2.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(
                Brush.horizontalGradient(listOf(GradientDeepNavy, GradientTealNavy))
              )
              .padding(16.dp)
          ) {
            Column {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.weight(1f)
                ) {
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
                      modifier = Modifier.size(21.dp)
                    )
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  Column {
                    Text(
                      text = "Context-Aware Gemini AI",
                      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                      color = Color.White
                    )
                    Text(
                      text = "Real-time streaming • ${selectedMode.modelName}",
                      style = MaterialTheme.typography.labelSmall,
                      color = AcademicGold
                    )
                  }
                }

                if (conversation.isNotEmpty()) {
                  IconButton(
                    onClick = onClearConversation,
                    modifier = Modifier.testTag("ai_clear_chat_button")
                  ) {
                    Icon(
                      imageVector = Icons.Default.DeleteSweep,
                      contentDescription = "Clear Chat",
                      tint = Color.White.copy(alpha = 0.85f)
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = selectedMode.description,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = Color.White.copy(alpha = 0.9f)
              )
            }
          }
        }
      }

      // Model / Chatbot Role Selector
      item(key = "mode_selector") {
        Column {
          Text(
            text = "Tutor Intelligence Mode:",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(6.dp))
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(TutorMode.entries) { mode ->
              val isSelected = selectedMode == mode
              val icon = when (mode) {
                TutorMode.HYDROGEN -> Icons.Default.AutoAwesome
                TutorMode.FLASH_LITE -> Icons.Default.Bolt
                TutorMode.ARGON -> Icons.Default.Hub
                TutorMode.CARBON -> Icons.Default.Psychology
              }
              FilterChip(
                selected = isSelected,
                onClick = { onSelectMode(mode) },
                leadingIcon = {
                  Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                  )
                },
                label = { Text(mode.label, maxLines = 1) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primary,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                  selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("tutor_mode_${mode.name.lowercase()}")
              )
            }
          }
        }
      }

      // Live Context Control & Course Selector
      item(key = "context_panel") {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
          ),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
              ) {
                Icon(
                  imageVector = Icons.Default.School,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text(
                    text = "Live App Context",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = if (includeAppContext) {
                      buildString {
                        append(selectedCourse?.code ?: "All Courses")
                        if (weakTopics.isNotEmpty()) append(" • ${weakTopics.size} weak topics")
                        append(" • Syllabus & Notes")
                      }
                    } else {
                      "General open Q&A (app syllabus detached)"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }

              Switch(
                checked = includeAppContext,
                onCheckedChange = onToggleAppContext,
                modifier = Modifier.testTag("ai_context_toggle")
              )
            }

            if (includeAppContext && courses.isNotEmpty()) {
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
                    onClick = { onSelectCourse(course.id) },
                    label = { Text("${course.code} - ${course.title.take(14)}") },
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                      selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("ai_context_course_${course.id}")
                  )
                }
              }
            }

            if (includeAppContext && !pinnedContextSnippet.isNullOrBlank()) {
              Spacer(modifier = Modifier.height(8.dp))
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Pinned Context: ${pinnedContextTitle ?: "Study Item"}",
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                      text = pinnedContextSnippet,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onPrimaryContainer,
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                  IconButton(
                    onClick = onClearPinnedContext,
                    modifier = Modifier
                      .size(48.dp)
                      .testTag("ai_clear_pinned_context_button")
                  ) {
                    Icon(
                      imageVector = Icons.Default.Close,
                      contentDescription = "Clear Pinned Context",
                      tint = MaterialTheme.colorScheme.onPrimaryContainer,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }

      // Quick Prompts when thread is empty or compact starter
      if (conversation.isEmpty()) {
        item(key = "suggested_prompts") {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "Suggested Contextual Prompts:",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.primary
            )
            suggestedPrompts.forEach { promptText ->
              Surface(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable(enabled = !isLoading) {
                    userQuery = ""
                    onAskGemini(promptText, selectedCourse?.title, null)
                  },
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
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
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                  )
                }
              }
            }
          }
        }
      }

      // Multi-Turn Chat Thread
      items(conversation, key = { it.id }) { msg ->
        if (msg.isUser) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            Surface(
              shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 4.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
              ),
              color = MaterialTheme.colorScheme.primaryContainer,
              modifier = Modifier.fillMaxWidth(0.88f)
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.Top) {
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
                if (!msg.contextBadge.isNullOrBlank()) {
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = "${msg.contextBadge} • ${msg.modeLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                  )
                }
              }
            }
          }
        } else {
          val borderColor = if (msg.isError) {
            MaterialTheme.colorScheme.error.copy(alpha = 0.55f)
          } else {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
          }
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
              containerColor = if (msg.isError) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
              } else {
                MaterialTheme.colorScheme.surface
              }
            ),
            border = BorderStroke(1.dp, borderColor),
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
                  if (msg.isStreaming) {
                    CircularProgressIndicator(
                      modifier = Modifier.size(16.dp),
                      strokeWidth = 2.dp,
                      color = MaterialTheme.colorScheme.primary
                    )
                  } else {
                    Icon(
                      imageVector = if (msg.isError) Icons.Default.ErrorOutline else Icons.Default.AutoAwesome,
                      contentDescription = null,
                      tint = if (msg.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  Column {
                    Text(
                      text = when {
                        msg.isError -> "Gemini AI Error"
                        msg.isStreaming -> "Gemini Streaming Live..."
                        else -> "Gemini AI Tutor"
                      },
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                      color = if (msg.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Text(
                      text = msg.modelName,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }

                if (!msg.isStreaming && msg.text.isNotBlank() && !msg.isError) {
                  IconButton(
                    onClick = {
                      clipboardManager.setText(AnnotatedString(msg.text))
                      onShowMessage("Copied response to clipboard")
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
              }

              Spacer(modifier = Modifier.height(10.dp))

              if (msg.isStreaming && msg.text.isBlank()) {
                Text(
                  text = "Thinking with your live course & study context...",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              } else {
                Text(
                  text = msg.text,
                  style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 23.sp,
                    fontSize = 14.5.sp
                  ),
                  color = if (msg.isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                )
              }

              if (msg.isError && aiChatUiState is AiChatUiState.Error) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Button(
                    onClick = { onRetryLast(false) },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("ai_retry_button")
                  ) {
                    Icon(
                      imageVector = Icons.Default.Refresh,
                      contentDescription = null,
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retry", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  }

                  if (selectedMode != TutorMode.HYDROGEN) {
                    OutlinedButton(
                      onClick = { onRetryLast(true) },
                      shape = RoundedCornerShape(10.dp),
                      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                      colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                      )
                    ) {
                      Text("Use Hydrogen", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
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
          placeholder = {
            Text(
              if (includeAppContext && selectedCourse != null) {
                "Ask about ${selectedCourse.code} or any topic..."
              } else {
                "Ask Gemini tutor any question..."
              }
            )
          },
          modifier = Modifier
            .weight(1f)
            .testTag("ai_tutor_input"),
          maxLines = 4,
          shape = RoundedCornerShape(14.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary
          )
        )

        Spacer(modifier = Modifier.width(8.dp))

        Button(
          onClick = {
            val queryToSend = userQuery.trim()
            if (queryToSend.isNotBlank() && !isLoading) {
              userQuery = ""
              onAskGemini(queryToSend, selectedCourse?.title, null)
            }
          },
          enabled = userQuery.isNotBlank() && !isLoading,
          shape = RoundedCornerShape(14.dp),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
          modifier = Modifier.testTag("ai_tutor_send_button")
        ) {
          if (isLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.onPrimary
            )
          } else {
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
}
