package ogp.com.gpstoggler3.su;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import ogp.com.gpstoggler3.Manifest;
import ogp.com.gpstoggler3.apps.Settings;
import ogp.com.gpstoggler3.debug.Constants;


public class RunMonitor {
    private static final String MONITOR_PACKAGE = "ogp.com.gpstoggler3monitor";
    private static final String MONITOR_APK = "gpstoggler3monitor.apk";
    private static final String SUCCESS = "Success";
    private static final String MONITOR_ACTIVITY = ".TransparentActivity";

    private Context context;


    public RunMonitor(Context context) {
        this.context = context;
    }


    public boolean installMonitor() {
        return installMonitor(false);
    }


    @SuppressWarnings("WeakerAccess")
    boolean installMonitor(boolean forced) {
        Log.v(Constants.TAG, String.format("RunMonitor::installMonitor%s. Entry...", forced ? " [forced]" : ""));

        // 0. Check if permissions to be granded
        if (Settings.isMonitorDeclined()) {
            Log.v(Constants.TAG, "RunMonitor::installMonitor. Exit [1].");
            return false;
        }

        // 1. Check if installed?
        boolean needUninstall;
        boolean needInstall;

        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(MONITOR_PACKAGE, PackageManager.GET_ACTIVITIES);

            if (forced) {
                Log.i(Constants.TAG, "RunMonitor::installMonitor. Monitor package found. Have to uninstall and install the new one...");
                needInstall = needUninstall = true;
            } else {
                Log.i(Constants.TAG, "RunMonitor::installMonitor. Monitor package found. Nothing to do here.");
                needInstall = needUninstall = false;
            }
        } catch (PackageManager.NameNotFoundException e) {
            needUninstall = false;
            needInstall = true;
            Log.w(Constants.TAG, "RunMonitor::installMonitor. Monitor package not found. Install it...");
        }

        // 2. Uninstall if needed
        if (needUninstall) {
            uninstallMonitor(false);
        }


        // 3. Instal if needed
        if (needInstall) {
            AssetManager am = context.getAssets();
            String sdPath = Environment.getExternalStorageDirectory().getPath();
            String copiedPath = sdPath + "/" + MONITOR_APK;
            File copyFile = new File(copiedPath);

            try {
                InputStream inputStream = am.open(MONITOR_APK);
                OutputStream outputStream = new FileOutputStream(copiedPath);

                int size = (int)am.openFd(MONITOR_APK).getLength();
                byte buf[] = new byte[size];
                int read = inputStream.read(buf);
                outputStream.write(buf, 0, read);

                outputStream.close();
                inputStream.close();

                Log.d(Constants.TAG, String.format("RunMonitor::installMonitor. APK copied with %d bytes of %d.", read, size));
            } catch (IOException e) {
                Log.v(Constants.TAG, "RunMonitor::installMonitor. Exit [Error 1]. Exception: ", e);
                return false;
            }


            List<String> returned = RootCaller.executeOnRoot("pm install -r -d " + copiedPath);
            if (null != returned && returned.size() > 0 && returned.get(0).startsWith(SUCCESS)) {
                Log.i(Constants.TAG, "RunMonitor::installMonitor. Monitor APK installed from: " + copiedPath);
            } else {
                Log.e(Constants.TAG, "RunMonitor::installMonitor. Failed to install Monitor APK from: " + copiedPath);

                if (null != returned) {
                    for (String line : returned) {
                        Log.v(Constants.TAG, ">>> " + line);
                    }
                }

                Log.v(Constants.TAG, "===================================");
            }


            boolean deleted = copyFile.delete();
            Log.d(Constants.TAG, "RunMonitor::installMonitor. APK " + (deleted ? "deleted." : "failed to delete."));
        }

        Log.v(Constants.TAG, "RunMonitor::installMonitor. Exit [2].");
        return true;
    }


    private boolean uninstallMonitor(boolean totally) {
        Log.v(Constants.TAG, String.format("RunMonitor::uninstallMonitor. Entry%s...", totally ? " (totally)" : "(reinstall)"));

        String command = String.format("pm uninstall%s %s", totally ? "" : "-k", MONITOR_PACKAGE);
        List<String> returned = RootCaller.executeOnRoot(command);
        boolean success = false;

        if (null != returned && returned.size() > 0 && returned.get(0).startsWith(SUCCESS)) {
            Log.i(Constants.TAG, "RunMonitor::installMonitor. Monitor APK [" + MONITOR_PACKAGE + "%s] uninstalled.");
            success = true;
        } else {
            Log.e(Constants.TAG, "RunMonitor::installMonitor. Failed to uninstall Monitor APK [" + MONITOR_PACKAGE + "].");

            if (null != returned) {
                for (String line : returned) {
                    Log.v(Constants.TAG, ">>> " + line);
                }
            }

            Log.v(Constants.TAG, "===================================");
        }

        Log.v(Constants.TAG, "RunMonitor::uninstallMonitor. Exit.");
        return success;
    }


    public boolean startMonitor() {
        Log.v(Constants.TAG, "RunMonitor::startMonitor. Entry...");

        // 0. Check if permissions to be granded
        if (Settings.isMonitorDeclined()) {
            Log.v(Constants.TAG, "RunMonitor::startMonitor. Exit [1].");
            return false;
        }

        Intent intent = new Intent();
        intent.setClassName(MONITOR_PACKAGE, MONITOR_ACTIVITY);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(intent);
            Log.i(Constants.TAG, "RunMonitor::startMonitor. Monitor activated successfully.");
        } catch (ActivityNotFoundException e) {
            Log.e(Constants.TAG, "RunMonitor::startMonitor. Monitor is not installed.");
        }

        Log.v(Constants.TAG, "RunMonitor::startMonitor. Exit [2].");
        return true;
    }
}
