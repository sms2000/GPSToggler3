package ogp.com.gpstoggler3.actuators;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

import ogp.com.gpstoggler3.global.Constants;


class GPSActuator implements GPSActuatorInterface {
    private Context context;

    GPSActuator(Context context) {
        this.context = context;
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
            Settings.Secure.putInt(context.getContentResolver(),
                    Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_HIGH_ACCURACY);
        } catch (Exception e) {
            Log.e(Constants.TAG, "GPSActuator::turnGpsOn. Exception: ", e);
        }
    }


    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void turnGpsOff() {
        try {
            Settings.Secure.putInt(context.getContentResolver(),
                        Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_BATTERY_SAVING);
        } catch (Exception e) {
            Log.e(Constants.TAG, "GPSActuator::turnGpsOff. Exception: ", e);
        }
    }
}
