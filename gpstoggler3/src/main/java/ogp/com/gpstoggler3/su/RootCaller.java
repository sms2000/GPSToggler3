package ogp.com.gpstoggler3.su;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ogp.com.gpstoggler3.global.Constants;


public class RootCaller {
    private final static String CMD_SU = "su";
    private final static String CMD_EXEC = "-c";
    private static final String[] SU_PATHES = {"/system/xbin/which", "/system/bin/which"};

    private static boolean securitySettingsSet = false;
    private static RootExecutor rootExecutor = null;

    public enum RootStatus {NO_ROOT, ROOT_FAILED, ROOT_GRANTED}


    public static class RootExecutor {
        private static final String COMMAND_ANSWER_END = ">###<";
        private static final String COMMAND_TAIL = ";echo '\n" + COMMAND_ANSWER_END + "'\n";
        private Process chperm;
        private BufferedReader reader;
        private BufferedWriter writer;

        RootExecutor(Process chperm) {
            this.chperm = chperm;
            this.reader = new BufferedReader(new InputStreamReader(chperm.getInputStream()));
            this.writer = new BufferedWriter(new OutputStreamWriter(chperm.getOutputStream()));
        }


        public synchronized List<String> executeOnRoot (String command) {
            Log.v(Constants.TAG, "RootCaller::RootExecutor::executeOnRoot. Entry...");
            Log.d(Constants.TAG, "RootCaller::RootExecutor::executeOnRoot. Running command: " + command);

            List<String> output = new ArrayList<>();

            try {
                int realLines = 0;
                command += COMMAND_TAIL;

                writer.write(command, 0, command.length());
                writer.flush();

                while (true) {
                    String string = reader.readLine();
                    if (null == string) {
                        continue;
                    } else if (string.contains(COMMAND_ANSWER_END)) {
                        break;
                    }

                    output.add(string);
                    realLines++;
                }

                if (0 < realLines) {
                    Log.d(Constants.TAG, String.format("RootCaller::RootExecutor::executeOnRoot. Output includes %d lines.", realLines));
                }
            } catch (IOException e) {
                Log.e(Constants.TAG, "RootCaller::RootExecutor::executeOnRoot. IOException with: " + e.getMessage());
                output = null;
            } catch (Exception e) {
                Log.e(Constants.TAG, "RootCaller::RootExecutor::executeOnRoot. Exception: ", e);
                output = null;
            }

            Log.v(Constants.TAG, "RootCaller::RootExecutor::executeOnRoot. Exit.");
            return output;
        }
    }



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


    public static RootStatus setSecureSettings(Context context, String packageName, String appServiceName) {
        if (securitySettingsSet) {
            return RootStatus.ROOT_GRANTED;
        }

        securitySettingsSet = true;

        Log.v(Constants.TAG, String.format("RootCaller::setSecureSettings. Entry (packageName: %s)...", packageName));

        Log.i(Constants.TAG, "RootCaller::setSecureSettings. Hacking Android. Attempt to set 'android.permission.WRITE_SECURE_SETTINGS'...");

        int stage = 0;
        String command = String.format("pm grant %s android.permission.WRITE_SECURE_SETTINGS", packageName);
        RootStatus success = executeSystemCommand(command, ++stage);

        if (RootStatus.ROOT_GRANTED == success) {
            command = String.format("pm grant %s android.permission.BIND_ACCESSIBILITY_SERVICE", packageName);
            success = executeSystemCommand(command, ++stage);
        }

        if (RootStatus.ROOT_GRANTED == success) {
            success = executeSystemCommand("settings put secure location_providers_allowed gps,network,wifi", ++stage);
        }

        if (RootStatus.ROOT_GRANTED == success) {
            String services = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (null == services) {
                services = "";
            }

            if (!services.contains(appServiceName)) {
                if (!services.isEmpty()) {
                    services += ":";
                }

                services += packageName + "/" + appServiceName;

                command = String.format("settings put secure enabled_accessibility_services %s", services);
                success = executeSystemCommand(command, ++stage);
            } else {
                Log.d(Constants.TAG, String.format(Locale.US, "----- Stage %d ommited ----", stage));
                success = RootStatus.ROOT_GRANTED;
            }
        }

        if (RootStatus.ROOT_GRANTED == success) {
            success = executeSystemCommand("settings put secure accessibility_enabled 1", ++stage);
        }

        if (RootStatus.ROOT_GRANTED == success) {
            Log.i(Constants.TAG, "RootCaller::setSecureSettings. All the systemizing tasks executed successfully.");
        } else {
            Log.e(Constants.TAG, "RootCaller::setSecureSettings. Some of the systemizing tasks failed.");
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


    static List<String> executeOnRoot(String command) {
        Log.v(Constants.TAG, "RootCaller::executeOnRoot. Entry...");

        List<String> output = null;
        RootExecutor executor = createRootProcess();
        if (null != executor) {
            output = executor.executeOnRoot(command);
            if (null != output) {
                Log.d(Constants.TAG, String.format("RootCaller::executeOnRoot. Executed command [%s]. Returned %d string(s).", command, output.size()));
            } else {
                Log.d(Constants.TAG, String.format("RootCaller::executeOnRoot. Executed command [%s]. Returned no strings.", command));
            }
        } else {
            Log.e(Constants.TAG, String.format("RootCaller::executeOnRoot. Failed to execute command [%s]. No 'root' process available.", command));
        }

        Log.v(Constants.TAG, "RootCaller::executeOnRoot. Exit.");
        return output;
    }


    public static RootExecutor createRootProcess() {
        if (null == rootExecutor) {
            try {
                Process chperm = Runtime.getRuntime().exec(new String[]{CMD_SU});
                return new RootExecutor(chperm);
            } catch (IOException ignored) {
                return null;
            }
        }

        return rootExecutor;
    }


    public static void terminateRootProcess(RootExecutor rootExecutor) {
        try {
            rootExecutor.chperm.destroy();
        } catch (Throwable ignored) {
        }
    }


    private static RootStatus executeSystemCommand(String command, int stage) {
        RootStatus success = RootStatus.ROOT_GRANTED;

        Log.e(Constants.TAG, String.format("RootCaller::setSecureSettings. Hacking Android. Attempt to set '%s'...", command));

        List<String> returned = executeOnRoot(command);
        boolean emptyResult = true;

        if (null != returned && 0 < returned.size()) {
            for (String line : returned) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                if (emptyResult) {
                    Log.d(Constants.TAG, String.format(Locale.US, "----- Result of stage %d ----", stage));
                    emptyResult = false;
                }

                Log.i(Constants.TAG, line);
            }
        }

        if (emptyResult) {
            Log.d(Constants.TAG, String.format(Locale.US, "----- Empty result of stage %d (expected) ----", stage));
        } else {
            success = RootStatus.ROOT_FAILED;
        }

        return success;
    }
}
