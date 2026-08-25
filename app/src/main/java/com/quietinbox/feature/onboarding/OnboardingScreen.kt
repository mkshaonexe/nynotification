package com.quietinbox.feature.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quietinbox.feature.onboarding.components.OnboardingStepAllowlist
import com.quietinbox.feature.onboarding.components.OnboardingStepDone
import com.quietinbox.feature.onboarding.components.OnboardingStepPermission
import com.quietinbox.feature.onboarding.components.OnboardingStepPromise
import com.quietinbox.feature.onboarding.components.OnboardingTopBar

/**
 * Main screen for the 4-step onboarding experience.
 */
@Composable
fun OnboardingScreen(
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Live permission polling on ON_RESUME
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissionOnResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // System back button handling
    BackHandler(enabled = uiState.currentStep != OnboardingStep.PROMISE && uiState.currentStep != OnboardingStep.DONE) {
        viewModel.previousStep()
    }

    Scaffold(
        topBar = {
            OnboardingTopBar(
                currentStepIndex = uiState.currentStep.index,
                totalSteps = 4,
                showBackButton = uiState.currentStep != OnboardingStep.PROMISE && uiState.currentStep != OnboardingStep.DONE,
                showSkipButton = uiState.currentStep == OnboardingStep.ALLOWLIST,
                onBackClick = { viewModel.previousStep() },
                onSkipClick = { viewModel.skipAllowlist() }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (uiState.currentStep) {
                OnboardingStep.PROMISE -> {
                    OnboardingStepPromise(
                        onGetStartedClick = { viewModel.nextStep() },
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                OnboardingStep.PERMISSION -> {
                    OnboardingStepPermission(
                        hasPermission = uiState.hasNotificationAccess,
                        oemBrand = uiState.oemBrand,
                        isOemExpanded = uiState.isOemGuidanceExpanded,
                        onToggleOemExpanded = { viewModel.toggleOemGuidance() },
                        onGrantPermissionClick = {
                            try {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                // Fallback to general settings
                                try {
                                    val fallback = Intent(Settings.ACTION_SETTINGS)
                                    context.startActivity(fallback)
                                } catch (_: Exception) {}
                            }
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                OnboardingStep.ALLOWLIST -> {
                    OnboardingStepAllowlist(
                        candidates = uiState.defaultAppCandidates,
                        onToggleCandidate = { pkg -> viewModel.toggleCandidate(pkg) },
                        onContinueClick = { viewModel.nextStep() },
                        onSkipClick = { viewModel.skipAllowlist() },
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                OnboardingStep.DONE -> {
                    OnboardingStepDone(
                        isSaving = uiState.isSaving,
                        onOpenHomeClick = {
                            viewModel.completeOnboarding(onComplete = onNavigateToHome)
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
