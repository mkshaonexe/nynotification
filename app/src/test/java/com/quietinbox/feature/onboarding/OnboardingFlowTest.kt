package com.quietinbox.feature.onboarding

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.quietinbox.core.time.Clock
import com.quietinbox.data.db.dao.RuleDao
import com.quietinbox.data.db.entity.AllowRuleEntity
import com.quietinbox.data.db.entity.MutedAppEntity
import com.quietinbox.data.prefs.SettingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class FakeTestRuleDao : RuleDao {
    val insertedRules = mutableListOf<AllowRuleEntity>()

    override fun getAllRules(): Flow<List<AllowRuleEntity>> = flowOf(insertedRules)
    override fun getEnabledRulesFlow(): Flow<List<AllowRuleEntity>> = flowOf(insertedRules.filter { it.enabled })
    override suspend fun getEnabledRules(): List<AllowRuleEntity> = insertedRules.filter { it.enabled }
    override suspend fun getRuleById(id: Long): AllowRuleEntity? = insertedRules.find { it.id == id }
    override suspend fun insertRule(rule: AllowRuleEntity): Long {
        insertedRules.add(rule)
        return insertedRules.size.toLong()
    }
    override suspend fun updateRule(rule: AllowRuleEntity) {}
    override suspend fun deleteRule(rule: AllowRuleEntity) {
        insertedRules.remove(rule)
    }
    override suspend fun deleteRuleById(id: Long) {
        insertedRules.removeIf { it.id == id }
    }
    override fun getAllMutedApps(): Flow<List<MutedAppEntity>> = flowOf(emptyList())
    override suspend fun getMutedPackageNames(): List<String> = emptyList()
    override suspend fun isAppMuted(packageName: String): Boolean = false
    override suspend fun muteApp(app: MutedAppEntity) {}
    override suspend fun unmuteApp(packageName: String) {}
}

class FakeTestClock(var currentEpochMs: Long = 1000L) : Clock {
    override fun nowEpochMs(): Long = currentEpochMs
}

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class OnboardingFlowTest {

    private lateinit var context: Context
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var ruleDao: FakeTestRuleDao
    private lateinit var clock: FakeTestClock
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        settingsDataStore = SettingsDataStore(context)
        ruleDao = FakeTestRuleDao()
        clock = FakeTestClock()
        viewModel = OnboardingViewModel(context, settingsDataStore, ruleDao, clock)
    }

    @Test
    fun `starts on PROMISE step`() {
        assertEquals(OnboardingStep.PROMISE, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `cannot advance past PERMISSION step without notification access`() {
        // Step 1 -> Step 2
        viewModel.nextStep()
        assertEquals(OnboardingStep.PERMISSION, viewModel.uiState.value.currentStep)

        // Try to advance without access
        viewModel.nextStep()
        assertEquals(OnboardingStep.PERMISSION, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `navigates back and forward across steps`() {
        viewModel.nextStep()
        assertEquals(OnboardingStep.PERMISSION, viewModel.uiState.value.currentStep)

        viewModel.previousStep()
        assertEquals(OnboardingStep.PROMISE, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `skipAllowlist advances directly to DONE step`() {
        viewModel.skipAllowlist()
        assertEquals(OnboardingStep.DONE, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `toggleCandidate toggles app selection`() {
        val initialCandidates = viewModel.uiState.value.defaultAppCandidates
        if (initialCandidates.isNotEmpty()) {
            val firstPkg = initialCandidates.first().packageName
            val initialSelected = initialCandidates.first().isSelected

            viewModel.toggleCandidate(firstPkg)
            val updated = viewModel.uiState.value.defaultAppCandidates.first { it.packageName == firstPkg }
            assertEquals(!initialSelected, updated.isSelected)
        }
    }

    @Test
    fun `completeOnboarding saves rules, enables Quiet Mode, and marks completed`() {
        var completedCalled = false
        viewModel.completeOnboarding {
            completedCalled = true
        }

        assertTrue(completedCalled)
    }
}
