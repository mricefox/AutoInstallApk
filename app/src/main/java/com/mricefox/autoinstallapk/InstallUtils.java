package com.mricefox.autoinstallapk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.content.FileProvider;

import java.io.File;

/**
 * <p>Author:MrIcefox
 * <p>Email:extremetsa@gmail.com
 * <p>Description:
 * <p>Date:2018/8/6
 */

public class InstallUtils {
    private InstallUtils() {
    }

    public static boolean canInstallNonMarketApps(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return context.getPackageManager().canRequestPackageInstalls();
        } else {
            return Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS, -1) == 1;
        }
    }


    public static void installApk(File apk, Activity activity, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Uri uri = FileProvider.getUriForFile(activity, "com.mricefox.autoinstallapk.fileprovider", apk);
            activity.grantUriPermission(activity.getPackageName(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
        } else {
            intent.setDataAndType(Uri.fromFile(apk), "application/vnd.android.package-archive");
        }
        if (requestCode < 0) {
            activity.startActivity(intent);
        } else {
            activity.startActivityForResult(intent, requestCode);
        }
    }

    public static boolean accessibilityEnabled(Context context) {
        //任意一个app开启了accessibility
        if (Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, -1) == 1) {
            String services = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            String[] arr = services.split(":");
            String autoInstall = context.getPackageName() + "/" + AutoInstallService.class.getCanonicalName();

            for (String svr : arr) {
                if (autoInstall.equalsIgnoreCase(svr)) {
                    return true;
                }
            }
        }
        return false;
    }
}
