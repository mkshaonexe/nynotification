package com.quietinbox.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsViewModelTest {

    @Test
    fun testFormatBytes() {
        fun formatBytes(bytes: Long): String {
            if (bytes < 1024) return "$bytes B"
            val kb = bytes / 1024.0
            if (kb < 1024) return String.format(java.util.Locale.US, "%.1f KB", kb)
            val mb = kb / 1024.0
            if (mb < 1024) return String.format(java.util.Locale.US, "%.1f MB", mb)
            val gb = mb / 1024.0
            return String.format(java.util.Locale.US, "%.2f GB", gb)
        }

        assertEquals("500 B", formatBytes(500L))
        assertEquals("1.0 KB", formatBytes(1024L))
        assertEquals("1.5 MB", formatBytes((1.5 * 1024 * 1024).toLong()))
        assertEquals("2.50 GB", formatBytes((2.5 * 1024 * 1024 * 1024).toLong()))
    }
}
