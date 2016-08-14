package com.jaredrummler.android.processes.models;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;


class ProcFile extends File {
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
                Log.e("AndroidAppProcess", "ProcFile::readFile. Exception: ", e);
            }
        }
    }


    ProcFile(String path) throws IOException {
        super(path);

        content = readFile(path);
    }
}
