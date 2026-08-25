package com.quietinbox.feature.inbox.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

/**
 * Utility functions for search normalization, phone digit matching, and text highlighting.
 */
object InboxSearchHelper {

    /**
     * Strips all non-digit characters from the input text.
     */
    fun normalizeToDigits(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val digits = text.filter { it.isDigit() }
        return digits.ifEmpty { null }
    }

    /**
     * Extracts phone digits query when the stripped query has 4 or more digits.
     */
    fun extractDigitsQuery(rawQuery: String?): String? {
        if (rawQuery.isNullOrBlank()) return null
        val digits = rawQuery.filter { it.isDigit() }
        return if (digits.length >= 4) digits else null
    }

    /**
     * Checks if a target phone digits string matches a search query's digits.
     * Handles formatted numbers (e.g. +880 1709-093872 matched by 1709093872 or 093872).
     */
    fun matchesDigits(targetDigits: String?, searchDigits: String?): Boolean {
        if (targetDigits.isNullOrBlank() || searchDigits.isNullOrBlank()) return false
        val normalizedTarget = normalizeToDigits(targetDigits) ?: return false
        val normalizedSearch = normalizeToDigits(searchDigits) ?: return false
        if (normalizedSearch.length < 4) return false
        return normalizedTarget.contains(normalizedSearch)
    }

    /**
     * Returns an [AnnotatedString] with occurrences of [query] highlighted.
     */
    fun highlightMatches(
        text: String?,
        query: String?,
        highlightColor: Color,
        textColor: Color
    ): AnnotatedString {
        if (text.isNullOrEmpty()) return AnnotatedString("")
        if (query.isNullOrBlank()) return AnnotatedString(text)

        val trimmedQuery = query.trim()
        val lowerText = text.lowercase()
        val lowerQuery = trimmedQuery.lowercase()

        return buildAnnotatedString {
            var startIndex = 0
            while (startIndex < text.length) {
                val matchIndex = lowerText.indexOf(lowerQuery, startIndex)
                if (matchIndex == -1) {
                    append(text.substring(startIndex))
                    break
                }
                if (matchIndex > startIndex) {
                    append(text.substring(startIndex, matchIndex))
                }
                val matchEnd = matchIndex + trimmedQuery.length
                pushStyle(
                    SpanStyle(
                        color = highlightColor,
                        fontWeight = FontWeight.Bold
                    )
                )
                append(text.substring(matchIndex, matchEnd))
                pop()
                startIndex = matchEnd
            }
        }
    }
}
