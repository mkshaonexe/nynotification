package com.quietinbox.service.ingest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNormalizerTest {

    @Test
    fun normalize_bangladeshNumbers_returns9DigitTail() {
        // Standard formats in Bangladesh
        val e164 = "+880 1709-093872"
        val national = "01709093872"
        val raw = "8801709093872"
        val tailOnly = "1709093872"

        assertEquals("709093872", PhoneNormalizer.normalize(e164))
        assertEquals("709093872", PhoneNormalizer.normalize(national))
        assertEquals("709093872", PhoneNormalizer.normalize(raw))
        assertEquals("709093872", PhoneNormalizer.normalize(tailOnly))
    }

    @Test
    fun normalize_indiaNumbers_returns9DigitTail() {
        // Standard formats in India
        val e164 = "+91 98765 43210"
        val national = "09876543210"
        val raw = "9876543210"

        assertEquals("876543210", PhoneNormalizer.normalize(e164))
        assertEquals("876543210", PhoneNormalizer.normalize(national))
        assertEquals("876543210", PhoneNormalizer.normalize(raw))
    }

    @Test
    fun normalize_usNumbers_returns9DigitTail() {
        // Standard formats in United States
        val usWithCountryCode = "+1 (555) 123-4567"
        val usNational = "555-123-4567"
        val usDigits = "5551234567"

        assertEquals("551234567", PhoneNormalizer.normalize(usWithCountryCode))
        assertEquals("551234567", PhoneNormalizer.normalize(usNational))
        assertEquals("551234567", PhoneNormalizer.normalize(usDigits))
    }

    @Test
    fun normalize_shortCodes_returnsAllDigits() {
        assertEquals("1234", PhoneNormalizer.normalize("1234"))
        assertEquals("999", PhoneNormalizer.normalize("999"))
        assertEquals("16216", PhoneNormalizer.normalize("16216"))
    }

    @Test
    fun normalize_nonDigitsAndEmpty_returnsNull() {
        assertNull(PhoneNormalizer.normalize(null))
        assertNull(PhoneNormalizer.normalize(""))
        assertNull(PhoneNormalizer.normalize("   "))
        assertNull(PhoneNormalizer.normalize("GP-INFO"))
        assertNull(PhoneNormalizer.normalize("bKash"))
    }

    @Test
    fun isPotentialPhoneNumber_identifiesPhoneStringsCorrectly() {
        assertTrue(PhoneNormalizer.isPotentialPhoneNumber("+880 1709-093872"))
        assertTrue(PhoneNormalizer.isPotentialPhoneNumber("01709093872"))
        assertTrue(PhoneNormalizer.isPotentialPhoneNumber("+1 (555) 123-4567"))
        assertTrue(PhoneNormalizer.isPotentialPhoneNumber("9876543210"))

        assertFalse(PhoneNormalizer.isPotentialPhoneNumber("WhatsApp"))
        assertFalse(PhoneNormalizer.isPotentialPhoneNumber("Promo 50% Off Today"))
        assertFalse(PhoneNormalizer.isPotentialPhoneNumber("1234")) // too short (< 7 digits)
        assertFalse(PhoneNormalizer.isPotentialPhoneNumber(null))
    }

    @Test
    fun extractAllDigits_extractsUncutDigits() {
        assertEquals("8801709093872", PhoneNormalizer.extractAllDigits("+880 1709-093872"))
        assertEquals("15551234567", PhoneNormalizer.extractAllDigits("+1 (555) 123-4567"))
        assertNull(PhoneNormalizer.extractAllDigits("NoDigitsHere"))
    }
}
