package ogp.com.gpstoggler3;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;

import ogp.com.gpstoggler3.apps.Settings;
import ogp.com.gpstoggler3.debug.Constants;
import ogp.com.gpstoggler3.actuators.GPSActuatorFactory;
import ogp.com.gpstoggler3.actuators.GPSActuatorInterface;
import ogp.com.gpstoggler3.apps.AppDatabaseProcessor;
import ogp.com.gpstoggler3.apps.AppEnumerator;
import ogp.com.gpstoggler3.apps.ListAppStore;
import ogp.com.gpstoggler3.apps.ListWatched;
import ogp.com.gpstoggler3.broadcasters.Broadcasters;
import ogp.com.gpstoggler3.receivers.LocationProviderInterface;
import ogp.com.gpstoggler3.receivers.LocationProviderReceiver;
import ogp.com.gpstoggler3.status.GPSStatus;
import ogp.com.gpstoggler3.su.RootCaller;


public class TogglerService extends Service implements TogglerServiceInterface, LocationProviderInterface {
    private static final long ACTIVATE_CLIENTS = 500;
    private static final long BIND_TO_MONITOR_TIMEOUT = 100;
    private static final int RESURRECT_FLOOD_ATTEMPTS = 10;
    private static final long RESURRECT_FLOOD_TIMEOUT = 200;

    private ListAppStore appList = new ListAppStore();
    private long lastNewAppList = 0;
    private AppEnumerator appEnumerator;
    private AppDatabaseProcessor appDatabaseProcessor = null;
    private Handler handler = new Handler();
    private WatchdogThread watchdogThread = null;
    private GPSActuatorInterface gpsActuator = null;
    private boolean isClicked = false;
    private MonitorServiceConnection monitorServiceConnection = new MonitorServiceConnection();
    private Intent bindIntent = new Intent();
    private Boolean previousGpsStatus;
    private long lastGpsStatusChangeTimestamp;
    private LocationProviderReceiver locationProviderReceiver = new LocationProviderReceiver(this);


    private class MonitorServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.v(Constants.TAG, "TogglerService::MonitorServiceConnection::onServiceConnected. Entry...");

            Log.v(Constants.TAG, "TogglerService::MonitorServiceConnection::onServiceConnected. Exit.");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.v(Constants.TAG, "TogglerService::MonitorServiceConnection::onServiceDisconnected. Entry...");

