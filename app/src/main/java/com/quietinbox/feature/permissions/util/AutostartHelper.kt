package com.quietinbox.feature.permissions.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.quietinbox.feature.onboarding.util.OemBrand

/**
 * Helper utility for launching manufacturer-specific autostart/background-management
 * settings screens, with fallback written guidance.
 */
object AutostartHelper {

    /**
     * Attempts to open the OEM autostart / background management settings.
     * Returns true if an intent was successfully launched, false if not supported.
     */
    fun openAutostartSettings(context: Context): Boolean {
        val brand = OemBrand.fromManufacturer()
        val intents = getOemIntents(context, brand)

        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return true
                }
            } catch (_: Exception) {
                // Try next candidate
            }
        }

        // Fallback to standard App Details settings
        return try {
            val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Returns detailed step-by-step written instructions for the given device manufacturer.
     */
    fun getWrittenInstructions(brand: OemBrand = OemBrand.fromManufacturer()): String {
        return when (brand) {
            OemBrand.XIAOMI ->
                "1. Open Settings → Apps → Manage apps (or Permissions).\n" +
                "2. Find 'Quiet Inbox' and tap it.\n" +
                "3. Enable 'Autostart'.\n" +
                "4. Tap 'Battery saver' and select 'No restrictions'."

            OemBrand.SAMSUNG ->
                "1. Open Settings → Battery and device care → Battery.\n" +
                "2. Tap 'Background usage limits' → 'Never sleeping apps'.\n" +
                "3. Tap '+' and add 'Quiet Inbox'."

            OemBrand.OPPO ->
                "1. Open Settings → Battery → App battery management (or More battery settings).\n" +
                "2. Tap 'Quiet Inbox'.\n" +
                "3. Enable 'Allow auto-launch' and 'Allow background activity'."

            OemBrand.VIVO ->
                "1. Open Settings → Battery → High background power consumption (or Background power consumption management).\n" +
                "2. Find 'Quiet Inbox' and enable it to allow background power."

            OemBrand.HUAWEI ->
                "1. Open Settings → Apps → App launch (or Launch).\n" +
                "2. Find 'Quiet Inbox' and toggle from Automatic to 'Manage manually'.\n" +
                "3. Enable 'Auto-launch', 'Secondary launch', and 'Run in background'."

            OemBrand.STOCK ->
                "1. Open Settings → Apps → Quiet Inbox.\n" +
                "2. Tap 'App battery usage' (or Battery).\n" +
                "3. Choose 'Unrestricted'."
        }
    }

    private fun getOemIntents(context: Context, brand: OemBrand): List<Intent> {
        val intents = mutableListOf<Intent>()

        when (brand) {
            OemBrand.XIAOMI -> {
                intents.add(Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")))
                intents.add(Intent("miui.intent.action.OP_AUTO_START").addCategory(Intent.CATEGORY_DEFAULT))
            }
            OemBrand.HUAWEI -> {
                intents.add(Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")))
                intents.add(Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")))
                intents.add(Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")))
            }
            OemBrand.OPPO -> {
                intents.add(Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")))
                intents.add(Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")))
                intents.add(Intent().setComponent(ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")))
            }
            OemBrand.VIVO -> {
                intents.add(Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")))
                intents.add(Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")))
                intents.add(Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")))
            }
            OemBrand.SAMSUNG -> {
                intents.add(Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")))
                intents.add(Intent().setComponent(ComponentName("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity")))
            }
            OemBrand.STOCK -> {
                // No OEM specific autostart app
            }
        }

        return intents
    }
}
