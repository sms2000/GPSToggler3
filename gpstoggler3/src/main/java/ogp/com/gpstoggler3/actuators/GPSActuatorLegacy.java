package ogp.com.gpstoggler3.actuators;


import android.content.Context;
import android.location.LocationManager;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.util.Log;

import ogp.com.gpstoggler3.global.Constants;
import ogp.com.gpstoggler3.interfaces.GPSActuatorInterface;


class GPSActuatorLegacy implements GPSActuatorInterface {
    private Context context;
    private String gpsStatusS = null;


    GPSActuatorLegacy(Context context) {
        this.context = context;

        initStatus();
    }


    @SuppressWarnings("deprecation")
    private void initStatus() {
        gpsStatusS = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

        if (gpsStatusS.contains("gps")) {
            gpsStatusS = gpsStatusS.replace("gps", "").replace(",,", ",");
        } else {
            gpsStatusS += ",gps";
        }
    }


    @Override
    public boolean isGPSOn() {
        String currentSet = Secure.getString(context.getContentResolver(), LocationManager.GPS_PROVIDER);

        return currentSet.contains("gps");
    }


    @SuppressWarnings("deprecation")
    @Override
    public void turnGpsOn() {
        String newSet = String.format("%s,%s",
                gpsStatusS,
                LocationManager.GPS_PROVIDER);

        try {
            Settings.Secure.putString(context.getContentResolver(),
                    Settings.Secure.LOCATION_PROVIDERS_ALLOWED,
                    newSet);
        } catch (Exception e) {
            Log.e(Constants.TAG, "GPSActuatorLegacy::turnGpsOn. Exception: ", e);
        }
    }


    @SuppressWarnings("deprecation")
    @Override
    public void turnGpsOff() {
        try {
            Settings.Secure.putString(context.getContentResolver(),
                    Settings.Secure.LOCATION_PROVIDERS_ALLOWED,
                    gpsStatusS);
        } catch (Exception e) {
            Log.e(Constants.TAG, "GPSActuatorLegacy::turnGpsOff. Exception: ", e);
        }
    }
}