            Log.i(Constants.TAG, "TogglerService::MonitorServiceConnection::onServiceDisconnected. Attempting to resurrect...");
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    bind2Toggler();
                }
            }, BIND_TO_MONITOR_TIMEOUT);

            Log.v(Constants.TAG, "TogglerService::MonitorServiceConnection::onServiceDisconnected. Exit.");
        }
    }


    private final ITogglerService.Stub remoteBinder = new ITogglerService.Stub() {
        public int getPid() {
            return TogglerService.this.getPid();
        }


        @Override
        public void enumerateApps() {
            TogglerService.this.enumerateApps();
        }


        @Override
        public ListAppStore listInstalledApps(long lastNewAppList) {
            return TogglerService.this.listInstalledApps(lastNewAppList);
        }


        @Override
        public ListWatched listWatchedApps() throws RemoteException {
            return TogglerService.this.listWatchedApps();
        }


        @Override
        public boolean storeWatchedApps(ListWatched appList) throws RemoteException {
            return TogglerService.this.storeWatchedApps(appList);
        }


        @Override
        public boolean loadAutomationState() throws RemoteException {
            boolean automation = TogglerService.this.loadAutomationState();

            watchdogThread.automationOnOff(automation);
            return automation;
        }


        @Override
        public void storeAutomationState(boolean automation) throws RemoteException {
            watchdogThread.automationOnOff(automation);

            TogglerService.this.storeAutomationState(automation);
        }


        @Override
        public GPSStatus onGps() throws RemoteException {
            return TogglerService.this.onGps();
        }


        @Override
        public void toggleGpsState() {
            TogglerService.this.toggleGpsState();
        }


        @Override
        public ListWatched listActivatedApps() {
            return TogglerService.this.listActivatedApps();
        }


        @Override
        public boolean isRootGranted() {
            return TogglerService.this.isRootGranted();
        }


        @Override
        public void setRootGranted(boolean rootGranted) {
            TogglerService.this.setRootGranted(rootGranted);
        }
    };


    private final BroadcastReceiver activityManagement = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (null == action) {
                Log.i(Constants.TAG, "TogglerService::activityManagement::onReceive. Empty action. Ignored.");
            } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
                Log.i(Constants.TAG, "TogglerService::activityManagement::onReceive. Screen On action.");

                watchdogThread.screenOnOff(true);
            } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                Log.i(Constants.TAG, "TogglerService::activityManagement::onReceive. Screen Off action.");

                watchdogThread.screenOnOff(false);
            } else if (action.equals(Broadcasters.GPS_PIC_CLICK)) {
                Log.i(Constants.TAG, "TogglerService::activityManagement::onReceive. Widget icon click...");

                widgetClickProcessing();
            } else {
                Log.i(Constants.TAG, "TogglerService::activityManagement::onReceive. Unknown action. Ignored.");
            }
        }
    };


    private final BroadcastReceiver automationState = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (null == action) {
                Log.i(Constants.TAG, "TogglerService::automationState::onReceive. Empty action. Ignored.");
            } else if (action.equals(Broadcasters.AUTO_STATE_CHANGED)) {
                Log.i(Constants.TAG, "TogglerService::automationState::onReceive. Automation State changed.");
                automationStateProcessing(intent);
            } else {
                Log.i(Constants.TAG, "TogglerService::automationState::onReceive. Unknown action. Ignored.");
            }
        }
    };


    @Override
    public int getPid() {
        return android.os.Process.myPid();
    }


    @Override
    public IBinder onBind(Intent arg) {
        Log.i(Constants.TAG, "TogglerService::onBind. Invoked. Activating clients...");

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                pushAppsDelayed();
            }
        }, ACTIVATE_CLIENTS);
        return remoteBinder;
    }


    @Override
    public void onCreate() {
        super.onCreate();

        Log.v(Constants.TAG, "TogglerService::onCreate. Entry...");

        Settings.allocate(this);

        appDatabaseProcessor = new AppDatabaseProcessor(this);
        appEnumerator = new AppEnumerator(this);

        gpsActuator = GPSActuatorFactory.GetActuator(this);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(Broadcasters.GPS_PIC_CLICK);
        registerReceiver(activityManagement, intentFilter);

        intentFilter = new IntentFilter(Broadcasters.AUTO_STATE_CHANGED);
        registerReceiver(automationState, intentFilter);

        watchdogThread = new WatchdogThread(this, this);

        locationProviderReceiver.registerReceiver(this);

        inititateMonitor(this);

        handler.post(new Runnable() {
            @Override
            public void run() {
                Log.i(Constants.TAG, "TogglerService::onCreate. Delayed inititalization invoked.");

                bindIntent.setClassName("ogp.com.gpstoggler3monitor", "ogp.com.gpstoggler3monitor.MonitorService");
                bind2Toggler();

                enumerateApps();
                locationProviderChanged();
                updateWidgets();
            }
        });

        Log.v(Constants.TAG, "TogglerService::onCreate. Exit.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(Constants.TAG, "TogglerService::onStartCommand. Invoked.");
        return START_STICKY;
    }


    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.i(Constants.TAG, "TogglerService::onTaskRemoved. Stopping service");

        stopSelf();
    }


    @Override
    public void onDestroy() {
        Log.v(Constants.TAG, "TogglerService::onDestroy. Entry...");

        locationProviderReceiver.unregisterReceiver(this);
        unregisterReceiver(automationState);
        unregisterReceiver(activityManagement);

        if (null != watchdogThread) {
            watchdogThread.finish();
            watchdogThread = null;
        }

        if (null != appDatabaseProcessor) {
            appDatabaseProcessor.finish();
            appDatabaseProcessor = null;
        }

        Log.i(Constants.TAG, "TogglerService::onDestroy. Service destroy request received. Resurrection attempt invoked.");

        new Thread() {
            @Override
            public void run() {
                Intent intent = new Intent(Broadcasters.RESURRECT);

                for (int i = 0; i < RESURRECT_FLOOD_ATTEMPTS; i++) {
                    sendBroadcast(intent);
                    Log.v(Constants.TAG, "TogglerService::Broadcasting resurrect request...");

                    try {
                        Thread.sleep(RESURRECT_FLOOD_TIMEOUT);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }.start();

        Log.v(Constants.TAG, "TogglerService::onDestroy. Exit.");

        super.onDestroy();
    }


    @Override
    public void enumerateApps() {
        Log.v(Constants.TAG, "TogglerService::enumerateApps. Entry...");

        if (enumerateAppsSynchro()) {
            pushAppsDelayed();
        }

        Log.v(Constants.TAG, "TogglerService::enumerateApps. Exit.");
    }


    @Override
    public ListAppStore listInstalledApps(long lastAppList) {
        Log.v(Constants.TAG, "TogglerService::listInstalledApps. Entry...");

        ListAppStore appList = new ListAppStore();

        if (lastNewAppList > lastAppList) {
            appList.addAll(this.appList);
            Log.d(Constants.TAG, String.format("TogglerService::listInstalledApps. Added [%d] apps.", appList.size()));
            Log.v(Constants.TAG, "TogglerService::listInstalledApps. Exit [OK].");
            return appList;
        } else {
            Log.d(Constants.TAG, "TogglerService::listInstalledApps. Nothing added.");
            Log.v(Constants.TAG, "TogglerService::listInstalledApps. Exit [null].");
            return null;
        }
    }

    @Override
    public ListWatched listWatchedApps() {
        Log.v(Constants.TAG, "TogglerService::listWatchedApps. Entry...");

        ListWatched list = appDatabaseProcessor.listWatchedApps();

        Log.v(Constants.TAG, "TogglerService::listWatchedApps. Exit");
        return list;
    }

    @Override
    public boolean storeWatchedApps(ListWatched appList) {
        Log.v(Constants.TAG, "TogglerService::storeWatchedApps. Entry...");

        boolean ret = false;

        if (checkAppDatabaseProcessor()) {
            ret = appDatabaseProcessor.saveApps(appList);
        }

        Log.v(Constants.TAG, "TogglerService::storeWatchedApps. Exit.");
        return ret;
    }

    @Override
    public boolean loadAutomationState() {
        Log.v(Constants.TAG, "TogglerService::loadAutomationState. Entry...");

        boolean ret = Settings.loadAutomationState();

        Log.v(Constants.TAG, "TogglerService::loadAutomationState. Exit.");
        return ret;
    }

    @Override
    public void storeAutomationState(boolean automation) {
        Log.v(Constants.TAG, "TogglerService::storeAutomationState. Entry...");

        Settings.storeAutomationState(automation);

        Log.v(Constants.TAG, "TogglerService::storeAutomationState. Exit.");
    }

    @Override
    public GPSStatus onGps() {
        Log.v(Constants.TAG, "TogglerService::onGps. Entry...");

        boolean ret = gpsActuator.isGPSOn();
        GPSStatus status = new GPSStatus();
        status.gpsOn = ret;
        status.gpsStatusTimestamp = lastGpsStatusChangeTimestamp;

        Log.v(Constants.TAG, "TogglerService::onGps. Exit.");
        return status;
    }

    @Override
    public boolean isRootGranted() {
        Log.v(Constants.TAG, "TogglerService::isRootGranted. Entry...");

        boolean ret = Settings.isRootGranted();

        Log.v(Constants.TAG, "TogglerService::isRootGranted. Exit.");
        return ret;
    }

    @Override
    public void setRootGranted(boolean rootGranted) {
        Log.v(Constants.TAG, "TogglerService::setRootGranted. Entry...");

        Settings.setRootGranted(rootGranted);

        Log.v(Constants.TAG, "TogglerService::setRootGranted. Entry...");
    }

    @Override
    public void toggleGpsState() {
        Log.v(Constants.TAG, "TogglerService::toggleGpsState. Entry...");

        boolean stateNow = gpsActuator.isGPSOn();

        Log.i(Constants.TAG, String.format("TogglerService::toggleGpsState. Initiating toggle. Old state [%s]", stateNow ? "ON" : "OFF"));

        setGpsState(!stateNow);

        Log.v(Constants.TAG, "TogglerService::toggleGpsState. Exit.");
    }


    @Override
    public ListWatched listActivatedApps() {
        Log.v(Constants.TAG, "TogglerService::listActivatedApps. Entry...");

        ListWatched activated;
        synchronized (this) {
            activated = WatchdogThread.getActivatedApps();
        }

        Log.v(Constants.TAG, "TogglerService::listActivatedApps. Exit.");
        return activated;
    }


    public void automationStateProcessing(Intent intentReceived) {
        Log.v(Constants.TAG, "TogglerService::automationStateProcessing. Entry...");

        boolean automationMode = intentReceived.getBooleanExtra(Broadcasters.AUTO_STATE_CHANGED_AUTOMATION, false);
        boolean gpsDecidedOn = intentReceived.getBooleanExtra(Broadcasters.AUTO_STATE_CHANGED_GPS, false);
        setGpsState(gpsDecidedOn);      // Actual (de)activation in the Automatic mode is here!

        int activated = WatchdogThread.getActivatedApps().size();
        Intent intent = new Intent(Broadcasters.GPS_STATE_CHANGED);
        intent.putExtra(Broadcasters.GPS_STATE_CHANGED_AUTO, true);            // Changed by automation.
        sendBroadcast(intent);

        Log.e(Constants.TAG, String.format("++++ TogglerService::automationStateProcessing. Status: [%s], automation: [%s], activated apps: [%d]",
                    gpsDecidedOn ? "ON" : "OFF", automationMode ? "ON" : "OFF", activated));
        Log.v(Constants.TAG, "TogglerService::automationStateProcessing. Exit.");
    }


    public static void startServiceForever(Context context) {
        Log.v(Constants.TAG, "TogglerService::startServiceForever. Entry...");

        Log.i(Constants.TAG, "TogglerService::startServiceForever. Starting service unstoppable...");

        String packageName = context.getPackageName();
        String command = String.format("am startservice %s/%s", packageName, TogglerService.class.getName());
        Log.i(Constants.TAG, "Applying: " + command);
        List<String> returned = RootCaller.executeOnRoot(command);
        if (null == returned || 0 == returned.size()) {
            Log.i(Constants.TAG, "TogglerService::startServiceForever. Succeeded.");
        } else {
            Log.i(Constants.TAG, "---- 'startservice' returned: ----");

            for (String line : returned) {
                Log.i(Constants.TAG, line);
            }

            Log.i(Constants.TAG, "----------------------------------");
        }

        Log.v(Constants.TAG, "TogglerService::startServiceForever. Exit.");
    }


    public static boolean startServiceAndBind(Context context, ServiceConnection serviceConnection) {
        Log.v(Constants.TAG, "TogglerService::startServiceAndBind. Entry...");

        boolean success = true;

        Log.i(Constants.TAG, "TogglerService::startServiceAndBind. Attempt to bind...");
        Intent intent = new Intent(context, TogglerService.class);
        if (!context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT | Context.BIND_ABOVE_CLIENT)) {
            Log.e(Constants.TAG, "TogglerService::startServiceAndBind. Attempt to bind failed.");
            success = false;
        } else {
            Log.i(Constants.TAG, "TogglerService::startServiceAndBind. Attempt to bind succeded.");
        }

        Log.v(Constants.TAG, "TogglerService::startServiceAndBind. Exit.");
        return success;
    }


    static void inititateMonitor(Context context) {
        Log.v(Constants.TAG, "TogglerService::inititateMonitor. Entry...");

        Log.i(Constants.TAG, "TogglerService::inititateMonitor. Initiating MonitorActivity...");

        try {
            Intent intent = new Intent();
            intent.setClassName("ogp.com.gpstoggler3monitor", "ogp.com.gpstoggler3monitor.TransparentActivity");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.w(Constants.TAG, "TogglerService::inititateMonitor. No Monitora application installed apparently. Autonomous mode doesn't guarantee permanent existence.");
        } catch (Exception e) {
            Log.e(Constants.TAG, "TogglerService::inititateMonitor. Exception: ", e);
        }

        Log.v(Constants.TAG, "TogglerService::inititateMonitor. Exit.");
    }


    private void updateWidgets() {
        Log.v(Constants.TAG, "TogglerService::updateWidgets. Entry...");

        Intent intent = new Intent(Broadcasters.RETRIVE_WIDGETS);
        sendBroadcast(intent);

        Log.i(Constants.TAG, "TogglerService::updateWidgets. All widgets have been updated.");
        Log.v(Constants.TAG, "TogglerService::updateWidgets. Exit.");
    }


    private void setItForeground() {
        Log.v(Constants.TAG, "TogglerService::setItForeground. Entry...");

        PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_NEW_TASK), 0);

        Notification.Builder noteBuilder = new Notification.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getDescriptionByStatus())
                .setSmallIcon(getIconIdByStatus())
                .setContentIntent(pi);

        startForeground(R.string.app_name, noteBuilder.build());

        Log.i(Constants.TAG, "TogglerService::setItForeground. Invoked.");
        Log.v(Constants.TAG, "TogglerService::setItForeground. Exit.");
    }


    private String getDescriptionByStatus() {
        if (!Settings.loadAutomationState()) {
            return getString(R.string.descr_inactive);
        } else {
            return getString(gpsActuator.isGPSOn() ? R.string.descr_active : R.string.descr_watching);
        }
    }


    private int getIconIdByStatus() {
        return gpsActuator.isGPSOn() ? R.drawable.status_on : R.drawable.status_off;
    }


    private void pushAppsDelayed() {
        Log.v(Constants.TAG, "TogglerService::pushAppsDelayed. Entry...");

        Intent intent = new Intent(Broadcasters.APPS_ENUMERATED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent.setFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING | Intent.FLAG_RECEIVER_FOREGROUND | Intent.FLAG_RECEIVER_NO_ABORT);
        } else {
            intent.setFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING | Intent.FLAG_RECEIVER_FOREGROUND);
        }

        sendBroadcast(intent);

        Log.i(Constants.TAG, "TogglerService::pushAppsDelayed. Receivers informed.");
        setItForeground();

        Log.v(Constants.TAG, "TogglerService::pushAppsDelayed. Exit.");
    }


    private boolean enumerateAppsSynchro() {
        Log.v(Constants.TAG, "TogglerService::enumerateAppsSynchro. Entry...");

        ListAppStore list = appEnumerator.execute();
        if (list.size() != appList.size()) {
            appList = list;
            lastNewAppList = System.currentTimeMillis();
            Log.i(Constants.TAG, String.format("TogglerService::enumerateAppsSynchro. Encountered [%d] apps. [1].", list.size()));
            Log.v(Constants.TAG, "TogglerService::enumerateAppsSynchro. Exit [1].");
            return true;
        } else {
            if (!appList.containsAll(list)) {
                appList = list;
                lastNewAppList = System.currentTimeMillis();
                Log.i(Constants.TAG, String.format("TogglerService::enumerateAppsSynchro. Encountered [%d] apps. [2].", list.size()));
                Log.v(Constants.TAG, "TogglerService::enumerateAppsSynchro. Exit [2].");
                return true;
            }
        }

        if (0 >= lastNewAppList) {
            appList = list;
            lastNewAppList = System.currentTimeMillis();
            Log.i(Constants.TAG, String.format("TogglerService::enumerateAppsSynchro. Encountered [%d] apps. [3].", list.size()));
            Log.v(Constants.TAG, "TogglerService::enumerateAppsSynchro. Exit [3].");
            return true;
        }

        Log.i(Constants.TAG, String.format("TogglerService::enumerateAppsSynchro. Encountered [%d] apps. [4].", list.size()));
        Log.v(Constants.TAG, "TogglerService::enumerateAppsSynchro. Exit [4].");
        return false;
    }


    @Override
    public void locationProviderChanged() {
        Log.v(Constants.TAG, "TogglerService::locationProviderChanged. Entry...");

        setItForeground();

        Boolean newGpsStatus = gpsActuator.isGPSOn();
        Log.i(Constants.TAG, "TogglerService::locationProviderChanged. GPS now " + (newGpsStatus ? "on." : "off."));

        if (newGpsStatus != previousGpsStatus) {
            previousGpsStatus = newGpsStatus;
            lastGpsStatusChangeTimestamp = System.currentTimeMillis();
        }

        Intent intent = new Intent(Broadcasters.GPS_STATE_CHANGED);
        intent.putExtra(Broadcasters.GPS_STATE_CHANGED_AUTO, false);             // Changed by outside activity or manual influence.

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent.setFlags(Intent.FLAG_RECEIVER_NO_ABORT);
        }

        sendBroadcast(intent);

        Log.v(Constants.TAG, "TogglerService::locationProviderChanged. Exit.");
    }


    private void setGpsState(boolean newState) {
        Log.v(Constants.TAG, "TogglerService::setGpsState. Entry...");

        if (newState) {
            gpsActuator.turnGpsOn();
        } else {
            gpsActuator.turnGpsOff();
        }

        Log.v(Constants.TAG, "TogglerService::setGpsState. Exit.");
    }


    private void widgetClickProcessing() {
        Log.v(Constants.TAG, "TogglerService::widgetClickProcessing. Entry...");

        if (!isClicked) {
            Log.d(Constants.TAG, "TogglerService::widgetClickProcessing. 1st click registered.");

            isClicked = true;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isClicked) {
                        Log.d(Constants.TAG, "TogglerService::widgetClickProcessing. Single click processing...");

                        isClicked = false;
                        processSingleClick();
                    }
                }
            }, Settings.getDoubleClickDelay());
        } else {
            Log.d(Constants.TAG, "TogglerService::widgetClickProcessing. Double click processing...");

            isClicked = false;
            processDoubleClick();
        }

        Log.v(Constants.TAG, "TogglerService::widgetClickProcessing. Exit.");
    }


    private void processSingleClick() {
        Log.v(Constants.TAG, "TogglerService::processSingleClick. Entry...");

        if (Settings.loadAutomationState()) {
            Log.i(Constants.TAG, "TogglerService::processSingleClick. Do nothing in Automation mode.");
        } else {
            Log.i(Constants.TAG, "TogglerService::processSingleClick. Toggle GPS state in Manual mode.");
            toggleGpsState();
        }

        Log.v(Constants.TAG, "TogglerService::processSingleClick. Exit.");
    }


    private void processDoubleClick() {
        Log.v(Constants.TAG, "TogglerService::processDoubleClick. Entry...");

        Log.i(Constants.TAG, "TogglerService::processDoubleClick. Starting the Main activity.");

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

        Log.v(Constants.TAG, "TogglerService::processDoubleClick. Exit.");
    }


    private void bind2Toggler() {
        Log.v(Constants.TAG, "TogglerService::bind2Toggler. Entry...");

        try {
            bindService(bindIntent, monitorServiceConnection, Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT | Context.BIND_ABOVE_CLIENT);
        } catch (Exception e) {
            Log.e(Constants.TAG, "MonitorService::bind2Toggler. Exception: ", e);
        }

        Log.v(Constants.TAG, "TogglerService::bind2Toggler. Exit.");
    }


    private boolean checkAppDatabaseProcessor() {
        try {
            return appDatabaseProcessor.isExists();
        } catch (Exception e) {
            Log.e(Constants.TAG, "TogglerService::checkAppDatabaseProcessor. Severe error. 'appDatabaseProcessor' lost.");
            return false;
        }
    }
}