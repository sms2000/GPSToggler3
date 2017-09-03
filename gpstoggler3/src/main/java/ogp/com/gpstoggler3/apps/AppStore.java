package ogp.com.gpstoggler3.apps;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;

public class AppStore {
    public enum AppState {DISABLED, FOREGROUND, BACKGROUND, STARTABLE}


    public String friendlyName;
    public String packageName;
    private boolean active;
    private AppState appState;
    private Drawable appIcon;
    private int inWidgets;


    public AppStore(String appName, String appPackage, Drawable appIcon) {
        this.appIcon = appIcon;
        this.friendlyName = appName;
        this.packageName = appPackage;
        this.active = false;
        this.appState = AppState.DISABLED;
        this.inWidgets = 0;
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


    public void setInWidgets(int inWidgets) {
        this.inWidgets = inWidgets;
    }


    public int getInWidgets() {
        return inWidgets;
    }
}
