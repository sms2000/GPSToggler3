package ogp.com.gpstoggler3.apps;

import android.content.Context;
import android.util.Log;

import ogp.com.gpstoggler3.global.Constants;
import ogp.com.gpstoggler3.settings.Settings;
import ogp.com.gpstoggler3.servlets.WorkerThread;


public class AppDatabaseProcessor {
    private ListWatched listApps;
    private WorkerThread storeThread = null;


    public AppDatabaseProcessor(Context context) {
        Log.v(Constants.TAG, "AppDatabaseProcessor::<init>. Entry...");

        listApps = Settings.loadWatchedApps();
        storeThread = new WorkerThread(context);

        Log.v(Constants.TAG, "AppDatabaseProcessor::<init>. Exit.");
    }


    public void finish() {
        Log.v(Constants.TAG, "AppDatabaseProcessor::finish. Entry...");

        if (null != storeThread) {
            Log.i(Constants.TAG, "AppDatabaseProcessor::finish. Stopping thread...");
            storeThread.kill();
        }

        Log.v(Constants.TAG, "AppDatabaseProcessor::finish. Exit.");
    }


    public ListWatched listWatchedApps() {
        return listApps;
    }


    public boolean isExists() {
        return true;
    }


    public boolean saveApps(final ListWatched listApps) {
        Log.v(Constants.TAG, "AppDatabaseProcessor::saveApps. Entry...");

        this.listApps = listApps;

        storeThread.post(new Runnable() {
            @Override
            public void run() {
                Settings.saveWatchedApps(listApps);
            }
        });

        Log.v(Constants.TAG, "AppDatabaseProcessor::saveApps. Exit.");
        return true;
    }
}
