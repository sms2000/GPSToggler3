package ogp.com.gpstoggler3.actuators;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;


class GPSActuator implements GPSActuatorInterface {
    private static final int UNKNOWN = -1;

    private Context context;
    private int gpsStatusI = UNKNOWN;


    GPSActuator(Context context) {
        this.context = context;

        initStatus();
    }


    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void initStatus() {
        try {
            gpsStatusI = Settings.Secure.getInt(context.getContentResolver(),
                    Settings.Secure.LOCATION_MODE);

            if (Settings.Secure.LOCATION_MODE_HIGH_ACCURACY == gpsStatusI) {
                gpsStatusI = Settings.Secure.LOCATION_MODE_BATTERY_SAVING;
            } else {
                gpsStatusI = Settings.Secure.LOCATION_MODE_HIGH_ACCURACY;
            }
        } catch (SettingNotFoundException e) {
            gpsStatusI = Settings.Secure.LOCATION_MODE_BATTERY_SAVING;
        }
    }


    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public boolean isGPSOn() {
        try {
            return Settings.Secure.LOCATION_MODE_HIGH_ACCURACY == Settings.Secure.getInt(context.getContentResolver(),
                    Settings.Secure.LOCATION_MODE);
        } catch (SettingNotFoundException e) {
            return false;
        }
    }


    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void turnGpsOn() {
        try {
            gpsStatusI = Settings.Secure.getInt(context.getContentResolver(),
                    Settings.Secure.LOCATION_MODE);

            if (Settings.Secure.LOCATION_MODE_HIGH_ACCURACY == gpsStatusI) {
                gpsStatusI = Settings.Secure.LOCATION_MODE_BATTERY_SAVING;
            }

            Settings.Secure.putInt(context.getContentResolver(),
                    Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_HIGH_ACCURACY);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void turnGpsOff() {
        try {
            if (UNKNOWN == gpsStatusI) {
                gpsStatusI = Settings.Secure.LOCATION_MODE_BATTERY_SAVING;
            } else {
                Settings.Secure.putInt(context.getContentResolver(),
                        Settings.Secure.LOCATION_MODE,
                        gpsStatusI);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
