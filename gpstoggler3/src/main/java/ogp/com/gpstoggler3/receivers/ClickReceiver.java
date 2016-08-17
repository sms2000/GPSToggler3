package ogp.com.gpstoggler3.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import ogp.com.gpstoggler3.TogglerService;
import ogp.com.gpstoggler3.broadcasters.Broadcasters;
import ogp.com.gpstoggler3.global.Constants;
import ogp.com.gpstoggler3.su.RootCaller;


public class ClickReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(Constants.TAG, "ClickReceiver::onReceive. Entry...");

        if (intent.getAction().equals(Broadcasters.GPS_PIC_CLICK)) {
            Log.i(Constants.TAG, "ClickReceiver::onReceive. Activating service...");

            RootCaller.setSecureSettings(context.getPackageName());
            TogglerService.startServiceForever(context.getApplicationContext());

            Log.i(Constants.TAG, "ClickReceiver::onReceive. Activating service finished.");
        }

        Log.v(Constants.TAG, "ClickReceiver::onReceive. Exit.");
    }
}
