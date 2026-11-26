package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * Verification result for the signed-in Firebase user's `'admin'` custom claim.
 * Mirrors `hasAdminRoleClaim()` in `firestore.rules`.
 */
sealed interface AdminClaimStatus {
  data object Checking : AdminClaimStatus

  data class Verified(
    val claimSource: String,
    val email: String?
  ) : AdminClaimStatus

  data class Missing(
    val email: String?,
    val errorMessage: String? = null
  ) : AdminClaimStatus
}

enum class AdminClaimGuardMode {
  /** Silently hides admin-only controls when the `'admin'` custom claim is absent. */
  HIDE_CONTROLS,

  /** Displays a prominent warning when the `'admin'` custom claim is absent and gates controls unless fallback admin access is permitted. */
  WARN_AND_GATE
}

@Stable
data class AdminClaimCheckState(
  val status: AdminClaimStatus,
  val isRefreshing: Boolean,
  val refresh: () -> Unit
) {
  val hasAdminClaim: Boolean
    get() = status is AdminClaimStatus.Verified
}

internal fun extractAdminClaimSource(claims: Map<String, Any>): String? {
  val roleClaim = (claims["role"] as? String)?.trim()?.lowercase()
  if (roleClaim == "admin" || roleClaim == "super_admin") {
    return "role=$roleClaim"
  }
  val adminFlag = claims["admin"] as? Boolean
  if (adminFlag == true) {
    return "admin=true"
  }
  return null
}

/**
 * Inspects the current Firebase Auth ID token for the server-signed `'admin'` custom claim
 * (`role == 'admin'` / `'super_admin'` or `admin == true`), re-evaluating automatically when
 * auth state or ID tokens change and exposing a manual force-refresh trigger.
 */
