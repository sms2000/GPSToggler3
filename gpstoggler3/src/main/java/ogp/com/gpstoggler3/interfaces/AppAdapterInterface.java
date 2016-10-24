package ogp.com.gpstoggler3.interfaces;

import ogp.com.gpstoggler3.apps.AppStore;
import ogp.com.gpstoggler3.controls.ThreeStateSwitch;

public interface AppAdapterInterface {
    void onClickAppLookup(AppStore appStore, AppStore.AppState appState);
}
