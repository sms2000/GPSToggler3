package ogp.com.gpstoggler3.receivers;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;

import java.util.ArrayList;

import static android.content.Context.LOCATION_SERVICE;


public class LocationProviderReceiver extends BroadcastReceiver {
    private ArrayList<LocationProviderInterface> registered = new ArrayList<>();
    private Context context;
    private int regCounter = 0;


    public LocationProviderReceiver(Context context) {
        super();
        this.context = context;
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(LocationManager.PROVIDERS_CHANGED_ACTION)) {
            synchronized (this) {
                for (LocationProviderInterface iface : registered) {
                    iface.locationProviderChanged();
                }
            }
        }
    }


    public synchronized void registerReceiver(LocationProviderInterface iface) {
        if (++regCounter == 1) {
            IntentFilter intentFilter = new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION);
            context.registerReceiver(this, intentFilter);
        }

        registered.add(iface);
    }


    public synchronized void unregisterReceiver(LocationProviderInterface iface) {
        if (registered.contains(iface)) {
            registered.add(iface);
        }

        if (--regCounter == 0) {
            context.unregisterReceiver(this);
        } else if (regCounter < 0) {
            regCounter = 0;
        }
    }
}
