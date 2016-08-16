package ogp.com.gpstoggler3.apps;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import ogp.com.gpstoggler3.debug.Constants;


public class Settings {
    static private final String PREFERENCES = "Preferences";
    static private final String WATCHER = "Automation";
    static private final String ROOT_GRANTED = "RootGranted";
    static private final String APP_LIST = "ActivatedApps";
    static private final String SEPARATOR = "_##_";
    static private final String APP_SEPARATOR = "_#_";
    static private final int DEF_DOUBLE_CLICK_DELAY = 300;
    private static final String MONITOR_DECLINED = "MonitorDeclined";

    public static Settings settingsSingleton = null;

    private static boolean multiWindowAware = false;
    private static boolean automation;
    private static boolean rootGranted;
    private static SharedPreferences settings;
    private static int doubleClickDelay = DEF_DOUBLE_CLICK_DELAY;


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

        settings = context.getSharedPreferences(PREFERENCES, 0);

        automation = settings.getBoolean(WATCHER, false);
        rootGranted = settings.getBoolean(ROOT_GRANTED, false);

        Log.v(Constants.TAG, "Settings::<init>. Exit.");
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
        editor.putBoolean(WATCHER, automation);

        editor.apply();

        Log.v(Constants.TAG, "Settings::storeAutomationState. Exit.");
    }


    static void saveWatchedApps(ListWatched listApps) {
        Log.v(Constants.TAG, "Settings::saveWatchedApps. Entry...");

        String serialized = "";

        for (AppStore app : listApps) {
            String appData = app.friendlyName + APP_SEPARATOR + app.packageName;

            if (0 != serialized.length()) {
                serialized += SEPARATOR;
            }

            serialized += appData;
        }

        SharedPreferences.Editor editor = settings.edit();
        editor.putString(APP_LIST, serialized);

        editor.apply();

        Log.i(Constants.TAG, String.format("Settings::saveWatchedApps. Saved %d watched applications.", listApps.size()));

        Log.v(Constants.TAG, "Settings::saveWatchedApps. Exit.");
    }


    static ListWatched loadWatchedApps() {
        Log.v(Constants.TAG, "Settings::loadWatchedApps. Entry...");

        ListWatched listApps = new ListWatched();
        String serialized = settings.getString(APP_LIST, "");
        String[] apps = 0 == serialized.length() ? null : serialized.split(SEPARATOR);
        if (null != apps) {
            for (String app : apps) {
                String[] splitted = app.split(APP_SEPARATOR);
                if (2 != splitted.length) {
                    continue;
                }

                listApps.add(new AppStore(splitted[0], splitted[1]));
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


    public static boolean getSplitAware() {
        return multiWindowAware;
    }
}
