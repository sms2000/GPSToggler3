package ogp.com.gpstoggler3;

import ogp.com.gpstoggler3.apps.ListAppStore;
import ogp.com.gpstoggler3.apps.ListWatched;
import ogp.com.gpstoggler3.status.GPSStatus;


interface ITogglerService {
    int getPid();
    void enumerateApps();

    ListAppStore listInstalledApps(long lastNewAppList);
    ListWatched listActivatedApps();

    ListWatched listWatchedApps();
    boolean storeWatchedApps(in ListWatched appList);

    boolean loadAutomationState();
    void storeAutomationState(boolean watcher);
    GPSStatus onGps();
    void toggleGpsState();
    boolean isRootGranted();
    void setRootGranted(boolean rootGranted);
}
