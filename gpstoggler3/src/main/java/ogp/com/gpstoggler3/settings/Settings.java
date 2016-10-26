package ogp.com.gpstoggler3.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import ogp.com.gpstoggler3.apps.AppStore;
import ogp.com.gpstoggler3.apps.ListWatched;
import ogp.com.gpstoggler3.global.Constants;


public class Settings {
    // From internal codes
    private static final String PREFERENCES = "%s_preferences";
    private static final String AUTOMATION = "automation_on";
    private static final String ROOT_GRANTED = "root_granted";
    private static final String APP_LIST = "activated_apps";

    // From ActivitySettings
    private static final String USE_MONITOR_APP = "use_monitor_app";
    private static final String MULTIWINDOW_AWARE = "multiwindow_aware";
    private static final String BACK_KEY_DELAY = "back_key_delay";
    private static final String DOUBLE_CLICK_DELAY = "widget_double_click";

    private static final int DEF_DOUBLE_CLICK_DELAY = 250;
    private static final int DEF_PREVENT_LONG_BACK_KEY_PRESS_DELAY = 250;

    private static final String APPS_SEPARATOR = "_##_";
    private static final String FIELD_SEPARATOR = "_#_";

    private static Settings settingsSingleton = null;
    private static SharedPreferences settings;


    public static void allocate(Context context) {
        if (null == settingsSingleton) {
            synchronized (context.getApplicationContext()) {
                if (null == settingsSingleton) {
                    settingsSingleton = new Settings(context);
                }
            }
        }
    }


    private Settings(Context context) {
        Log.v(Constants.TAG, "Settings::<init>. Entry...");

        String prefFile = String.format(PREFERENCES, context.getPackageName());
        settings = context.getSharedPreferences(prefFile, 0);

        Log.v(Constants.TAG, "Settings::<init>. Exit.");
    }


    public static boolean loadAutomationState() {
        try {
            return settings.getBoolean(AUTOMATION, false);
        } catch (Exception ignored) {
            return false;
        }
    }


    public static boolean isRootGranted() {
        try {
            return settings.getBoolean(ROOT_GRANTED, false);
        } catch (Exception ignored) {
            return false;
        }
    }


    public static void setRootGranted(boolean rootGranted) {
        Log.v(Constants.TAG, "Settings::setRootGranted. Entry...");

        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(ROOT_GRANTED, rootGranted);
        editor.apply();

        Log.v(Constants.TAG, "Settings::setRootGranted. Exit.");
    }


    public static void storeAutomationState(boolean automation) {
        Log.v(Constants.TAG, "Settings::storeAutomationState. Entry...");

        SharedPreferences.Editor editor = settings.edit();

        editor.putBoolean(AUTOMATION, automation);
        editor.apply();

        Log.v(Constants.TAG, "Settings::storeAutomationState. Exit.");
    }


    public static void saveWatchedApps(ListWatched listApps) {
        Log.v(Constants.TAG, "Settings::saveWatchedApps. Entry...");

        String serialized = "";

        for (AppStore app : listApps) {
            String appData = app.friendlyName + FIELD_SEPARATOR + app.packageName + FIELD_SEPARATOR + app.getAppState().name() + APPS_SEPARATOR;
            serialized += appData;
        }

        SharedPreferences.Editor editor = settings.edit();
        editor.putString(APP_LIST, serialized);

        editor.apply();

        Log.i(Constants.TAG, String.format("Settings::saveWatchedApps. Saved %d watched applications.", listApps.size()));

        Log.v(Constants.TAG, "Settings::saveWatchedApps. Exit.");
    }


    public static ListWatched loadWatchedApps() {
        Log.v(Constants.TAG, "Settings::loadWatchedApps. Entry...");

        ListWatched listApps = new ListWatched();
        String serialized = settings.getString(APP_LIST, "");
        String[] apps = 0 == serialized.length() ? null : serialized.split(APPS_SEPARATOR);
        if (null != apps) {
            for (String app : apps) {
                String[] splitted = app.split(FIELD_SEPARATOR);
                if (2 <= splitted.length) {
                    AppStore appStore = new AppStore(splitted[0], splitted[1]);

                    if (3 <= splitted.length) {
                        try {
                            AppStore.AppState state = AppStore.AppState.valueOf(splitted[2]);
                            appStore.setAppState(state);
                        } catch (Exception ignored) {
                        }
                    }

                    listApps.add(appStore);
                }
            }
        }

        Log.i(Constants.TAG, String.format("Settings::loadWatchedApps. Found %d watched applications.", listApps.size()));

        Log.v(Constants.TAG, "Settings::loadWatchedApps. Exit.");
        return listApps;
    }


    public static int getDoubleClickDelay() {
        try {
            return settings.getInt(DOUBLE_CLICK_DELAY, DEF_DOUBLE_CLICK_DELAY);
        } catch (Exception ignored) {
            return DEF_DOUBLE_CLICK_DELAY;
        }
    }


    public static boolean isMonitorAppUsed() {
        return settings.getBoolean(USE_MONITOR_APP, false);
    }


    public static void declineMonitor() {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(USE_MONITOR_APP, false);
        editor.apply();
    }


    public static boolean getMultiWindowAware() {
        try {
            return settings.getBoolean(MULTIWINDOW_AWARE, false);
        } catch (Exception ignored) {
            return false;
        }
    }


    public static int getLongBackKeyPressDelay() {
        try {
            return settings.getInt(BACK_KEY_DELAY, DEF_PREVENT_LONG_BACK_KEY_PRESS_DELAY);
        } catch (Exception ignored) {
            return DEF_PREVENT_LONG_BACK_KEY_PRESS_DELAY;
        }

    }
}
