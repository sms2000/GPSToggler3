package ogp.com.gpstoggler3.apps;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;

public class AppStore {
    public enum AppState {DISABLED, FOREGROUND, BACKGROUND}


    public String friendlyName;
    public String packageName;
    private boolean active;
    private AppState appState;
    private Drawable appIcon;


    public AppStore(String appName, String appPackage, Drawable appIcon) {
        this.appIcon = appIcon;
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


    public Drawable getAppIcon() {
        return appIcon;
    }
}
