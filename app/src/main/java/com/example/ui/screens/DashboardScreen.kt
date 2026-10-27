package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.model.AnnouncementEntity
import com.example.data.local.model.CourseEntity
import com.example.data.local.model.QuizAttemptEntity
import com.example.data.local.model.UserEntity
import com.example.ui.theme.AcademicBluePrimary
import com.example.ui.theme.AcademicCobalt
import com.example.ui.theme.AcademicGold
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.GradientDeepNavy
import com.example.ui.theme.GradientOcean
import com.example.ui.theme.GradientTealNavy
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.Screen

@Composable
fun DashboardScreen(
  user: UserEntity?,
  courses: List<CourseEntity>,
  announcements: List<AnnouncementEntity>,
  attempts: List<QuizAttemptEntity> = emptyList(),
  onCourseClick: (CourseEntity) -> Unit,
  onNavigate: (Screen) -> Unit,
  onStartQuickPractice: (CourseEntity) -> Unit,
  onAnnouncementsClick: () -> Unit = { onNavigate(Screen.ANNOUNCEMENTS) },
  isUnlocked: Boolean = true,
  onUnlockClick: () -> Unit = {},
  isOnline: Boolean = true,
  isCourseDownloaded: ((CourseEntity) -> Boolean)? = null,
  modifier: Modifier = Modifier
) {
  val totalFromAttempts = attempts.sumOf { it.totalQuestions }
  val correctFromAttempts = attempts.sumOf { it.correctCount }
  val questionsAnswered = totalFromAttempts.coerceAtLeast(user?.questionsAnsweredCount ?: 0)
  val correctAnswers = correctFromAttempts.coerceAtLeast(user?.correctAnswersCount ?: 0)
  val accuracy = if (questionsAnswered > 0) (correctAnswers * 100) / questionsAnswered else 0
  val streak = user?.streakDays ?: 1
  val recentCourse = courses.firstOrNull()
  val totalQuizzes = attempts.size

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .testTag("dashboard_screen"),
    contentPadding = PaddingValues(bottom = 32.dp)
  ) {
    // 1. Premium Banking-Style Header (Deep Navy Gradient + Floating Academic Card + Quick Actions)
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(
            Brush.verticalGradient(
              colors = listOf(
                GradientDeepNavy,
                GradientOcean,
                GradientTealNavy
              )
            )
          )
          .padding(horizontal = 20.dp, vertical = 22.dp)
      ) {
        Column {
          // Profile & Greeting Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              // Squircle Avatar with border
              Surface(
                modifier = Modifier
                  .size(46.dp)
                  .clip(RoundedCornerShape(14.dp)),
                color = Color.White.copy(alpha = 0.15f),
                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.35f))
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = user?.name?.firstOrNull()?.uppercase() ?: "S",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                  )
                }
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Welcome ${user?.name?.split(" ")?.firstOrNull() ?: "Student"}!",
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                  color = Color.White.copy(alpha = 0.75f)
                )
                Text(
                  text = "Let's See Your Statistics",
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.2.sp
                  ),
                  color = Color.White
                )
              }
            }

            // Streak Badge
            Surface(
              color = Color.White.copy(alpha = 0.12f),
              shape = RoundedCornerShape(20.dp),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.LocalFireDepartment,
                  contentDescription = "Streak",
                  tint = AcademicGold,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "$streak d",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = Color.White
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(18.dp))

          // Floating "Physical Card" Style Academic Statistics Card
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(24.dp)),
            color = Color.White.copy(alpha = 0.08f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.18f))
          ) {
            Column(
              modifier = Modifier.padding(18.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
              ) {
                Column {
                  Text(
                    text = "ACADEMIC SCORE",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      letterSpacing = 1.sp
                    ),
                    color = Color.White.copy(alpha = 0.65f)
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "$accuracy%",
                    style = MaterialTheme.typography.headlineMedium.copy(
                      fontWeight = FontWeight.Black
                    ),
                    color = Color.White
                  )
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = "▲ $questionsAnswered Questions",
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                      color = SuccessGreen
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "• $totalQuizzes Quizzes",
                      style = MaterialTheme.typography.labelSmall,
                      color = Color.White.copy(alpha = 0.6f)
                    )
                  }
                }

                // Mini Sparkline Graph representing the student's progress
                Box(
                  modifier = Modifier
                    .width(120.dp)
                    .height(64.dp)
                ) {
                  AcademicSparklineChart(
                    modifier = Modifier.fillMaxSize()
                  )
                }
              }

              Spacer(modifier = Modifier.height(16.dp))

              // Card bottom details bar
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Surface(
                  color = Color.White.copy(alpha = 0.12f),
                  shape = RoundedCornerShape(8.dp)
                ) {
                  Text(
                    text = user?.department ?: "University Freshman",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = AccentCyan,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }

                Text(
                  text = if (isUnlocked) "UNLOCKED PASS" else "FREE TIER",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                  ),
                  color = if (isUnlocked) AcademicGold else Color.White.copy(alpha = 0.7f)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Quick Action Pills Row (Matching the "Send" pill style from reference image)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            HeaderQuickPill(
              icon = Icons.Default.School,
              label = "Courses",
              onClick = { onNavigate(Screen.COURSES) },
              modifier = Modifier.weight(1f)
            )
            HeaderQuickPill(
              icon = Icons.AutoMirrored.Filled.Assignment,
              label = "Exams",
              onClick = { onNavigate(Screen.PAST_EXAMS) },
              modifier = Modifier.weight(1f)
            )
            HeaderQuickPill(
              icon = Icons.Default.AutoAwesome,
              label = "AI Tutor",
              onClick = { onNavigate(Screen.AI_TUTOR) },
              accent = AcademicGold,
              modifier = Modifier.weight(1f)
            )
            HeaderQuickPill(
              icon = Icons.Default.PlayArrow,
              label = "Practice",
              onClick = {
                if (recentCourse != null) onStartQuickPractice(recentCourse)
                else onNavigate(Screen.COURSES)
              },
              modifier = Modifier.weight(1f)
            )
          }
        }
      }
    }

    // 2. High-Contrast Lower Section with 32.dp Rounded Top
    // Administrator Portal Quick Action Banner (if admin)
    if (user?.role == "ADMIN") {
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)),
          border = BorderStroke(1.dp, AcademicGold.copy(alpha = 0.6f)),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onNavigate(Screen.ADMIN_PORTAL) }
            .testTag("dashboard_admin_portal_banner")
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AcademicGold.copy(alpha = 0.2f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.AdminPanelSettings,
                contentDescription = "Admin Portal",
                tint = AcademicGold,
                modifier = Modifier.size(24.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Administrator Authorization Active",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
              Text(
                text = "Full curriculum control, courses, questions & user roles.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
              )
            }
            Button(
              onClick = { onNavigate(Screen.ADMIN_PORTAL) },
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
              Text("Manage", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
          }
        }
      }
    }

    // Locked Materials Warning Banner (if locked)
    if (!isUnlocked) {
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("dashboard_locked_resources_banner")
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(22.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Materials Locked",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Enter your 6-digit access code for full lecture notes & exams.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Button(
              onClick = onUnlockClick,
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = AcademicCobalt)
            ) {
              Text("Unlock", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
          }
        }
      }
    }

    // Urgent Announcements Bar
    if (announcements.isNotEmpty()) {
      item {
        val latestAnn = announcements.first()
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onAnnouncementsClick() },
          color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f))
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Campaign,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.tertiary,
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = latestAnn.title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = latestAnn.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = "View",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }
    }

    // 3. Continue Learning / Active Course Hero Card
    if (recentCourse != null) {
      item {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Continue Learning",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onBackground
            )
            Text(
              text = "View All >",
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
              ),
              modifier = Modifier.clickable { onNavigate(Screen.COURSES) }
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          Card(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onCourseClick(recentCourse) },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(2.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
          ) {
            Column(modifier = Modifier.padding(18.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Surface(
                  color = MaterialTheme.colorScheme.primaryContainer,
                  shape = RoundedCornerShape(8.dp)
                ) {
                  Text(
                    text = recentCourse.code,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                  )
                }
                if (isCourseDownloaded?.invoke(recentCourse) == true) {
                  Surface(
                    color = SuccessGreen.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(12.dp))
                      Spacer(modifier = Modifier.width(3.dp))
                      Text(
                        text = "Offline Ready",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = SuccessGreen
                      )
                    }
                  }
                } else {
                  Text(
                    text = "${recentCourse.category} • Freshmen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              Text(
                text = recentCourse.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )

              Text(
                text = recentCourse.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )

              Spacer(modifier = Modifier.height(14.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "${recentCourse.totalQuestions} Questions • University Verified",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                  color = MaterialTheme.colorScheme.primary
                )

                Button(
                  onClick = { onStartQuickPractice(recentCourse) },
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                  shape = RoundedCornerShape(12.dp),
                  contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Practice Now", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
              }
            }
          }
        }
      }
    }

    // 4. Two Modern Feature Tiles (Bills Due / Service cards inspiration from reference)
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Study Goal / Schedule Tile
        Card(
          modifier = Modifier
            .weight(1f)
            .clickable { onNavigate(Screen.PROFILE) },
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(2.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(AcademicGold.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = AcademicGold,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "Daily Target",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "${user?.studyGoalMinutesPerDay ?: 45} mins / day",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Notes & Formulas Tile
        Card(
          modifier = Modifier
            .weight(1f)
            .clickable { onNavigate(Screen.NOTES) },
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(2.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(AcademicCobalt.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = AcademicCobalt,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "Revision Notes",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Formulas & Summary",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }

    // 5. Freshman Courses Carousel
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Freshman Courses",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onBackground
        )
        Text(
          text = "See All (${courses.size})",
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
          ),
          modifier = Modifier.clickable { onNavigate(Screen.COURSES) }
        )
      }
    }

    item {
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(courses) { course ->
          FreshmanCourseCard(
            course = course,
            onClick = { onCourseClick(course) }
          )
        }
      }
    }
  }
}

