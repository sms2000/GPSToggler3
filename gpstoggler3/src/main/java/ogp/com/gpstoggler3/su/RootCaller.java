package ogp.com.gpstoggler3.su;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import ogp.com.gpstoggler3.global.Constants;
import ogp.com.gpstoggler3.results.RPCResult;
import ogp.com.gpstoggler3.servlets.ExecuteWithTimeout;


public class RootCaller {
    private static final String CMD_SU = "su";
    private static final String[] SU_PATHES = {"/system/xbin/which", "/system/bin/which"};

    private static boolean securitySettingsSet = false;
    private static RootExecutor rootExecutor = null;
    private static int createdCounter = 0;

    public enum RootStatus {NO_ROOT, ROOT_FAILED, ROOT_GRANTED}


    public static class RootExecutor extends ExecuteWithTimeout {
        private static String COMMAND_ANSWER_END = "%d_FINISH_%d";
        private static String COMMAND_TAIL;
        private Process chperm;
        private BufferedReader reader = null;
        private BufferedWriter writer = null;


        private static class ExecuteOnRoot extends ExecuteParams {
            private String command;

            ExecuteOnRoot(final String command) {
                this.command = command;
            }
        }


        static {
            long random = Math.abs(new Random().nextInt());
            COMMAND_ANSWER_END = String.format(Locale.US, COMMAND_ANSWER_END, random, random);
            COMMAND_TAIL = ";echo '\n" + COMMAND_ANSWER_END + "'\n";
            Log.i(Constants.TAG, "RootCaller::RootExecutor::<static>. Final random string: " + COMMAND_ANSWER_END);
        }

        RootExecutor() {
            super();

            Log.v(Constants.TAG, "RootCaller::RootExecutor::<init>. Entry/Exit.");
        }


        private synchronized void initialize() {
            Log.v(Constants.TAG, "RootCaller::RootExecutor::initialize. Entry...");

            try {
                chperm = Runtime.getRuntime().exec(CMD_SU);
                reader = new BufferedReader(new InputStreamReader(chperm.getInputStream()));
                writer = new BufferedWriter(new OutputStreamWriter(chperm.getOutputStream()));

                Log.i(Constants.TAG, "RootCaller::RootExecutor::initialize. Created a root process.");
            } catch (Exception ignored) {
                chperm = null;
                Log.e(Constants.TAG, "RootCaller::RootExecutor::initialize. Failed to create a root process.");
            }

            Log.v(Constants.TAG, "RootCaller::RootExecutor::initialize. Exit.");
        }


        private void destroyProcess() {
            try {
                chperm.destroy();
            } catch (Exception ignored) {
            }

            chperm = null;
        }

        public RPCResult executeOnRoot(String command) {
            Log.v(Constants.TAG, "RootCaller::RootExecutor::executeOnRoot. Entry...");
            Log.d(Constants.TAG, "RootCaller::RootExecutor::executeOnRoot. Running command: " + command);

            RPCResult output;

            try {
                command += COMMAND_TAIL;
                output = sinkRootCommand(command);
            } catch (Exception e) {
                Log.e(Constants.TAG, "RootCaller::RootExecutor::executeOnRoot. Exception: ", e);
                output = new RPCResult(e);
            }

            Log.v(Constants.TAG, "RootCaller::RootExecutor::executeOnRoot. Exit.");
            return output;
        }


        public RPCResult executeCommander(String command, int timeoutMS) {
            Log.v(Constants.TAG, "RootCaller::RootExecutor::executeCommander. Entry...");
            Log.d(Constants.TAG, "RootCaller::RootExecutor::executeCommander. Running command: " + command);

            String command2Exec = RootCommander.getCommanderPath() + " " + COMMAND_ANSWER_END + " " + command + "\n";
            ExecuteOnRoot executeOnRoot = new ExecuteOnRoot(command2Exec);

            RPCResult result = execute(executeOnRoot, timeoutMS);
            if (!result.isError()) {
                Log.d(Constants.TAG, "RootCaller::RootExecutor::executeCommander. Proper result returned.");
            } else {
                Log.d(Constants.TAG, "RootCaller::RootExecutor::executeCommander. Ecxception returned. Timeout?");
            }

            Log.v(Constants.TAG, "RootCaller::RootExecutor::executeCommander. Exit.");
            return result;
        }


        @Override
        public RPCResult executeWithResult(final ExecuteParams params) throws InvocationTargetException {
            Log.v(Constants.TAG, "RootCaller::RootExecutor::executeWithResult. Entry...");

            RPCResult result;
            if (params instanceof ExecuteOnRoot) {
                ExecuteOnRoot executeOnRoot = (ExecuteOnRoot) params;
                result = sinkRootCommand(executeOnRoot.command);
            } else {
                executedError(new InvalidParameterException("Bad parameters: " + params.getClass().getName()));
                result = new RPCResult(new InvalidParameterException(String.format("'%s' instead of 'ExecuteOnRoot'", params.getClass().getName())));
            }

            Log.v(Constants.TAG, "RootCaller::RootExecutor::executeWithResult. Exit.");
            return result;
        }


