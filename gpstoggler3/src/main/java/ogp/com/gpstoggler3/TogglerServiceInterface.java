package ogp.com.gpstoggler3;

import ogp.com.gpstoggler3.apps.ListAppStore;
import ogp.com.gpstoggler3.apps.ListWatched;
import ogp.com.gpstoggler3.status.GPSStatus;


interface TogglerServiceInterface {
    int getPid();

    ListAppStore listInstalledApps(long lastNewAppList);
    ListWatched listActivatedApps();

    void reloadInstalledApps();
    ListWatched listWatchedApps();
    boolean storeWatchedApps(ListWatched appList);

    boolean loadAutomationState();
    void storeAutomationState(boolean watcher);
    GPSStatus onGps();
    void toggleGpsState();
    boolean isRootGranted();
    void setRootGranted(boolean rootGranted);
}
