package com.quietinbox.core.firewall

import com.quietinbox.core.model.CapturedNotification
import com.quietinbox.data.db.entity.AllowRuleEntity
import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Matches incoming captured notifications against user-configured [AllowRuleEntity] rules (Rule 4).
 *
 * Supports three rule types:
 * - APP: Exact package name matching.
 * - SENDER: Diacritic- and case-insensitive sender name contains,
 *   as well as 9-digit / partial-digit tail matching for phone numbers.
 * - WORD: Whole-word boundary matching (default) or substring matching ("CONTAINS")
 *   across title, body, subText, and senderName.
 */
@Singleton
class RuleMatcher @Inject constructor() {

    companion object {
        const val TYPE_APP = "APP"
        const val TYPE_SENDER = "SENDER"
        const val TYPE_WORD = "WORD"

        const val MATCH_MODE_EXACT = "EXACT"
        const val MATCH_MODE_CONTAINS = "CONTAINS"
        const val MATCH_MODE_DIGITS = "DIGITS"
    }

    /**
     * Finds the first enabled allow rule that matches the given [notification].
     *
     * @param notification The captured notification.
     * @param rules The list of active [AllowRuleEntity] rules to test against.
     * @return The first matching [AllowRuleEntity], or null if none match.
     */
    fun findMatchingRule(
        notification: CapturedNotification,
        rules: List<AllowRuleEntity>
    ): AllowRuleEntity? {
        for (rule in rules) {
            if (!rule.enabled) continue
            if (matches(notification, rule)) {
                return rule
            }
        }
        return null
    }

    /**
     * Evaluates whether a single enabled [rule] matches the given [notification].
     *
     * @param notification The captured notification.
     * @param rule The rule to test.
     * @return True if the notification satisfies the rule criteria, false otherwise.
     */
    fun matches(notification: CapturedNotification, rule: AllowRuleEntity): Boolean {
        if (!rule.enabled || rule.value.isBlank()) return false

        return when (rule.type.uppercase(Locale.ROOT)) {
            TYPE_APP -> matchApp(notification, rule)
            TYPE_SENDER -> matchSender(notification, rule)
            TYPE_WORD -> matchWord(notification, rule)
            else -> false
        }
    }

    private fun matchApp(n: CapturedNotification, rule: AllowRuleEntity): Boolean {
        return n.packageName.equals(rule.value.trim(), ignoreCase = true)
    }

    private fun matchSender(n: CapturedNotification, rule: AllowRuleEntity): Boolean {
        val ruleValue = rule.value.trim()
        val normalizedRule = normalizeText(ruleValue)

        // 1. Check senderName (case & diacritic-insensitive contains)
        val normalizedSenderName = normalizeText(n.senderName)
        if (normalizedSenderName.isNotEmpty() && normalizedSenderName.contains(normalizedRule)) {
            return true
        }

        // 2. Check title as fallback sender name (e.g. SMS where sender is the title)
        val normalizedTitle = normalizeText(n.title)
        if (normalizedTitle.isNotEmpty() && normalizedTitle.contains(normalizedRule)) {
            return true
        }

        // 3. Digit-tail match against senderDigits (design.md §5.4)
        val ruleDigits = ruleValue.filter { it.isDigit() }
        val senderDigits = n.senderDigits?.filter { it.isDigit() }.orEmpty()

        if (ruleDigits.length >= 4 && senderDigits.isNotEmpty()) {
            val ruleTail = ruleDigits.takeLast(9)
            val senderTail = senderDigits.takeLast(9)

            if (senderDigits.contains(ruleTail) ||
                ruleDigits.contains(senderTail) ||
                senderDigits.contains(ruleDigits) ||
                ruleDigits.contains(senderDigits) ||
                senderTail == ruleTail
            ) {
                return true
            }
        }

        return false
    }

    private fun matchWord(n: CapturedNotification, rule: AllowRuleEntity): Boolean {
        val ruleWord = rule.value.trim()
        if (ruleWord.isEmpty()) return false

        val combinedText = buildString {
            n.title?.let { append(it).append(' ') }
            n.body?.let { append(it).append(' ') }
            n.subText?.let { append(it).append(' ') }
            n.senderName?.let { append(it).append(' ') }
        }
        if (combinedText.isBlank()) return false

        val normalizedText = normalizeText(combinedText)
        val normalizedRuleWord = normalizeText(ruleWord)

        if (rule.matchMode.equals(MATCH_MODE_CONTAINS, ignoreCase = true)) {
            return normalizedText.contains(normalizedRuleWord)
        }

        // Default: whole-word matching with Unicode word boundaries
        return matchesWholeWord(normalizedText, normalizedRuleWord)
    }

    /**
     * Performs whole-word boundary matching on normalized text, supporting both English and Unicode scripts (e.g. Bangla).
     */
    private fun matchesWholeWord(text: String, word: String): Boolean {
        if (word.isEmpty()) return false
        val escaped = Regex.escape(word)
        val regex = Regex("(?:^|[^\\p{L}\\p{N}])$escaped(?:$|[^\\p{L}\\p{N}])")
        return regex.containsMatchIn(text)
    }

    /**
     * Normalizes text by decomposing diacritics, stripping combining marks, and converting to lowercase.
     */
    fun normalizeText(input: String?): String {
        if (input.isNullOrBlank()) return ""
        val nfd = Normalizer.normalize(input, Normalizer.Form.NFD)
        val withoutDiacritics = "\\p{M}".toRegex().replace(nfd, "")
        return withoutDiacritics.lowercase(Locale.ROOT).trim()
    }
}