        @Override
        public void executedError(Exception e) {
            Log.v(Constants.TAG, "RootCaller::RootExecutor::executedError. Entry...");

            Log.w(Constants.TAG, String.format("RootCaller::RootExecutor::executedError. Exception happenned [%s]. Reinitialize 'root' process.", e.getMessage()));

            Log.v(Constants.TAG, "RootCaller::RootExecutor::executedError. Exit.");
        }


        private synchronized RPCResult sinkRootCommand(String command) {
            RPCResult result;
            List<String> received = new ArrayList<>();

            try {
                boolean initNow = true;

                try {
                    chperm.exitValue();
                } catch (IllegalThreadStateException ignored) {
                    initNow = false;
                } catch (Exception ignored) {
                }

                if (initNow) {
                    initialize();
                }

                writer.write(command, 0, command.length());
                writer.flush();

                while (true) {
                    String string = reader.readLine();
                    if (null == string) {
                        continue;
                    } else if (string.equals(COMMAND_ANSWER_END)) {
                        break;
                    }

                    received.add(string);
                }

                Log.d(Constants.TAG, String.format("RootCaller::RootExecutor::sinkRootCommand. Output includes %d line(s).", received.size()));
                result = new RPCResult(received);
            } catch (Exception e) {
                Log.e(Constants.TAG, "RootCaller::RootExecutor::sinkRootCommand. Exception with: " + e.getMessage());
                e.printStackTrace();

                destroyProcess();
                executedError(new InvalidParameterException(e.getMessage()));
                result = new RPCResult(e);
            }

            return result;
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

        RPCResult returned = executeOnRoot("settings list secure");
        if (returned.isError()) {
            Log.v(Constants.TAG, "RootCaller::checkSecureSettings. Exit [1].");
            return false;
        }

        boolean success = false;

        for (Object answerO : returned.getList()) {
            if (answerO instanceof String) {
                String answerS = (String) answerO;

                if (answerS.startsWith("location_providers_allowed=")) {
                    if (answerS.matches("^.*?(gps|wifi|network).*$")) {
                        Log.i(Constants.TAG, "RootCaller::checkSecureSettings. Yes, the location access granted.");
                        success = true;
                    } else {
                        Log.e(Constants.TAG, "RootCaller::checkSecureSettings. No, the location access denied.");
                    }

                    Log.v(Constants.TAG, "RootCaller::checkSecureSettings. Exit [2].");
                    return success;
                }
            }
        }

        Log.w(Constants.TAG, "RootCaller::checkSecureSettings. No, the location access denied.");
        Log.v(Constants.TAG, "RootCaller::checkSecureSettings. Exit [3].");
        return false;
    }


    static RPCResult executeOnRoot(String command) {
        Log.v(Constants.TAG, "RootCaller::executeOnRoot. Entry...");

        RPCResult output = null;
        RootExecutor executor = createRootProcess();
        if (null != executor) {
            output = executor.executeOnRoot(command);
            if (!output.isError()) {
                Log.d(Constants.TAG, String.format("RootCaller::executeOnRoot. Executed command [%s]. Returned %d string(s).", command, output.size()));

                if (output.size() > 0) {
                    for (int i = 0; i < output.size(); i++) {
                        Log.v(Constants.TAG, String.format("RootCaller::executeOnRoot. Output (%d): '%s'.", i + 1, output.get(i)));
                    }
                }
            } else {
                Log.d(Constants.TAG, String.format("RootCaller::executeOnRoot. Executed command [%s]. Returned no strings.", command));
            }
        } else {
            Log.e(Constants.TAG, String.format("RootCaller::executeOnRoot. Failed to execute command [%s]. No 'root' process available.", command));
        }

        Log.v(Constants.TAG, "RootCaller::executeOnRoot. Exit.");
        return output;
    }


    public static synchronized RootExecutor createRootProcess() {
        if (null == rootExecutor) {
            try {
                rootExecutor = new RootExecutor();

                if (++createdCounter == 1) {
                    Log.w(Constants.TAG, "RootCaller::createRootProcess. New root executor created. First of the kind.");
                } else {
                    Log.w(Constants.TAG, String.format("RootCaller::createRootProcess. New root executor created. Instance counter: %d.", createdCounter));
                }
            } catch (Exception e) {
                Log.e(Constants.TAG, "RootCaller::createRootProcess. Exception: ", e);
                rootExecutor = null;
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

        Log.i(Constants.TAG, String.format("RootCaller::executeSystemCommand. Hacking Android. Attempt to set '%s'...", command));

        RPCResult returned = executeOnRoot(command);
        boolean emptyResult = true;

        if (!returned.isError() && 0 < returned.size()) {
            for (Object lineO : returned.getList()) {
                if (lineO instanceof String) {
                    String lineS = (String) lineO;

                    if (lineS.trim().isEmpty()) {
                        continue;
                    }

                    if (emptyResult) {
                        Log.d(Constants.TAG, String.format(Locale.US, "----- Result of stage %d ----", stage));
                        emptyResult = false;
                    }

                    Log.i(Constants.TAG, lineS);
                }
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
