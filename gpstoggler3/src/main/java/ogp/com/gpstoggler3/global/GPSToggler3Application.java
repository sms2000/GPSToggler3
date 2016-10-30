package ogp.com.gpstoggler3.global;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.util.Log;

import ogp.com.gpstoggler3.settings.Settings;
import ogp.com.gpstoggler3.su.RootCommander;


public class GPSToggler3Application extends Application {
    @SuppressLint("StaticFieldLeak")
    private static GPSToggler3Application globalApp;
    @SuppressLint("StaticFieldLeak")
    private static Activity mainActivity;


    @Override
    public void onCreate() {
        Log.v(Constants.TAG, "GPSToggler3Application::onCreate. Entry...");

        Settings.allocate(this);

        globalApp = this;
        super.onCreate();

        RootCommander.initialize(this);

        Log.i(Constants.TAG, String.format("GPSToggler3Application::onCreate. Package: %s.", getPackageName()));
        Log.v(Constants.TAG, "GPSToggler3Application::onCreate. Exit.");
    }


    public static GPSToggler3Application getApp() {
        return globalApp;
    }


    public static Context getContext() {
        try {
            return globalApp.getBaseContext();
        } catch (NullPointerException e) {
            return null;
        }
    }


    public static void setMainActivity(Activity activity) {
        mainActivity = activity;
    }


    public static Activity getMainActivity() {
        try {
            mainActivity.getApplication();
            return mainActivity;
        } catch (Exception e) {
            return null;
        }
    }
}
