package ogp.com.gpstoggler3;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ogp.com.gpstoggler3.apps.AppAdapter;
import ogp.com.gpstoggler3.apps.AppStore;
import ogp.com.gpstoggler3.apps.ListAppStore;
import ogp.com.gpstoggler3.apps.ListWatched;
import ogp.com.gpstoggler3.broadcasters.Broadcasters;
import ogp.com.gpstoggler3.global.Constants;
import ogp.com.gpstoggler3.global.GPSToggler3Application;
import ogp.com.gpstoggler3.interfaces.AppAdapterInterface;
import ogp.com.gpstoggler3.results.RPCResult;
import ogp.com.gpstoggler3.services.AppActivityService;
import ogp.com.gpstoggler3.services.TogglerService;
import ogp.com.gpstoggler3.servlets.WorkerThread;
import ogp.com.gpstoggler3.settings.Settings;
import ogp.com.gpstoggler3.status.GPSStatus;
import ogp.com.gpstoggler3.su.RootCaller;
import ogp.com.gpstoggler3.su.RunMonitor;

import static ogp.com.gpstoggler3.su.RootCaller.RootStatus.NO_ROOT;
import static ogp.com.gpstoggler3.su.RootCaller.RootStatus.ROOT_GRANTED;


public class MainActivity extends AppCompatActivity implements AppAdapterInterface {
    private static final long WAIT_FOR_GPS_REACTION = 1000;
    private static final int MAX_LOG_LINES = 200;
    private static final int REQ_WRITE_EXTERNAL_STORAGE = 1;

    private static AppAdapter adapter = null;
    private static long lastAppList = 0;
    private static String version;

    private static boolean secureSettingsSet = false;
    private static boolean systemizedAttempt = false;

    private Boolean orientationLandscape;
    private ListView listApps;
    private TextView logView;
    private ToggleButton modeButton;
    private ToggleButton gpsButton;
    private ScrollView logScroll;
    private static List<String> log = new ArrayList<>();
    private DateFormat timeFormat;
    private TogglerServiceConnection serviceConnection = new TogglerServiceConnection();
    private ITogglerService togglerBinder = null;
    private Handler handler = new Handler();
    private String packageName;
    private ProgressDialog progress;
    private WorkerThread activityThread = new WorkerThread();
    private GoogleApiClient client;
    private long backPressedTime = 0;
    private static boolean debugMode = false;


    private BroadcastReceiver gpsStateChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(Constants.TAG, "MainActivity::gpsStateChangedReceiver::onReceive. Entry...");

            if (null != togglerBinder) {
                try {
                    gpsStateChanged(togglerBinder.onGps().gpsOn);
                } catch (RemoteException | NullPointerException e) {
                    Log.e(Constants.TAG, "MainActivity::gpsStateChangedReceiver::onReceive. Exception in 'onGps'.");
                }
            }

