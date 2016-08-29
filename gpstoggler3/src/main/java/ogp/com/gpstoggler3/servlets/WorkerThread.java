package ogp.com.gpstoggler3.servlets;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import ogp.com.gpstoggler3.global.Constants;


public class WorkerThread extends Thread {
    protected Context context;
    private Handler handler;


    public WorkerThread(Context context) {
        this.context = context;
        start();
    }


    public void kill() {
        handler.getLooper().quit();
    }


    public void post(Runnable runnable) {
        handler.post(runnable);
    }


    @SuppressWarnings("WeakerAccess")
    public void postDelayed(Runnable runnable, int delay) {
        handler.postDelayed(runnable, delay);
    }


    @Override
    public void run() {
        Log.v(Constants.TAG, "WorkerThread::run. Entry...");

        Looper.prepare();
        handler = new Handler();
        Looper.loop();

        Log.v(Constants.TAG, "WorkerThread::run. Exit.");
    }
}


