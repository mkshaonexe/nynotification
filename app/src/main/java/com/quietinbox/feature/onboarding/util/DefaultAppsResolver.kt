package com.quietinbox.feature.onboarding.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.Telephony
import android.telecom.TelecomManager
import com.quietinbox.R

/**
 * Category of essential system apps presented during onboarding.
 */
enum class DefaultAppCategory(val defaultLabelResId: Int) {
    PHONE(R.string.onboarding_app_phone),
    MESSAGES(R.string.onboarding_app_messages),
    CLOCK(R.string.onboarding_app_clock)
}

/**
 * Represents a pre-selected default app choice.
 */
data class DefaultAppCandidate(
    val packageName: String,
    val label: String,
    val category: DefaultAppCategory,
    val isSelected: Boolean = true
)

/**
 * Utility for resolving essential communication and alarm applications
 * using system intents rather than hardcoded package names.
 */
object DefaultAppsResolver {

    /**
     * Resolves the default dialer, SMS, and clock packages on the device.
     */
    fun resolveDefaultApps(context: Context): List<DefaultAppCandidate> {
        val pm = context.packageManager
        val candidates = mutableListOf<DefaultAppCandidate>()

        // 1. Phone / Dialer
        val phonePkg = resolveDialerPackage(context, pm)
        if (phonePkg != null) {
            val label = getAppLabel(pm, phonePkg, context.getString(R.string.onboarding_app_phone))
            candidates.add(DefaultAppCandidate(phonePkg, label, DefaultAppCategory.PHONE, isSelected = true))
        }

        // 2. Messages / SMS
        val smsPkg = resolveSmsPackage(context, pm)
        if (smsPkg != null && smsPkg != phonePkg) {
            val label = getAppLabel(pm, smsPkg, context.getString(R.string.onboarding_app_messages))
            candidates.add(DefaultAppCandidate(smsPkg, label, DefaultAppCategory.MESSAGES, isSelected = true))
        }

        // 3. Clock / Alarms
        val clockPkg = resolveClockPackage(pm)
        if (clockPkg != null && clockPkg != phonePkg && clockPkg != smsPkg) {
            val label = getAppLabel(pm, clockPkg, context.getString(R.string.onboarding_app_clock))
            candidates.add(DefaultAppCandidate(clockPkg, label, DefaultAppCategory.CLOCK, isSelected = true))
        }

        return candidates
    }

    private fun resolveDialerPackage(context: Context, pm: PackageManager): String? {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        val defaultDialer = telecomManager?.defaultDialerPackage
        if (!defaultDialer.isNullOrBlank()) {
            return defaultDialer
        }

        val dialIntent = Intent(Intent.ACTION_DIAL)
        val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.resolveActivity(dialIntent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.resolveActivity(dialIntent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        return resolveInfo?.activityInfo?.packageName
    }

    private fun resolveSmsPackage(context: Context, pm: PackageManager): String? {
        val defaultSms = Telephony.Sms.getDefaultSmsPackage(context)
        if (!defaultSms.isNullOrBlank()) {
            return defaultSms
        }

        val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))
        val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.resolveActivity(smsIntent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.resolveActivity(smsIntent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        return resolveInfo?.activityInfo?.packageName
    }

    private fun resolveClockPackage(pm: PackageManager): String? {
        val alarmIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
        val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.resolveActivity(alarmIntent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.resolveActivity(alarmIntent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        return resolveInfo?.activityInfo?.packageName
    }

    private fun getAppLabel(pm: PackageManager, packageName: String, fallback: String): String {
        return try {
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            fallback
        }
    }
}
