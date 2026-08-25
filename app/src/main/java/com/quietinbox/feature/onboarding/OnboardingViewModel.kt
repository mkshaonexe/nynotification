package com.quietinbox.feature.onboarding

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietinbox.core.time.Clock
import com.quietinbox.data.db.dao.RuleDao
import com.quietinbox.data.db.entity.AllowRuleEntity
import com.quietinbox.data.prefs.SettingsDataStore
import com.quietinbox.feature.onboarding.util.DefaultAppCandidate
import com.quietinbox.feature.onboarding.util.DefaultAppsResolver
import com.quietinbox.feature.onboarding.util.OemBrand
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Step definitions for the 4-step onboarding flow.
 */
enum class OnboardingStep(val index: Int) {
    PROMISE(0),
    PERMISSION(1),
    ALLOWLIST(2),
    DONE(3)
}

/**
 * UI State for the onboarding flow.
 */
data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.PROMISE,
    val hasNotificationAccess: Boolean = false,
    val oemBrand: OemBrand = OemBrand.fromManufacturer(),
    val isOemGuidanceExpanded: Boolean = false,
    val defaultAppCandidates: List<DefaultAppCandidate> = emptyList(),
    val isSaving: Boolean = false
)

/**
 * ViewModel managing the 4-step onboarding process, permission polling,
 * default app resolution, allowlist persistence, and completion state.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore,
    private val ruleDao: RuleDao,
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        OnboardingUiState(
            hasNotificationAccess = checkNotificationAccess(),
            defaultAppCandidates = DefaultAppsResolver.resolveDefaultApps(context)
        )
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun nextStep() {
        _uiState.update { current ->
            val nextStep = when (current.currentStep) {
                OnboardingStep.PROMISE -> OnboardingStep.PERMISSION
                OnboardingStep.PERMISSION -> {
                    // Only advance if permission is actually granted
                    if (current.hasNotificationAccess) OnboardingStep.ALLOWLIST else current.currentStep
                }
                OnboardingStep.ALLOWLIST -> OnboardingStep.DONE
                OnboardingStep.DONE -> OnboardingStep.DONE
            }
            current.copy(currentStep = nextStep)
        }
    }

    fun previousStep() {
        _uiState.update { current ->
            val prevStep = when (current.currentStep) {
                OnboardingStep.PROMISE -> OnboardingStep.PROMISE
                OnboardingStep.PERMISSION -> OnboardingStep.PROMISE
                OnboardingStep.ALLOWLIST -> OnboardingStep.PERMISSION
                OnboardingStep.DONE -> OnboardingStep.ALLOWLIST
            }
            current.copy(currentStep = prevStep)
        }
    }

    fun skipAllowlist() {
        _uiState.update { it.copy(currentStep = OnboardingStep.DONE) }
    }

    fun toggleCandidate(packageName: String) {
        _uiState.update { current ->
            val updated = current.defaultAppCandidates.map { candidate ->
                if (candidate.packageName == packageName) {
                    candidate.copy(isSelected = !candidate.isSelected)
                } else {
                    candidate
                }
            }
            current.copy(defaultAppCandidates = updated)
        }
    }

    fun toggleOemGuidance() {
        _uiState.update { it.copy(isOemGuidanceExpanded = !it.isOemGuidanceExpanded) }
    }

    /**
     * Polls notification listener access on resume and automatically advances
     * to the allowlist step when granted.
     */
    fun checkPermissionOnResume() {
        val granted = checkNotificationAccess()
        _uiState.update { current ->
            val shouldAdvance = granted && current.currentStep == OnboardingStep.PERMISSION
            current.copy(
                hasNotificationAccess = granted,
                currentStep = if (shouldAdvance) OnboardingStep.ALLOWLIST else current.currentStep
            )
        }
    }

    /**
     * Persists selected allow rules, enables Quiet Mode by default,
     * marks onboarding as completed, and invokes [onComplete].
     */
    fun completeOnboarding(onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            try {
                // Save selected default allow rules
                val selectedApps = _uiState.value.defaultAppCandidates.filter { it.isSelected }
                val now = clock.nowEpochMs()
                for (app in selectedApps) {
                    val rule = AllowRuleEntity(
                        type = "APP",
                        value = app.packageName,
                        matchMode = "EXACT",
                        enabled = true,
                        createdAt = now
                    )
                    ruleDao.insertRule(rule)
                }

                // Default settings: Quiet Mode ON, Onboarding Completed
                settingsDataStore.setQuietModeEnabled(true)
                settingsDataStore.setOnboardingCompleted(true)

                _uiState.update { it.copy(isSaving = false) }
                onComplete()
            } catch (_: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                onComplete()
            }
        }
    }

    private fun checkNotificationAccess(): Boolean {
        return try {
            val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
            enabledPackages.contains(context.packageName)
        } catch (_: Exception) {
            false
        }
    }
}
