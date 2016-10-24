package ogp.com.gpstoggler3.apps;

public class AppStore {
    public enum AppState {DISABLED, FOREGROUND, BACKGROUND};


    public String friendlyName;
    public String packageName;
    private boolean active;
    private AppState appState;


    public AppStore(String appName, String appPackage) {
        this.friendlyName = appName;
        this.packageName = appPackage;
        this.active = false;
        this.appState = AppState.DISABLED;
    }


    boolean getActive() {
        return active;
    }


    public AppState getAppState() {
        return appState;
    }


    void setActive(boolean active) {
        this.active = active;
    }


    public void setAppState(AppState appState) {
        this.appState = appState;
    }
}
