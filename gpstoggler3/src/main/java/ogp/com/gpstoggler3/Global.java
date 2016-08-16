package ogp.com.gpstoggler3;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import ogp.com.gpstoggler3.debug.Constants;


public class Global extends Application {
    private static Global globalApp;

    @Override
    public void onCreate() {
        Log.v(Constants.TAG, "Global::onCreate. Entry...");

        globalApp = this;
        super.onCreate();

        Log.i(Constants.TAG, String.format("Global::onCreate. Package: %s.", getPackageName()));
        Log.v(Constants.TAG, "Global::onCreate. Exit.");
    }


    /*
    @Override
    public void onTerminate() {
        Log.v(Constants.TAG, "Global::onTerminate. Entry...");

        Log.w(Constants.TAG, "Global::onTerminate. Last message.");

        globalApp = null;
        super.onTerminate();

        Log.v(Constants.TAG, "Global::onTerminate. Exit.");
    }
    */

    public static Global getApp() {
        return globalApp;
    }


    public static Context getContext() {
        try {
            return globalApp.getBaseContext();
        } catch (NullPointerException e) {
            return null;
        }
    }
}
