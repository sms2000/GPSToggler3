package ogp.com.gpstoggler3.actuators;

import android.content.Context;
import android.util.Log;

import ogp.com.gpstoggler3.global.Constants;
import ogp.com.gpstoggler3.interfaces.GPSActuatorInterface;

public class GPSActuatorFactory {
    private static GPSActuatorInterface singletonGPSActuatorInterface = null;


    public static GPSActuatorInterface GetActuator(Context context) {
        Log.v(Constants.TAG, "GPSActuatorFactory::GetActuator. Entry...");

        if (null == singletonGPSActuatorInterface) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                singletonGPSActuatorInterface = new GPSActuator(context.getApplicationContext());
                Log.i(Constants.TAG, "GPSActuatorFactory::GetActuator. Selected KitKat+ actuator.");
            } else {
                singletonGPSActuatorInterface = new GPSActuatorLegacy(context.getApplicationContext());
                Log.i(Constants.TAG, "GPSActuatorFactory::GetActuator. Selected Legacy actuator.");
            }
        }

        Log.v(Constants.TAG, "GPSActuatorFactory::GetActuator. Exit.");
        return singletonGPSActuatorInterface;
    }
}
