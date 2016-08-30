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
    private static final String MONITOR_DECLINED = "use_monitor_app";
    private static final String MULTIWINDOW_AWARE = "multiwindow_aware";
    private static final String BACK_KEY_DELAY = "back_key_delay";
    private static final String DOUBLE_CLICK_DELAY = "widget_double_click";

    private static final int DEF_DOUBLE_CLICK_DELAY = 250;
    private static final int DEF_PREVENT_LONG_BACK_KEY_PRESS_DELAY = 250;

    private static final String APPS_SEPARATOR = "_##_";
    private static final String FIELD_SEPARATOR = "_#_";

    private static Settings settingsSingleton = null;

    private static SharedPreferences settings;
    private static boolean multiWindowAware = false;
    private static boolean automation;
    private static boolean rootGranted;
    private static int doubleClickDelay = DEF_DOUBLE_CLICK_DELAY;
    private static int preventLongBackKeyPressDelay = DEF_PREVENT_LONG_BACK_KEY_PRESS_DELAY;


    public static Settings allocate(Context context) {
        if (null == settingsSingleton) {
            synchronized (context.getApplicationContext()) {
                if (null == settingsSingleton) {
                    settingsSingleton = new Settings(context);
                }
            }
        }

        return settingsSingleton;
    }


    private Settings(Context context) {
        Log.v(Constants.TAG, "Settings::<init>. Entry...");

        String prefFile = String.format(PREFERENCES, context.getPackageName());
        settings = context.getSharedPreferences(prefFile, 0);

        try {
            automation = settings.getBoolean(AUTOMATION, false);
        } catch (Exception ignored) {
            automation = false;
        }

        try {
            rootGranted = settings.getBoolean(ROOT_GRANTED, false);
        } catch (Exception ignored) {
            rootGranted = false;
        }

        reloadSettingsInternal();

        Log.v(Constants.TAG, "Settings::<init>. Exit.");
    }


    public static void reloadSettings() {
        Log.v(Constants.TAG, "Settings::reloadSettings. Entry...");

        settingsSingleton.reloadSettingsInternal();

        Log.v(Constants.TAG, "Settings::reloadSettings. Exit.");
    }


    public static boolean loadAutomationState() {
        return automation;
    }


    public static boolean isRootGranted() {
        return rootGranted;
    }


    public static void setRootGranted(boolean rootGranted) {
        Log.v(Constants.TAG, "Settings::setRootGranted. Entry...");

        Settings.rootGranted = rootGranted;

        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(ROOT_GRANTED, rootGranted);

        editor.apply();

        Log.v(Constants.TAG, "Settings::setRootGranted. Exit.");
    }


    public static void storeAutomationState(boolean automation) {
        Log.v(Constants.TAG, "Settings::storeAutomationState. Entry...");

        Settings.automation = automation;

        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(AUTOMATION, automation);

        editor.apply();

        Log.v(Constants.TAG, "Settings::storeAutomationState. Exit.");
    }


    public static void saveWatchedApps(ListWatched listApps) {
        Log.v(Constants.TAG, "Settings::saveWatchedApps. Entry...");

        String serialized = "";

        for (AppStore app : listApps) {
            String appData = app.friendlyName + FIELD_SEPARATOR + app.packageName + APPS_SEPARATOR;
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
                    listApps.add(new AppStore(splitted[0], splitted[1]));
                }
            }
        }

        Log.i(Constants.TAG, String.format("Settings::loadWatchedApps. Found %d watched applications.", listApps.size()));

        Log.v(Constants.TAG, "Settings::loadWatchedApps. Exit.");
        return listApps;
    }


    public static int getDoubleClickDelay() {
        return doubleClickDelay;
    }


    public static boolean isMonitorDeclined() {
        return settings.getBoolean(MONITOR_DECLINED, false);
    }


    public static void declineMonitor() {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(MONITOR_DECLINED, true);
        editor.apply();
    }


    public static boolean getMultiWindowAware() {
        return multiWindowAware;
    }


    public static int getLongBackKeyPressDelay() {
        return preventLongBackKeyPressDelay;
    }


    private void reloadSettingsInternal() {
        Log.v(Constants.TAG, "Settings::reloadSettingsInternal. Entry...");

        try {
            multiWindowAware = settings.getBoolean(MULTIWINDOW_AWARE, false);
        } catch (Exception ignored) {
            multiWindowAware = false;
        }

        try {
            preventLongBackKeyPressDelay = settings.getInt(BACK_KEY_DELAY, DEF_PREVENT_LONG_BACK_KEY_PRESS_DELAY);
        } catch (Exception ignored) {
            preventLongBackKeyPressDelay = DEF_PREVENT_LONG_BACK_KEY_PRESS_DELAY;
        }

        try {
            doubleClickDelay = settings.getInt(DOUBLE_CLICK_DELAY, DEF_DOUBLE_CLICK_DELAY);
        } catch (Exception ignored) {
            doubleClickDelay = DEF_DOUBLE_CLICK_DELAY;
        }

        Log.v(Constants.TAG, "Settings::reloadSettingsInternal. Exit.");
    }
}
