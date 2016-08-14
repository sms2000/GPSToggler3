package ogp.com.gpstoggler3.su;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import ogp.com.gpstoggler3.debug.Constants;


public class RootCaller {
    private final static String CMD_SU = "su";
    private final static String CMD_EXEC = "-c";
    private static final String[] SU_PATHES = {"/system/xbin/which", "/system/bin/which"};

    public enum RootStatus {NO_ROOT, ROOT_FAILED, ROOT_GRANTED}


    public static RootStatus ifRootAvailable() {
        Log.v(Constants.TAG, "MainActivity::ifRootAvailable. Entry...");

        RootStatus success = RootStatus.NO_ROOT;
        java.lang.Process chperm = null;

        for (String whichPath : SU_PATHES) {
            try {
                chperm = Runtime.getRuntime().exec(new String[]{whichPath, CMD_SU});
                BufferedReader in = new BufferedReader(new InputStreamReader(chperm.getInputStream()));
                if (in.readLine() != null) {
                    success = RootStatus.ROOT_GRANTED;
                    Log.i(Constants.TAG, "MainActivity::ifRootAvailable. 'Root' exists.");
                    break;
                }

                Log.i(Constants.TAG, "MainActivity::ifRootAvailable. 'Root' doesn't exist.");
            } catch (IOException e) {
                Log.e(Constants.TAG, "MainActivity::ifRootAvailable. No 'root' available.");
            } catch (Throwable t) {
                Log.e(Constants.TAG, "MainActivity::ifRootAvailable. Exception: ", t);
            } finally {
                if (chperm != null) {
                    chperm.destroy();
                }
            }
        }

        Log.v(Constants.TAG, "MainActivity::ifRootAvailable. Exit.");
        return success;
    }


    public static RootStatus setSecureSettings(String packageName) {
        Log.v(Constants.TAG, String.format("RootCaller::setSecureSettings. Entry (packageName: %s)...", packageName));

        RootStatus success = RootStatus.ROOT_FAILED;

        Log.i(Constants.TAG, "RootCaller::setSecureSettings. Hacking Android. Attempt to set 'android.permission.WRITE_SECURE_SETTINGS'...");

        String command = String.format("pm grant %s android.permission.WRITE_SECURE_SETTINGS", packageName);
        List<String> returned = executeOnRoot(command);

        if (null != returned && 0 < returned.size()) {
            Log.d(Constants.TAG, "----- Result of stage 1 ----");
            for (String line : returned) {
                Log.i(Constants.TAG, line);
            }

            Log.d(Constants.TAG, "----- Result of stage 1 ----");
        } else {
            Log.d(Constants.TAG, "----- Empty result of stage 1 (expected) ----");
            success = RootStatus.ROOT_GRANTED;
        }

        /*
        if (RootStatus.ROOT_GRANTED == success) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignore) {
            }

            Log.i(Constants.TAG, "RootCaller::setSecureSettings. Hacking Android. Attempt to set 'android.permission.REAL_GET_TASKS'...");

            command = String.format("pm grant %s android.permission.REAL_GET_TASKS", packageName);
            returned = executeOnRoot(command);

            if (null != returned && 0 < returned.size()) {
                Log.d(Constants.TAG, "----- Result of stage 2 ----");
                for (String line : returned) {
                    Log.i(Constants.TAG, line);
                }

                Log.d(Constants.TAG, "----- Result of stage 2 ----");
            } else {
                Log.d(Constants.TAG, "----- Empty result of stage 2 (expected) ----");
                success = RootStatus.ROOT_GRANTED;
            }
        }
        */

        if (RootStatus.ROOT_GRANTED == success) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignore) {
            }

            Log.i(Constants.TAG, "RootCaller::setSecureSettings. Hacking Android. Attempt to set 'settings put secure location_providers_allowed gps,network,wifi'...");

            returned = executeOnRoot("settings put secure location_providers_allowed gps,network,wifi");

            if (null != returned && 0 < returned.size()) {
                Log.d(Constants.TAG, "----- Result of stage 3 ----");
                for (String line : returned) {
                    Log.i(Constants.TAG, line);
                }
                Log.d(Constants.TAG, "----- Result of stage 3 ----");

                success = RootStatus.ROOT_FAILED;
            } else {
                Log.d(Constants.TAG, "----- Empty result of stage 3 (expected) ----");
                success = RootStatus.ROOT_GRANTED;
            }
        }

        Log.v(Constants.TAG, "RootCaller::setSecureSettings. Exit [2].");
        return success;
    }


    public static boolean checkSecureSettings() {
        Log.v(Constants.TAG, "RootCaller::checkSecureSettings. Entry...");
        Log.d(Constants.TAG, "RootCaller::checkSecureSettings. Checking if Location access granted?");

        List<String> returned = executeOnRoot("settings list secure");
        if (null == returned) {
            Log.v(Constants.TAG, "RootCaller::checkSecureSettings. Exit [1].");
            return false;
        }

        boolean success = false;

        for (String answer : returned) {
            if (answer.startsWith("location_providers_allowed=")) {
                if (answer.matches("^.*?(gps|wifi|network).*$")) {
                    Log.i(Constants.TAG, "RootCaller::checkSecureSettings. Yes, the location access granted.");
                    success = true;
                } else {
                    Log.e(Constants.TAG, "RootCaller::checkSecureSettings. No, the location access denied.");
                }

                Log.v(Constants.TAG, "RootCaller::checkSecureSettings. Exit [2].");
                return success;
            }
        }

        Log.w(Constants.TAG, "RootCaller::checkSecureSettings. No, the location access denied.");
        Log.v(Constants.TAG, "RootCaller::checkSecureSettings. Exit [3].");
        return false;
    }


    public static ArrayList<String> executeOnRoot(String command) {
        Process chperm = null;

        try {
            chperm = Runtime.getRuntime().exec(new String[]{CMD_SU, CMD_EXEC, command});
            BufferedReader in = new BufferedReader(new InputStreamReader(chperm.getInputStream()));
            ArrayList<String> returned = new ArrayList<>();

            for (String answer = in.readLine(); null != answer; answer = in.readLine()) {
                returned.add(answer);
            }

            return returned;
        } catch (IOException e) {
            return null;
        } finally {
            if (null != chperm) {
                chperm.destroy();
            }
        }
    }
}
