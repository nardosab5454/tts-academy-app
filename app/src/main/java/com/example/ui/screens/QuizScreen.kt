package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.model.QuestionEntity
import com.example.data.local.model.QuizAttemptEntity
import com.example.ui.components.DetailTopBar
import com.example.ui.components.ExplanationCard
import com.example.ui.components.QuestionOptionItem
import com.example.ui.theme.AcademicBluePrimary
import com.example.ui.theme.AcademicGold
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.ActiveQuizState

@Composable
fun QuizScreen(
  state: ActiveQuizState,
  isBookmarked: Boolean,
  onSelectAnswer: (String, String) -> Unit,
  onSubmitPracticeAnswer: (String) -> Unit,
  onNext: () -> Unit,
  onPrevious: () -> Unit,
  onJumpToIndex: (Int) -> Unit,
  onFinishQuiz: () -> Unit,
  onToggleBookmark: () -> Unit,
  onExit: () -> Unit,
  onAskAiTutor: ((QuestionEntity, String?) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  var showExitDialog by remember { mutableStateOf(false) }
  var showFinishConfirmDialog by remember { mutableStateOf(false) }

  if (state.questions.isEmpty()) {
    EmptyStateView("No questions loaded.")
    return
  }

  val currentQuestion = state.questions[state.currentIndex]
  val selectedAnswer = state.selectedAnswers[currentQuestion.id]
  val isRevealed = selectedAnswer != null

  Column(
    modifier = modifier
      .fillMaxSize()
      .testTag("quiz_screen")
  ) {
    // Header Bar
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 2.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = { showExitDialog = true }) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit Quiz")
        }

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = state.title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            maxLines = 1
          )
          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
              color = if (state.isExamMode) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
              shape = RoundedCornerShape(4.dp)
            ) {
              Text(
                text = if (state.isExamMode) "EXAM MODE" else "PRACTICE MODE",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (state.isExamMode) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Question ${state.currentIndex + 1} of ${state.questions.size}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        IconButton(onClick = onToggleBookmark) {
          Icon(
            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
            contentDescription = "Bookmark",
            tint = if (isBookmarked) AcademicGold else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    // Progress Bar
    LinearProgressIndicator(
      progress = { (state.currentIndex + 1).toFloat() / state.questions.size.toFloat() },
      modifier = Modifier.fillMaxWidth(),
      color = MaterialTheme.colorScheme.primary
    )

    // Question Number Carousel Selector
    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
      itemsIndexed(state.questions) { index, q ->
        val isCurrent = index == state.currentIndex
        val isAnswered = state.selectedAnswers.containsKey(q.id)

        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(
              when {
                isCurrent -> MaterialTheme.colorScheme.primary
                isAnswered -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
              }
            )
            .clickable { onJumpToIndex(index) },
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "${index + 1}",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = when {
              isCurrent -> MaterialTheme.colorScheme.onPrimary
              isAnswered -> MaterialTheme.colorScheme.onPrimaryContainer
              else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
          )
        }
      }
    }

    // Question Content and Options
    LazyColumn(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Question Card
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
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(4.dp)
              ) {
                Text(
                  text = "Question ${state.currentIndex + 1}",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSecondaryContainer,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(4.dp)
              ) {
                Text(
                  text = currentQuestion.difficulty,
                  style = MaterialTheme.typography.labelSmall,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
              text = currentQuestion.questionText,
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                lineHeight = 24.sp
              ),
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }

      // Choice A
      item {
        QuestionOptionItem(
          optionKey = "A",
          optionText = currentQuestion.optionA,
          isSelected = selectedAnswer == "A",
          isCorrect = currentQuestion.correctAnswer == "A",
          isRevealed = isRevealed,
          onClick = { onSelectAnswer(currentQuestion.id, "A") }
        )
      }

      // Choice B
      item {
        QuestionOptionItem(
          optionKey = "B",
          optionText = currentQuestion.optionB,
          isSelected = selectedAnswer == "B",
          isCorrect = currentQuestion.correctAnswer == "B",
          isRevealed = isRevealed,
          onClick = { onSelectAnswer(currentQuestion.id, "B") }
        )
      }

      // Choice C
      item {
        QuestionOptionItem(
          optionKey = "C",
          optionText = currentQuestion.optionC,
          isSelected = selectedAnswer == "C",
          isCorrect = currentQuestion.correctAnswer == "C",
          isRevealed = isRevealed,
          onClick = { onSelectAnswer(currentQuestion.id, "C") }
        )
      }

      // Choice D
      item {
        QuestionOptionItem(
          optionKey = "D",
          optionText = currentQuestion.optionD,
          isSelected = selectedAnswer == "D",
          isCorrect = currentQuestion.correctAnswer == "D",
          isRevealed = isRevealed,
          onClick = { onSelectAnswer(currentQuestion.id, "D") }
        )
      }



      // Detailed Explanation Card
      if (isRevealed) {
        item {
          ExplanationCard(
            question = currentQuestion,
            selectedAnswer = selectedAnswer,
            onAskAiTutor = { q -> onAskAiTutor?.invoke(q, selectedAnswer) }
          )
        }
      }
    }

    // Bottom Navigation Controls
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 4.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedButton(
          onClick = onPrevious,
          enabled = state.currentIndex > 0,
          modifier = Modifier.testTag("quiz_prev_button")
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Prev")
        }

        if (state.currentIndex < state.questions.lastIndex) {
          Button(
            onClick = onNext,
            modifier = Modifier.testTag("quiz_next_button")
          ) {
            Text("Next")
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
          }
        } else {
          Button(
            onClick = { showFinishConfirmDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
            modifier = Modifier.testTag("quiz_finish_button")
          ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Submit Quiz")
          }
        }
      }
    }
  }

  // Confirm Exit Dialog
  if (showExitDialog) {
    AlertDialog(
      onDismissRequest = { showExitDialog = false },
      title = { Text("Leave Quiz?") },
      text = { Text("Your current quiz session answers will not be saved if you exit now.") },
      confirmButton = {
        TextButton(onClick = {
          showExitDialog = false
          onExit()
        }) {
          Text("Leave", color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { showExitDialog = false }) {
          Text("Continue Quiz")
        }
      }
    )
  }

  // Confirm Submit Dialog
  if (showFinishConfirmDialog) {
    val answeredCount = state.selectedAnswers.size
    val totalCount = state.questions.size

    AlertDialog(
      onDismissRequest = { showFinishConfirmDialog = false },
      title = { Text("Submit Quiz") },
      text = {
        Text("You have answered $answeredCount out of $totalCount questions. Are you ready to submit and calculate your score?")
      },
      confirmButton = {
        Button(
          onClick = {
            showFinishConfirmDialog = false
            onFinishQuiz()
          },
          colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
        ) {
          Text("Confirm Submit")
        }
      },
      dismissButton = {
        TextButton(onClick = { showFinishConfirmDialog = false }) {
          Text("Review First")
        }
      }
    )
  }
}

