package ogp.com.gpstoggler3.services;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import ogp.com.gpstoggler3.AppSelectActivity;
import ogp.com.gpstoggler3.apps.ApplicationsWatchdog;
import ogp.com.gpstoggler3.ITogglerService;
import ogp.com.gpstoggler3.MainActivity;
import ogp.com.gpstoggler3.R;
import ogp.com.gpstoggler3.actuators.GPSActuatorFactory;
import ogp.com.gpstoggler3.decisioncenter.DecisionCenter;
import ogp.com.gpstoggler3.global.GPSToggler3Application;
import ogp.com.gpstoggler3.interfaces.GPSActuatorInterface;
import ogp.com.gpstoggler3.apps.AppDatabaseProcessor;
import ogp.com.gpstoggler3.apps.AppEnumerator;
import ogp.com.gpstoggler3.apps.ListAppStore;
import ogp.com.gpstoggler3.apps.ListWatched;
import ogp.com.gpstoggler3.interfaces.TogglerServiceInterface;
import ogp.com.gpstoggler3.servlets.WorkerThread;
import ogp.com.gpstoggler3.settings.Settings;
import ogp.com.gpstoggler3.broadcasters.Broadcasters;
import ogp.com.gpstoggler3.global.Constants;
import ogp.com.gpstoggler3.interfaces.LocationProviderInterface;
import ogp.com.gpstoggler3.receivers.LocationProviderReceiver;
import ogp.com.gpstoggler3.status.GPSStatus;
import ogp.com.gpstoggler3.su.RootCaller;


public class TogglerService extends Service implements TogglerServiceInterface, LocationProviderInterface {
    private static final int ACTIVATE_CLIENTS_TIMEOUT = 500;
    private static final int BIND_TO_MONITOR_TIMEOUT = 100;
    private static final int RESURRECT_FLOOD_ATTEMPTS = 10;
    private static final int RESURRECT_FLOOD_TIMEOUT = 200;
    private static final long HOLD_PERIOD_MS = 2000;

    public static final String CHANNEL_ID = "com.ogp.com.gpstoggler3.notification.ANDROID";

    private ListAppStore appList = new ListAppStore(this);
    private long lastNewAppList = 0;
    private AppEnumerator appEnumerator;
    private AppDatabaseProcessor appDatabaseProcessor = null;
    private ApplicationsWatchdog watchdogThread = null;
    private GPSActuatorInterface gpsActuator = null;
    private boolean isClicked = false;
    private MonitorServiceConnection monitorServiceConnection = new MonitorServiceConnection();
    private Intent bind2MonitorIntent = new Intent();
    private Boolean previousGpsStatus;
    private long lastGpsStatusChangeTimestamp;
    private LocationProviderReceiver locationProviderReceiver = new LocationProviderReceiver(this);
    private WorkerThread serverThread = null;
    private DecisionCenter decisionCenter = new DecisionCenter(this);
    private Boolean screenOn = null;
    private Uri signalUri;


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
            serverThread.postDelayed(new Runnable() {
                @Override
                public void run() {
                    bind2Monitor();
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

            broadcastGpsStateChanged();
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
    };


    private final BroadcastReceiver activityManagement = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (null == action) {
                Log.i(Constants.TAG, "TogglerService::activityManagement::onReceive. Empty action. Ignored.");
            } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
                Log.i(Constants.TAG, "TogglerService::activityManagement::onReceive. Screen On action.");

                screenOnOff(true);
            } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                Log.i(Constants.TAG, "TogglerService::activityManagement::onReceive. Screen Off action.");

                screenOnOff(false);
            } else if (action.equals(Broadcasters.GPS_PIC_CLICK)) {
                Log.i(Constants.TAG, "TogglerService::activityManagement::onReceive. Widget icon click...");

                widgetClickProcessing();
            } else if (action.equals(Broadcasters.APP_PIC_CLICK)) {
                int index = intent.getIntExtra(Broadcasters.APP_PIC_CLICK_EXTRA_INDEX, 0);
                Log.i(Constants.TAG, String.format("TogglerService::activityManagement::onReceive. App start icon click for [%d]...", index));

                appStartClickProcessing(index);
            } else if (action.equals(Broadcasters.ENUMERATE_INSTALLED_APPS)) {
                Log.i(Constants.TAG, "TogglerService::activityManagement::onReceive. Enumerate installed apps.");

                enumerateInstalledAppsInThread();
            } else if (action.equals(Broadcasters.WINDOW_STACK_CHANGED)) {
                Log.i(Constants.TAG, "TogglerService::activityManagement::onReceive. Window stack changed.");

                if (null != watchdogThread) {
                    watchdogThread.activateNow();
                }

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

        serverThread.postDelayed(new Runnable() {
            @Override
            public void run() {
                pushAppsDelayed();
            }
        }, ACTIVATE_CLIENTS_TIMEOUT);
        return remoteBinder;
    }


