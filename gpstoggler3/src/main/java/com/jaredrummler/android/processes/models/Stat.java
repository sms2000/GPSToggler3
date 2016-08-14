package com.jaredrummler.android.processes.models;

import java.io.IOException;
import java.util.Locale;


final class Stat extends ProcFile {
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
