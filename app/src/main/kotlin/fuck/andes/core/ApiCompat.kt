package fuck.andes.core

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.WindowManager

@Suppress("DEPRECATION")
object ApiCompat {

    fun getPackageInfo(pm: PackageManager, packageName: String, flags: Int = 0): PackageInfo? =
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                pm.getPackageInfo(packageName, flags)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }

    fun getApplicationInfo(pm: PackageManager, packageName: String, flags: Int = 0): ApplicationInfo? =
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(flags.toLong()))
            } else {
                pm.getApplicationInfo(packageName, flags)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }

    fun getInstalledPackages(pm: PackageManager, flags: Int = 0): List<PackageInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(flags)
        }

    fun getInstalledApplications(pm: PackageManager, flags: Int = 0): List<ApplicationInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledApplications(flags)
        }

    fun queryIntentActivities(pm: PackageManager, intent: Intent, flags: Int = 0): List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, flags)
        }

    fun resolveActivity(pm: PackageManager, intent: Intent, flags: Int = 0): ResolveInfo? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.resolveActivity(intent, flags)
        }

    @Suppress("UNCHECKED_CAST")
    fun <T : Parcelable> getParcelableArrayList(bundle: Bundle, key: String, clazz: Class<T>): ArrayList<T>? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getParcelableArrayList(key, clazz)
        } else {
            val list = bundle.getParcelableArrayList(key) ?: return null
            val result = ArrayList<T>(list.size)
            for (item in list) {
                if (clazz.isInstance(item)) {
                    result.add(item as T)
                }
            }
            result
        }

    fun <T : Parcelable> getParcelable(bundle: Bundle, key: String, clazz: Class<T>): T? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getParcelable(key, clazz)
        } else {
            bundle.getParcelable(key) as? T
        }

    fun <T : Parcelable> getParcelableExtra(intent: Intent, key: String, clazz: Class<T>): T? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(key, clazz)
        } else {
            intent.getParcelableExtra(key) as? T
        }

    fun getRealDisplaySize(context: Context): Point {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val point = Point()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = wm.currentWindowMetrics
            point.x = metrics.bounds.width()
            point.y = metrics.bounds.height()
        } else {
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealSize(point)
        }
        return point
    }

    fun hasAppListPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return true
        }
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    val isAtLeastR: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    val isAtLeastT: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val isAtLeastS: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}
