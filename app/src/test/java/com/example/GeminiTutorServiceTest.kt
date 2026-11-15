package com.example

import com.example.data.remote.GeminiTutorService
import com.example.data.remote.TutorContextPayload
import com.example.data.remote.TutorMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiTutorServiceTest {

  @Test
  fun tutorModes_mapToExpectedGeminiModelIds() {
    assertEquals("gemini-4-hydrogen", TutorMode.HYDROGEN.modelName)
    assertEquals("gemini-3.1-flash-lite", TutorMode.FLASH_LITE.modelName)
    assertEquals("gemini-4-argon", TutorMode.ARGON.modelName)
    assertEquals("gemini-4-carbon", TutorMode.CARBON.modelName)
  }

  @Test
  fun buildSystemInstruction_includesLiveAppContextWhenEnabled() {
    val payload = TutorContextPayload(
      studentName = "Abebe",
      department = "Engineering",
      dailyGoalMinutes = 60,
      streakDays = 5,
      questionsAnswered = 42,
      overallAccuracyPercent = 78,
      weakTopicsSummary = "Limits & Continuity (45% accuracy)",
      activeCourseCode = "MATH 101",
      activeCourseTitle = "Calculus I",
      activeCourseCategory = "Mathematics",
      activeCourseDescription = "Limits, derivatives, and integration.",
      courseTopicsSummary = "Limits, Derivatives",
      courseNotesSummary = "- Chapter 1: L'Hopital's Rule for 0/0",
      coursePastExamsSummary = "Midterm 2024",
      includeAppContext = true
    )

    val instruction = GeminiTutorService.buildSystemInstruction(TutorMode.ARGON, payload)

    assertTrue(instruction.contains("Interactive STEM Coach"))
    assertTrue(instruction.contains("Abebe"))
    assertTrue(instruction.contains("42 questions answered, 78% accuracy"))
    assertTrue(instruction.contains("Limits & Continuity (45% accuracy)"))
    assertTrue(instruction.contains("MATH 101 - Calculus I"))
    assertTrue(instruction.contains("L'Hopital's Rule"))
    assertTrue(instruction.contains("Midterm 2024"))
  }

  @Test
  fun buildSystemInstruction_omitsAppContextWhenDisabled() {
    val payload = TutorContextPayload(
      studentName = "Abebe",
      activeCourseCode = "MATH 101",
      activeCourseTitle = "Calculus I",
      courseNotesSummary = "- Secret Note",
      includeAppContext = false
    )

    val instruction = GeminiTutorService.buildSystemInstruction(TutorMode.FLASH_LITE, payload)

    assertTrue(instruction.contains("Rapid Study Coach"))
    assertFalse(instruction.contains("LIVE STUDENT CONTEXT"))
    assertFalse(instruction.contains("Calculus I"))
    assertFalse(instruction.contains("Secret Note"))
  }

  @Test
  fun buildSanitizedHistory_enforcesAlternatingUserModelPairs() {
    val rawHistory = listOf(
      "Welcome message" to false, // leading model turn -> should be dropped
      "   " to true,              // blank -> dropped
      "First question" to true,
      "Follow-up without reply" to true, // duplicate consecutive user -> skipped
      "First answer" to false,
      "Second answer" to false,          // duplicate consecutive model -> skipped
      "Pending question" to true         // trailing user turn -> dropped
    )

    val sanitized = GeminiTutorService.buildSanitizedHistory(rawHistory)

    assertEquals(2, sanitized.size)
    assertEquals("user", sanitized[0].role)
    assertEquals("model", sanitized[1].role)
  }
}