    @Override
    public void onCreate() {
        super.onCreate();

        Log.v(Constants.TAG, "TogglerService::onCreate. Entry...");

        Settings.allocate(this);

        signalUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.empty);
        serverThread = new WorkerThread();
        appDatabaseProcessor = new AppDatabaseProcessor(this);
        appEnumerator = new AppEnumerator(this);

        gpsActuator = GPSActuatorFactory.GetActuator(this);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(Broadcasters.GPS_PIC_CLICK);
        intentFilter.addAction(Broadcasters.APP_PIC_CLICK);
        intentFilter.addAction(Broadcasters.ENUMERATE_INSTALLED_APPS);
        intentFilter.addAction(Broadcasters.WINDOW_STACK_CHANGED);
        registerReceiver(activityManagement, intentFilter);

        intentFilter = new IntentFilter(Broadcasters.AUTO_STATE_CHANGED);
        registerReceiver(automationState, intentFilter);

        watchdogThread = new ApplicationsWatchdog(this, this);
        locationProviderReceiver.registerReceiver(this);

        initiateMonitor(this);

        initiateHumptyDumpty();
        enumerateInstalledAppsInThread();
        initiateAutomationState();

        broadcastGpsStateChanged();

        Log.v(Constants.TAG, "TogglerService::onCreate. Exit.");
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(Constants.TAG, "TogglerService::onStartCommand. Invoked.");
        return START_STICKY;
    }


    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.i(Constants.TAG, "TogglerService::onTaskRemoved. Ignored.");
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

        if (null != serverThread) {
            serverThread.kill();
            serverThread = null;
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
    public ListAppStore listInstalledApps(long lastAppList) {
        Log.v(Constants.TAG, "TogglerService::listInstalledApps. Entry...");

        ListAppStore appList = new ListAppStore(this);

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

        Log.i(Constants.TAG, String.format("TogglerService::onGps. Current status [%s], last changed at: %d", ret ? "ON" : "OFF", lastGpsStatusChangeTimestamp));
        Log.v(Constants.TAG, "TogglerService::onGps. Exit.");
        return status;
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
    public Boolean isScreenOn() {
        return screenOn;
    }


    @Override
    public ListWatched listActivatedApps() {
        Log.v(Constants.TAG, "TogglerService::listActivatedApps. Entry...");

        ListWatched activated = ApplicationsWatchdog.getActivatedApps();

        Log.v(Constants.TAG, "TogglerService::listActivatedApps. Exit.");
        return activated;
    }


    public void automationStateProcessing(Intent intentReceived) {
        Log.v(Constants.TAG, "TogglerService::automationStateProcessing. Entry...");

        boolean automationMode = intentReceived.getBooleanExtra(Broadcasters.AUTO_STATE_CHANGED_AUTOMATION, false);
        if (automationMode) {
            boolean gpsDecidedOn = intentReceived.getBooleanExtra(Broadcasters.AUTO_STATE_CHANGED_GPS, false);
            setGpsState(gpsDecidedOn);      // Actual (de)activation in the Automatic mode is here!

            int size = ApplicationsWatchdog.getActivatedApps().size();
            broadcastGpsStateChanged();

            Log.i(Constants.TAG, String.format("TogglerService::automationStateProcessing. Status: [%s], automation: [ON], activated apps: [%d].",
                    gpsDecidedOn ? "ON" : "OFF", size));
        } else {
            Log.i(Constants.TAG, "TogglerService::automationStateProcessing. automation: [OFF].");
        }

        Log.v(Constants.TAG, "TogglerService::automationStateProcessing. Exit.");
    }


    public static void startServiceForever(Context context) {
        Log.v(Constants.TAG, "TogglerService::startServiceForever. Entry...");

        if (Settings.isRootGranted()) {
            Log.i(Constants.TAG, "TogglerService::startServiceForever. Starting service unstoppable...");

            String className = AppActivityService.class.getCanonicalName();
            String packageName = context.getPackageName();
            RootCaller.setSecureSettings(context, packageName, className);

            Intent intent = new Intent(context.getApplicationContext(), TogglerService.class);
            context.getApplicationContext().startService(intent);

            Log.i(Constants.TAG, "TogglerService::startServiceForever. Service has been started.");
        } else {
            Log.w(Constants.TAG, "TogglerService::startServiceForever. Service is not started due to 'root' access not granted yet.");
        }

        Log.v(Constants.TAG, "TogglerService::startServiceForever. Exit.");
    }


    public static boolean startServiceAndBind(Context context, ServiceConnection serviceConnection) {
        Log.v(Constants.TAG, "TogglerService::startServiceAndBind. Entry...");

        boolean success = false;

        Log.i(Constants.TAG, "TogglerService::startServiceAndBind. Attempt to bind...");
        Intent intent = new Intent(context, TogglerService.class);

        try {
            context.unbindService(serviceConnection);
        } catch (IllegalArgumentException ignored) {
        }

        if (context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT | Context.BIND_ABOVE_CLIENT)) {
            success = true;
            Log.i(Constants.TAG, "TogglerService::startServiceAndBind. Attempt to bind succeeded.");
        } else {
            Log.e(Constants.TAG, "TogglerService::startServiceAndBind. Attempt to bind failed.");
        }

        Log.v(Constants.TAG, "TogglerService::startServiceAndBind. Exit.");
        return success;
    }


    public static void initiateMonitor(Context context) {
        Log.v(Constants.TAG, "TogglerService::initiateMonitor. Entry...");

        Log.i(Constants.TAG, "TogglerService::initiateMonitor. Initiating MonitorActivity...");

        try {
            Intent intent = new Intent();
            intent.setClassName(Broadcasters.HD_MONITOR_PACKAGE, Broadcasters.HD_MONITOR_ACTIVITY);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (ActivityNotFoundException | IllegalArgumentException e) {
            Log.w(Constants.TAG, "TogglerService::initiateMonitor. No Monitor application installed apparently. Autonomous mode doesn't guarantee permanent existence.");
        } catch (Exception e) {
            Log.e(Constants.TAG, "TogglerService::initiateMonitor. Exception: ", e);
        }

        Log.v(Constants.TAG, "TogglerService::initiateMonitor. Exit.");
    }


    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    private void setItForeground() {
        Log.v(Constants.TAG, "TogglerService::setItForeground. Entry...");

        PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_NEW_TASK), 0);

        int iconId = getIconIdByStatus();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setAutoCancel(true)
                    .setSmallIcon(iconId)
                    .setContentTitle(getString(R.string.channel_1_pesistense_name))
                    .setSound(signalUri)
                    .setContentIntent(pi);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, getString(R.string.channel_1_pesistense_name), NotificationManager.IMPORTANCE_DEFAULT);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .build();

            channel.setDescription(getString(R.string.channel_1_pesistense_description));
            channel.setSound(signalUri, audioAttributes);
            notificationManager.createNotificationChannel(channel);

            builder.setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE);
            Notification notification = builder.build();

            startForeground(R.string.app_name, notification);
        } else {
            Notification.Builder noteBuilder = new Notification.Builder(this)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getDescriptionByStatus())
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), iconId))
                    .setSmallIcon(iconId)
                    .setContentIntent(pi);

            Notification notification = noteBuilder.getNotification();

            startForeground(R.string.app_name, notification);
        }

        Log.i(Constants.TAG, "TogglerService::setItForeground. Invoked.");
        Log.v(Constants.TAG, "TogglerService::setItForeground. Exit.");
    }


    private void initiateAutomationState() {
        Log.v(Constants.TAG, "TogglerService::initiateAutomationState. Entry...");

        boolean automationState = Settings.loadAutomationState();
        watchdogThread.automationOnOff(automationState);
        Log.i(Constants.TAG, String.format("Automation state initiated as [%s].", automationState ? "ON" : "OFF"));

        Log.v(Constants.TAG, "TogglerService::initiateAutomationState. Exit.");
    }


    private void broadcastGpsStateChanged() {
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(Broadcasters.GPS_STATE_CHANGED_AUTO, Settings.loadAutomationState());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent.setFlags(Intent.FLAG_RECEIVER_NO_ABORT | Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        }

        sendBroadcast(intent);

        intent = new Intent(Broadcasters.GPS_STATE_CHANGED);
        intent.putExtra(Broadcasters.GPS_STATE_CHANGED_AUTO, Settings.loadAutomationState());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent.setFlags(Intent.FLAG_RECEIVER_NO_ABORT | Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        }

        sendBroadcast(intent);
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


    private void initiateHumptyDumpty() {
        serverThread.post(new Runnable() {
            @Override
            public void run() {
                Log.i(Constants.TAG, "TogglerService::initiateHumptyDumpty. Delayed initialization invoked.");

                bind2MonitorIntent.setClassName(Broadcasters.HD_MONITOR_PACKAGE, Broadcasters.HD_MONITOR_SERVICE);
                bind2Monitor();
            }
        });
    }


    private void enumerateInstalledAppsInThread() {
        Log.v(Constants.TAG, "TogglerService::enumerateInstalledAppsInThread. Entry...");

        serverThread.post(new Runnable() {
            @Override
            public void run() {
                long previousUpdate = lastNewAppList;

                ListAppStore list = appEnumerator.execute();
                if (list.size() != appList.size()) {
                    appList = list;
                    lastNewAppList = System.currentTimeMillis();
                    Log.i(Constants.TAG, String.format("TogglerService::enumerateInstalledAppsInThread::run. Encountered [%d] apps. Updating [1]...", list.size()));
                } else {
                    if (!appList.containsAll(list)) {
                        appList = list;
                        lastNewAppList = System.currentTimeMillis();
                        Log.i(Constants.TAG, String.format("TogglerService::enumerateInstalledAppsInThread::run. Encountered [%d] apps. Updating [2]...", list.size()));
                    }
                }

                if (0 >= lastNewAppList) {
                    appList = list;
                    lastNewAppList = System.currentTimeMillis();
                    Log.i(Constants.TAG, String.format("TogglerService::enumerateInstalledAppsInThread::run. Encountered [%d] apps. Updating [3]...", list.size()));
                } else {
                    Log.i(Constants.TAG, String.format("TogglerService::enumerateInstalledAppsInThread::run. Encountered [%d] apps. Not updating.", list.size()));
                }

                if (previousUpdate != lastNewAppList) {
                    pushAppsDelayed();
                }

                Log.v(Constants.TAG, "TogglerService::enumerateInstalledAppsInThread::run. Exit.");
            }
        });
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
            broadcastGpsStateChanged();
        }

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
            serverThread.postDelayed(new Runnable() {
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


    private void appStartClickProcessing(int widgetIndex) {
        Log.v(Constants.TAG, String.format("TogglerService::appStartClickProcessing. Entry for widget [%s]...", widgetIndex));

        String packageName = Settings.getPackageForWidget(widgetIndex);
        if (null == packageName) {
            Log.i(Constants.TAG, String.format("TogglerService::appStartClickProcessing. Widget [%d] is not initialized yet. Configure...", widgetIndex));

            Intent intent = new Intent(this, AppSelectActivity.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetIndex);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (null != intent) {
                watchdogThread.holdTill(System.currentTimeMillis() + HOLD_PERIOD_MS);
                setGpsState(true);
                startActivity(intent);

                Log.i(Constants.TAG, String.format("TogglerService::appStartClickProcessing. Enabled the GPS and started the launch activity for the package [%s].", packageName));
            } else {
                Log.w(Constants.TAG, String.format("TogglerService::appStartClickProcessing. Failed to obtain the lasunch activity for the package [%s].", packageName));
            }
        }

        Log.v(Constants.TAG, "TogglerService::appStartClickProcessing. Exit.");
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
        Context context = GPSToggler3Application.getMainActivity();
        if (null == context) {
            context = getApplicationContext();
        }

        try {
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            context.startActivity(intent);
        } catch (Exception e) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }

        Log.v(Constants.TAG, "TogglerService::processDoubleClick. Exit.");
    }


    private void bind2Monitor() {
        Log.v(Constants.TAG, "TogglerService::bind2Monitor. Entry...");

        try {
            bindService(bind2MonitorIntent, monitorServiceConnection, Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT | Context.BIND_ABOVE_CLIENT);
        } catch (Exception e) {
            Log.e(Constants.TAG, "MonitorService::bind2Monitor. Exception: ", e);
        }

        Log.v(Constants.TAG, "TogglerService::bind2Monitor. Exit.");
    }


    private boolean checkAppDatabaseProcessor() {
        try {
            return appDatabaseProcessor.isExists();
        } catch (Exception e) {
            Log.e(Constants.TAG, "TogglerService::checkAppDatabaseProcessor. Severe error. 'appDatabaseProcessor' lost.");
            return false;
        }
    }


    private void screenOnOff(boolean screenOn) {
        Log.v(Constants.TAG,String.format ("TogglerService::screenOnOff. Entry for [%s]...", screenOn ? "on" : "off"));

        this.screenOn = screenOn;
        watchdogThread.screenOnOff(screenOn);

        if (onGps().gpsOn && decisionCenter.possibleSetGps(false)) {
            // disable
            setGpsState(false);
        } else if (!onGps().gpsOn && decisionCenter.possibleSetGps(true)) {
            // enable
            setGpsState(true);
        }

        Log.v(Constants.TAG, "TogglerService::screenOnOff. Exit.");
    }
}