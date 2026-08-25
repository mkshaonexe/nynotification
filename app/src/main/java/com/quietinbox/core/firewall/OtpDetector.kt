package com.quietinbox.core.firewall

import com.quietinbox.core.model.CapturedNotification
import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects whether a notification represents a One-Time Password (OTP),
 * two-factor authentication (2FA) token, or verification code (Rule 5).
 *
 * Privacy & Security:
 * - This engine NEVER extracts, returns, or persists the verification code.
 * - Supports multilingual keywords in English and Bangla.
 * - Handles Android 15+ redacted bodies where notification text is masked by the OS,
 *   using title/channel/app heuristics to ensure the notification safely breaks through.
 */
@Singleton
class OtpDetector @Inject constructor() {

    companion object {
        // Multilingual OTP keywords in English and Bengali
        private val OTP_KEYWORDS = listOf(
            // English
            "otp",
            "code",
            "verification",
            "verify",
            "pin",
            "passcode",
            "password",
            "auth",
            "2fa",
            "mfa",
            "security code",
            "confirmation code",
            "one-time",
            "onetime",
            "secret code",
            "login code",
            // Bengali (Bangla)
            "ওটিপি",
            "কোড",
            "পিন",
            "যাচাইকরণ",
            "ভেরিফিকেশন",
            "পাসকোড",
            "নিরাপত্তা কোড"
        )

        // Standalone 4 to 8 digit token regex bounded by non-digits
        private val STANDALONE_CODE_REGEX = Regex("(?:^|[^\\p{N}])(\\d{4,8})(?:$|[^\\p{N}])")

        // Keywords in title, subtext, or channelId indicating OTP even when body is redacted
        private val TITLE_HEURISTIC_KEYWORDS = listOf(
            "otp",
            "verification",
            "verify",
            "security code",
            "2fa",
            "auth code",
            "passcode",
            "login code",
            "confirmation code",
            "কোড",
            "ওটিপি",
            "ভেরিফিকেশন"
        )
    }

    /**
     * Evaluates if the notification contains or represents an OTP / verification code.
     *
     * @param notification The notification to evaluate.
     * @return True if classified as an OTP, false otherwise.
     */
    fun isOtp(notification: CapturedNotification): Boolean {
        val body = notification.body
        val title = notification.title
        val subText = notification.subText
        val channelId = notification.channelId
        val appLabel = notification.appLabel

        // 1. Title/subtext/channel heuristics (handles Android 15+ redacted bodies)
        if (matchesTitleOrChannelHeuristic(title, subText, channelId, appLabel)) {
            return true
        }

        // 2. Body-based token + keyword detection
        if (!body.isNullOrBlank()) {
            val normalizedBody = normalize(body)
            val hasOtpKeyword = containsOtpKeyword(normalizedBody)
            val hasDigitToken = STANDALONE_CODE_REGEX.containsMatchIn(body)

            if (hasOtpKeyword && hasDigitToken) {
                return true
            }

            // Also match if title contains keyword and body contains the 4-8 digit token
            if (hasDigitToken && !title.isNullOrBlank()) {
                val normalizedTitle = normalize(title)
                if (containsOtpKeyword(normalizedTitle)) {
                    return true
                }
            }
        }

        return false
    }

    private fun matchesTitleOrChannelHeuristic(
        title: String?,
        subText: String?,
        channelId: String?,
        appLabel: String?
    ): Boolean {
        val textToInspect = listOfNotNull(title, subText, channelId).joinToString(" ")
        if (textToInspect.isBlank()) return false

        val normalized = normalize(textToInspect)
        for (kw in TITLE_HEURISTIC_KEYWORDS) {
            val normalizedKw = normalize(kw)
            if (normalizedKw.length <= 4) {
                val regex = Regex("(?:^|[^\\p{L}\\p{N}])${Regex.escape(normalizedKw)}(?:$|[^\\p{L}\\p{N}])")
                if (regex.containsMatchIn(normalized)) return true
            } else {
                if (normalized.contains(normalizedKw)) return true
            }
        }
        return false
    }

    private fun containsOtpKeyword(normalizedText: String): Boolean {
        for (kw in OTP_KEYWORDS) {
            val normalizedKw = normalize(kw)
            if (normalizedKw.length <= 4) {
                val regex = Regex("(?:^|[^\\p{L}\\p{N}])${Regex.escape(normalizedKw)}(?:$|[^\\p{L}\\p{N}])")
                if (regex.containsMatchIn(normalizedText)) return true
            } else {
                if (normalizedText.contains(normalizedKw)) return true
            }
        }
        return false
    }

    private fun normalize(input: String): String {
        val nfd = Normalizer.normalize(input, Normalizer.Form.NFD)
        return "\\p{M}".toRegex().replace(nfd, "").lowercase(Locale.ROOT)
    }
}