@Composable
fun QuizResultScreen(
  attempt: QuizAttemptEntity,
  onReviewAnswers: () -> Unit,
  onRetakeQuiz: () -> Unit,
  onReturnToCourse: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(24.dp)
      .testTag("quiz_result_screen"),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    // Score Badge
    Box(
      modifier = Modifier
        .size(130.dp)
        .clip(CircleShape)
        .background(
          if (attempt.scorePercent >= 70) SuccessGreen.copy(alpha = 0.15f) else ErrorRed.copy(alpha = 0.15f)
        ),
      contentAlignment = Alignment.Center
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = "${attempt.scorePercent}%",
          style = MaterialTheme.typography.headlineLarge.copy(
            fontWeight = FontWeight.ExtraBold,
            color = if (attempt.scorePercent >= 70) SuccessGreen else ErrorRed
          )
        )
        Text(
          text = if (attempt.scorePercent >= 70) "PASSED" else "NEEDS REVIEW",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
          color = if (attempt.scorePercent >= 70) SuccessGreen else ErrorRed
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text(
      text = "Quiz Completed!",
      style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onSurface
    )

    Text(
      text = attempt.courseTitle,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Results Summary Card
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(2.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        horizontalArrangement = Arrangement.SpaceAround
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("Correct", style = MaterialTheme.typography.labelMedium, color = SuccessGreen)
          Text("${attempt.correctCount}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("Incorrect", style = MaterialTheme.typography.labelMedium, color = ErrorRed)
          Text("${attempt.incorrectCount}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("Time", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text("${attempt.timeSpentSeconds}s", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        }
      }
    }

    Spacer(modifier = Modifier.height(28.dp))

    Button(
      onClick = onReviewAnswers,
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag("review_answers_button"),
      shape = RoundedCornerShape(12.dp)
    ) {
      Text("Review Detailed Answers & Explanations", style = MaterialTheme.typography.titleSmall)
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedButton(
      onClick = onRetakeQuiz,
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag("retake_quiz_button"),
      shape = RoundedCornerShape(12.dp)
    ) {
      Icon(Icons.Default.Refresh, contentDescription = null)
      Spacer(modifier = Modifier.width(6.dp))
      Text("Retake Quiz", style = MaterialTheme.typography.titleSmall)
    }

    Spacer(modifier = Modifier.height(12.dp))

    TextButton(
      onClick = onReturnToCourse,
      modifier = Modifier.fillMaxWidth()
    ) {
      Text("Return to Course")
    }
  }
}

@Composable
fun ReviewAnswersScreen(
  questions: List<QuestionEntity>,
  selectedAnswers: Map<String, String>,
  onBackClick: () -> Unit,
  onAskAiTutor: ((QuestionEntity, String?) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .testTag("review_answers_screen")
  ) {
    DetailTopBar(
      title = "Answer Explanations Review",
      subtitle = "${questions.size} Questions",
      onBackClick = onBackClick
    )

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      itemsIndexed(questions) { index, q ->
        val chosen = selectedAnswers[q.id]
        val isCorrect = chosen.equals(q.correctAnswer, ignoreCase = true)

        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(2.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Question ${index + 1}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
              )
              Surface(
                color = if (isCorrect) SuccessGreen.copy(alpha = 0.15f) else ErrorRed.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp)
              ) {
                Text(
                  text = if (isCorrect) "CORRECT" else "INCORRECT",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = if (isCorrect) SuccessGreen else ErrorRed,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
              text = q.questionText,
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
              color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Show choices in review mode
            QuestionOptionItem("A", q.optionA, chosen == "A", q.correctAnswer == "A", true, {})
            Spacer(modifier = Modifier.height(6.dp))
            QuestionOptionItem("B", q.optionB, chosen == "B", q.correctAnswer == "B", true, {})
            Spacer(modifier = Modifier.height(6.dp))
            QuestionOptionItem("C", q.optionC, chosen == "C", q.correctAnswer == "C", true, {})
            Spacer(modifier = Modifier.height(6.dp))
            QuestionOptionItem("D", q.optionD, chosen == "D", q.correctAnswer == "D", true, {})

            Spacer(modifier = Modifier.height(14.dp))

            // Detailed Explanation
            ExplanationCard(
              question = q,
              selectedAnswer = chosen,
              onAskAiTutor = { question -> onAskAiTutor?.invoke(question, chosen) }
            )
          }
        }
      }
    }
  }
}
