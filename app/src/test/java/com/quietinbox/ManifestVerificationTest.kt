package com.quietinbox

import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

class ManifestVerificationTest {

    @Test
    fun manifestContainsNoInternetAndNoQueryAllPackages() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        val content = if (manifestFile.exists()) {
            manifestFile.readText()
        } else {
            File("app/src/main/AndroidManifest.xml").readText()
        }

        assertFalse(
            "Manifest must never contain INTERNET permission",
            content.contains("android.permission.INTERNET")
        )
        assertFalse(
            "Manifest must never contain QUERY_ALL_PACKAGES permission",
            content.contains("android.permission.QUERY_ALL_PACKAGES")
        )
    }
}
