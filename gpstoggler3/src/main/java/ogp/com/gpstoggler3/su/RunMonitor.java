package ogp.com.gpstoggler3.su;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import ogp.com.gpstoggler3.results.RPCResult;
import ogp.com.gpstoggler3.settings.Settings;
import ogp.com.gpstoggler3.global.Constants;


public class RunMonitor {
    private static final String MONITOR_PACKAGE = "ogp.com.gpstoggler3monitor";
    private static final String MONITOR_APK = "gpstoggler3monitor.apk";
    private static final String SUCCESS = "Success";
    private static final String MONITOR_ACTIVITY = ".TransparentActivity";
    private static final int MIN_MONITOR_SIZE = 4 * 1024;
    private static final int MAX_MONITOR_SIZE = 32 * 1024 * 1024;

    private Context context;


    public RunMonitor(Context context) {
        this.context = context;
    }


    public boolean installMonitor() {
        return installMonitor(false);
    }


    public static byte[] readMonitorApk(Context context) {
        Log.v(Constants.TAG, "RunMonitor::readMonitorApk. Entry...");

        AssetManager am = context.getAssets();
        InputStream inputStream = null;

        try {
            inputStream = am.open(MONITOR_APK);

            for (int bufLen = MIN_MONITOR_SIZE; bufLen <= MAX_MONITOR_SIZE; bufLen <<= 1) {
                byte[] buf = new byte[bufLen + 1];
                int read = inputStream.read(buf);
                if (read == bufLen + 1) {
                    Log.v(Constants.TAG, String.format("RunMonitor::readMonitorApk. APK has been read partially, %d bytes only.", read));

                    inputStream.reset();
                } else {
                    Log.d(Constants.TAG, String.format("RunMonitor::readMonitorApk. APK has been read, %d bytes.", read));
                    byte[] buf2 = new byte[read];
                    System.arraycopy(buf, 0, buf2, 0, read);

                    Log.v(Constants.TAG, "RunMonitor::readMonitorApk. Exit [1].");
                    return buf2;
                }
            }

            Log.e(Constants.TAG, "RunMonitor::readMonitorApk. Failed to read the APK file. Too large APK or generic read fault.");
            Log.v(Constants.TAG, "RunMonitor::readMonitorApk. Exit [2].");
            return null;
        } catch (IOException e) {
            Log.e(Constants.TAG, "RunMonitor::readMonitorApk. Exit [Error 1]. Exception: ", e);
            Log.v(Constants.TAG, "RunMonitor::readMonitorApk. Exit [3].");
            return null;
        } finally {
            try {
                if (null != inputStream) {
                    inputStream.close();
                }
            } catch (IOException ignored) {
            }
        }
    }


    @SuppressWarnings("WeakerAccess")
    boolean installMonitor(boolean forced) {
        Log.v(Constants.TAG, String.format("RunMonitor::installMonitor%s. Entry...", forced ? " [forced]" : ""));

        // 0. Check if permissions to be granded
        if (!Settings.isMonitorAppUsed()) {
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
            byte[] buffer = readMonitorApk(context);
            if (null != buffer) {
                String sdPath = Environment.getExternalStorageDirectory().getPath();
                String copiedPath = sdPath + "/" + MONITOR_APK;
                OutputStream outputStream = null;

                try {
                    outputStream = new FileOutputStream(copiedPath);
                    outputStream.write(buffer, 0, buffer.length);

                    Log.d(Constants.TAG, String.format("RunMonitor::installMonitor. APK copied with %d bytes.", buffer.length));
                } catch (IOException e) {
                    Log.v(Constants.TAG, "RunMonitor::installMonitor. Exit [Error 1]. Exception: ", e);
                    return false;
                } finally {
                    try {
                        if (null != outputStream) {
                            outputStream.close();
                        }
                    } catch (IOException ignored) {
                    }
                }


                RPCResult returned = RootCaller.executeOnRoot("pm install -r -d " + copiedPath);
                if (!returned.isError() && 0 < returned.size() && ((String)returned.get(0)).startsWith(SUCCESS)) {
                    Log.i(Constants.TAG, "RunMonitor::installMonitor. Monitor APK installed from: " + copiedPath);
                } else {
                    Log.e(Constants.TAG, "RunMonitor::installMonitor. Failed to install Monitor APK from: " + copiedPath);

                    if (!returned.isError()) {
                        for (Object lineO : returned.getList()) {
                            Log.v(Constants.TAG, ">>> " + lineO);
                        }
                    }

                    Log.v(Constants.TAG, "===================================");
                }

                File copyFile = new File(copiedPath);
                boolean deleted = copyFile.delete();
                Log.d(Constants.TAG, "RunMonitor::installMonitor. APK " + (deleted ? "deleted." : "failed to delete."));
            } else {
                Log.e(Constants.TAG, "RunMonitor::installMonitor. Failed to read APK.");
            }
        }

        Log.v(Constants.TAG, "RunMonitor::installMonitor. Exit [2].");
        return true;
    }


    private boolean uninstallMonitor(boolean totally) {
        Log.v(Constants.TAG, String.format("RunMonitor::uninstallMonitor. Entry%s...", totally ? " (totally)" : "(reinstall)"));

        String command = String.format("pm uninstall%s %s", totally ? "" : "-k", MONITOR_PACKAGE);
        RPCResult returned = RootCaller.executeOnRoot(command);
        boolean success = false;

        if (!returned.isError() && 0 < returned.size() && ((String)returned.get(0)).startsWith(SUCCESS)) {
            Log.i(Constants.TAG, "RunMonitor::installMonitor. Monitor APK [" + MONITOR_PACKAGE + "%s] uninstalled.");
            success = true;
        } else {
            Log.e(Constants.TAG, "RunMonitor::installMonitor. Failed to uninstall Monitor APK [" + MONITOR_PACKAGE + "].");

            if (!returned.isError()) {
                for (Object lineO : returned.getList()) {
                    Log.v(Constants.TAG, ">>> " + lineO);
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
        if (!Settings.isMonitorAppUsed()) {
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
