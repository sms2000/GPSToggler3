package ogp.com.gpstoggler3.apps;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import ogp.com.gpstoggler3.global.Constants;
import ogp.com.gpstoggler3.results.RPCResult;
import ogp.com.gpstoggler3.su.RootCaller;


class RootProcessManager {
    private static final String CMD_LIST_PIDS = "ls /proc";
    private static final String CMD_GET_CMDLINE = "cat /proc/%s/cmdline";
    private static final String CMD_GET_STAT = "cat /proc/%s/stat";
    private static final String CMD_GET_CGROUP = "cat /proc/%s/cgroup";
    private static final int POLICY_INDEX = 40;
    private static final String POLICY_FOREGROUND = "0";

    private static boolean noCGroup = false;


    static class AndroidAppProcess {
        private String packageName;
        private boolean foreground;


        AndroidAppProcess(String packageName, boolean foreground) {
            this.packageName = packageName;
            this.foreground = foreground;
        }


        public String getPackageName() {
            return packageName;
        }


        boolean isForeground() {
            return foreground;
        }
    }
}
