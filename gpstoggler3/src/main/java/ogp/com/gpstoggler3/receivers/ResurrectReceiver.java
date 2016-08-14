package ogp.com.gpstoggler3.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import ogp.com.gpstoggler3.debug.Constants;
import ogp.com.gpstoggler3.TogglerService;


public class ResurrectReceiver extends BroadcastReceiver {
    private static final long DELAY_RESURRECT = 100;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(Constants.TAG, "ResurrectReceiver.onReceive. Entry...");
        Log.w(Constants.TAG, "ResurrectReceiver.onReceive. Attempting to resurrect the service...");

        final Context appContext = context.getApplicationContext();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent myIntent = new Intent(appContext, TogglerService.class);
                appContext.startService(myIntent);

                Log.w(Constants.TAG, "ResurrectionService. 'startService' invoked.");
            }
        }, DELAY_RESURRECT);

        Log.v(Constants.TAG, "ResurrectReceiver.onReceive. Exit.");
    }
}
