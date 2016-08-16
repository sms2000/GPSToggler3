package ogp.com.gpstoggler3monitor;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;


public class MonitorService extends Service {
    final static String TAG = "GPSToggler3Monitor";
    final static String ACTION_RESURRECT_ME = "ogp.com.gpstoggler3.broadcasters.RESURRECT_NOW";

    private static final String PACKAGE_SCHEME = "package";
    private static final int PRIORITY = 1000;
    private static final Intent resurrectIntent = new Intent(ACTION_RESURRECT_ME);
    private static final Handler handler = new Handler();
    private static final String PACKAGE_MASK = "ogp.com.gpstoggler3";

    private Intent bindIntent = new Intent();
    private TogglerServiceConnection serviceConnection = new TogglerServiceConnection();
    private RegEventReceiver regEventReceiver = new RegEventReceiver();


    public class RegEventReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(MonitorService.TAG, "RegEventReceiver::onReceive. Entry...");

            String action = intent.getAction();
            if (null == action) {
                action = "<unknwon>";
            }

            Log.i(MonitorService.TAG, "RegEventReceiver::onReceive. Action: " + action);

            switch (action) {
                case Intent.ACTION_PACKAGE_REMOVED:
                    if (intent.getBooleanExtra(Intent.EXTRA_DATA_REMOVED, false)) {
                        try {
                            String removedPackage = intent.getData().getSchemeSpecificPart();
                            if (removedPackage.endsWith(PACKAGE_MASK)) {
                                Log.i(MonitorService.TAG, "RegEventReceiver::onReceive. Parent package removed. Uninstall self.");

                                intent = new Intent(Intent.ACTION_DELETE);
                                intent.setData(Uri.parse(String.format("package:%s", getPackageName())));
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                Log.i(MonitorService.TAG, "UninstallActivity::uninstallNow. Using System uninstall.");
                                startActivity(intent);
                            }
                        } catch (Exception e) {
                            Log.e(MonitorService.TAG, "RegEventReceiver::onReceive. Exception: ", e);
                        }
                    }
                    break;
            }

            Log.v(MonitorService.TAG, "RegEventReceiver::onReceive. Exit.");
        }
    }


    private class TogglerServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.v(TAG, "MonitorService::TogglerServiceConnection::onServiceConnected. Entry...");

            Log.v(TAG, "MonitorService::TogglerServiceConnection::onServiceConnected. Exit.");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.v(TAG, "MonitorService::TogglerServiceConnection::onServiceDisconnected. Entry...");

            Log.i(TAG, "MonitorService::TogglerServiceConnection::onServiceDisconnected. Attempting to resurrect...");
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    bind2Toggler();
                }
            }, 100);

            Log.v(TAG, "MonitorService::TogglerServiceConnection::onServiceDisconnected. Exit.");
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {
        Log.v(TAG, "MonitorService::onCreate. Entry...");
        super.onCreate();

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addDataScheme(PACKAGE_SCHEME);
        intentFilter.setPriority(PRIORITY);
        registerReceiver(regEventReceiver, intentFilter);

        sendBroadcast(resurrectIntent);

        bindIntent.setClassName("ogp.com.gpstoggler3", "ogp.com.gpstoggler3.TogglerService");
        bind2Toggler();

        Log.v(TAG, "MonitorService::onCreate. Exit.");
    }


    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.v(TAG, "MonitorService::onTaskRemoved. Entry...");

        sendBroadcast(resurrectIntent);
        Log.v(TAG, "MonitorService::onTaskRemoved. Exit.");
    }


    @Override
    public void onDestroy() {
        Log.v(TAG, "MonitorService::onDestroy. Entry...");

        unregisterReceiver(regEventReceiver);

        super.onDestroy();

        Log.v(TAG, "MonitorService::onDestroy. Exit.");
    }


    private void bind2Toggler() {
        Log.v(TAG, "MonitorService::bind2Toggler. Entry...");

        try {
            bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT | Context.BIND_ABOVE_CLIENT);
        } catch (Exception e) {
            Log.e(TAG, "MonitorService::bind2Toggler. Exception: ", e);
        }

        Log.v(TAG, "MonitorService::bind2Toggler. Exit.");
    }
}