@Composable
fun rememberAdminClaimState(
  onRefreshComplete: () -> Unit = {}
): AdminClaimCheckState {
  val auth = remember { FirebaseAuth.getInstance() }
  var firebaseUser by remember { mutableStateOf<FirebaseUser?>(auth.currentUser) }
  var tokenGeneration by remember { mutableIntStateOf(0) }
  var manualRefreshCount by remember { mutableIntStateOf(0) }
  var status by remember { mutableStateOf<AdminClaimStatus>(AdminClaimStatus.Checking) }
  var isRefreshing by remember { mutableStateOf(false) }

  DisposableEffect(auth) {
    val authListener = FirebaseAuth.AuthStateListener { updatedAuth ->
      firebaseUser = updatedAuth.currentUser
    }
    val idTokenListener = FirebaseAuth.IdTokenListener { updatedAuth ->
      firebaseUser = updatedAuth.currentUser
      tokenGeneration += 1
    }
    auth.addAuthStateListener(authListener)
    auth.addIdTokenListener(idTokenListener)
    onDispose {
      auth.removeAuthStateListener(authListener)
      auth.removeIdTokenListener(idTokenListener)
    }
  }

  LaunchedEffect(firebaseUser?.uid, tokenGeneration, manualRefreshCount) {
    val user = firebaseUser
    if (user == null) {
      status = AdminClaimStatus.Missing(email = null)
      isRefreshing = false
      return@LaunchedEffect
    }

    val forceRefresh = manualRefreshCount > 0
    if (forceRefresh) {
      isRefreshing = true
    } else if (status !is AdminClaimStatus.Verified) {
      status = AdminClaimStatus.Checking
    }

    status = try {
      val tokenResult = user.getIdToken(forceRefresh).await()
      val claimSource = extractAdminClaimSource(tokenResult.claims)
      if (claimSource != null) {
        AdminClaimStatus.Verified(claimSource = claimSource, email = user.email)
      } else {
        AdminClaimStatus.Missing(email = user.email)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      AdminClaimStatus.Missing(
        email = user.email,
        errorMessage = e.message
      )
    } finally {
      isRefreshing = false
    }

    if (forceRefresh) {
      onRefreshComplete()
    }
  }

  return AdminClaimCheckState(
    status = status,
    isRefreshing = isRefreshing,
    refresh = { manualRefreshCount += 1 }
  )
}

/**
 * Composable guard that verifies the Firebase Auth `'admin'` custom claim for the signed-in user.
 *
 * - In [AdminClaimGuardMode.HIDE_CONTROLS], hides [content] whenever the claim is missing
 *   (unless [allowFallbackAdminAccess] is explicitly enabled for verified fallback admins).
 * - In [AdminClaimGuardMode.WARN_AND_GATE], displays a warning card when the claim is missing
 *   and hides [content] unless [allowFallbackAdminAccess] is true (in which case a non-blocking
 *   warning banner is shown above [content]).
 */
@Composable
fun AdminClaimGuard(
  modifier: Modifier = Modifier,
  mode: AdminClaimGuardMode = AdminClaimGuardMode.WARN_AND_GATE,
  allowFallbackAdminAccess: Boolean = false,
  onClaimRefreshed: () -> Unit = {},
  missingClaimWarning: (@Composable (AdminClaimStatus.Missing, Boolean, () -> Unit) -> Unit)? = null,
  content: @Composable (AdminClaimStatus) -> Unit
) {
  val claimState = rememberAdminClaimState(onRefreshComplete = onClaimRefreshed)

  when (val status = claimState.status) {
    is AdminClaimStatus.Verified -> {
      Box(modifier = modifier) {
        content(status)
      }
    }

    AdminClaimStatus.Checking -> {
      if (allowFallbackAdminAccess) {
        Box(modifier = modifier) {
          content(status)
        }
      } else if (mode == AdminClaimGuardMode.WARN_AND_GATE) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          shape = RoundedCornerShape(16.dp),
          modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("admin_claim_checking_indicator")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              strokeWidth = 2.5.dp
            )
            Text(
              text = stringResource(R.string.admin_claim_checking_message),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }

    is AdminClaimStatus.Missing -> {
      when {
        mode == AdminClaimGuardMode.HIDE_CONTROLS && !allowFallbackAdminAccess -> {
          // Intentionally hide admin-only controls when the 'admin' custom claim is missing.
        }

        mode == AdminClaimGuardMode.HIDE_CONTROLS && allowFallbackAdminAccess -> {
          Box(modifier = modifier) {
            content(status)
          }
        }

        else -> {
          Column(modifier = modifier.fillMaxWidth()) {
            AnimatedVisibility(
              visible = true,
              enter = fadeIn() + expandVertically(),
              exit = fadeOut() + shrinkVertically()
            ) {
              if (missingClaimWarning != null) {
                missingClaimWarning(status, claimState.isRefreshing, claimState.refresh)
              } else {
                AdminClaimMissingWarningCard(
                  status = status,
                  isFallbackActive = allowFallbackAdminAccess,
                  isRefreshing = claimState.isRefreshing,
                  onRefreshClick = claimState.refresh,
                  modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
              }
            }
            if (allowFallbackAdminAccess) {
              content(status)
            }
          }
        }
      }
    }
  }
}

@Composable
fun AdminClaimMissingWarningCard(
  status: AdminClaimStatus.Missing,
  isFallbackActive: Boolean,
  isRefreshing: Boolean,
  onRefreshClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val containerColor = if (isFallbackActive) {
    MaterialTheme.colorScheme.tertiaryContainer
  } else {
    MaterialTheme.colorScheme.errorContainer
  }
  val contentColor = if (isFallbackActive) {
    MaterialTheme.colorScheme.onTertiaryContainer
  } else {
    MaterialTheme.colorScheme.onErrorContainer
  }
  val borderColor = if (isFallbackActive) {
    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)
  } else {
    MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
  }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = containerColor),
    border = BorderStroke(1.dp, borderColor),
    modifier = modifier
      .fillMaxWidth()
      .testTag("admin_claim_missing_warning")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Box(
          modifier = Modifier.size(36.dp),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.GppMaybe,
            contentDescription = stringResource(R.string.admin_claim_warning_icon_desc),
            tint = contentColor,
            modifier = Modifier.size(24.dp)
          )
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(R.string.admin_claim_missing_title),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = contentColor
          )
          if (!status.email.isNullOrBlank()) {
            Text(
              text = status.email,
              style = MaterialTheme.typography.labelSmall,
              color = contentColor.copy(alpha = 0.85f)
            )
          }
        }
      }

      Text(
        text = if (isFallbackActive) {
          stringResource(R.string.admin_claim_missing_fallback_body)
        } else {
          stringResource(R.string.admin_claim_missing_blocked_body)
        },
        style = MaterialTheme.typography.bodySmall,
        color = contentColor
      )

      if (!status.errorMessage.isNullOrBlank()) {
        Text(
          text = status.errorMessage,
          style = MaterialTheme.typography.labelSmall,
          color = contentColor.copy(alpha = 0.9f)
        )
      }

      Spacer(modifier = Modifier.height(2.dp))

      OutlinedButton(
        onClick = onRefreshClick,
        enabled = !isRefreshing,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.5f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        modifier = Modifier
          .defaultMinSize(minHeight = 48.dp)
          .testTag("admin_claim_refresh_button")
      ) {
        if (isRefreshing) {
          CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = contentColor
          )
        } else {
          Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = stringResource(R.string.admin_claim_refresh_icon_desc),
            modifier = Modifier.size(18.dp)
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (isRefreshing) {
            stringResource(R.string.admin_claim_refreshing_button)
          } else {
            stringResource(R.string.admin_claim_refresh_button)
          },
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
        )
      }
    }
  }
}
