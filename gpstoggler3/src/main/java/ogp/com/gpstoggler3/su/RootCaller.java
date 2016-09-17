package ogp.com.gpstoggler3.su;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import ogp.com.gpstoggler3.global.Constants;


public class RootCaller {
    private final static String CMD_SU = "su";
    private final static String CMD_EXEC = "-c";
    private static final String[] SU_PATHES = {"/system/xbin/which", "/system/bin/which"};

    private static boolean securitySettingsSet = false;


    public enum RootStatus {NO_ROOT, ROOT_FAILED, ROOT_GRANTED}


    public static class RootExecutor {
        private static final String COMMAND_ANSWER_END = ">###<";
        private static final String COMMAND_TAIL = ";echo '\n" + COMMAND_ANSWER_END + "'\n";
        private Process chperm;
        private BufferedReader reader;
        private BufferedWriter writer;

        public RootExecutor(Process chperm) {
            this.chperm = chperm;
            this.reader = new BufferedReader(new InputStreamReader(chperm.getInputStream()));
            this.writer = new BufferedWriter(new OutputStreamWriter(chperm.getOutputStream()));
        }


        public List<String> executeOnRoot (String command) {
            Log.v(Constants.TAG, "RootCaller::RootExecutor::executeOnRoot. Entry...");

            List<String> output = new ArrayList<>();

            try {
                command += COMMAND_TAIL;

                writer.write(command, 0, command.length());
                writer.flush();

                String string = "";
                while (true) {
                    string = reader.readLine();
                    if (null == string) {
                        continue;
                    } else if (string.contains(COMMAND_ANSWER_END)) {
                        break;
                    }

                    output.add(string);
                }


                Log.d(Constants.TAG, String.format("RootCaller::RootExecutor::executeOnRoot. Output includes %d lines.", output.size()));
            } catch (Exception e) {
                Log.e(Constants.TAG, "RootCaller::RootExecutor::executeOnRoot. Exception: ", e);
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


    public static RootExecutor createRootProcess() {
        Process chperm = null;

        try {
            chperm = Runtime.getRuntime().exec(new String[]{CMD_SU});

            return new RootExecutor(chperm);
        } catch (IOException e) {
            return null;
        }
    }


    public static void terminateRootProcess(RootExecutor rootExecutor) {
        try {
            rootExecutor.chperm.destroy();
        } catch (Throwable ignored) {
        }
    }


    private static RootStatus executeSystemCommand(String command, int stage) {
        RootStatus success = RootStatus.ROOT_GRANTED;

        try {
            Thread.sleep(100);
        } catch (InterruptedException ignore) {
        }

        Log.e(Constants.TAG, String.format("RootCaller::setSecureSettings. Hacking Android. Attempt to set '%s'...", command));

        List<String> returned = executeOnRoot(command);

        if (null != returned && 0 < returned.size()) {
            String logS = String.format(Locale.US, "----- Result of stage %d ----", stage);

            Log.d(Constants.TAG, logS);
            for (String line : returned) {
                Log.i(Constants.TAG, line);
            }
            Log.d(Constants.TAG, logS);

            success = RootStatus.ROOT_FAILED;
        } else {
            Log.d(Constants.TAG, String.format(Locale.US, "----- Empty result of stage %d (expected) ----", stage));
        }

        return success;
    }
}
