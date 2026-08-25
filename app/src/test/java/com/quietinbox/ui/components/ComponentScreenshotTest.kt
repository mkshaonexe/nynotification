package com.quietinbox.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.quietinbox.core.health.HealthState
import com.quietinbox.ui.theme.QuietInboxTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7Pro)
class ComponentScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val roborazziRule = RoborazziRule(
        composeRule = composeRule,
        captureRoot = composeRule.onRoot(),
        options = RoborazziRule.Options(
            outputDirectoryPath = "build/outputs/roborazzi"
        )
    )

    @Test
    fun statTileDark() {
        composeRule.setContent {
            QuietInboxTheme(darkTheme = true) {
                StatTile(
                    label = "Silenced",
                    value = "1,284",
                    subCaption = "~3h attention saved"
                )
            }
        }
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/stat_tile_dark.png")
    }

    @Test
    fun quietSwitchDark() {
        composeRule.setContent {
            QuietInboxTheme(darkTheme = true) {
                QuietSwitch(
                    enabled = true,
                    onEnabledChange = {},
                    subtitle = "On since 9:14 AM"
                )
            }
        }
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/quiet_switch_dark.png")
    }

    @Test
    fun healthBannerDark() {
        composeRule.setContent {
            QuietInboxTheme(darkTheme = true) {
                HealthBanner(
                    healthState = HealthState.CONNECTED,
                    onClick = {}
                )
            }
        }
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/health_banner_dark.png")
    }

    @Test
    fun emptyStateDark() {
        composeRule.setContent {
            QuietInboxTheme(darkTheme = true) {
                EmptyState(
                    title = "Nothing captured yet",
                    subtitle = "Notifications will show up here once received.",
                    icon = Icons.Default.Inbox,
                    actionLabel = "Grant Access",
                    onActionClick = {}
                )
            }
        }
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/empty_state_dark.png")
    }
}
