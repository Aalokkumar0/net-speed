package com.netspeed.monitor.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

/**
 * Metadata containing One UI and Android version information for Samsung and non-Samsung devices.
 */
data class OneUIInfo(
    val isSamsung: Boolean,
    val oneUIVersion: Int,        // Major version (e.g., 6 for One UI 6.x)
    val oneUIMinorVersion: Int,   // Minor version (e.g., 1 for One UI 6.1)
    val androidVersion: Int,      // Build.VERSION.SDK_INT
    val formattedVersion: String  // e.g., "One UI 6.1", "One UI 5.0", or "Android 15 (API 35)"
)

/**
 * Encapsulates an intent with a descriptive human-readable label for debugging and logging.
 */
data class NamedIntent(
    val name: String,
    val intent: Intent
)

/**
 * Utility object for detecting Samsung One UI platform versions and navigating
 * directly to device-specific battery optimization / background usage limits screens.
 */
object SamsungDeviceUtils {

    private const val TAG = "SamsungDeviceUtils"

    /**
     * Checks whether the current device is manufactured by Samsung.
     */
    fun isSamsungDevice(): Boolean {
        return Build.MANUFACTURER.equals("samsung", ignoreCase = true) ||
                Build.BRAND.equals("samsung", ignoreCase = true)
    }

    /**
     * Inspects the platform to detect Samsung One UI version.
     * Uses reflection on [Build.VERSION.SEM_PLATFORM_INT] with an accurate fallback
     * based on Android SDK level.
     */
    fun getOneUIInfo(): OneUIInfo {
        val isSamsung = isSamsungDevice()
        val sdkInt = Build.VERSION.SDK_INT

        if (!isSamsung) {
            return OneUIInfo(
                isSamsung = false,
                oneUIVersion = 0,
                oneUIMinorVersion = 0,
                androidVersion = sdkInt,
                formattedVersion = "Android ${Build.VERSION.RELEASE} (API $sdkInt)"
            )
        }

        // Attempt to extract Samsung Extension Platform (SEP) integer
        val semPlatformInt = getSemPlatformInt()

        var majorVersion = 0
        var minorVersion = 0

        if (semPlatformInt > 0) {
            // Samsung Extension Platform integer mapping:
            // 90000 = One UI 1.0 (Android 9)
            // 100000 = One UI 1.1 / 1.5
            // 110000 = One UI 2.0 (Android 10)
            // 110100 = One UI 2.1
            // 110500 = One UI 2.5
            // 120000 = One UI 3.0 (Android 11)
            // 120100 = One UI 3.1
            // 130000 = One UI 4.0 (Android 12)
            // 130100 = One UI 4.1
            // 140000 = One UI 5.0 (Android 13)
            // 140100 = One UI 5.1
            // 150000 = One UI 6.0 (Android 14)
            // 150100 = One UI 6.1
            // 160000 = One UI 7.0 (Android 15)
            // 160100 = One UI 7.1
            if (semPlatformInt >= 90000) {
                majorVersion = (semPlatformInt - 90000) / 10000
                minorVersion = ((semPlatformInt - 90000) % 10000) / 100
            }
        }

        // Fallback if SEM_PLATFORM_INT could not be retrieved
        if (majorVersion <= 0) {
            majorVersion = when {
                sdkInt >= 35 -> 7 // Android 15 -> One UI 7.x
                sdkInt == 34 -> 6 // Android 14 -> One UI 6.x
                sdkInt == 33 -> 5 // Android 13 -> One UI 5.x
                sdkInt in 31..32 -> 4 // Android 12/12L -> One UI 4.x
                sdkInt == 30 -> 3 // Android 11 -> One UI 3.x
                sdkInt == 29 -> 2 // Android 10 -> One UI 2.x
                sdkInt == 28 -> 1 // Android 9 -> One UI 1.x
                else -> 1
            }
            minorVersion = 0
        }

        val formatted = if (minorVersion > 0) {
            "One UI $majorVersion.$minorVersion"
        } else {
            "One UI $majorVersion.0"
        }

        return OneUIInfo(
            isSamsung = true,
            oneUIVersion = majorVersion,
            oneUIMinorVersion = minorVersion,
            androidVersion = sdkInt,
            formattedVersion = formatted
        )
    }

    /**
     * Reads hidden Samsung platform integer via reflection.
     */
    private fun getSemPlatformInt(): Int {
        return try {
            val field = Build.VERSION::class.java.getDeclaredField("SEM_PLATFORM_INT")
            field.isAccessible = true
            field.getInt(null)
        } catch (_: Throwable) {
            -1
        }
    }

