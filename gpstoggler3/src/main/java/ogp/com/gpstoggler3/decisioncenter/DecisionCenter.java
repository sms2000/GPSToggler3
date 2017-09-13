package ogp.com.gpstoggler3.decisioncenter;


import android.util.Log;

import ogp.com.gpstoggler3.global.Constants;
import ogp.com.gpstoggler3.interfaces.TogglerServiceInterface;
import ogp.com.gpstoggler3.settings.Settings;


public class DecisionCenter {
    private TogglerServiceInterface togglerService;


    public DecisionCenter(TogglerServiceInterface togglerService) {
        this.togglerService = togglerService;
    }


    public boolean possibleSetGps(boolean couldSetGps) {
        if (null != isGpsOn() && null != isScreenOn()) {
            if (couldSetGps) {
                if (!isGpsOn() || !isScreenOffGpsOff() || isScreenOn()) {
                    Log.i(Constants.TAG, "DecisionCenter::possibleSetGps. Enable GPS possible.");
                    return true;
                }
            } else {
                if (isGpsOn() && isScreenOffGpsOff() && !isScreenOn()) {
                    Log.i(Constants.TAG, "DecisionCenter::possibleSetGps. Disable GPS possible.");
                    return true;
                }
            }
        }

        Log.i(Constants.TAG, "DecisionCenter::possibleSetGps. " + (couldSetGps ? "Enable" : "Disable") + " GPS impossible.");
        return false;
    }


    private Boolean isScreenOn() {
        return togglerService.isScreenOn();
    }


    private boolean isScreenOffGpsOff() {
        return Settings.keepGpsOffWhenMonitorOff();
    }


    private Boolean isGpsOn() {
        return togglerService.onGps().gpsOn;
    }
}
