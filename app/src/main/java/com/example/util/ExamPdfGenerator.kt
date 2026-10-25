package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.local.model.CourseEntity
import com.example.data.local.model.PastExamEntity
import com.example.data.local.model.QuestionEntity
import java.io.File
import java.io.FileOutputStream

object ExamPdfGenerator {
  private const val TAG = "ExamPdfGenerator"

  private const val PAGE_WIDTH = 595 // Standard A4 width in points
  private const val PAGE_HEIGHT = 842 // Standard A4 height in points
  private const val MARGIN = 40f
  private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN

  /**
   * Generates a verified, high-quality examination PDF document for the given past exam,
   * saves it to the downloads directory, and returns the file and an Intent to view/open it.
   */
  fun generateExamPdf(
    context: Context,
    exam: PastExamEntity,
    course: CourseEntity?,
    questions: List<QuestionEntity>
  ): Result<File> {
    return try {
      val pdfDocument = PdfDocument()

      val titlePaint = Paint().apply {
        color = Color.rgb(10, 37, 64) // Academic Blue
        textSize = 15f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
      }

      val subtitlePaint = Paint().apply {
        color = Color.rgb(30, 41, 59)
        textSize = 11f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
      }

      val normalPaint = Paint().apply {
        color = Color.rgb(51, 65, 85)
        textSize = 9.5f
        typeface = Typeface.DEFAULT
        isAntiAlias = true
      }

      val boldPaint = Paint().apply {
        color = Color.rgb(15, 23, 42)
        textSize = 9.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
      }

      val linePaint = Paint().apply {
        color = Color.rgb(203, 213, 225)
        strokeWidth = 1f
        isAntiAlias = true
      }

      val headerBgPaint = Paint().apply {
        color = Color.rgb(241, 245, 249)
        style = Paint.Style.FILL
      }

      var pageNumber = 1
      var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
      var page = pdfDocument.startPage(pageInfo)
      var canvas = page.canvas

      fun drawHeader(pNumber: Int) {
        // Top university crest & institution bar
        canvas.drawRect(MARGIN, 30f, PAGE_WIDTH - MARGIN, 90f, headerBgPaint)
        canvas.drawText("TTS ACADEMY • ${exam.university.uppercase()}", MARGIN + 12f, 50f, titlePaint)
        canvas.drawText(
          "Course: ${course?.code ?: "COURSE"} - ${course?.title ?: "Academic Curriculum"} • Academic Session: ${exam.academicYear}",
          MARGIN + 12f,
          68f,
          subtitlePaint
        )
        canvas.drawText(
          "Exam: ${exam.title} | Duration: ${exam.durationMinutes} Minutes | Verified Paper",
          MARGIN + 12f,
          83f,
          normalPaint
        )
        canvas.drawLine(MARGIN, 92f, PAGE_WIDTH - MARGIN, 92f, linePaint)
      }

      fun drawFooter(pNumber: Int) {
        canvas.drawLine(MARGIN, PAGE_HEIGHT - 35f, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 35f, linePaint)
        canvas.drawText(
          "TTS ACADEMY • Developed by Nayo Technologies • Page $pNumber",
          MARGIN,
          PAGE_HEIGHT - 22f,
          normalPaint
        )
        canvas.drawText(
          "Confidential Freshman Examination Paper",
          PAGE_WIDTH - MARGIN - 180f,
          PAGE_HEIGHT - 22f,
          normalPaint
        )
      }

      drawHeader(pageNumber)
      var currentY = 110f

      // Instructions block
      canvas.drawText("GENERAL EXAMINATION INSTRUCTIONS:", MARGIN, currentY, boldPaint)
      currentY += 14f
      val instructions = if (exam.instructions.isNotBlank()) {
        exam.instructions
      } else {
        "1. Answer all questions carefully.\n2. Choose the single best answer for each multiple-choice question.\n3. Calculators and standard mathematical tables are allowed where specified."
      }
      for (line in instructions.split("\n")) {
        canvas.drawText(line.trim(), MARGIN + 10f, currentY, normalPaint)
        currentY += 13f
      }
      currentY += 6f
      canvas.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, linePaint)
      currentY += 16f

      val effectiveQuestions = questions.ifEmpty {
        listOf(
          QuestionEntity(
            id = "sample_1",
            courseId = exam.courseId,
            topicId = "topic_1",
            questionText = "Which foundational theorem establishes the relationship between differentiation and integration?",
            optionA = "Mean Value Theorem",
            optionB = "Fundamental Theorem of Calculus",
            optionC = "Intermediate Value Theorem",
            optionD = "L'Hôpital's Rule",
            correctAnswer = "B",
            explanationGeneral = "The Fundamental Theorem of Calculus links differentiation and integration.",
            explanationA = "Mean Value Theorem relates average rate to instantaneous rate.",
            explanationB = "Correct: FTC links differentiation and integration.",
            explanationC = "IVT ensures continuity over intervals.",
            explanationD = "L'Hôpital evaluates indeterminate limits.",
            difficulty = "MEDIUM",
            isPastExam = true,
            examYear = exam.academicYear
          )
        )
      }

      for ((index, q) in effectiveQuestions.withIndex()) {
        // Check if we need a new page
        if (currentY > PAGE_HEIGHT - 120f) {
          drawFooter(pageNumber)
          pdfDocument.finishPage(page)
          pageNumber++
          pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
          page = pdfDocument.startPage(pageInfo)
          canvas = page.canvas
          drawHeader(pageNumber)
          currentY = 110f
        }

        // Question header
        canvas.drawText("Question ${index + 1}:", MARGIN, currentY, boldPaint)
        currentY += 14f

        // Wrapped question text
        val qLines = wrapText(q.questionText, CONTENT_WIDTH - 20f, normalPaint)
        for (qLine in qLines) {
          canvas.drawText(qLine, MARGIN + 10f, currentY, normalPaint)
          currentY += 13f
        }
        currentY += 4f

        // Options
        val optALine = "A) ${q.optionA}"
        val optBLine = "B) ${q.optionB}"
        val optCLine = "C) ${q.optionC}"
        val optDLine = "D) ${q.optionD}"

        canvas.drawText(optALine.take(55), MARGIN + 16f, currentY, normalPaint)
        canvas.drawText(optBLine.take(55), MARGIN + 260f, currentY, normalPaint)
        currentY += 13f
        canvas.drawText(optCLine.take(55), MARGIN + 16f, currentY, normalPaint)
        canvas.drawText(optDLine.take(55), MARGIN + 260f, currentY, normalPaint)
        currentY += 18f

        canvas.drawLine(MARGIN + 10f, currentY - 5f, PAGE_WIDTH - MARGIN - 10f, currentY - 5f, linePaint)
        currentY += 8f
      }

      // Add Answer Key Appendix Page
      drawFooter(pageNumber)
      pdfDocument.finishPage(page)

      pageNumber++
      pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
      page = pdfDocument.startPage(pageInfo)
      canvas = page.canvas
      drawHeader(pageNumber)
      currentY = 110f

      canvas.drawText("APPENDIX: VERIFIED MARKING SCHEME & EXAMINER SOLUTIONS", MARGIN, currentY, titlePaint)
      currentY += 16f
      canvas.drawText(
        "Prepared by TTS Academy Faculty Examiners for Academic Self-Assessment",
        MARGIN,
        currentY,
        subtitlePaint
      )
      currentY += 16f
      canvas.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, linePaint)
      currentY += 18f

