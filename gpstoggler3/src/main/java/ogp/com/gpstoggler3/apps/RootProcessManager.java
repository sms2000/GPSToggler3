package ogp.com.gpstoggler3.apps;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import ogp.com.gpstoggler3.global.Constants;
import ogp.com.gpstoggler3.su.RootCaller;


public class RootProcessManager {
    private static final String CMD_LIST_PIDS = "ls /proc";
    private static final String CMD_GET_CMDLINE = "cat /proc/%s/cmdline";
    private static final String CMD_GET_STAT = "cat /proc/%s/stat";

    private RootCaller.RootExecutor rootExecutor;


    public static class AndroidAppProcess {
        private int pid;
        private String packageName;
        private boolean foreground;


        AndroidAppProcess(int pid, String packageName, boolean foreground) {
            this.pid = pid;
            this.packageName = packageName;
            this.foreground = foreground;
        }


        public String getPackageName() {
            return packageName;
        }


        public boolean isForeground() {
            return foreground;
        }


        private void initPid(int pid) {
        }
    }


    public RootProcessManager(Context context) {
        rootExecutor = RootCaller.createRootProcess();
    }


    public void finish() {
        RootCaller.terminateRootProcess(rootExecutor);
    }


    public List<AndroidAppProcess> enumerate(ListWatched watchedApps) {
        Log.v(Constants.TAG, "RootProcessManager::enumerate. Entry...");

        long timeNow = System.currentTimeMillis();

        List<AndroidAppProcess> apps = new ArrayList<>();
        int enumerated = watchedApps.size();

        String cmdlineCommand = CMD_LIST_PIDS;
        List<String> pids = rootExecutor.executeOnRoot(cmdlineCommand);

        if (null != pids) {
            for (String sPid : pids) {
                if (!sPid.matches("\\d+")) {
                    continue;
                }

                try {
                    cmdlineCommand = String.format(CMD_GET_CMDLINE, sPid);
                    List<String> cmdlineResult = rootExecutor.executeOnRoot(cmdlineCommand);

                    String appPackageName = cmdlineResult.get(0).trim();
                    if (appPackageName.isEmpty()) {
                        continue;
                    }

                    if (watchedApps.containsPackage(appPackageName)) {
                        cmdlineCommand = String.format(CMD_GET_STAT, sPid);
                        cmdlineResult = rootExecutor.executeOnRoot(cmdlineCommand);
                        String appState = cmdlineResult.get(0).trim().split("\\s+")[2];

                        if (appState.equals("R") || appState.equals("S")) {
                            AndroidAppProcess process = new AndroidAppProcess(Integer.decode(sPid), appPackageName, appState.equals("R"));
                            apps.add(process);
                        }

                        if (apps.size() >= enumerated) {
                            break;
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }


        Log.e(Constants.TAG, String.format("RootProcessManager::enumerate. %d/%d apps enumerated at %d ms.", pids.size(), apps.size(), System.currentTimeMillis() - timeNow));
        Log.v(Constants.TAG, "RootProcessManager::enumerate. Exit.");
        return apps;
    }
}
