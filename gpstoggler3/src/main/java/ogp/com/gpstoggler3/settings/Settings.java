package ogp.com.gpstoggler3.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

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
    public static final String BACK_DELAY = "back_delay";
    public static final String ROOT_TIMEOUT_DELAY = "root_timeout_delay";
    public static final String DOUBLE_CLICK_DELAY = "widget_double_click";
    public static final String ON_POLLING_DELAY = "on_polling_delay";
    public static final String OFF_POLLING_DELAY = "off_polling_delay";
    private static final String WAZE_DEBUG = "waze_debug";

    private static final String MONITOR_OFF_GPS_OFF = "monitor_off_gps_off";

    private static final int DEF_DOUBLE_CLICK_DELAY = 250;
    private static final int DEF_PREVENT_LONG_BACK_KEY_PRESS_DELAY = 250;
    private static final int DEF_ROOT_TIMEOUT_DELAY = 1000;
    private static final int DEF_ON_POLLING_DELAY = 10000;
    private static final int DEF_OFF_POLLING_DELAY = 30000;

    private static final String APPS_SEPARATOR = "_##_";
    private static final String FIELD_SEPARATOR = "_#_";
    private static final String WIDGET_INDEX = "winget_index_%d";
    private static final String MAX_WIDGET_INDEX = "widget_max_index";


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


    public static ListWatched loadWatchedApps(Context context) {
        Log.v(Constants.TAG, "Settings::loadWatchedApps. Entry...");

        ListWatched listApps = new ListWatched();
        String serialized = settings.getString(APP_LIST, "");
        String[] apps = 0 == serialized.length() ? null : serialized.split(APPS_SEPARATOR);
        if (null != apps) {
            for (String app : apps) {
                String[] splitted = app.split(FIELD_SEPARATOR);
                if (2 <= splitted.length) {
                    AppStore appStore = new AppStore(splitted[0], splitted[1], getPackageIcon(context, splitted[1]));

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


    public static Drawable getPackageIcon(Context context, String packageName) {
        Drawable drawable = null;

        try {
            drawable = context.getApplicationContext().getPackageManager().getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(Constants.TAG, String.format("Settings::getPackageIcon. No icon for the package [%s].", packageName));
        }

        if (null == drawable) {
            Log.w(Constants.TAG, String.format("Settings::getPackageIcon. No icon for the package [%s].", packageName));
        }

        return drawable;
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


    public static boolean keepGpsOffWhenMonitorOff() {
        return settings.getBoolean(MONITOR_OFF_GPS_OFF, false);
    }


    public static void preserveAccountName(String pref_key, String accName) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(pref_key, accName);
        editor.apply();
    }


    public static String retriveAccountName(String pref_key) {
        try {
            return settings.getString(pref_key, "");
        } catch (Exception ignored) {
            return "";
        }
    }


    public static int getLongBackKeyPressDelay() {
        try {
            return settings.getInt(BACK_DELAY, DEF_PREVENT_LONG_BACK_KEY_PRESS_DELAY);
        } catch (Exception ignored) {
            return DEF_PREVENT_LONG_BACK_KEY_PRESS_DELAY;
        }
    }


    public static int getRootTimeoutDelay() {
        try {
            return settings.getInt(ROOT_TIMEOUT_DELAY, DEF_ROOT_TIMEOUT_DELAY);
        } catch (Exception ignored) {
            return DEF_ROOT_TIMEOUT_DELAY;
        }
    }


    public static int getOnPollingDelay() {
        try {
            return settings.getInt(ON_POLLING_DELAY, DEF_ON_POLLING_DELAY);
        } catch (Exception ignored) {
            return DEF_ON_POLLING_DELAY;
        }
    }


    public static int getOffPollingDelay() {
        try {
            return settings.getInt(OFF_POLLING_DELAY, DEF_OFF_POLLING_DELAY);
        } catch (Exception ignored) {
            return DEF_OFF_POLLING_DELAY;
        }
    }


    public static boolean getWazeDebug() {
        try {
            return settings.getBoolean(WAZE_DEBUG, false);
        } catch (Exception ignored) {
            return false;
        }
    }


    public static String getPackageForWidget(int widgetIndex) {
        try {
            return settings.getString(String.format(Locale.ENGLISH, WIDGET_INDEX, widgetIndex), null);
        } catch (Exception ignored) {
            return null;
        }
    }


    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        Bitmap bitmap;
        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }


    public static int countWidgets(String packageName) {
        int count = 0;
        int maxIndex = 0;

        try {
            maxIndex = settings.getInt(MAX_WIDGET_INDEX, 0);
        } catch (Exception ignored) {
        }

        for (int i = 1; i <= maxIndex; i++) {
            String pN = getPackageForWidget(i);
            if (null != pN && pN.equals(packageName)) {
                count++;
            }
        }

        return count;
    }


    public static void setWidget(int index, String packageName) {
        int maxIndex = 0;

        try {
            maxIndex = settings.getInt(MAX_WIDGET_INDEX, 0);
        } catch (Exception ignored) {
        }


        SharedPreferences.Editor editor = settings.edit();
        String key = String.format(Locale.ENGLISH, WIDGET_INDEX, index);
        editor.putString(key, packageName);

        if (index > maxIndex) {
            editor.putInt(MAX_WIDGET_INDEX, index);
        }

        editor.apply();
    }


    public static void dropWidgets(int[] appWidgetIds) {
        SharedPreferences.Editor editor = settings.edit();

        for (int index : appWidgetIds) {
            String key = String.format(Locale.ENGLISH, WIDGET_INDEX, index);
            editor.remove(key);
        }

        editor.apply();
    }


    public static String prepareDataForStore() {
        JSONObject json = new JSONObject();

        try {
            json.put(ON_POLLING_DELAY, getOnPollingDelay());
        } catch (JSONException ignored) {
        }

        try {
            json.put(WAZE_DEBUG, getWazeDebug());
        } catch (JSONException ignored) {
        }

        try {
            json.put(USE_MONITOR_APP, isMonitorAppUsed());
        } catch (JSONException ignored) {
        }

        try {
            json.put(OFF_POLLING_DELAY, getOffPollingDelay());
        } catch (JSONException ignored) {
        }

        try {
            json.put(BACK_DELAY, getLongBackKeyPressDelay());
        } catch (JSONException ignored) {
        }

        try {
            json.put(ROOT_TIMEOUT_DELAY, getRootTimeoutDelay());
        } catch (JSONException ignored) {
        }

        try {
            json.put(DOUBLE_CLICK_DELAY, getDoubleClickDelay());
        } catch (JSONException ignored) {
        }

        try {
            json.put(MONITOR_OFF_GPS_OFF, keepGpsOffWhenMonitorOff());
        } catch (JSONException ignored) {
        }

        try {
            String jsonS = json.toString(4);
            Log.d(Constants.TAG, String.format("Settings::prepareDataForStore. JSON created: [%s].", jsonS));
            return jsonS;
        } catch (JSONException e) {
            Log.i(Constants.TAG, "Settings::prepareDataForStore. JSON failed.");
        }

        return null;
    }
}
