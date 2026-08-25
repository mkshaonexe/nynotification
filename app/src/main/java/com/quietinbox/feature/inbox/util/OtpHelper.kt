package com.quietinbox.feature.inbox.util

/**
 * Utility for detecting verification/OTP codes in notification titles and bodies.
 * Evaluated at render time without persistent storage of extracted codes.
 */
object OtpHelper {

    private val OTP_KEYWORDS = listOf(
        "otp",
        "code",
        "verification",
        "verify",
        "pin",
        "password",
        "passcode",
        "secret",
        "security code",
        "কোড",
        "ওটিপি",
        "পিন",
        "পাসকোড"
    )

    private val DIGIT_TOKEN_REGEX = Regex("""\b(\d{4,8})\b""")

    /**
     * Attempts to extract a standalone 4–8 digit verification code if OTP keywords are present.
     */
    fun extractOtpCode(title: String?, body: String?): String? {
        val combinedText = "${title.orEmpty()} ${body.orEmpty()}"
        if (combinedText.isBlank()) return null

        val lower = combinedText.lowercase()
        val hasOtpKeyword = OTP_KEYWORDS.any { lower.contains(it) }
        if (!hasOtpKeyword) return null

        val match = DIGIT_TOKEN_REGEX.find(combinedText)
        return match?.groupValues?.getOrNull(1)
    }

    /**
     * Returns true if the notification appears to contain a verification/OTP code.
     */
    fun isOtpNotification(title: String?, body: String?): Boolean {
        return extractOtpCode(title, body) != null
    }
}
