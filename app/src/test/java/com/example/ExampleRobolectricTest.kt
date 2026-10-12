package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SeedData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("TTS Academy", appName)
  }

  @Test
  fun `verify freshman curriculum seed courses count`() {
    val courses = SeedData.getCourses()
    assertTrue("Curriculum should have 11 to 13 courses", courses.size in 11..13)
    val math = courses.find { it.code == "MATH 101" }
    assertNotNull("MATH 101 should exist", math)
    assertEquals("Calculus I", math?.title)
  }

  @Test
  fun `verify question structure with choice explanations`() {
    val questions = SeedData.getQuestions()
    assertTrue("Questions list should not be empty", questions.isNotEmpty())
    questions.forEach { q ->
      assertTrue("Question text should not be empty", q.questionText.isNotBlank())
      assertTrue("Option A should not be empty", q.optionA.isNotBlank())
      assertTrue("Option B should not be empty", q.optionB.isNotBlank())
      assertTrue("Option C should not be empty", q.optionC.isNotBlank())
      assertTrue("Option D should not be empty", q.optionD.isNotBlank())
      assertTrue("Correct answer should be A, B, C, or D", q.correctAnswer in listOf("A", "B", "C", "D"))
      assertTrue("General explanation must be present", q.explanationGeneral.isNotBlank())
      assertTrue("Explanation for A must be present", q.explanationA.isNotBlank())
      assertTrue("Explanation for B must be present", q.explanationB.isNotBlank())
      assertTrue("Explanation for C must be present", q.explanationC.isNotBlank())
      assertTrue("Explanation for D must be present", q.explanationD.isNotBlank())
    }
  }

  @Test
  fun `verify admin account seed credentials`() {
    val users = SeedData.getDefaultUsers()
    val admin = users.find { it.email == "spikenarrd@gmail.com" }
    assertNotNull("Admin spikenarrd@gmail.com must exist", admin)
    assertEquals("ADMIN", admin?.role)
    assertEquals("Spike@6776", admin?.password)
  }
}
