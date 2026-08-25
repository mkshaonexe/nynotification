package com.quietinbox.feature.onboarding

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.quietinbox.feature.onboarding.util.DefaultAppsResolver
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DefaultAppsResolverTest {

    @Test
    fun `resolveDefaultApps executes without exception and returns candidate list`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val candidates = DefaultAppsResolver.resolveDefaultApps(context)
        assertNotNull(candidates)
    }
}
