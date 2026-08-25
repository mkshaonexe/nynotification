package com.quietinbox.feature.onboarding

import com.quietinbox.R
import com.quietinbox.feature.onboarding.util.OemBrand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OemGuidanceTest {

    @Test
    fun `test Samsung resolution and guidance`() {
        val brand = OemBrand.fromManufacturer("samsung")
        assertEquals(OemBrand.SAMSUNG, brand)
        assertEquals(R.string.onboarding_oem_samsung, brand.instructionsResId)
        assertTrue(brand.isAggressive)
    }

    @Test
    fun `test Xiaomi, Redmi, POCO and Blackshark resolution`() {
        assertEquals(OemBrand.XIAOMI, OemBrand.fromManufacturer("Xiaomi"))
        assertEquals(OemBrand.XIAOMI, OemBrand.fromManufacturer("redmi"))
        assertEquals(OemBrand.XIAOMI, OemBrand.fromManufacturer("POCO"))
        assertEquals(OemBrand.XIAOMI, OemBrand.fromManufacturer("Blackshark"))
        assertEquals(R.string.onboarding_oem_xiaomi, OemBrand.XIAOMI.instructionsResId)
        assertTrue(OemBrand.XIAOMI.isAggressive)
    }

    @Test
    fun `test Oppo, OnePlus, and Realme resolution`() {
        assertEquals(OemBrand.OPPO, OemBrand.fromManufacturer("OPPO"))
        assertEquals(OemBrand.OPPO, OemBrand.fromManufacturer("OnePlus"))
        assertEquals(OemBrand.OPPO, OemBrand.fromManufacturer("realme"))
        assertEquals(R.string.onboarding_oem_oppo, OemBrand.OPPO.instructionsResId)
        assertTrue(OemBrand.OPPO.isAggressive)
    }

    @Test
    fun `test Vivo and iQOO resolution`() {
        assertEquals(OemBrand.VIVO, OemBrand.fromManufacturer("vivo"))
        assertEquals(OemBrand.VIVO, OemBrand.fromManufacturer("iQOO"))
        assertEquals(R.string.onboarding_oem_vivo, OemBrand.VIVO.instructionsResId)
        assertTrue(OemBrand.VIVO.isAggressive)
    }

    @Test
    fun `test Huawei and Honor resolution`() {
        assertEquals(OemBrand.HUAWEI, OemBrand.fromManufacturer("HUAWEI"))
        assertEquals(OemBrand.HUAWEI, OemBrand.fromManufacturer("Honor"))
        assertEquals(R.string.onboarding_oem_huawei, OemBrand.HUAWEI.instructionsResId)
        assertTrue(OemBrand.HUAWEI.isAggressive)
    }

    @Test
    fun `test stock Android and unknown manufacturers fall back to stock`() {
        assertEquals(OemBrand.STOCK, OemBrand.fromManufacturer("Google"))
        assertEquals(OemBrand.STOCK, OemBrand.fromManufacturer("Pixel"))
        assertEquals(OemBrand.STOCK, OemBrand.fromManufacturer("motorola"))
        assertEquals(OemBrand.STOCK, OemBrand.fromManufacturer("Sony"))
        assertEquals(OemBrand.STOCK, OemBrand.fromManufacturer("UnknownBrand"))
        assertEquals(OemBrand.STOCK, OemBrand.fromManufacturer(""))
        assertEquals(R.string.onboarding_oem_stock, OemBrand.STOCK.instructionsResId)
        assertFalse(OemBrand.STOCK.isAggressive)
    }
}
