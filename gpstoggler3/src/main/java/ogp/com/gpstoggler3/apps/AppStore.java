package ogp.com.gpstoggler3.apps;

public class AppStore {
    public String friendlyName;
    public String packageName;
    private boolean lookup;
    private boolean active;


    public AppStore(String appName, String appPackage) {
        this.friendlyName = appName;
        this.packageName = appPackage;
        this.lookup = false;
        this.active = false;
    }


    public boolean getActive() {
        return active;
    }


    public boolean getLookup() {
        return lookup;
    }


    public void setActive(boolean active) {
        this.active = active;
    }


    public void setLookup(boolean lookup) {
        this.lookup = lookup;
    }
}
