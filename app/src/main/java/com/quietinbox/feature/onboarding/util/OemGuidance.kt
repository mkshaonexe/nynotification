package com.quietinbox.feature.onboarding.util

import android.os.Build
import androidx.annotation.StringRes
import com.quietinbox.R

/**
 * Identifies OEM manufacturers and provides manufacturer-specific guidance
 * for navigating system settings to grant notification listener access.
 */
enum class OemBrand(
    val displayName: String,
    @StringRes val instructionsResId: Int,
    val isAggressive: Boolean
) {
    SAMSUNG("Samsung", R.string.onboarding_oem_samsung, isAggressive = true),
    XIAOMI("Xiaomi / Redmi / POCO", R.string.onboarding_oem_xiaomi, isAggressive = true),
    OPPO("Oppo / OnePlus / Realme", R.string.onboarding_oem_oppo, isAggressive = true),
    VIVO("Vivo / iQOO", R.string.onboarding_oem_vivo, isAggressive = true),
    HUAWEI("Huawei / Honor", R.string.onboarding_oem_huawei, isAggressive = true),
    STOCK("Android", R.string.onboarding_oem_stock, isAggressive = false);

    companion object {
        /**
         * Resolves the device's [OemBrand] from the given [manufacturer] string (defaults to [Build.MANUFACTURER]).
         */
        fun fromManufacturer(manufacturer: String = Build.MANUFACTURER): OemBrand {
            val normalized = manufacturer.trim().lowercase()
            return when {
                normalized.contains("samsung") -> SAMSUNG
                normalized.contains("xiaomi") || normalized.contains("redmi") ||
                    normalized.contains("poco") || normalized.contains("blackshark") -> XIAOMI
                normalized.contains("oppo") || normalized.contains("oneplus") ||
                    normalized.contains("realme") -> OPPO
                normalized.contains("vivo") || normalized.contains("iqoo") -> VIVO
                normalized.contains("huawei") || normalized.contains("honor") -> HUAWEI
                else -> STOCK
            }
        }
    }
}
