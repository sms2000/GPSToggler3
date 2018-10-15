package ogp.com.gpstoggler3.apps;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.media.audiofx.BassBoost;

import ogp.com.gpstoggler3.settings.Settings;

public class AppStore {
    public enum AppState {DISABLED, FOREGROUND, BACKGROUND, STARTABLE}
    public enum BTState {LEAVE_AS_IS, ENABLE_WHEN_RUN}


    public String friendlyName;
    public String packageName;
    private boolean active;
    private AppState appState;
    private BTState btState;
    private Drawable appIcon;
    private int inWidgets;


    public AppStore(String appName, String appPackage, Drawable appIcon) {
        this.appIcon = appIcon;
        this.friendlyName = appName;
        this.packageName = appPackage;
        this.active = false;
        this.appState = AppState.DISABLED;
        this.btState = BTState.LEAVE_AS_IS;
        this.inWidgets = Settings.countWidgets(this.packageName);
    }


    boolean getActive() {
        return active;
    }


    public AppState getAppState() {
        return appState;
    }


    public BTState getBTState() {
        return btState;
    }


    public void setBTState(BTState state) {
        btState = state;
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


    public int getInWidgets() {
        return inWidgets;
    }
}