            Log.v(Constants.TAG, "MainActivity::gpsStateChangedReceiver::onReceive. Exit.");
        }
    };


    private Action getIndexApiAction() {
        Thing object = new Thing.Builder().setName(this.getResources().getString(R.string.main_page))
                .setUrl(Uri.parse("https://github.com/sms2000"))
                .build();

        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }


    private class TogglerServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.v(Constants.TAG, "MainActivity::TogglerServiceConnection::onServiceConnected. Entry...");

            togglerBinder = ITogglerService.Stub.asInterface(binder);

            int myPID = Process.myPid();
            int serverPID = 0;
            try {
                serverPID = togglerBinder.getPid();
            } catch (RemoteException e) {
                Log.e(Constants.TAG, "MainActivity::onServiceConnected. Exception: ", e);
            }

            if (0 != serverPID) {
                Log.d(Constants.TAG, String.format("MainActivity::onServiceConnected. App PID = %d, Server PID = %d.", myPID, serverPID));
                addLogMessage(R.string.connected_to_service);
            } else {
                Log.e(Constants.TAG, String.format("MainActivity::onServiceConnected. Failed to connect the server. App PID = %d.", myPID));
                addLogMessage(R.string.failed_connect_to_service);
            }

            try {
                gpsStateChanged(togglerBinder.onGps().gpsOn);
            } catch (RemoteException | NullPointerException e) {
                Log.e(Constants.TAG, "MainActivity::TogglerServiceConnection::onServiceConnected. Exception in 'onGps'.");
            }

            resurrectLog();

            progress.dismiss();
            reloadInstalledApps();
            Log.v(Constants.TAG, "MainActivity::TogglerServiceConnection::onServiceConnected. Exit.");
        }


        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.v(Constants.TAG, "MainActivity::TogglerServiceConnection::onServiceDisconnected. Entry...");
            togglerBinder = null;
            Log.v(Constants.TAG, "MainActivity::TogglerServiceConnection::onServiceDisconnected. Exit.");
        }
    }


    private void reloadInstalledApps() {
        Intent intent = new Intent(Broadcasters.ENUMERATE_INSTALLED_APPS);
        sendBroadcast(intent);
    }


    private BroadcastReceiver serviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(Constants.TAG, "MainActivity::serviceReceiver::OnReceive. Entry...");

            if (intent.getAction().equals(Broadcasters.APPS_ENUMERATED)) {
                enumerateInstalledApps();
                setControls();
            } else if (intent.getAction().equals(Broadcasters.AUTO_STATE_CHANGED)) {
                automationStateChanged(intent.getBooleanExtra(Broadcasters.AUTO_STATE_CHANGED_AUTOMATION, false));
            }

            Log.v(Constants.TAG, "MainActivity::serviceReceiver::OnReceive. Exit.");
        }
    };


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.v(Constants.TAG, "MainActivity::onCreate. Entry...");
        debugMode = 0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE);

        GPSToggler3Application.setMainActivity(this);

        Settings.allocate(this);

        packageName = getPackageName();
        progress = new ProgressDialog(this);

        progress.setTitle("");
        progress.setMessage(getString(R.string.loading_apps));
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.show();

        timeFormat = DateFormat.getTimeInstance();
        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);
        orientationLandscape = displaySize.x > displaySize.y;

        IntentFilter filter = new IntentFilter(Broadcasters.GPS_STATE_CHANGED);
        registerReceiver(gpsStateChangedReceiver, filter);

        filter = new IntentFilter();
        filter.addAction(Broadcasters.APPS_ENUMERATED);
        filter.addAction(Broadcasters.AUTO_STATE_CHANGED);
        registerReceiver(serviceReceiver, filter);

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            version = "<unknwon>";
        }


        initializeScreen();

        adapter = new AppAdapter(this, this);

        connect2Services();

        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
        Log.v(Constants.TAG, "MainActivity::onCreate. Exit.");
    }


    private void connect2Services() {
        activityThread.post(new Runnable() {
            @Override
            public void run() {
                Log.v(Constants.TAG, "MainActivity::connect2Services::run. Entry...");

                RunMonitor.readMonitorApk(MainActivity.this);
                systemize();

                if (TogglerService.startServiceAndBind(MainActivity.this, serviceConnection)) {
                    Log.i(Constants.TAG, "MainActivity::connect2Services::run. Binding in process.");



                    /*
                    decideOnMonitor();
                    */
                } else {
                    Log.e(Constants.TAG, "MainActivity::connect2Services::run. Failed to bind.");
                    progress.dismiss();
                    noService();
                }

                Log.v(Constants.TAG, "MainActivity::connect2Services::run. Exit.");
            }
        });
    }


    /*
    private void decideOnMonitor() {
        if (Settings.isMonitorAppUsed()) {
            Log.v(Constants.TAG, "MainActivity::decideOnMonitor. Monitor installation declined. Not asking anymore.");
            return;
        }

        activityThread.post(new Runnable() {
            @Override
            public void run() {
                Log.v(Constants.TAG, "MainActivity::decideOnMonitor::run. Entry...");

                if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                        dialog.setTitle(R.string.permission_request);
                        dialog.setMessage(R.string.req_write_external_storage);
                        dialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_WRITE_EXTERNAL_STORAGE);
                                Log.d(Constants.TAG, "MainActivity::decideOnMonitor::run::run. Pressed <Yes>.");
                            }
                        });

                        dialog.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                Settings.declineMonitor();
                                Log.d(Constants.TAG, "MainActivity::decideOnMonitor::run::run. Pressed <No>.");
                            }
                        });

                        dialog.show();
                    } else {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_WRITE_EXTERNAL_STORAGE);
                    }
                } else {
                    loadMonitor();
                }

                Log.v(Constants.TAG, "MainActivity::decideOnMonitor::run. Exit.");
            }
        });
    }
    */

    private void loadMonitor() {
        final RunMonitor runMonitor = new RunMonitor(this);
        activityThread.post(new Runnable() {
            @Override
            public void run() {
                runMonitor.installMonitor();
            }
        });

        activityThread.post(new Runnable() {
            @Override
            public void run() {
                runMonitor.startMonitor();
            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQ_WRITE_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadMonitor();
                } else {
                    Settings.declineMonitor();
                }

                break;
        }
    }


    @Override
    public void onStart() {
        super.onStart();

        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }


    @Override
    public void onResume() {
        Log.v(Constants.TAG, "MainActivity::onResume. Entry...");

        super.onResume();

        reloadInstalledApps();

        Log.v(Constants.TAG, "MainActivity::onResume. Exit.");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings, menu);

        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startSettingsActivity();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                final int delay = Settings.getLongBackKeyPressDelay();
                if (0 < delay) {
                    backPressedTime = System.currentTimeMillis();
                    Log.d(Constants.TAG, "MainActivity::onKeyDown. Recognized 'back key' pressed. Escape procedure initiated...");

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (0 < backPressedTime) {
                                Log.w(Constants.TAG, "MainActivity::onKeyDown. Escaping the termination by 'long back press'. Close the activity before Android killed the whole process.");
                                finish();
                            }
                        }
                    }, delay);
                }
        }

        return super.onKeyDown(keyCode, event);
    }


    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                backPressedTime = 0;
                Log.d(Constants.TAG, "MainActivity::onKeyUp. Recognized 'back key' unpressed. Escape procedure cancelled.");
                break;

            case KeyEvent.KEYCODE_MENU:
                Log.d(Constants.TAG, "MainActivity::onKeyUp. Recognized 'menu' unpressed. Start the 'SettingsActivity'.");
                startSettingsActivity();
                break;
        }

        return super.onKeyUp(keyCode, event);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Constants.SETTINGS_REQUEST_CODE == requestCode &&
                Constants.SETTINGS_KILLED == resultCode) {
            Log.d(Constants.TAG, "MainActivity::onActivityResult. Recognized 'long back press' from sub-activity. Killing the 'MainActivity' to prevent process extermination...");
            finish();
        }
    }


    @Override
    public void onStop() {
        super.onStop();

        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }


    @Override
    protected void onDestroy() {
        Log.v(Constants.TAG, "MainActivity::onDestroy. Entry...");
        activityThread.kill();

        progress.dismiss();

        unregisterReceiver(gpsStateChangedReceiver);
        unregisterReceiver(serviceReceiver);

        unbindService(serviceConnection);

        super.onDestroy();
        Log.v(Constants.TAG, "MainActivity::onDestroy. Exit.");
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.v(Constants.TAG, "MainActivity::onConfigurationChanged. Entry...");

        super.onConfigurationChanged(newConfig);
        progress.dismiss();

        boolean oldOrientation = orientationLandscape;
        orientationLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE;

        if (oldOrientation != orientationLandscape) {
            initializeScreen();
            setControls();
            resurrectLog();
        }

        Log.v(Constants.TAG, "MainActivity::onConfigurationChanged. Exit.");
    }


    private void initializeScreen() {
        Log.v(Constants.TAG, "MainActivity::initializeScreen. Entry...");

        setTitle(String.format(getString(R.string.app_title), version));

        setContentView(orientationLandscape ? R.layout.activity_l : R.layout.activity_p);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        listApps = findViewById(R.id.listApps);
        logScroll = findViewById(R.id.scrollLog);
        logView = findViewById(R.id.textLog);

        modeButton = findViewById(R.id.toggleAutomatic);
        gpsButton = findViewById(R.id.toggleGPS);

        gpsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        gpsButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() != MotionEvent.ACTION_UP) {
                    return true;
                }

                v.setEnabled(false);


                final Boolean oldGpsState;

                try {
                    oldGpsState = togglerBinder.onGps().gpsOn;
                } catch (RemoteException | NullPointerException e) {
                    Log.e(Constants.TAG, "MainActivity::initializeScreen. Exception in 'onGps'.");
                    return false;
                }


                Log.i(Constants.TAG, String.format("MainActivity::initializeScreen. Toggling GPS state from %s to %s.", gpsState2String(oldGpsState), gpsState2String(!oldGpsState)));

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        expectGpsStateChanged(oldGpsState);

                    }
                }, WAIT_FOR_GPS_REACTION);

                toggleGpsState();

                v.performClick();
                return true;
            }
        });

        listApps.setAdapter(adapter);
        FloatingActionButton fab = findViewById(R.id.fab);
        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                }
            });
        }

        TogglerService.startServiceForever(this);
        Log.v(Constants.TAG, "MainActivity::initializeScreen. Exit.");
    }


    @Override
    public void onClickAppLookup(final AppStore appStore, final AppStore.AppState appState) {
        Log.v(Constants.TAG, "MainActivity::onClickAppLookup. Entry...");

        activityThread.post(new Runnable() {
            @Override
            public void run() {
                appStore.setAppState(appState);

                ListWatched appList = new ListWatched();
                for (int i = 0; i < adapter.getCount(); i++) {
                    AppStore app = adapter.getItem(i);
                    assert app != null;
                    if (app.getAppState() != AppStore.AppState.DISABLED) {
                        appList.add(app);
                    }
                }

                if (checkBinder()) {
                    try {
                        Log.d(Constants.TAG, "MainActivity::onClickAppLookup::run. Informing the service to preserve the new list of watched apps...");
                        togglerBinder.storeWatchedApps(appList);
                        Log.d(Constants.TAG, "MainActivity::onClickAppLookup::run. Service informed to preserve the new list of watched apps.");
                    } catch (RemoteException e) {
                        Log.e(Constants.TAG, "MainActivity::onClickAppLookup::run. Exception: ", e);
                    }


                    int strIndex = appState == AppStore.AppState.DISABLED ? R.string.remove_lookup : (appState == AppStore.AppState.FOREGROUND ? R.string.add_lookup_f : R.string.add_lookup_b);
                    addLogMessage(String.format(getString(strIndex), appStore.friendlyName));
                }
            }
        });

        Log.v(Constants.TAG, "MainActivity::onClickAppLookup. Exit.");
    }


    public void onClickMode(View modeButton) {
        Log.v(Constants.TAG, "MainActivity::onClickMode. Entry...");

        boolean automation = ((ToggleButton) modeButton).isChecked();

        try {
            togglerBinder.storeAutomationState(automation);
            addLogMessage(automation ? R.string.activate_lookup : R.string.deactivate_lookup);
            Log.d(Constants.TAG, "MainActivity::onClickMode. Saved auomation status: " + gpsState2String(automation));
        } catch (Exception e) {
            Log.e(Constants.TAG, "MainActivity::onClickMode. Exception: ", e);
        }

        setControls();

        Log.v(Constants.TAG, "MainActivity::onClickMode. Exit.");
    }


    private void enumerateInstalledApps() {
        Log.v(Constants.TAG, "MainActivity::enumerateInstalledApps. Entry...");

        ListAppStore appList = null;
        ListWatched appSelected = null;

        if (null != togglerBinder) {
            try {
                Log.d(Constants.TAG, "MainActivity::enumerateInstalledApps. Updating the app list.");
                appList = togglerBinder.listInstalledApps(lastAppList);
                if (null == appList) {
                    Log.d(Constants.TAG, "MainActivity::enumerateInstalledApps. Nothing received.");
                    return;
                }

                lastAppList = System.currentTimeMillis();
                Log.d(Constants.TAG, String.format("MainActivity::enumerateInstalledApps. Received [%d] apps.", appList.size()));
            } catch (Throwable e) {
                Log.d(Constants.TAG, "MainActivity::enumerateInstalledApps. Exception [1]: ", e);
                addLogMessage(getString(R.string.selected_failed));
                Log.v(Constants.TAG, "MainActivity::enumerateInstalledApps. Exit [1].");
                return;
            }

            try {
                Log.d(Constants.TAG, "MainActivity::enumerateInstalledApps. Updating the watched list.");
                appSelected = togglerBinder.listWatchedApps();

                for (AppStore app : appList) {
                    AppStore.AppState state = AppStore.AppState.DISABLED;
                    if (appSelected.containsPackage(app.packageName)) {
                        state = appSelected.getState(app.packageName);
                    }

                    app.setAppState(state);
                }


                if (adapter.updateCollection(appList)) {
                    listApps.setAdapter(adapter);
                }

                Log.d(Constants.TAG, String.format("MainActivity::enumerateInstalledApps. Found [%d] watched apps.", appSelected.size()));
            } catch (Exception e) {
                Log.e(Constants.TAG, "MainActivity::enumerateInstalledApps. Exception [2]: ", e);
                addLogMessage(getString(R.string.selected_failed));
                Log.v(Constants.TAG, "MainActivity::enumerateInstalledApps. Exit [2].");
                return;
            }
        }

        if (null != appList && 0 < appList.size()) {
            addLogMessage(R.string.loaded_apps);
            addLogMessage(getString(R.string.enumerated, appList.size(), appSelected.size()));
        } else {
            addLogMessage(R.string.no_apps_found);
        }

        Log.v(Constants.TAG, "MainActivity::enumerateInstalledApps. Exit.");
    }


    private void automationStateChanged(boolean automationOn) {
        Log.v(Constants.TAG, "MainActivity::automationStateChanged. Entry...");

        ListWatched listActivated = new ListWatched();

        try {
            Log.d(Constants.TAG, "MainActivity::automationStateChanged. Updating the activated apps list.");

            if (automationOn) {
                listActivated = togglerBinder.listActivatedApps();
                if (null == listActivated) {
                    if (null != adapter) {
                        uploadActivatedApps(null);
                    }

                    Log.d(Constants.TAG, "MainActivity::automationStateChanged. Nothing received.");

                    addLogMessage(getString(R.string.activated, 0));
                    Log.v(Constants.TAG, "MainActivity::automationStateChanged. Exit [1].");
                    return;
                }

                Log.d(Constants.TAG, String.format("MainActivity::automationStateChanged. Enabled and received [%d] activated apps.", listActivated.size()));
            } else {
                Log.d(Constants.TAG, "MainActivity::automationStateChanged. Disabled.");
            }
        } catch (Throwable e) {
            Log.d(Constants.TAG, "MainActivity::automationStateChanged. Exception [1]: ", e);
            addLogMessage(getString(R.string.activated_failed));
            Log.v(Constants.TAG, "MainActivity::automationStateChanged. Exit [2].");
            return;
        }

        if (null != adapter) {
            uploadActivatedApps(listActivated);
            addLogMessage(getString(R.string.activated, listActivated.size()));
        }

        Log.v(Constants.TAG, "MainActivity::automationStateChanged. Exit.");
    }


    private void uploadActivatedApps(ListWatched listActivated) {
        Log.v(Constants.TAG, "MainActivity::uploadActivatedApps. Entry...");

        adapter.uploadActivatedApps(listActivated);

        listApps.invalidate();

        Log.v(Constants.TAG, "MainActivity::uploadActivatedApps. Exit.");
    }


    private void toggleGpsState() {
        Log.v(Constants.TAG, "MainActivity::toggleGpsState. Entry...");

        try {
            togglerBinder.toggleGpsState();
            addLogMessage(R.string.toggle_attempted);
            Log.i(Constants.TAG, "MainActivity::toggleGpsState. Attempted.");
        } catch (RemoteException e) {
            Log.e(Constants.TAG, "MainActivity::toggleGpsState. Exception [1]: ", e);
            addLogMessage(R.string.toggle_failed);
        }

        Log.v(Constants.TAG, "MainActivity::toggleGpsState. Exit.");
    }


    private void expectGpsStateChanged(Boolean oldState) {
        Log.v(Constants.TAG, "MainActivity::expectGpsStateChanged. Entry...");

        Boolean gpsState = null;
        try {
            Log.v(Constants.TAG, String.format("MainActivity::expectGpsStateChanged. Old state: [%s],", oldState ? "ON" : "OFF"));

            gpsState = togglerBinder.onGps().gpsOn;
        } catch (RemoteException | NullPointerException e) {
            Log.e(Constants.TAG, "MainActivity::expectGpsStateChanged. Exception in 'onGps'.");
        }

        if (null == gpsState || gpsState == oldState) {
            addLogMessage(R.string.error_set_gps_state);
            Log.e(Constants.TAG, "MainActivity::expectGpsStateChanged. Failed to set the new GPS state.");
        }

        setControls();

        Log.v(Constants.TAG, "MainActivity::expectGpsStateChanged. Exit.");
    }


    private void startSettingsActivity() {
        Log.v(Constants.TAG, "MainActivity::startSettingsActivity. Entry...");

        Intent intent = new Intent(this, SettingsActivity.class);
        startActivityForResult(intent, Constants.SETTINGS_REQUEST_CODE);

        Log.v(Constants.TAG, "MainActivity::startSettingsActivity. Exit.");
    }


    private void gpsStateChanged(Boolean oldState) {
        Log.v(Constants.TAG, "MainActivity::gpsStateChanged. Entry...");

        long timestamp;
        Boolean gpsState;

        try {
            if (null != togglerBinder) {
                GPSStatus status = togglerBinder.onGps();
                gpsState = status.gpsOn;
                timestamp = status.gpsStatusTimestamp;
            } else {
                addLogMessage(R.string.error_obtain_gps_state);
                Log.e(Constants.TAG, "MainActivity::gpsStateChanged. Failed to obtain the new state [2].");
                return;
            }
        } catch (RemoteException e) {
            addLogMessage(R.string.error_obtain_gps_state);
            Log.e(Constants.TAG, "MainActivity::gpsStateChanged. Failed to obtain the new state [1].");
            return;
        }

        if (oldState == gpsState) {
            Log.d(Constants.TAG, "MainActivity::gpsStateChanged. State not changed: " + gpsState2String(gpsState));
            addLogMessage(String.format(getString(R.string.gps_state_same), gpsState2String(gpsState)));
        } else {
            Log.d(Constants.TAG, "MainActivity::gpsStateChanged. State changed: " + gpsState2String(gpsState) + " at timestamp: " + timestamp);
            addLogMessage(String.format(getString(R.string.gps_state), gpsState2String(gpsState)));
        }

        setControls();
        Log.v(Constants.TAG, "MainActivity::gpsStateChanged. Exit.");
    }


    private void addLogMessage(int resId) {
        String strLog = getString(resId);
        addLogMessage(strLog);
    }


    private String gpsState2String(Boolean state) {
        if (null == state) {
            return "[?]";
        } else if (state) {
            return "[ON]";
        } else {
            return "[OFF]";
        }
    }


    private synchronized void addLogMessage(String strLog) {
        while (log.size() > MAX_LOG_LINES) {
            log.remove(0);
        }

        Date date = new Date();

        String logLine = timeFormat.format(date) + " : " + strLog;
        log.add(logLine);

        handler.post(new Runnable() {
            @Override
            public void run() {
                showLog();
            }
        });
    }


    private void showLog() {
        String combined = "";
        for (String line : log) {
            if (!combined.isEmpty()) {
                combined += "\n";
            }

            combined += line;
        }

        logView.setText(combined);

        handler.post(new Runnable() {
            @Override
            public void run() {
                logScroll.smoothScrollBy(0, logView.getBottom());
            }
        });
    }


    private void setControls() {
        Log.v(Constants.TAG, "MainActivity::setControls. Entry...");

        boolean automation = false;

        if (null != togglerBinder) {
            try {
                automation = togglerBinder.loadAutomationState();

                if (modeButton.isChecked() != automation) {
                    modeButton.setChecked(automation);

                    addLogMessage(automation ? R.string.activate_lookup : R.string.deactivate_lookup);
                    Log.d(Constants.TAG, "MainActivity::setControls. Loaded Automation status: " + automation);
                }
            } catch (Exception e) {
                addLogMessage(R.string.error_load_automation_state);
                Log.e(Constants.TAG, "MainActivity::setControls. Exception: ", e);
            }
        }


        Boolean gpsState = null;
        try {
            if (null != togglerBinder) {
                gpsState = togglerBinder.onGps().gpsOn;
            }
        } catch (RemoteException e) {
            Log.e(Constants.TAG, "MainActivity::expectGpsStateChanged. Exception in 'onGps'.");
        }


        gpsButton.setEnabled(!automation && null != gpsState);
        gpsButton.setChecked(null != gpsState && gpsState);

        Log.v(Constants.TAG, "MainActivity::setControls. Exit.");
    }


    private void resurrectLog() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                showLog();
            }
        });
    }


    private void systemize() {
        Log.v(Constants.TAG, "MainActivity::systemize. Entry...");

        if (!systemizedAttempt) {
            systemizedAttempt = true;

            if (!Settings.isRootGranted()) {
                Log.v(Constants.TAG, "MainActivity::systemize. No, the 'root' must be obtained.");
                if (ROOT_GRANTED == RootCaller.ifRootAvailable()) {
                    obtainRoot();
                } else {
                    if (!debugMode) {
                        noRoot();
                    }
                }
            } else {
                RootCaller.RootExecutor rootExecutor = RootCaller.createRootProcess();
                if (null != rootExecutor) {
                    RootCaller.terminateRootProcess(rootExecutor);
                }

                Log.v(Constants.TAG, "MainActivity::systemize. Yes, the 'root' was obtained earlier. May continue.");
            }
        }

        Log.v(Constants.TAG, "MainActivity::systemize. Exit.");
    }


    private void noRoot() {
        Log.v(Constants.TAG, "MainActivity::noRoot. Entry...");

        progress.dismiss();

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.warning);
        dialog.setMessage(R.string.no_root);
        dialog.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        finishApplication(0);
                        Log.d(Constants.TAG, "MainActivity::noRoot. Pressed <OK>.");
                    }
                });

        dialog.setNegativeButton(R.string.as_is,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        Log.d(Constants.TAG, "MainActivity::noRoot. Pressed <As Is>.");
                    }
                });

        dialog.show();

        Log.v(Constants.TAG, "MainActivity::noRoot. Exit.");
    }


    private void finishApplication(int messageId) {
        if (0 < messageId) {
            Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show();
        }

        finish();
        Log.v(Constants.TAG, "MainActivity::finishApplication. Finished.");
    }


    private void noService() {
        Log.v(Constants.TAG, "MainActivity::noService. Entry...");

        progress.dismiss();

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.warning);
        dialog.setMessage(R.string.no_service);
        dialog.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int id) {
                        dialog.cancel();
                        TogglerService.startServiceAndBind(MainActivity.this, serviceConnection);
                        Log.d(Constants.TAG, "MainActivity::noService. Pressed <OK>.");
                    }
                });

        dialog.show();

        Log.v(Constants.TAG, "MainActivity::noService. Exit.");
    }


    private void systemizeFailed() {
        Log.v(Constants.TAG, "MainActivity::systemizeFailed. Entry...");

        progress.dismiss();

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.error);
        dialog.setMessage(R.string.systemize_failed);
        dialog.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int id) {
                        dialog.cancel();
                        finishApplication(0);
                        Log.d(Constants.TAG, "MainActivity::systemizeFailed. Pressed <OK>.");
                    }
                });

        dialog.show();

        Log.v(Constants.TAG, "MainActivity::systemizeFailed. Exit.");
    }


    private void setSecureSettings() {
        Log.v(Constants.TAG, "MainActivity::setSecureSettings. Entry...");

        if (!secureSettingsSet) {
            if (NO_ROOT == RootCaller.setSecureSettings(MainActivity.this, packageName, AppActivityService.class.getCanonicalName())) {
                setRootGranted(false);

                if (debugMode) {
                    setRootGranted(true);
                    secureSettingsSet = true;
                } else {
                    noRoot();
                }
            } else if (RootCaller.checkSecureSettings()) {
                setRootGranted(true);
                secureSettingsSet = true;
            } else {
                systemizeFailed();
            }
        }

        Log.v(Constants.TAG, "MainActivity::setSecureSettings. Exit.");
    }


    private void obtainRoot() {
        Log.v(Constants.TAG, "MainActivity::obtainRoot. Entry...");

        progress.dismiss();

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.warning);
        dialog.setMessage(R.string.ask_4_root);
        dialog.setPositiveButton(R.string.yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int id) {
                        dialog.cancel();

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(Constants.TAG, "MainActivity::obtainRoot. Pressed <Yes>.");

                                RootCaller.RootExecutor rootExecutor = RootCaller.createRootProcess();
                                if (null != rootExecutor) {
                                    RPCResult output = rootExecutor.executeOnRoot("ls /");
                                    if (!output.isError()) {
                                        Log.i(Constants.TAG, "MainActivity::obtainRoot. 'root' obtained.");
                                        Settings.setRootGranted(true);

                                        setSecureSettings();
                                        return;
                                    }
                                }

                                Log.e(Constants.TAG, "MainActivity::obtainRoot. 'root' denied or not available.");
                                finishApplication(R.string.root_denied);
                            }
                        });
                    }
                });

        dialog.setNegativeButton(R.string.no,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int id) {
                        dialog.cancel();
                        finishApplication(R.string.user_not_willing_root);
                        Log.d(Constants.TAG, "MainActivity::obtainRoot. Pressed <No>.");
                    }
                });

        dialog.show();

        Log.v(Constants.TAG, "MainActivity::obtainRoot. Exit.");
    }


    private void setRootGranted(boolean rootGranted) {
        Log.v(Constants.TAG, "MainActivity::setRootGranted. Entry...");

        Settings.setRootGranted(rootGranted);

        Log.i(Constants.TAG, String.format("MainActivity::setRootGranted. Set [%s] succeeded.", rootGranted ? "TRUE" : "FALSE"));
        Log.v(Constants.TAG, "MainActivity::setRootGranted. Exit.");
    }


    private boolean checkBinder() {
        try {
            togglerBinder.getPid();
            return true;
        } catch (Exception e) {
            addLogMessage(R.string.internal_error_1);
            Log.e(Constants.TAG, "MainActivity::checkBinder. Severe problem. Lost service connection.");
            return false;
        }
    }
}
