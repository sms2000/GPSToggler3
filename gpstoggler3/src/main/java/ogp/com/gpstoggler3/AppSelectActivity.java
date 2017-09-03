package ogp.com.gpstoggler3;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ListView;

import ogp.com.gpstoggler3.apps.AppSelectAdapter;
import ogp.com.gpstoggler3.apps.AppStore;
import ogp.com.gpstoggler3.apps.ListAppStore;
import ogp.com.gpstoggler3.broadcasters.Broadcasters;
import ogp.com.gpstoggler3.global.Constants;
import ogp.com.gpstoggler3.interfaces.AppAdapterInterface;
import ogp.com.gpstoggler3.services.TogglerService;
import ogp.com.gpstoggler3.servlets.WorkerThread;


public class AppSelectActivity extends AppCompatActivity implements AppAdapterInterface {
    private ProgressDialog progress;

    private static AppSelectAdapter adapter = null;
    private static long lastAppList = 0;

    private ListView listApps;
    private WorkerThread activityThread = new WorkerThread();
    private ITogglerService togglerBinder = null;
    private AppSelectActivity.TogglerServiceConnection serviceConnection = new AppSelectActivity.TogglerServiceConnection();


    private class TogglerServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.v(Constants.TAG, "AppSelectActivity::TogglerServiceConnection::onServiceConnected. Entry...");

            togglerBinder = ITogglerService.Stub.asInterface(binder);

            int myPID = Process.myPid();
            int serverPID = 0;
            try {
                serverPID = togglerBinder.getPid();
            } catch (RemoteException e) {
                Log.e(Constants.TAG, "AppSelectActivity::onServiceConnected. Exception: ", e);
            }

            if (0 != serverPID) {
                Log.d(Constants.TAG, String.format("AppSelectActivity::onServiceConnected. App PID = %d, Server PID = %d.", myPID, serverPID));
            } else {
                Log.e(Constants.TAG, String.format("AppSelectActivity::onServiceConnected. Failed to connect the server. App PID = %d.", myPID));
            }

            progress.dismiss();
            reloadInstalledApps();
            Log.v(Constants.TAG, "AppSelectActivity::TogglerServiceConnection::onServiceConnected. Exit.");
        }


        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.v(Constants.TAG, "AppSelectActivity::TogglerServiceConnection::onServiceDisconnected. Entry...");
            togglerBinder = null;
            Log.v(Constants.TAG, "AppSelectActivity::TogglerServiceConnection::onServiceDisconnected. Exit.");
        }
    }


    private BroadcastReceiver serviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(Constants.TAG, "AppSelectActivity::serviceReceiver::OnReceive. Entry...");

            if (intent.getAction().equals(Broadcasters.APPS_ENUMERATED)) {
                enumerateInstalledApps();
            }

            Log.v(Constants.TAG, "AppSelectActivity::serviceReceiver::OnReceive. Exit.");
        }
    };


    private void reloadInstalledApps() {
        Intent intent = new Intent(Broadcasters.ENUMERATE_INSTALLED_APPS);
        sendBroadcast(intent);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.v(Constants.TAG, "AppSelectActivity::onCreate. Entry...");

        setResult(RESULT_OK);
        progress = new ProgressDialog(this);

        progress.setTitle("");
        progress.setMessage(getString(R.string.loading_apps));
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.show();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Broadcasters.APPS_ENUMERATED);
        registerReceiver(serviceReceiver, filter);

        initializeScreen();

        adapter = new AppSelectAdapter(this, this);

        connect2Services();

        Log.v(Constants.TAG, "AppSelectActivity::onCreate. Exit.");
    }


    @Override
    protected void onDestroy() {
        Log.v(Constants.TAG, "AppSelectActivity::onDestroy. Entry...");
        activityThread.kill();

        progress.dismiss();

        unregisterReceiver(serviceReceiver);
        unbindService(serviceConnection);

        super.onDestroy();
        Log.v(Constants.TAG, "AppSelectActivity::onDestroy. Exit.");
    }


    private void initializeScreen() {
        Log.v(Constants.TAG, "AppSelectActivity::initializeScreen. Entry...");

        setContentView(R.layout.activity_app_select);

        listApps = findViewById(R.id.listApps);

        listApps.setAdapter(adapter);

        Log.v(Constants.TAG, "AppSelectActivity::initializeScreen. Exit.");
    }

    @Override
    public void onClickAppLookup(AppStore appStore, AppStore.AppState appState) {
        Log.v(Constants.TAG, "AppSelectActivity::onClickAppLookup. Entry...");

        activityThread.post(new Runnable() {
            @Override
            public void run() {


            }
        });

        Log.v(Constants.TAG, "AppSelectActivity::onClickAppLookup. Exit.");

    }


    private void connect2Services() {
        activityThread.post(new Runnable() {
            @Override
            public void run() {
                Log.v(Constants.TAG, "AppSelectActivity::connect2Services::run. Entry...");

                if (TogglerService.startServiceAndBind(AppSelectActivity.this, serviceConnection)) {
                    Log.i(Constants.TAG, "AppSelectActivity::connect2Services::run. Binding in process.");
                } else {
                    Log.e(Constants.TAG, "AppSelectActivity::connect2Services::run. Failed to bind.");
                    progress.dismiss();
                }

                Log.v(Constants.TAG, "AppSelectActivity::connect2Services::run. Exit.");
            }
        });
    }

    private void enumerateInstalledApps() {
        Log.v(Constants.TAG, "AppSelectActivity::enumerateInstalledApps. Entry...");

        ListAppStore appList;

        if (null != togglerBinder) {
            try {
                Log.d(Constants.TAG, "AppSelectActivity::enumerateInstalledApps. Updating the app list.");
                appList = togglerBinder.listInstalledApps(lastAppList);
                if (null == appList) {
                    Log.d(Constants.TAG, "AppSelectActivity::enumerateInstalledApps. Nothing received.");
                    return;
                }

                lastAppList = System.currentTimeMillis();

                for (AppStore app : appList) {
                    app.setAppState(AppStore.AppState.STARTABLE);
                }

                if (adapter.updateCollection(appList)) {
                    listApps.setAdapter(adapter);
                }

                Log.d(Constants.TAG, String.format("AppSelectActivity::enumerateInstalledApps. Received [%d] apps.", appList.size()));
            } catch (Throwable e) {
                Log.d(Constants.TAG, "AppSelectActivity::enumerateInstalledApps. Exception [1]: ", e);
                Log.v(Constants.TAG, "AppSelectActivity::enumerateInstalledApps. Exit [1].");
                return;
            }
        }

        Log.v(Constants.TAG, "AppSelectActivity::enumerateInstalledApps. Exit.");
    }

}