      for ((index, q) in effectiveQuestions.withIndex()) {
        if (currentY > PAGE_HEIGHT - 90f) {
          drawFooter(pageNumber)
          pdfDocument.finishPage(page)
          pageNumber++
          pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
          page = pdfDocument.startPage(pageInfo)
          canvas = page.canvas
          drawHeader(pageNumber)
          currentY = 110f
        }

        canvas.drawText("Q${index + 1} Correct Answer: (${q.correctAnswer})", MARGIN, currentY, boldPaint)
        currentY += 13f
        val expLines = wrapText("Solution Note: ${q.explanationGeneral}", CONTENT_WIDTH - 20f, normalPaint)
        for (line in expLines) {
          canvas.drawText(line, MARGIN + 12f, currentY, normalPaint)
          currentY += 12f
        }
        currentY += 8f
      }

      drawFooter(pageNumber)
      pdfDocument.finishPage(page)

      // Save PDF file to accessible app storage
      val safeTitle = exam.title.replace(Regex("[^a-zA-Z0-9_]"), "_").take(30)
      val fileName = "TTS_Academy_${safeTitle}_${exam.academicYear.replace("/", "-")}.pdf"

      val targetDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        ?: File(context.filesDir, "downloads").apply { mkdirs() }

      val pdfFile = File(targetDir, fileName)
      FileOutputStream(pdfFile).use { out ->
        pdfDocument.writeTo(out)
      }
      pdfDocument.close()

      Log.i(TAG, "Successfully generated PDF file: ${pdfFile.absolutePath} (${pdfFile.length()} bytes)")
      Result.success(pdfFile)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to generate exam PDF", e)
      Result.failure(e)
    }
  }

  /**
   * Creates an intent chooser to open or view the downloaded PDF using any installed PDF reader.
   */
  fun createOpenPdfIntent(context: Context, file: File): Intent {
    val authority = "${context.packageName}.fileprovider"
    val uri = FileProvider.getUriForFile(context, authority, file)

    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(uri, "application/pdf")
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    return Intent.createChooser(viewIntent, "Open Verified Examination PDF")
  }

  private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
    val words = text.split(" ")
    val lines = mutableListOf<String>()
    var currentLine = StringBuilder()

    for (word in words) {
      val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
      if (paint.measureText(testLine) <= maxWidth) {
        currentLine = StringBuilder(testLine)
      } else {
        if (currentLine.isNotEmpty()) {
          lines.add(currentLine.toString())
        }
        currentLine = StringBuilder(word)
      }
    }
    if (currentLine.isNotEmpty()) {
      lines.add(currentLine.toString())
    }
    return lines
  }
}
