package ogp.com.gpstoggler3.apps;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import ogp.com.gpstoggler3.debug.Constants;


public class AppDatabaseProcessor {
    private Handler handler;
    private ListWatched listApps;
    private StoreThread storeThread = null;
    private Context context;


    private class StoreThread extends Thread {
        StoreThread() {
            super();
            start();
        }


        @Override
        public void run() {
            Log.v(Constants.TAG, "AppDatabaseProcessor::StoreThread::run. Entry...");

            Looper.prepare();
            handler = new Handler();
            Looper.loop();

            Log.v(Constants.TAG, "AppDatabaseProcessor::StoreThread::run. Exit.");
        }


        void finish() {
            Log.v(Constants.TAG, "AppDatabaseProcessor::finish::run. Entry...");

            handler.post(null);

            Log.v(Constants.TAG, "AppDatabaseProcessor::finish::run. Exit.");
        }
    }


    public AppDatabaseProcessor(Context context) {
        Log.v(Constants.TAG, "AppDatabaseProcessor::<init>. Entry...");

        this.context = context;

        listApps = Settings.allocate(context).loadWatchedApps();

        storeThread = new StoreThread();

        Log.v(Constants.TAG, "AppDatabaseProcessor::<init>. Exit.");
    }


    public void finish() {
        Log.v(Constants.TAG, "AppDatabaseProcessor::finish. Entry...");

        if (null != storeThread) {
            Log.i(Constants.TAG, "AppDatabaseProcessor::finish. Stopping thread...");
            storeThread.finish();
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

        handler.post(new Runnable() {
            @Override
            public void run() {
                Settings.allocate(context).saveWatchedApps(listApps);
            }
        });

        Log.v(Constants.TAG, "AppDatabaseProcessor::saveApps. Exit.");
        return true;
    }
}
