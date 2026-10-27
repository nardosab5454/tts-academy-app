package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.theme.AcademicAmberDark
import com.example.ui.theme.AcademicBlueDark
import com.example.ui.theme.AcademicBluePrimary
import com.example.ui.theme.AcademicCobalt
import com.example.ui.theme.AcademicGold
import com.example.ui.viewmodel.MainViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SignInScreen(
  viewModel: MainViewModel,
  onAuthSuccess: () -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedTab by remember { mutableIntStateOf(0) } // 0: Sign In, 1: Register
  var authErrorMessage by remember { mutableStateOf<String?>(null) }
  var isSubmitting by remember { mutableStateOf(false) }

  // Sign In inputs
  var loginEmail by remember { mutableStateOf("") }
  var loginPassword by remember { mutableStateOf("") }
  var isLoginPasswordVisible by remember { mutableStateOf(false) }

  // Register inputs
  var regName by remember { mutableStateOf("") }
  var regUniversity by remember { mutableStateOf("Addis Ababa University (AAU)") }
  var regEmail by remember { mutableStateOf("") }
  var regPassword by remember { mutableStateOf("") }
  var isRegPasswordVisible by remember { mutableStateOf(false) }

  Surface(
    modifier = modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .navigationBarsPadding()
    ) {
      // 1. World-Class Curved Gradient Header (Inspiration Styles 01 & 06)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
          .background(
            Brush.verticalGradient(
              colors = listOf(
                com.example.ui.theme.GradientDeepNavy,
                com.example.ui.theme.GradientOcean,
                com.example.ui.theme.GradientTealNavy
              )
            )
          )
          .statusBarsPadding()
          .padding(horizontal = 24.dp)
          .padding(top = 28.dp, bottom = 32.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          // Floating Glowing 3D Emblem Container
          Box(
            modifier = Modifier
              .size(76.dp)
              .clip(CircleShape)
              .background(Color.White.copy(alpha = 0.15f))
              .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.School,
              contentDescription = "TTS Academy Crest",
              tint = AcademicGold,
              modifier = Modifier.size(40.dp)
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = if (selectedTab == 0) "Welcome Back!" else "Create Account",
            style = MaterialTheme.typography.headlineMedium.copy(
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = 0.5.sp
            ),
            color = Color.White,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = if (selectedTab == 0) {
              "Sign in to access your freshman courses & exams"
            } else {
              "Join TTS Academy to start practicing & learning"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
          )
        }
      }

      // 2. Main Form Content Area
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp)
          .padding(top = 20.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Sleek Pill Mode Selector (Sign In vs Register)
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxSize()
              .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            // Sign In Tab
            Surface(
              onClick = {
                selectedTab = 0
                authErrorMessage = null
              },
              shape = RoundedCornerShape(20.dp),
              color = if (selectedTab == 0) MaterialTheme.colorScheme.surface else Color.Transparent,
              shadowElevation = if (selectedTab == 0) 2.dp else 0.dp,
              modifier = Modifier
                .weight(1f)
                .fillMaxSize()
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = "Sign In",
                  style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                  ),
                  color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            // Register Tab
            Surface(
              onClick = {
                selectedTab = 1
                authErrorMessage = null
              },
              shape = RoundedCornerShape(20.dp),
              color = if (selectedTab == 1) MaterialTheme.colorScheme.surface else Color.Transparent,
              shadowElevation = if (selectedTab == 1) 2.dp else 0.dp,
              modifier = Modifier
                .weight(1f)
                .fillMaxSize()
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = "Register",
                  style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                  ),
                  color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Error Feedback Alert Banner
        AnimatedVisibility(visible = !authErrorMessage.isNullOrBlank()) {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 16.dp)
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                Icons.Default.ErrorOutline,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = authErrorMessage ?: "",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onErrorContainer
              )
            }
          }
        }

        // --- TAB 0: SIGN IN ---
        if (selectedTab == 0) {
          // Email Field with Soft Pill Container & Icon
          ModernInputField(
            value = loginEmail,
            onValueChange = {
              loginEmail = it
              authErrorMessage = null
            },
            placeholder = "Email Address",
            leadingIcon = Icons.Default.Email,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            testTag = "login_email_input"
          )

          Spacer(modifier = Modifier.height(14.dp))

          // Password Field with Eye Toggle
          ModernInputField(
            value = loginPassword,
            onValueChange = {
              loginPassword = it
              authErrorMessage = null
            },
            placeholder = "Password",
            leadingIcon = Icons.Default.Lock,
            isPassword = true,
            isPasswordVisible = isLoginPasswordVisible,
            onTogglePasswordVisibility = { isLoginPasswordVisible = !isLoginPasswordVisible },
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            testTag = "login_password_input"
          )

          Spacer(modifier = Modifier.height(22.dp))

          // Primary Gradient Pill CTA Button (54dp height)
          ModernGradientButton(
            text = "Sign In",
            isLoading = isSubmitting,
            onClick = {
              val email = loginEmail.trim()
              val password = loginPassword.trim()
              if (email.isBlank() || password.isBlank()) {
                authErrorMessage = "Please enter both email and password."
                return@ModernGradientButton
              }
              if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                authErrorMessage = "Please enter a valid email address."
                return@ModernGradientButton
              }
              isSubmitting = true
              authErrorMessage = null
              viewModel.login(email, password) { success, errorMsg ->
                isSubmitting = false
                if (success) {
                  onAuthSuccess()
                } else {
                  authErrorMessage = errorMsg
                }
              }
            },
            testTag = "sign_in_button"
          )

          Spacer(modifier = Modifier.height(20.dp))

          // "or continue with" divider
          ModernDivider(text = "or continue with")

          Spacer(modifier = Modifier.height(18.dp))

          // Google One-Tap Sign In Pill Button
          GoogleSignInButton(
            onAuthSuccess = onAuthSuccess,
            onAuthError = { authErrorMessage = it }
          )

          Spacer(modifier = Modifier.height(24.dp))

          // Switch Prompt
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "Don't have an account? ",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = "Sign up",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
              color = AcademicCobalt,
              modifier = Modifier.clickable {
                selectedTab = 1
                authErrorMessage = null
              }
            )
          }
        }

        // --- TAB 1: REGISTER ---
        if (selectedTab == 1) {
          ModernInputField(
            value = regName,
            onValueChange = {
              regName = it
              authErrorMessage = null
            },
            placeholder = "Full Name",
            leadingIcon = Icons.Default.Person,
            imeAction = ImeAction.Next,
            testTag = "register_name_input"
          )

          Spacer(modifier = Modifier.height(12.dp))

          Spacer(modifier = Modifier.height(12.dp))

          // University Field + Quick Selector Chips (Ethiopian Universities for Freshman)
          Column(modifier = Modifier.fillMaxWidth()) {
            ModernInputField(
              value = regUniversity,
              onValueChange = {
                regUniversity = it
                authErrorMessage = null
              },
              placeholder = "Your University (e.g. Addis Ababa University)",
              leadingIcon = Icons.Default.School,
              imeAction = ImeAction.Next,
              testTag = "register_university_input"
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
              text = "Select Ethiopian University:",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              ETHIOPIAN_UNIVERSITIES.forEach { uni ->
                val isSelected = regUniversity == uni
                Surface(
                  onClick = { regUniversity = uni },
                  shape = RoundedCornerShape(8.dp),
                  color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                  border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                  Text(
                    text = uni,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          ModernInputField(
            value = regEmail,
            onValueChange = {
              regEmail = it
              authErrorMessage = null
            },
            placeholder = "Email Address",
            leadingIcon = Icons.Default.Email,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            testTag = "register_email_input"
          )

          Spacer(modifier = Modifier.height(12.dp))

          ModernInputField(
            value = regPassword,
            onValueChange = {
              regPassword = it
              authErrorMessage = null
            },
            placeholder = "Create Password",
            leadingIcon = Icons.Default.Lock,
            isPassword = true,
            isPasswordVisible = isRegPasswordVisible,
            onTogglePasswordVisibility = { isRegPasswordVisible = !isRegPasswordVisible },
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            testTag = "register_password_input"
          )

          Spacer(modifier = Modifier.height(22.dp))

          // Primary Register Button
          ModernGradientButton(
            text = "Create Student Account",
            isLoading = isSubmitting,
            onClick = {
              val name = regName.trim()
              val email = regEmail.trim()
              val password = regPassword.trim()
              val university = regUniversity.trim()

              if (name.isBlank() || email.isBlank() || password.isBlank()) {
                authErrorMessage = "Please complete Name, Email, and Password."
                return@ModernGradientButton
              }
              if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                authErrorMessage = "Please enter a valid email address."
                return@ModernGradientButton
              }
              if (password.length < 6) {
                authErrorMessage = "Password must be at least 6 characters long."
                return@ModernGradientButton
              }
              isSubmitting = true
              authErrorMessage = null
              viewModel.register(
                name = name,
                email = email,
                password = password,
                studentId = "TTS-FRESHMAN",
                department = university.ifBlank { "Addis Ababa University (AAU)" }
              ) { success, errorMsg ->
                isSubmitting = false
                if (success) {
                  onAuthSuccess()
                } else {
                  authErrorMessage = errorMsg
                }
              }
            },
            testTag = "create_account_button"
          )

          Spacer(modifier = Modifier.height(20.dp))

          ModernDivider(text = "or continue with")

          Spacer(modifier = Modifier.height(18.dp))

          GoogleSignInButton(
            onAuthSuccess = onAuthSuccess,
            onAuthError = { authErrorMessage = it }
          )

          Spacer(modifier = Modifier.height(16.dp))

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "Already have an account? ",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = "Sign in",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
              color = AcademicCobalt,
              modifier = Modifier.clickable {
                selectedTab = 0
                authErrorMessage = null
              }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ModernInputField(
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
  leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
  isPassword: Boolean = false,
  isPasswordVisible: Boolean = false,
  onTogglePasswordVisibility: (() -> Unit)? = null,
  keyboardType: KeyboardType = KeyboardType.Text,
  imeAction: ImeAction = ImeAction.Next,
  testTag: String
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    placeholder = {
      Text(
        text = placeholder,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
      )
    },
    leadingIcon = {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        modifier = Modifier.size(34.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = leadingIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    },
    trailingIcon = if (isPassword && onTogglePasswordVisibility != null) {
      {
        IconButton(onClick = onTogglePasswordVisibility) {
          Icon(
            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
            contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
          )
        }
      }
    } else null,
    visualTransformation = if (isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
    singleLine = true,
    shape = RoundedCornerShape(18.dp),
    keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
    colors = OutlinedTextFieldDefaults.colors(
      focusedContainerColor = MaterialTheme.colorScheme.surface,
      unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
      focusedBorderColor = MaterialTheme.colorScheme.primary,
      unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    ),
    modifier = Modifier
      .fillMaxWidth()
      .testTag(testTag)
  )
}

@Composable
private fun ModernGradientButton(
  text: String,
  isLoading: Boolean,
  onClick: () -> Unit,
  testTag: String
) {
  Button(
    onClick = onClick,
    enabled = !isLoading,
    shape = RoundedCornerShape(18.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = Color.Transparent,
      contentColor = Color.White
    ),
    contentPadding = PaddingValues(0.dp),
    modifier = Modifier
      .fillMaxWidth()
      .height(54.dp)
      .clip(RoundedCornerShape(18.dp))
      .background(
        Brush.horizontalGradient(
          colors = listOf(AcademicCobalt, AcademicBluePrimary)
        )
      )
      .testTag(testTag)
  ) {
    if (isLoading) {
      CircularProgressIndicator(
        modifier = Modifier.size(22.dp),
        color = Color.White,
        strokeWidth = 2.dp
      )
    } else {
      Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.3.sp
        )
      )
    }
  }
}

@Composable
private fun ModernDivider(text: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
  ) {
    HorizontalDivider(
      modifier = Modifier.weight(1f),
      color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    )
    Text(
      text = "  $text  ",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    HorizontalDivider(
      modifier = Modifier.weight(1f),
      color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    )
  }
}

@Composable
fun GoogleSignInButton(
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }
  var isLoading by remember { mutableStateOf(false) }

  Surface(
    onClick = {
      isLoading = true
      onGoogleSignInClicked(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = {
          isLoading = false
          onAuthSuccess()
        },
        onAuthError = { errorMsg ->
          isLoading = false
          onAuthError(errorMsg)
        },
        scope = coroutineScope,
        onAuthCancelled = {
          isLoading = false
        }
      )
    },
    enabled = !isLoading,
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
    shadowElevation = 1.dp,
    modifier = modifier
      .fillMaxWidth()
      .height(52.dp)
      .testTag("google_sign_in_button")
  ) {
    Box(contentAlignment = Alignment.Center) {
      if (isLoading) {
        CircularProgressIndicator(
          modifier = Modifier.size(22.dp),
          color = MaterialTheme.colorScheme.primary,
          strokeWidth = 2.dp
        )
      } else {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          // Google G multi-color or stylized icon
          Surface(
            shape = CircleShape,
            color = Color(0xFF4285F4).copy(alpha = 0.12f),
            modifier = Modifier.size(28.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = "G",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Black,
                  color = Color(0xFF4285F4)
                )
              )
            }
          }
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = "Continue with Google",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }
  }
}

fun onGoogleSignInClicked(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit,
  scope: CoroutineScope,
  onAuthCancelled: () -> Unit = {}
) {
  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (e: Exception) {
    onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
    return
  }

  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()
  val activity = generateSequence(context) { (it as? ContextWrapper)?.baseContext }
    .filterIsInstance<Activity>()
    .firstOrNull() ?: (context as? Activity)

  if (activity == null) {
    onAuthError("Parent Activity required for Google Sign-In.")
    return
  }

  scope.launch {
    try {
      val result = credentialManager.getCredential(activity, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        FirebaseAuth.getInstance().signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onAuthError("Unexpected credential type received")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w("Auth", "Google Sign-In flow cancelled: ${e.message}", e)
      onAuthCancelled()
    } catch (e: Exception) {
      val errorMsg = e.message ?: ""
      Log.e("Auth", "Google Sign-In failed: $errorMsg", e)
      if (errorMsg.contains("DEVELOPER_ERROR", ignoreCase = true) || errorMsg.contains("10:") || errorMsg.contains("statusCode=10")) {
        Log.e(
          "Auth",
          "DEVELOPER_ERROR (statusCode 10): Google Play Services rejected the connection. " +
            "Ensure package name (com.aistudio.ttsacademy.krvmtq) matches, SHA-1 (31:1F:32:F9:0F:D6:DB:41:1D:E7:B1:1E:FF:81:33:91:D2:9C:C0:28) " +
            "and SHA-256 (7F:0F:B9:E5:1E:D1:4D:26:9B:74:05:60:61:73:62:18:9D:F6:A8:29:55:0F:15:1C:4C:9E:46:35:78:2B:20:44) fingerprints " +
            "are registered in Firebase Console -> Project Settings -> Android Apps, and Google Sign-In is enabled in Firebase Authentication."
        )
        onAuthError("Google Sign-In configuration error (DEVELOPER_ERROR). Please verify SHA fingerprints and Google provider in Firebase Console.")
      } else {
        onAuthError(e.localizedMessage ?: "Sign in failed")
      }
    }
  }
}

fun attemptAutoSignIn(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onUnauthenticated: () -> Unit,
  scope: CoroutineScope
) {
  if (FirebaseAuth.getInstance().currentUser != null) {
    onAuthSuccess()
    return
  }
  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (e: Exception) {
    onUnauthenticated()
    return
  }

  val googleIdOption = GetGoogleIdOption.Builder()
    .setFilterByAuthorizedAccounts(true)
    .setServerClientId(clientId)
    .setAutoSelectEnabled(true)
    .build()

  val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
  val activity = generateSequence(context) { (it as? ContextWrapper)?.baseContext }
    .filterIsInstance<Activity>()
    .firstOrNull() ?: (context as? Activity)
  val targetContext = activity ?: context

  scope.launch {
    try {
      val result = credentialManager.getCredential(targetContext, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        FirebaseAuth.getInstance().signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onUnauthenticated()
      }
    } catch (e: Exception) {
      Log.d("Auth", "Auto sign-in silent check: ${e.message}")
      onUnauthenticated()
    }
  }
}

fun performSignOut(
  context: Context,
  credentialManager: CredentialManager,
  onSignOutComplete: () -> Unit,
  scope: CoroutineScope
) {
  FirebaseAuth.getInstance().signOut()
  scope.launch {
    try {
      credentialManager.clearCredentialState(ClearCredentialStateRequest())
    } catch (e: Exception) {
      Log.e("Auth", "Failed to clear credential state", e)
    } finally {
      onSignOutComplete()
    }
  }
}
