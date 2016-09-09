package com.jaredrummler.android.processes;

import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ogp.com.gpstoggler3.global.Constants;


public class ProcessManager {
    public static List<AndroidAppProcess> getRunningAppProcesses() {
        List<AndroidAppProcess> processes = new ArrayList<>();
        File[] files = new File("/proc").listFiles();

        for (File file : files) {
            if (file.isDirectory()) {
                int pid;

                try {
                    pid = Integer.parseInt(file.getName());
                } catch (NumberFormatException e) {
                    continue;
                }

                try {
                    processes.add(new AndroidAppProcess(pid));
                } catch (Exception e) {
                    Log.e(Constants.TAG, "ProcessManager::getRunningAppProcesses. Exception: ", e);
                }
            }
        }

        return processes;
    }

    static private final class Stat extends ProcFile {
        private final String[] fields;


        static Stat get(int pid) throws IOException {
            return new Stat(String.format(Locale.US, "/proc/%d/stat", pid));
        }


        private Stat(String path) throws IOException {
            super(path);
            fields = content.split("\\s+");
        }


        String getComm() {
            return fields[1].replace("(", "").replace(")", "");
        }


        String getStatus() {
            return fields[2];
        }
    }

    static class ProcFile extends File {
        public final String content;


        static String readFile(String path) throws IOException {
            BufferedReader reader = null;

            try {
                StringBuilder output = new StringBuilder();
                reader = new BufferedReader(new FileReader(path));

                for (String line = reader.readLine(), newLine = ""; line != null; line = reader.readLine()) {
                    output.append(newLine).append(line);
                    newLine = "\n";
                }

                return output.toString();
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (Exception e) {
                    Log.e(Constants.TAG, "ProcFile::readFile. Exception: ", e);
                }
            }
        }


        ProcFile(String path) throws IOException {
            super(path);

            content = readFile(path);
        }
    }

    static private final class Cgroup extends ProcFile {
        final String[] groups;
        boolean foreground;


        public static Cgroup get(int pid) throws IOException {
            return new Cgroup(String.format(Locale.US, "/proc/%d/cgroup", pid));
        }


        private Cgroup(String path) throws IOException {
            super(path);

            groups = content.split("\n");

            for (String str : groups) {
                if (str.contains("cpuset")) {
                    foreground = str.contains("foreground");
                    return;
                }
            }


            foreground = false;
        }
    }

    public static class AndroidAppProcess {
        private final String name;
        private boolean foreground;


        AndroidAppProcess(int pid) throws IOException {
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
}
