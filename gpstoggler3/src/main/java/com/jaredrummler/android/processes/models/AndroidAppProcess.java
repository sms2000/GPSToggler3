package com.jaredrummler.android.processes.models;

import android.text.TextUtils;

import java.io.IOException;
import java.util.Locale;


public class AndroidAppProcess {
    private final String name;
    private boolean foreground;


    public AndroidAppProcess(int pid) throws IOException {
        this.name = getProcessName(pid);
        this.foreground = isProcessForeground(pid);
    }


    public String getPackageName() {
        return name.split(":")[0];
    }


    public boolean isForeground() {
        return foreground;
    }


    private static String getProcessName(int pid) throws IOException {
        String cmdline = null;

        try {
            cmdline = ProcFile.readFile(String.format(Locale.US, "/proc/%d/cmdline", pid)).trim();
        } catch (IOException ignored) {
        }

        if (TextUtils.isEmpty(cmdline)) {
            return Stat.get(pid).getComm();
        }

        return cmdline;
    }


    private static boolean isProcessForeground(int pid) {
        try {
            return Cgroup.get(pid).foreground;
        } catch (IOException e) {
            return false;
        }
    }
}
