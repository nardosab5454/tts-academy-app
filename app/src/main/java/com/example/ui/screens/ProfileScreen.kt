package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import android.content.Intent
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.data.local.model.AccessCodeEntity
import com.example.data.local.model.UserEntity
import com.example.ui.components.DetailTopBar
import com.example.ui.theme.AcademicAmberDark
import com.example.ui.theme.AcademicBlueDark
import com.example.ui.theme.AcademicBluePrimary
import com.example.ui.theme.AcademicCobalt
import com.example.ui.theme.AcademicGold
import com.example.ui.theme.SuccessGreen
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

private val ETHIOPIAN_UNIVERSITIES = listOf(
  "Addis Ababa University (AAU)",
  "ASTU (Adama)",
  "AASTU",
  "Bahir Dar Univ.",
  "Hawassa Univ.",
  "Jimma Univ.",
  "Haramaya Univ.",
  "Gondar Univ.",
  "Arba Minch Univ.",
  "Mekelle Univ."
)

private val TARGET_PRESETS = listOf(15, 30, 45, 60, 90, 120)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
  user: UserEntity?,
  allUsers: List<UserEntity>,
  currentTheme: String,
  accessCodes: List<AccessCodeEntity> = emptyList(),
  onGenerateCode: (note: String, format: String, customCode: String?, (String) -> Unit) -> Unit = { _, _, _, _ -> },
  onDeleteCode: (String) -> Unit = {},
  onThemeSelect: (String) -> Unit,
  onUpdateSettings: (department: String, dailyGoalMinutes: Int) -> Unit = { _, _ -> },
  onRedeemAccessCode: (String, (Boolean, String) -> Unit) -> Unit = { _, _ -> },
  onLogin: (String, String, (Boolean, String) -> Unit) -> Unit,
  onSwitchUser: (String) -> Unit,
  onOpenAdminPortal: () -> Unit,
  onBackClick: () -> Unit,
  onSignOut: () -> Unit = {},
  isOnline: Boolean = true,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  // Academic Settings Editing State
  var isEditingDepartment by remember { mutableStateOf(false) }
  var selectedDepartment by remember(user?.department) {
    mutableStateOf(user?.department?.ifBlank { "Addis Ababa University (AAU)" } ?: "Addis Ababa University (AAU)")
  }
  var customDeptInput by remember { mutableStateOf("") }

  var dailyGoalMinutes by remember(user?.studyGoalMinutesPerDay) {
    mutableIntStateOf(user?.studyGoalMinutesPerDay ?: 45)
  }
  var hasUnsavedChanges by remember { mutableStateOf(false) }

  // Access Code inline redemption & Admin generation
  val clipboardManager = LocalClipboardManager.current
  var showRedeemInput by remember { mutableStateOf(false) }
  var accessCodeInput by remember { mutableStateOf("") }
  var isRedeemingCode by remember { mutableStateOf(false) }
  var redeemFeedbackMessage by remember { mutableStateOf<String?>(null) }
  var isRedeemSuccess by remember { mutableStateOf(false) }

  // Admin Code Dispatcher States
  var lastAdminGeneratedCode by remember { mutableStateOf<String?>(null) }
  var isCodeCopied by remember { mutableStateOf(false) }
  var isGeneratingCode by remember { mutableStateOf(false) }
  var showAllAdminCodes by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("profile_screen"),
    contentPadding = PaddingValues(bottom = 36.dp)
  ) {
    item {
      DetailTopBar(
        title = "Student Profile & Settings",
        onBackClick = onBackClick
      )
    }

    // 1. World-Class Modern Hero Banner & Avatar Card
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          // Curved Top Banner with Gradient
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(110.dp)
              .background(
                Brush.horizontalGradient(
                  colors = listOf(AcademicBluePrimary, AcademicCobalt)
                )
              ),
            contentAlignment = Alignment.TopEnd
          ) {
            // Subtle decorative circles
            Box(
              modifier = Modifier
                .size(100.dp)
                .padding(top = 10.dp, end = 10.dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape)
            )
          }

          // Avatar overlapping the banner
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 20.dp)
              .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Box(
              modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .padding(4.dp)
            ) {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .clip(CircleShape)
                  .background(
                    Brush.linearGradient(
                      colors = listOf(AcademicCobalt, AcademicBluePrimary)
                    )
                  ),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = user?.name?.firstOrNull()?.uppercase() ?: "S",
                  style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White
                  )
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
              text = user?.name ?: "Freshman Student",
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
              ),
              color = MaterialTheme.colorScheme.onSurface,
              textAlign = TextAlign.Center
            )

            Text(
              text = user?.email ?: "student@ttsacademy.edu",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Role & Status Badges
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Role Chip
              Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = if (user?.role == "ADMIN") Icons.Default.AdminPanelSettings else Icons.Default.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = if (user?.role == "ADMIN") "ADMINISTRATOR" else "FRESHMAN STUDENT",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                  )
                }
              }

              // Premium Status Chip
              val isPremium = user?.isPremium == true || user?.role == "ADMIN"
              Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isPremium) AcademicGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = if (isPremium) Icons.Default.Verified else Icons.Default.Key,
                    contentDescription = null,
                    tint = if (isPremium) AcademicGold else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = if (isPremium) "PREMIUM ACCESS" else "STANDARD ACCESS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isPremium) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }
        }
      }
    }

    // 2. Interactive Study Statistics Summary
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Streak
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = AcademicGold,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "${user?.streakDays ?: 1}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
            }
            Text("Day Streak", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }

          Box(
            modifier = Modifier
              .height(30.dp)
              .width(1.dp)
              .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
          )

          // Questions Answered
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "${user?.questionsAnsweredCount ?: 0}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
            }
            Text("Answered", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }

          Box(
            modifier = Modifier
              .height(30.dp)
              .width(1.dp)
              .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
          )

          // Daily Target
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = AcademicCobalt,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "${user?.studyGoalMinutesPerDay ?: 45}m",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
            }
            Text("Daily Goal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }
    }

    // 3. EDITABLE DEPARTMENT & DAILY STUDY TARGET (USER'S EXPLICIT REQUEST)
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                shape = CircleShape,
                color = AcademicBluePrimary.copy(alpha = 0.12f),
                modifier = Modifier.size(36.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = AcademicBluePrimary,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Academic University & Goals",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Customize your university and daily study target",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
          Spacer(modifier = Modifier.height(14.dp))

          // --- Feature A: University Customization ---
          Text(
            text = "Your University / Institution:",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(6.dp))

          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { isEditingDepartment = !isEditingDepartment }
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
              ) {
                Icon(
                  imageVector = Icons.Default.School,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = selectedDepartment,
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit University",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
              )
            }
          }

          // Expanded University Selector
          AnimatedVisibility(visible = isEditingDepartment) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
            ) {
              Text(
                text = "Select from Ethiopian universities or type custom:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(8.dp))

              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                ETHIOPIAN_UNIVERSITIES.forEach { uni ->
                  val isSelected = selectedDepartment == uni
                  Surface(
                    onClick = {
                      selectedDepartment = uni
                      hasUnsavedChanges = true
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                  ) {
                    Text(
                      text = uni,
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                      color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              // Custom University Input
              OutlinedTextField(
                value = customDeptInput,
                onValueChange = {
                  customDeptInput = it
                  if (it.isNotBlank()) {
                    selectedDepartment = it
                    hasUnsavedChanges = true
                  }
                },
                placeholder = { Text("Type custom university name...") },
                label = { Text("Other University") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("custom_department_input"),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = MaterialTheme.colorScheme.primary
                )
              )
            }
          }

          Spacer(modifier = Modifier.height(18.dp))

          // --- Feature B: Daily Study Target Customization ---
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Daily Study Target:",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = AcademicCobalt.copy(alpha = 0.15f)
            ) {
              Text(
                text = "$dailyGoalMinutes mins / day",
                style = MaterialTheme.typography.titleSmall.copy(
                  fontWeight = FontWeight.Bold,
                  color = AcademicCobalt
                ),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Target Quick Preset Chips
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            TARGET_PRESETS.forEach { preset ->
              val isSelected = dailyGoalMinutes == preset
              Surface(
                onClick = {
                  dailyGoalMinutes = preset
                  hasUnsavedChanges = true
                },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) AcademicCobalt else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.weight(1f)
              ) {
                Text(
                  text = "${preset}m",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                  color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.padding(vertical = 8.dp),
                  textAlign = TextAlign.Center
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Stepper + Slider
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            IconButton(
              onClick = {
                if (dailyGoalMinutes > 10) {
                  dailyGoalMinutes -= 5
                  hasUnsavedChanges = true
                }
              },
              modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), CircleShape)
            ) {
              Icon(Icons.Default.Remove, contentDescription = "Decrease Goal", modifier = Modifier.size(18.dp))
            }

            Slider(
              value = dailyGoalMinutes.toFloat(),
              onValueChange = {
                dailyGoalMinutes = it.toInt()
                hasUnsavedChanges = true
              },
              valueRange = 10f..180f,
              steps = 33, // 5 min increments
              modifier = Modifier.weight(1f),
              colors = SliderDefaults.colors(
                thumbColor = AcademicCobalt,
                activeTrackColor = AcademicCobalt
              )
            )

            IconButton(
              onClick = {
                if (dailyGoalMinutes < 180) {
                  dailyGoalMinutes += 5
                  hasUnsavedChanges = true
                }
              },
              modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), CircleShape)
            ) {
              Icon(Icons.Default.Add, contentDescription = "Increase Goal", modifier = Modifier.size(18.dp))
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = when {
              dailyGoalMinutes < 30 -> "💡 Light revision: Suitable for busy class days."
              dailyGoalMinutes in 30..60 -> "⭐ Recommended for steady semester mastery and high quiz scores."
              else -> "🔥 Intensive preparation: Excellent for upcoming exam weeks!"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(16.dp))

          // Save Changes Button
          Button(
            onClick = {
              onUpdateSettings(selectedDepartment, dailyGoalMinutes)
              hasUnsavedChanges = false
              isEditingDepartment = false
            },
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("save_academic_settings_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = AcademicBluePrimary,
              contentColor = Color.White
            )
          ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (hasUnsavedChanges) "Save Academic Profile Changes" else "Settings Saved",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
          }
        }
      }
    }

    // 4. Premium Pass & 6-Digit Code Redemption (Admin Key Dispatcher)
    if (user?.role == "ADMIN") {
      item {
        val isAdmin = true
        val isPremium = true
        val activeCodes = accessCodes.filter { !it.isRedeemed && System.currentTimeMillis() <= it.expiresAt }

      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp)
          .testTag("premium_membership_key_box")
          .clickable {
            if (isAdmin && !isGeneratingCode) {
              isGeneratingCode = true
              onGenerateCode("Permanent Academic Access Pass", "NUMERIC", null) { code ->
                isGeneratingCode = false
                lastAdminGeneratedCode = code
                isCodeCopied = false
              }
            }
          },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          // Header Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.VpnKey,
                contentDescription = "Key Generator",
                tint = if (isAdmin) AcademicGold else if (isPremium) SuccessGreen else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Premium Membership & Key",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
                if (isAdmin) {
                  Text(
                    text = "Tap box to get a 6-digit single-use pass",
                    style = MaterialTheme.typography.bodySmall,
                    color = AcademicCobalt,
                    fontWeight = FontWeight.SemiBold
                  )
                }
              }
            }

            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (isAdmin) AcademicGold.copy(alpha = 0.18f) else if (isPremium) SuccessGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            ) {
              Text(
                text = if (isAdmin) "ADMIN KEY ENGINE" else if (isPremium) "ACTIVE" else "UNVERIFIED",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isAdmin) AcademicGold else if (isPremium) SuccessGreen else MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // ADMIN MODE: One-Click 6-Digit Code Generation
          if (isAdmin) {
            Text(
              text = "As an administrator, clicking this box generates a secure 6-digit access code for student enrollment. This code unlocks all university freshman courses, past exams, and revision notes permanently on the student's device.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Primary Click/Generate Button
            Button(
              onClick = {
                if (!isGeneratingCode) {
                  isGeneratingCode = true
                  onGenerateCode("Permanent Academic Access Pass", "NUMERIC", null) { code ->
                    isGeneratingCode = false
                    lastAdminGeneratedCode = code
                    isCodeCopied = false
                  }
                }
              },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("admin_generate_key_button"),
              colors = ButtonDefaults.buttonColors(containerColor = AcademicCobalt)
            ) {
              if (isGeneratingCode) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generating 6-Digit Pass...")
              } else {
                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Get 6-Digit Student Access Pass", fontWeight = FontWeight.Bold)
              }
            }

            // Display Last Generated Code Box
            if (lastAdminGeneratedCode != null) {
              Spacer(modifier = Modifier.height(14.dp))
              Surface(
                color = AcademicCobalt.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, AcademicCobalt.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(
                  modifier = Modifier.padding(16.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "6-DIGIT PASSCODE READY",
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                      color = AcademicCobalt
                    )
                    Surface(
                      color = AcademicGold.copy(alpha = 0.2f),
                      shape = RoundedCornerShape(6.dp)
                    ) {
                      Text(
                        text = "PERMANENT PASS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        color = AcademicAmberDark,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(8.dp))

                  // Big 6-Digit Code
                  Text(
                    text = lastAdminGeneratedCode ?: "",
                    style = MaterialTheme.typography.headlineLarge.copy(
                      fontWeight = FontWeight.Black,
                      fontFamily = FontFamily.Monospace,
                      letterSpacing = 6.sp
                    ),
                    color = AcademicBluePrimary,
                    textAlign = TextAlign.Center
                  )

                  Text(
                    text = "Single-use • One-time student login / pass",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )

                  Spacer(modifier = Modifier.height(12.dp))

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Button(
                      onClick = {
                        clipboardManager.setText(AnnotatedString(lastAdminGeneratedCode ?: ""))
                        isCodeCopied = true
                      },
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier.weight(1f),
                      colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCodeCopied) SuccessGreen else MaterialTheme.colorScheme.primary
                      )
                    ) {
                      Icon(if (isCodeCopied) Icons.Default.Check else Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(if (isCodeCopied) "Copied!" else "Copy Code")
                    }

                    OutlinedButton(
                      onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                          type = "text/plain"
                          putExtra(Intent.EXTRA_SUBJECT, "TTS Academy Student Passcode")
                          putExtra(
                            Intent.EXTRA_TEXT,
                            "Your student access code for TTS Academy is: $lastAdminGeneratedCode\n\nThis single-use code unlocks all university academic materials permanently on your device."
                          )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Student Passcode"))
                      },
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier.weight(1f)
                    ) {
                      Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("Share")
                    }
                  }
                }
              }
            }

            // Active Codes Archive Toggle
            if (accessCodes.isNotEmpty()) {
              Spacer(modifier = Modifier.height(12.dp))
              HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
              Spacer(modifier = Modifier.height(8.dp))

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { showAllAdminCodes = !showAllAdminCodes },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Active Codes Vault (${activeCodes.size} active / ${accessCodes.size} total)",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = if (showAllAdminCodes) "Hide ▲" else "View All ▼",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = AcademicCobalt
                )
              }

              AnimatedVisibility(visible = showAllAdminCodes) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  accessCodes.take(10).forEach { item ->
                    val isExpired = System.currentTimeMillis() > item.expiresAt
                    val minsLeft = ((item.expiresAt - System.currentTimeMillis()) / 60000).coerceAtLeast(0)

                    Surface(
                      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Row(
                        modifier = Modifier
                          .fillMaxWidth()
                          .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Column(modifier = Modifier.weight(1f)) {
                          Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                              text = item.code,
                              style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp
                              ),
                              color = if (item.isRedeemed || isExpired) MaterialTheme.colorScheme.outline else AcademicBluePrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                              color = if (item.isRedeemed) MaterialTheme.colorScheme.surfaceVariant else if (isExpired) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f) else SuccessGreen.copy(alpha = 0.15f),
                              shape = RoundedCornerShape(4.dp)
                            ) {
                              Text(
                                text = if (item.isRedeemed) "REDEEMED" else if (isExpired) "EXPIRED" else "${minsLeft}m LEFT",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                color = if (item.isRedeemed) MaterialTheme.colorScheme.onSurfaceVariant else if (isExpired) MaterialTheme.colorScheme.error else SuccessGreen,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                              )
                            }
                          }
                          Text(
                            text = if (item.isRedeemed && !item.redeemedByEmail.isNullOrBlank()) "Used by: ${item.redeemedByEmail}" else item.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                          )
                        }

                        Row {
                          IconButton(
                            onClick = { clipboardManager.setText(AnnotatedString(item.code)) },
                            modifier = Modifier.size(32.dp)
                          ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                          }
                          IconButton(
                            onClick = { onDeleteCode(item.code) },
                            modifier = Modifier.size(32.dp)
                          ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  } else {
    // 4b. Student Academic Access Status (Simple status only, key box is admin only)
    item {
        val isUnlocked = user?.isPremium == true
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = if (isUnlocked) Icons.Default.CheckCircle else Icons.Default.Lock,
                  contentDescription = null,
                  tint = if (isUnlocked) SuccessGreen else MaterialTheme.colorScheme.error,
                  modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = "Academic Materials Access",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isUnlocked) SuccessGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
              ) {
                Text(
                  text = if (isUnlocked) "UNLOCKED" else "LOCKED",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = if (isUnlocked) SuccessGreen else MaterialTheme.colorScheme.error,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
              text = if (isUnlocked) {
                "Your student account has permanent unlocked access to all freshman university courses, question banks, past papers, and revision notes on this device. Even if you log out and log in again, your materials remain permanently unlocked."
              } else {
                "University past exams, official solutions, and lecture notes are currently locked on this device. When you open any locked paper or note, enter your 6-digit access code from your administrator to unlock all materials forever on this device."
              },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              lineHeight = 20.sp
            )
          }
        }
      }
    }

    // 5. Appearance & Theme Mode Selector
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Palette,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "App Theme",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Light, Dark, or System Match",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            ThemePillButton(
              icon = Icons.Default.LightMode,
              label = "Light",
              isSelected = currentTheme == "LIGHT",
              onClick = { onThemeSelect("LIGHT") }
            )
            ThemePillButton(
              icon = Icons.Default.DarkMode,
              label = "Dark",
              isSelected = currentTheme == "DARK",
              onClick = { onThemeSelect("DARK") }
            )
            ThemePillButton(
              icon = Icons.Default.PhoneAndroid,
              label = "System",
              isSelected = currentTheme == "SYSTEM",
              onClick = { onThemeSelect("SYSTEM") }
            )
          }
        }
      }
    }



    // 6. Fast Account Switcher (Strictly for Administrator use)
    if (user?.role == "ADMIN" && allUsers.size > 1) {
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Text(
              text = "Switch Account / Profile",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              allUsers.forEach { targetUser ->
                val isSelected = targetUser.id == user?.id
                Surface(
                  onClick = { onSwitchUser(targetUser.id) },
                  shape = RoundedCornerShape(12.dp),
                  color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Column {
                      Text(
                        text = targetUser.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                      )
                      Text(
                        text = "${targetUser.email} • ${targetUser.role}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }

                    if (isSelected) {
                      Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    // 7. Academic System Console (Visible to authenticated administrators)
    if (user?.role == "ADMIN") {
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onOpenAdminPortal() }
              .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Academic System Console",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                  text = "Manage courses, questions, access codes & users",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
              }
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          }
        }
      }
    }

    // 8. About the App & Developer (Nayo Technologies)
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "About TTS Academy",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "TTS Academy is university freshman course tutoring for Ethiopian students, built to help students conquer calculus, physics, computing, chemistry, biology, and general freshman courses.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
          )
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "App is Developed by Nayo Technologies",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
          )
          Text(
            text = "Dedicated to crafting world-class digital learning solutions for Ethiopian higher education.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    // 9. Sign Out Button
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              coroutineScope.launch {
                try {
                  CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
                } catch (_: Exception) {}
                FirebaseAuth.getInstance().signOut()
              }
              onSignOut()
            }
            .padding(18.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              Icons.AutoMirrored.Filled.Logout,
              contentDescription = "Sign Out",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Sign Out",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.error
              )
              Text(
                text = "Securely disconnect this device session",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ThemePillButton(
  icon: ImageVector,
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Surface(
    modifier = Modifier
      .clip(RoundedCornerShape(10.dp))
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(10.dp),
    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
  ) {
    Box(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(18.dp)
      )
    }
  }
}
