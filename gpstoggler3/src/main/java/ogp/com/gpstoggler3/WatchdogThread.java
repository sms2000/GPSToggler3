package ogp.com.gpstoggler3;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;

import com.jaredrummler.android.processes.ProcessManager;
import com.jaredrummler.android.processes.models.AndroidAppProcess;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ogp.com.gpstoggler3.apps.AppStore;
import ogp.com.gpstoggler3.apps.ListWatched;
import ogp.com.gpstoggler3.apps.Settings;
import ogp.com.gpstoggler3.broadcasters.Broadcasters;
import ogp.com.gpstoggler3.global.Constants;


class WatchdogThread extends Thread {
    private static final long TIMEOUT_SCREEN_OFF = 30000;   // Polling time (ms) when screen off
    private static final long TIMEOUT_OFF = 4000;           // Polling time (ms) when GPS off
    private static final long TIMEOUT_ON = 10000;           // Polling time (ms) when GPS on

    private static final ListWatched lastActivatedApps = new ListWatched();
    private static final int MAX_MANDATORY_UPDATES = 2;

    private Context context;
    private ActivityManager activityManager;
    private boolean active;
    private Boolean screenOn = null;
    private boolean automationOn = false;
    private Boolean gpsDecidedOn = null;
    private TogglerServiceInterface togglerServiceInterface = null;
    private Handler handler = new Handler();
    private SortComparator comparator = new SortComparator();
    private int countDownForMandatoryUpdates = MAX_MANDATORY_UPDATES;


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
            this.activatedApps = activatedApps;
        }


        @Override
        public void run() {
            try {
                Log.v(Constants.TAG, "StatusChange::run. reportGPSSoftwareStatus succeeded for " + enableGPS);

                Intent intent = new Intent(Broadcasters.AUTO_STATE_CHANGED);
                intent.putExtra(Broadcasters.AUTO_STATE_CHANGED_AUTOMATION, enableAutomation);
                intent.putExtra(Broadcasters.AUTO_STATE_CHANGED_GPS, enableGPS);
                intent.putExtra(Broadcasters.AUTO_STATE_CHANGED_ACTIVATED_APPS, (Parcelable) activatedApps);

                context.sendBroadcast(intent);

                Log.e(Constants.TAG, String.format("^^^^ StatusChange::run. Bundle sent with %d application(s).", activatedApps.size()));
            } catch (Exception e) {
                Log.e(Constants.TAG, "StatusChange::run. Exception: ", e);
            }
        }
    }


    WatchdogThread(Context context,TogglerServiceInterface togglerServiceInterface) {
        Log.v(Constants.TAG, "WatchdogThread. Entry...");

        this.context = context;
        this.togglerServiceInterface = togglerServiceInterface;
        this.activityManager = (ActivityManager) context.getSystemService(Activity.ACTIVITY_SERVICE);

        active = true;
        start();

        Log.v(Constants.TAG, "WatchdogThread. Exit.");
    }


    static ListWatched getActivatedApps() {
        synchronized (lastActivatedApps) {
            return lastActivatedApps;
        }
    }


    void finish() {
        Log.v(Constants.TAG, "WatchdogThread. Finalized...");

        active = false;

        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {
                Log.v(Constants.TAG, "WatchdogThread. Exception in 'wait'. Interrupted?");
            }
        }

        Log.i(Constants.TAG, "WatchdogThread. Joined.");
    }


    @Override
    public void run() {
        Log.v(Constants.TAG, "WatchdogThread. Started.");

        while (active) {
            if (automationOn && null != togglerServiceInterface.listActivatedApps()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    verifyGPSSoftwareRunning21();
                } else {
                    verifyGPSSoftwareRunning();
                }

                try {
                    if (null == screenOn || screenOn) {
                        Thread.sleep(null != gpsDecidedOn && gpsDecidedOn ? TIMEOUT_ON : TIMEOUT_OFF);
                    } else {
                        Thread.sleep(TIMEOUT_SCREEN_OFF);
                    }
                } catch (InterruptedException e) {
                    Log.e(Constants.TAG, "WatchdogThread. Exception in 'sleep'. Interrupted?");
                }
            }
        }

        synchronized (this) {
            notify();
        }

        Log.v(Constants.TAG, "WatchdogThread. Finished.");
    }


    void screenOnOff(boolean screenOn) {
        this.screenOn = screenOn;

        interrupt();
    }


    synchronized void automationOnOff(boolean automation) {
        Log.v(Constants.TAG, "WatchdogThread::automationOnOff. Entry...");

        if (this.automationOn != automation) {
            this.automationOn = automation;

            Log.i(Constants.TAG, String.format("WatchdogThread::automationOnOff. %s automation.", automation ? "Enabling" : "Disabling"));

            interrupt();

            if (!automation) {
                synchronized (lastActivatedApps) {
                    gpsDecidedOn = null;
                    lastActivatedApps.clear();
                }

                handler.post(new StatusChange(automationOn, gpsDecidedOn, lastActivatedApps));
            }
        }

        Log.v(Constants.TAG, "WatchdogThread::automationOnOff. Exit.");
    }


    synchronized private void verifyGPSSoftwareRunning() {
        ListWatched activatedApps = new ListWatched();
        ListWatched watchedApps = togglerServiceInterface.listWatchedApps();
        int importance = Settings.getSplitAware() ?
                ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE : ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;

        List<ActivityManager.RunningAppProcessInfo> list = activityManager.getRunningAppProcesses();

        for (ActivityManager.RunningAppProcessInfo iterator : list) {
            if (importance >= iterator.importance) {
                for (AppStore iterator2 : watchedApps) {
                    if (iterator.processName.equals(iterator2.packageName)) {
                        activatedApps.add(iterator2);
                        Log.v(Constants.TAG, "WatchdogThread::verifyGPSSoftwareRunning. GPS status active due to running process: " + iterator.processName);
                    }
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
                    if (!activatedApps.get(i).equals(lastActivatedApps.get(i))) {
                        equal = false;
                        break;
                    }
                }
            }
        }


        boolean gpsOnNow = togglerServiceInterface.onGps().gpsOn;
        if (gpsStatusNow != gpsOnNow || !equal || 0 < countDownForMandatoryUpdates) {
            Log.i(Constants.TAG, "WatchdogThread::verifyGPSSoftwareRunning. GPS software status changed. Now it's " + (gpsStatusNow ? "running." : "stopped."));

            gpsDecidedOn = gpsStatusNow;
            if (!equal) {
                synchronized (lastActivatedApps) {
                    lastActivatedApps.clear();
                    for (int i = 0; i < activatedApps.size(); i++) {
                        lastActivatedApps.add(activatedApps.get(i));
                    }
                }
            }

            if (0 < countDownForMandatoryUpdates) {
                countDownForMandatoryUpdates--;
            }

            handler.post(new StatusChange(true, gpsDecidedOn, lastActivatedApps));
        }

        Log.v(Constants.TAG, String.format("WatchdogThread::verifyGPSSoftwareRunning. Total processes: %d, watched processes: %d, activated: %d/%d.",
                list.size(), togglerServiceInterface.listActivatedApps().size(), lastActivatedApps.size(), activatedApps.size()));
    }


    private void verifyGPSSoftwareRunning21() {
        ListWatched activatedApps = new ListWatched();
        ListWatched watchedApps = togglerServiceInterface.listWatchedApps();
        boolean forceForeground = !Settings.getSplitAware();

        List<AndroidAppProcess> list = ProcessManager.getRunningAppProcesses();

        for (AndroidAppProcess iterator : list) {
            if (forceForeground && !iterator.isForeground()) {
                continue;
            }

            for (AppStore iterator2 : watchedApps) {
                if (iterator.getPackageName().equals(iterator2.packageName)) {
                    activatedApps.add(iterator2);
                    Log.v(Constants.TAG, "WatchdogThread::verifyGPSSoftwareRunning21. GPS status active due to running process: " + iterator2);
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
                    if (!activatedApps.get(i).equals(lastActivatedApps.get(i))) {
                        equal = false;
                        break;
                    }
                }
            }
        }


        boolean gpsOnNow = togglerServiceInterface.onGps().gpsOn;
        if (gpsStatusNow != gpsOnNow || !equal || 0 < countDownForMandatoryUpdates) {
            Log.i(Constants.TAG, "WatchdogThread::verifyGPSSoftwareRunning21. GPS software status changed. Now it's " + (gpsStatusNow ? "running." : "stopped."));

            gpsDecidedOn = gpsStatusNow;
            if (!equal) {
                synchronized (lastActivatedApps) {
                    lastActivatedApps.clear();
                    lastActivatedApps.addAll(activatedApps);

                    for (AppStore app : lastActivatedApps) {
                        Log.e(Constants.TAG, String.format("##!!!## >>> %s", app.packageName));

                    }
                }
            }

            if (0 < countDownForMandatoryUpdates) {
                countDownForMandatoryUpdates--;
            }

            handler.post(new StatusChange(true, gpsDecidedOn, lastActivatedApps));
        }

        Log.d(Constants.TAG, String.format("WatchdogThread::verifyGPSSoftwareRunning21. Total processes: %d, watched processes: %d, activated: %d/%d.",
                list.size(), togglerServiceInterface.listActivatedApps().size(), lastActivatedApps.size(), activatedApps.size()));
    }
}
