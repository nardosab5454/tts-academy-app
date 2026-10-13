package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ai.AiExplanationResult
import com.example.ui.theme.AcademicGold
import com.example.ui.theme.SuccessGreen

@Composable
fun AiClarificationDialog(
  contextTitle: String,
  contextSnippet: String,
  courseContext: String? = null,
  aiResult: AiExplanationResult?,
  isLoading: Boolean,
  onDismiss: () -> Unit,
  onAskQuestion: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var followUpText by remember { mutableStateOf("") }

  val promptSuggestions = listOf(
    "Explain step-by-step",
    "Why are the other options wrong?",
    "Real-world university application",
    "Give an intuitive analogy",
    "Show another worked example"
  )

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.88f)
        .testTag("ai_clarification_dialog"),
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // Header Bar
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.AutoAwesome,
                  contentDescription = null,
                  tint = AcademicGold,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Gemini AI Academic Research",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Grounded with Google Search • gemini-3.5-flash",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }

            IconButton(onClick = onDismiss) {
              Icon(Icons.Default.Close, contentDescription = "Close")
            }
          }
        }

        // Content Scrollable Area
        LazyColumn(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
          contentPadding = PaddingValues(vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Context Chip
          item {
            Card(
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text(
                  text = contextTitle,
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = contextSnippet,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 3
                )
              }
            }
          }

          // Suggestion Chips
          item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(promptSuggestions) { prompt ->
                Surface(
                  shape = RoundedCornerShape(16.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant,
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                  modifier = Modifier.clickable { onAskQuestion(prompt) }
                ) {
                  Text(
                    text = prompt,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                  )
                }
              }
            }
          }

          // Loading Indicator
          if (isLoading) {
            item {
              Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(18.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                  )
                  Spacer(modifier = Modifier.width(14.dp))
                  Column {
                    Text(
                      text = "Analyzing with Gemini AI...",
                      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                      text = "Querying academic knowledge & Google Search grounding...",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }
            }
          }

          // AI Response Card
          if (aiResult != null && !isLoading) {
            item {
              Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, if (aiResult.isError) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(16.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.Lightbulb,
                      contentDescription = null,
                      tint = AcademicGold,
                      modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = "Clarification & Academic Explanation",
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.onSurface
                    )
                  }

                  Spacer(modifier = Modifier.height(10.dp))

                  Text(
                    text = aiResult.responseText,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = MaterialTheme.colorScheme.onSurface
                  )

                  // Grounding Sources
                  if (aiResult.sources.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(Icons.Default.Language, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = "Google Search Grounded References:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = SuccessGreen
                      )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    aiResult.sources.forEach { source ->
                      Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                          .fillMaxWidth()
                          .padding(vertical = 2.dp)
                      ) {
                        Row(
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                          Spacer(modifier = Modifier.width(6.dp))
                          Text(
                            text = source.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                          )
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }

        // Bottom Input Row for Follow-up questions
        Surface(
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 4.dp,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedTextField(
              value = followUpText,
              onValueChange = { followUpText = it },
              placeholder = { Text("Ask follow-up question...", fontSize = 14.sp) },
              modifier = Modifier.weight(1f),
              singleLine = true,
              shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = {
                if (followUpText.isNotBlank()) {
                  val q = followUpText
                  followUpText = ""
                  onAskQuestion(q)
                }
              },
              enabled = followUpText.isNotBlank() && !isLoading,
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
              Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(18.dp))
            }
          }
        }
      }
    }
  }
}