@Composable
private fun HeaderQuickPill(
  icon: ImageVector,
  label: String,
  onClick: () -> Unit,
  accent: Color = Color.White,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier
      .clip(RoundedCornerShape(20.dp))
      .clickable(onClick = onClick),
    color = Color.White.copy(alpha = 0.12f),
    shape = RoundedCornerShape(20.dp),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = accent,
        modifier = Modifier.size(15.dp)
      )
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp
        ),
        color = Color.White,
        maxLines = 1
      )
    }
  }
}

@Composable
private fun AcademicSparklineChart(
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier) {
    val width = size.width
    val height = size.height

    val path = Path().apply {
      moveTo(0f, height * 0.75f)
      cubicTo(
        width * 0.25f, height * 0.9f,
        width * 0.35f, height * 0.4f,
        width * 0.5f, height * 0.55f
      )
      cubicTo(
        width * 0.65f, height * 0.7f,
        width * 0.75f, height * 0.1f,
        width * 0.85f, height * 0.2f
      )
      lineTo(width, height * 0.35f)
    }

    // Glowing cyan stroke
    drawPath(
      path = path,
      color = Color(0xFF7DD3FC),
      style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
    )

    // Current peak highlight dot
    drawCircle(
      color = Color.White,
      radius = 4.dp.toPx(),
      center = androidx.compose.ui.geometry.Offset(width * 0.75f, height * 0.1f)
    )
  }
}

@Composable
fun FreshmanCourseCard(
  course: CourseEntity,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .width(220.dp)
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(2.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text(
            text = course.code,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
          )
        }
        Text(
          text = course.category,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = course.title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      Text(
        text = course.description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "${course.totalQuestions} Questions",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.primary
        )
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
