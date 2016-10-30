package ogp.com.gpstoggler3.su;


import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import ogp.com.gpstoggler3.global.Constants;

import static android.content.Context.MODE_PRIVATE;

public class RootCommander {
    private static final String COMMANDER = "commander";
    private static final int BUFFER_LEN = 64 * 1024;

    private static String commanderPath = null;


    public static void initialize(Context context) {
        copyCommander(context);
    }


    static String getCommanderPath() {
        return commanderPath;
    }


    private static void copyCommander(Context context) {
        Log.v(Constants.TAG, "RootCommander::copyCommander. Entry...");

        String abi;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            abi = Build.SUPPORTED_ABIS[0];
        } else {
            //noinspection deprecation
            abi = Build.CPU_ABI;
        }

        boolean copyIt = true;
        String folder = "";
        if (abi.contains("armeabi-v7a")) {
            folder = "armeabi-v7a";
        } else if (abi.contains("armeabi")) {
            folder = "armeabi";
        } else if (abi.contains("x86_64")) {
            folder = "x86_64";
        } else if (abi.contains("x86")) {
            folder = "x86";
        } else if (abi.contains("mips64")) {
            folder = "mips64";
        } else if (abi.contains("mips")) {
            folder = "mips";
        } else {
            copyIt = false;
        }


        if (copyIt) {
            AssetManager assetManager = context.getAssets();
            try {
                InputStream in = assetManager.open(folder + "/" + COMMANDER);
                OutputStream out = context.openFileOutput(COMMANDER, MODE_PRIVATE);
                long size = 0;
                byte[] buff = new byte[BUFFER_LEN];
                int nRead;

                while ((nRead = in.read(buff)) != -1) {
                    out.write(buff, 0, nRead);
                    size += nRead;
                }

                out.flush();
                commanderPath = context.getFilesDir() + "/" + COMMANDER;
                File execFile = new File(commanderPath);
                if (execFile.setExecutable(true)) {
                    Log.d(Constants.TAG, "RootCommander::copyCommander. Copy success: " + size + " bytes. Executable for ABI [" + abi + "] is ready to be called.");
                } else {
                    Log.e(Constants.TAG, "RootCommander::copyCommander. Copy success: " + size + " bytes. But executable for ABI [" + abi + "] is not callable.");
                    commanderPath = null;
                }

                out.close();
            } catch (IOException e) {
                Log.e(Constants.TAG, "RootCommander::copyCommander. Failed to copy the 'commander'. Exception: ", e);
            }
        } else {
            Log.e(Constants.TAG, "RootCommander::copyCommander. Failed to copy the 'commander'. Unknown ABI [" + abi + "].");
        }

        Log.v(Constants.TAG, "RootCommander::copyCommander. Entry...");
    }
}
