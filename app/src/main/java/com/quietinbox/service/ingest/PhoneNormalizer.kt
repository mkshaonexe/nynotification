package com.quietinbox.service.ingest

/**
 * Normalizes phone numbers across different regional formats (BD, IN, US, and E.164).
 * Strips all formatting and non-digit characters, and extracts a 9-digit tail to enable
 * consistent matching regardless of country code or leading zero variations.
 */
object PhoneNormalizer {

    private const val TAIL_LENGTH = 9

    /**
     * Extracts digits from the input string and returns the normalized representation.
     * If the string contains at least [TAIL_LENGTH] digits, returns the last [TAIL_LENGTH] digits.
     * Otherwise returns all digits (if any), or null if no digits are present.
     */
    fun normalize(raw: String?): String? {
        if (raw.isNullOrBlank()) return null

        val digits = raw.filter { it.isDigit() }
        if (digits.isEmpty()) return null

        return if (digits.length >= TAIL_LENGTH) {
            digits.takeLast(TAIL_LENGTH)
        } else {
            digits
        }
    }

    /**
     * Extracts all raw digits from the input string without truncating to the 9-digit tail.
     * Returns null if no digits are found.
     */
    fun extractAllDigits(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val digits = raw.filter { it.isDigit() }
        return digits.ifEmpty { null }
    }

    /**
     * Determines whether the given string looks like a phone number or sender with phone digits.
     */
    fun isPotentialPhoneNumber(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return false
        val digitsCount = raw.count { it.isDigit() }
        // If majority of characters are digits or valid phone punctuation (+, -, space, parens)
        val validPhoneChars = raw.count { it.isDigit() || it == '+' || it == '-' || it == ' ' || it == '(' || it == ')' }
        return digitsCount >= 7 && (validPhoneChars.toDouble() / raw.length) >= 0.75
    }
}
