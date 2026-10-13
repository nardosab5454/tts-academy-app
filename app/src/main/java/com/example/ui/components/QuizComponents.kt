package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.model.QuestionEntity
import com.example.ui.theme.AcademicBluePrimary
import com.example.ui.theme.AcademicCobalt
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorRedDark
import com.example.ui.theme.ErrorRedLight
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenDark
import com.example.ui.theme.SuccessGreenLight

@Composable
fun QuestionOptionItem(
  optionKey: String, // "A", "B", "C", "D"
  optionText: String,
  isSelected: Boolean,
  isCorrect: Boolean,
  isRevealed: Boolean, // true in practice mode after submit or in exam review
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  // Determine color states
  val backgroundColor: Color
  val borderColor: Color
  val textColor: Color
  val badgeBgColor: Color
  val badgeTextColor: Color

  if (isRevealed) {
    if (isCorrect) {
      backgroundColor = SuccessGreenLight
      borderColor = SuccessGreen
      textColor = SuccessGreenDark
      badgeBgColor = SuccessGreen
      badgeTextColor = Color.White
    } else if (isSelected) {
      backgroundColor = ErrorRedLight
      borderColor = ErrorRed
      textColor = ErrorRedDark
      badgeBgColor = ErrorRed
      badgeTextColor = Color.White
    } else {
      backgroundColor = MaterialTheme.colorScheme.surface
      borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
      textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
      badgeBgColor = MaterialTheme.colorScheme.surfaceVariant
      badgeTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    }
  } else {
    if (isSelected) {
      backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
      borderColor = MaterialTheme.colorScheme.primary
      textColor = MaterialTheme.colorScheme.onSurface
      badgeBgColor = MaterialTheme.colorScheme.primary
      badgeTextColor = MaterialTheme.colorScheme.onPrimary
    } else {
      backgroundColor = MaterialTheme.colorScheme.surface
      borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
      textColor = MaterialTheme.colorScheme.onSurface
      badgeBgColor = MaterialTheme.colorScheme.surfaceVariant
      badgeTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    }
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .clickable(enabled = !isRevealed, onClick = onClick)
      .testTag("option_${optionKey}"),
    shape = RoundedCornerShape(12.dp),
    color = backgroundColor,
    border = BorderStroke(if (isSelected || (isRevealed && isCorrect)) 2.dp else 1.dp, borderColor)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Option Circle Badge (A, B, C, D)
      Box(
        modifier = Modifier
          .size(34.dp)
          .clip(CircleShape)
          .background(badgeBgColor),
        contentAlignment = Alignment.Center
      ) {
        if (isRevealed && isCorrect) {
          Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Correct",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
          )
        } else if (isRevealed && isSelected) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Incorrect",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
          )
        } else {
          Text(
            text = optionKey,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = badgeTextColor
          )
        }
      }

      Spacer(modifier = Modifier.width(14.dp))

      Text(
        text = optionText,
        style = MaterialTheme.typography.bodyMedium.copy(
          fontWeight = if (isSelected || (isRevealed && isCorrect)) FontWeight.SemiBold else FontWeight.Normal,
          fontSize = 15.sp,
          lineHeight = 22.sp
        ),
        color = textColor,
        modifier = Modifier.weight(1f)
      )
    }
  }
}

@Composable
fun ExplanationCard(
  question: QuestionEntity,
  selectedAnswer: String?,
  onAskAiTutor: ((QuestionEntity) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val isCorrect = selectedAnswer.equals(question.correctAnswer, ignoreCase = true)

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ),
    border = BorderStroke(
      1.dp,
      if (isCorrect) SuccessGreen.copy(alpha = 0.6f) else ErrorRed.copy(alpha = 0.6f)
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f)
        ) {
          Icon(
            imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Lightbulb,
            contentDescription = null,
            tint = if (isCorrect) SuccessGreen else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
          )
          Text(
            text = if (isCorrect) "Excellent! Correct Answer: (${question.correctAnswer})" else "Correct Answer is (${question.correctAnswer})",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = if (isCorrect) SuccessGreenDark else MaterialTheme.colorScheme.onSurface
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = question.explanationGeneral,
        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
        color = MaterialTheme.colorScheme.onSurface
      )

      if (onAskAiTutor != null) {
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
          onClick = { onAskAiTutor(question) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Ask Gemini AI Tutor for Clarification",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = "Detailed Breakdown of Choices:",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary
      )

      Spacer(modifier = Modifier.height(8.dp))

      OptionExplanationRow("A", question.optionA, question.explanationA, question.correctAnswer == "A")
      OptionExplanationRow("B", question.optionB, question.explanationB, question.correctAnswer == "B")
      OptionExplanationRow("C", question.optionC, question.explanationC, question.correctAnswer == "C")
      OptionExplanationRow("D", question.optionD, question.explanationD, question.correctAnswer == "D")
    }
  }
}

@Composable
private fun OptionExplanationRow(
  key: String,
  optionText: String,
  explanation: String,
  isCorrectOption: Boolean
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    verticalAlignment = Alignment.Top
  ) {
    Surface(
      shape = RoundedCornerShape(4.dp),
      color = if (isCorrectOption) SuccessGreenLight else MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, if (isCorrectOption) SuccessGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
      Text(
        text = key,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = if (isCorrectOption) SuccessGreenDark else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
      )
    }

    Spacer(modifier = Modifier.width(8.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = explanation,
        style = MaterialTheme.typography.bodySmall.copy(
          color = if (isCorrectOption) SuccessGreenDark else MaterialTheme.colorScheme.onSurfaceVariant,
          lineHeight = 18.sp
        )
      )
    }
  }
}