    /**
     * Returns a prioritized list of Samsung-specific Device Care and Battery management intents.
     */
    fun getSamsungBatteryIntents(context: Context): List<NamedIntent> {
        val intents = mutableListOf<NamedIntent>()

        // 1. One UI 5.x / 6.x / 7.x Device Care Battery Screen
        intents.add(
            NamedIntent(
                name = "Samsung Device Care Battery (One UI 5/6/7)",
                intent = Intent().apply {
                    component = ComponentName(
                        "com.samsung.android.lool",
                        "com.samsung.android.sm.battery.ui.BatteryActivity"
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        )

        // 2. One UI Background Usage Limits (Direct to Sleeping / Never sleeping apps)
        intents.add(
            NamedIntent(
                name = "Samsung Background Usage Limits",
                intent = Intent().apply {
                    component = ComponentName(
                        "com.samsung.android.lool",
                        "com.samsung.android.sm.battery.ui.setting.AppSleepListActivity"
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        )

        // 3. One UI 3.x / 4.x Device Care Battery Screen
        intents.add(
            NamedIntent(
                name = "Samsung Device Care Battery (One UI 3/4)",
                intent = Intent().apply {
                    component = ComponentName(
                        "com.samsung.android.lool",
                        "com.samsung.android.sm.ui.battery.BatteryActivity"
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        )

        // 4. One UI 3.x / 4.x Sleep List Screen
        intents.add(
            NamedIntent(
                name = "Samsung App Sleep List (One UI 3/4)",
                intent = Intent().apply {
                    component = ComponentName(
                        "com.samsung.android.lool",
                        "com.samsung.android.sm.ui.battery.AppSleepListActivity"
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        )

        // 5. Samsung Smart Manager (Alternative package name)
        intents.add(
            NamedIntent(
                name = "Samsung Smart Manager Battery",
                intent = Intent().apply {
                    component = ComponentName(
                        "com.samsung.android.sm",
                        "com.samsung.android.sm.battery.ui.BatteryActivity"
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        )

        // 6. Samsung Smart Manager (Legacy/Alternative package name)
        intents.add(
            NamedIntent(
                name = "Samsung Smart Manager Battery (Legacy)",
                intent = Intent().apply {
                    component = ComponentName(
                        "com.samsung.android.sm",
                        "com.samsung.android.sm.ui.battery.BatteryActivity"
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        )

        // 7. Samsung Smart Manager China Variant
        intents.add(
            NamedIntent(
                name = "Samsung Smart Manager Battery (CN)",
                intent = Intent().apply {
                    component = ComponentName(
                        "com.samsung.android.sm_cn",
                        "com.samsung.android.sm.ui.battery.BatteryActivity"
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        )

        // 8. Samsung Device Care Main Dashboard
        intents.add(
            NamedIntent(
                name = "Samsung Device Care Dashboard",
                intent = Intent().apply {
                    component = ComponentName(
                        "com.samsung.android.lool",
                        "com.samsung.android.sm.ui.dashboard.DashboardActivity"
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        )

        // 9. Samsung SM Battery Action Intent
        intents.add(
            NamedIntent(
                name = "Samsung SM Action Battery",
                intent = Intent("com.samsung.android.sm.ACTION_BATTERY").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        )

        return intents
    }

    /**
     * Resolves the best available Samsung battery intent by checking PackageManager.
     * Returns the first resolvable Intent, or null if none match.
     */
    fun resolveBestSamsungBatteryIntent(context: Context): Intent? {
        if (!isSamsungDevice()) return null

        val pm = context.packageManager
        val intents = getSamsungBatteryIntents(context)

        for (namedIntent in intents) {
            try {
                val resolveInfo = pm.resolveActivity(namedIntent.intent, PackageManager.MATCH_DEFAULT_ONLY)
                if (resolveInfo != null) {
                    Log.d(TAG, "Resolved Samsung battery intent: ${namedIntent.name}")
                    return namedIntent.intent
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed resolving intent ${namedIntent.name}: ${e.message}")
            }
        }
        return null
    }

    /**
     * Launches the optimal battery optimization / background settings screen.
     * Priority:
     * 1. Samsung One UI Device Care / Background Limits screen (if Samsung device)
     * 2. Android ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS (direct prompt for this package)
     * 3. Android ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS (system-wide list)
     * 4. Android ACTION_APPLICATION_DETAILS_SETTINGS (App info page fallback)
     *
     * @return true if an activity was successfully launched, false otherwise.
     */
    fun launchBatteryOptimization(context: Context): Boolean {
        // 1. Try Samsung specific intent first
        val samsungIntent = resolveBestSamsungBatteryIntent(context)
        if (samsungIntent != null) {
            try {
                context.startActivity(samsungIntent)
                Log.d(TAG, "Successfully launched Samsung battery screen")
                return true
            } catch (e: Exception) {
                Log.w(TAG, "Failed to launch Samsung intent, trying standard Android fallback: ${e.message}")
            }
        }

        // 2. Try direct package-specific ignore battery optimization request
        try {
            val requestIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (context.packageManager.resolveActivity(requestIntent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                context.startActivity(requestIntent)
                Log.d(TAG, "Launched ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS")
                return true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS: ${e.message}")
        }

        // 3. Try standard Ignore Battery Optimization Settings list
        try {
            val listIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (context.packageManager.resolveActivity(listIntent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                context.startActivity(listIntent)
                Log.d(TAG, "Launched ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS")
                return true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS: ${e.message}")
        }

        // 4. Ultimate fallback: Application Details Settings
        try {
            val appDetailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(appDetailsIntent)
            Log.d(TAG, "Launched ACTION_APPLICATION_DETAILS_SETTINGS")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "All battery optimization intents failed: ${e.message}")
            return false
        }
    }
}
