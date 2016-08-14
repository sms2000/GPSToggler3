package ogp.com.gpstoggler3monitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class EventReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(MonitorService.TAG, "EventReceiver::onReceive. Entry...");

        String action = intent.getAction();
        if (null == action) {
            action = "<unknwon>";
        }

        Log.i(MonitorService.TAG, "EventReceiver::onReceive. Action: " + action);

        switch (action) {
            case Intent.ACTION_BOOT_COMPLETED:
            case MonitorService.ACTION_RESURRECT_ME:
                Intent serviceIntent = new Intent(context.getApplicationContext(), MonitorService.class);
                context.getApplicationContext().startService(serviceIntent);
                Log.i(MonitorService.TAG, "EventReceiver::onReceive. Started main service.");
                break;
        }

        Log.v(MonitorService.TAG, "EventReceiver::onReceive. Exit.");
    }
}
