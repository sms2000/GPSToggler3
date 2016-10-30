package ogp.com.gpstoggler3.apps;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ogp.com.gpstoggler3.interfaces.TogglerServiceInterface;
import ogp.com.gpstoggler3.results.RPCResult;
import ogp.com.gpstoggler3.settings.Settings;
import ogp.com.gpstoggler3.broadcasters.Broadcasters;
import ogp.com.gpstoggler3.global.Constants;
import ogp.com.gpstoggler3.su.RootCaller;


public class ApplicationsWatchdog extends Thread {
    private static final long DELAYED_ACTIVATION = 250;     // Activate thread after xxx ms
    private static final byte FOREGROUND = 'F';
    private static final long MIN_RUNTIME_MS = 10;

    private static final ListWatched lastActivatedApps = new ListWatched();
    private static final String EXECUTOR_COMMAND = "read_proc";

    private boolean initialPost = false;
    private Context context;
    private ActivityManager activityManager;
    private boolean active;
    private Boolean screenOn = null;
    private boolean automationOn = false;
    private Boolean gpsDecidedOn = null;
    private TogglerServiceInterface togglerServiceInterface = null;
    private Handler handler = new Handler();
    private SortComparator comparator = new SortComparator();
    private RootCaller.RootExecutor rootExecutor;


    private class SortComparator implements Comparator<AppStore> {
        @Override
        public int compare(AppStore o1, AppStore o2) {
            return o1.packageName.compareTo(o2.packageName);
        }
    }


    private class StatusChange implements Runnable {
        final private Boolean enableAutomation;
        final private Boolean enableGPS;
        private ListWatched activatedApps;


        private StatusChange(Boolean enableAutomation, Boolean enableGPS, ListWatched activatedApps) {
            this.enableAutomation = enableAutomation;
            this.enableGPS = enableGPS;

            synchronized (lastActivatedApps) {
                ListWatched copyWatched = new ListWatched();
                copyWatched.addAll(activatedApps);
                this.activatedApps = copyWatched;
            }
        }


        @Override
        public void run() {
            Log.v(Constants.TAG, "ApplicationsWatchdog::StatusChange::run. Entry. reportGPSSoftwareStatus succeeded for " + enableGPS);

            try {
                Intent intent = new Intent(Broadcasters.AUTO_STATE_CHANGED);
                intent.putExtra(Broadcasters.AUTO_STATE_CHANGED_AUTOMATION, enableAutomation);
                intent.putExtra(Broadcasters.AUTO_STATE_CHANGED_GPS, enableGPS);
                intent.putExtra(Broadcasters.AUTO_STATE_CHANGED_ACTIVATED_APPS, (Parcelable) activatedApps);

                context.sendBroadcast(intent);

                Log.d(Constants.TAG, String.format("ApplicationsWatchdog::StatusChange::run. Bundle sent with %d application(s).", activatedApps.size()));
            } catch (Exception e) {
                Log.e(Constants.TAG, "ApplicationsWatchdog::StatusChange::run. Exception: ", e);
            }

            Log.v(Constants.TAG, "ApplicationsWatchdog::StatusChange::run. Exit.");
        }
    }


    public ApplicationsWatchdog(Context context, TogglerServiceInterface togglerServiceInterface) {
        Log.v(Constants.TAG, "ApplicationsWatchdog. Entry...");

        this.context = context;
        this.togglerServiceInterface = togglerServiceInterface;
        this.activityManager = (ActivityManager) context.getSystemService(Activity.ACTIVITY_SERVICE);
        this.rootExecutor = null;

        active = true;
        start();

        Log.v(Constants.TAG, "ApplicationsWatchdog. Exit.");
    }


    public static ListWatched getActivatedApps() {
        synchronized (lastActivatedApps) {
            ListWatched copyWatched = new ListWatched();
            copyWatched.addAll(lastActivatedApps);
            return copyWatched;
        }
    }


    public void activateNow() {
        Log.v(Constants.TAG, "ApplicationsWatchdog::activateNow. Entry...");

        Log.i(Constants.TAG, "ApplicationsWatchdog::activateNow. Manual activation interrupting the watchdog wait.");
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                ApplicationsWatchdog.this.interrupt();
            }
        }, DELAYED_ACTIVATION);

        Log.v(Constants.TAG, "ApplicationsWatchdog::activateNow. Exit.");
    }


    public void finish() {
        Log.v(Constants.TAG, "ApplicationsWatchdog. Finalized...");

        active = false;

        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {
                Log.v(Constants.TAG, "ApplicationsWatchdog. Exception in 'wait'. Interrupted?");
            }
        }

        Log.i(Constants.TAG, "ApplicationsWatchdog. Joined.");
    }


    @Override
    public void run() {
        Log.v(Constants.TAG, "ApplicationsWatchdog::run. Started.");

        while (active) {
            if (automationOn && null != togglerServiceInterface.listActivatedApps()) {
                long timeDelta = System.currentTimeMillis();
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    verifyGPSSoftwareRunning21();
                } else {
                    verifyGPSSoftwareRunning();
                }

                timeDelta = System.currentTimeMillis() - timeDelta;
                if (MIN_RUNTIME_MS <= timeDelta) {
                    Log.i(Constants.TAG, String.format("ApplicationsWatchdog::run. Spen %d msec enumerating running applications.", timeDelta));
                }
            } else {
                Log.v(Constants.TAG, "ApplicationsWatchdog::run. Idle...");
            }


            try {
                if (null == screenOn || screenOn) {
                    Thread.sleep(Settings.getOnPollingDelay());
                } else {
                    Thread.sleep(Settings.getOffPollingDelay());
                }
            } catch (InterruptedException e) {
                Log.d(Constants.TAG, "ApplicationsWatchdog::run. Exception in 'sleep'. Interrupted?");
            }
        }

        synchronized (this) {
            notify();
        }

        Log.v(Constants.TAG, "ApplicationsWatchdog::run. Finished.");
    }


    public void screenOnOff(boolean screenOn) {
        this.screenOn = screenOn;

        interrupt();
    }


    public synchronized void automationOnOff(boolean automation) {
        Log.v(Constants.TAG, "ApplicationsWatchdog::automationOnOff. Entry...");

        if (this.automationOn != automation) {
            this.automationOn = automation;

            Log.i(Constants.TAG, String.format("ApplicationsWatchdog::automationOnOff. %s automation.", automation ? "Enabling" : "Disabling"));

            interrupt();

            handler.post(new StatusChange(automationOn, gpsDecidedOn, lastActivatedApps));
        }

        Log.v(Constants.TAG, "ApplicationsWatchdog::automationOnOff. Exit.");
    }


    synchronized private void verifyGPSSoftwareRunning() {
        ListWatched activatedApps = new ListWatched();
        ListWatched watchedApps = togglerServiceInterface.listWatchedApps();

        List<ActivityManager.RunningAppProcessInfo> list = activityManager.getRunningAppProcesses();

        for (ActivityManager.RunningAppProcessInfo iterator : list) {
            for (AppStore iterator2 : watchedApps) {
                if (iterator.processName.equals(iterator2.packageName)) {
                    if (iterator.importance > ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && iterator2.getAppState() == AppStore.AppState.FOREGROUND) {
                        continue;
                    }

                    activatedApps.add(iterator2);
                    Log.v(Constants.TAG, "ApplicationsWatchdog::verifyGPSSoftwareRunning. GPS status active due to running process: " + iterator.processName);
                }
            }
        }


        boolean gpsStatusNow = 0 < activatedApps.size();
        if (gpsStatusNow) {
            Collections.sort(activatedApps, comparator);
        }


        boolean equal;
        synchronized (lastActivatedApps) {
            equal = activatedApps.size() == lastActivatedApps.size();
            if (equal) {
                for (int i = 0; i < activatedApps.size(); i++) {
                    if (!activatedApps.packageExists(lastActivatedApps.get(i))) {
                        equal = false;
                        break;
                    }
                }
            }


            boolean gpsOnNow = togglerServiceInterface.onGps().gpsOn;
            if (gpsStatusNow != gpsOnNow || !equal || !initialPost) {
                initialPost = true;

                Log.i(Constants.TAG, "ApplicationsWatchdog::verifyGPSSoftwareRunning. GPS software status changed. Now it's " + (gpsStatusNow ? "running." : "stopped."));

                gpsDecidedOn = gpsStatusNow;
                if (!equal) {
                    lastActivatedApps.clear();
                    lastActivatedApps.addAll(activatedApps);

                    for (AppStore app : lastActivatedApps) {
                        Log.v(Constants.TAG, String.format("ApplicationsWatchdog::verifyGPSSoftwareRunning. Active application: %s.", app.packageName));
                    }
                }

                handler.post(new StatusChange(true, gpsDecidedOn, lastActivatedApps));
            }

            Log.v(Constants.TAG, String.format("ApplicationsWatchdog::verifyGPSSoftwareRunning. Total processes: %d, watched processes: %d, activated: %d/%d.",
                    list.size(), togglerServiceInterface.listActivatedApps().size(), lastActivatedApps.size(), activatedApps.size()));
        }
    }


    private void verifyGPSSoftwareRunning21() {
        if (!Settings.isRootGranted()) {
            Log.i(Constants.TAG, "ApplicationsWatchdog::verifyGPSSoftwareRunning21. No 'root' granted yet.");
            return;
        }

        ListWatched activatedApps = new ListWatched();
        ListWatched watchedApps = togglerServiceInterface.listWatchedApps();
        List<RootProcessManager.AndroidAppProcess> list;

        if (null == rootExecutor) {
            rootExecutor = RootCaller.createRootProcess();
            if (null != rootExecutor) {
                Log.w(Constants.TAG, "ApplicationsWatchdog::verifyGPSSoftwareRunning21. 'root' granted and RootExecutor created.");
            } else {
                Log.e(Constants.TAG, "ApplicationsWatchdog::verifyGPSSoftwareRunning21. Failed to create 'RootExecutor'.");
                return;
            }
        }

        RPCResult output = rootExecutor.executeCommander(EXECUTOR_COMMAND, Settings.getRootTimeoutDelay());
        if (!output.isError()) {
            list = parseExecutorOutput(output);

            for (RootProcessManager.AndroidAppProcess iterator : list) {
                for (AppStore iterator2 : watchedApps) {
                    if (iterator.getPackageName().equals(iterator2.packageName)) {
                        if (!iterator.isForeground() && iterator2.getAppState() == AppStore.AppState.FOREGROUND) {
                            continue;
                        }

                        activatedApps.add(iterator2);
                        Log.v(Constants.TAG, "ApplicationsWatchdog::verifyGPSSoftwareRunning21. GPS status active due to running process: " + iterator2);
                    }
                }
            }
        } else {
            Log.d(Constants.TAG, "ApplicationsWatchdog::verifyGPSSoftwareRunning21. No suitable applications found.");
            return;
        }


        boolean gpsStatusNow = 0 < activatedApps.size();
        if (gpsStatusNow) {
            Collections.sort(activatedApps, comparator);
        }


        synchronized (lastActivatedApps) {
            boolean equal = activatedApps.size() == lastActivatedApps.size();
            if (equal) {
                for (int i = 0; i < activatedApps.size(); i++) {
                    if (!activatedApps.get(i).equals(lastActivatedApps.get(i))) {
                        equal = false;
                        break;
                    }
                }
            }


            boolean gpsOnNow = togglerServiceInterface.onGps().gpsOn;
            if (gpsStatusNow != gpsOnNow || !equal || !initialPost) {
                initialPost = true;

                Log.i(Constants.TAG, "ApplicationsWatchdog::verifyGPSSoftwareRunning21. GPS software status changed. Now it's " + (gpsStatusNow ? "running." : "stopped."));

                gpsDecidedOn = gpsStatusNow;
                if (!equal) {
                    lastActivatedApps.clear();
                    lastActivatedApps.addAll(activatedApps);

                    for (AppStore app : lastActivatedApps) {
                        Log.v(Constants.TAG, String.format("ApplicationsWatchdog::verifyGPSSoftwareRunning21. Active application: %s.", app.packageName));
                    }
                }

                handler.post(new StatusChange(true, gpsDecidedOn, lastActivatedApps));
            }


            Log.d(Constants.TAG, String.format("ApplicationsWatchdog::verifyGPSSoftwareRunning21. Total processes: %d, watched processes: %d, activated: %d/%d.",
                    list.size(), togglerServiceInterface.listActivatedApps().size(), lastActivatedApps.size(), activatedApps.size()));
        }
    }


    private List<RootProcessManager.AndroidAppProcess> parseExecutorOutput(RPCResult result) {
        List<RootProcessManager.AndroidAppProcess> list = new ArrayList<>();

        if (result.isList()) {
            for (Object appO : result.getList()) {
                if (appO instanceof String) {
                    String appS = (String)appO;
                    if (appS.length() < 2) {
                        continue;
                    }

                    String packageName = appS.substring(1, appS.length());
                    boolean foreground = appS.getBytes()[0] == FOREGROUND;
                    RootProcessManager.AndroidAppProcess process = new RootProcessManager.AndroidAppProcess(packageName, foreground);
                    list.add(process);
                }
            }
        }

        return list;
    }
}
