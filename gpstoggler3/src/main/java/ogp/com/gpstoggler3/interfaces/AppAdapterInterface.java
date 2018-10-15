package ogp.com.gpstoggler3.interfaces;

import ogp.com.gpstoggler3.apps.AppStore;

public interface AppAdapterInterface {
    void onClickAppLookup(AppStore appStore, AppStore.AppState appState);
    void onClickBTLookup(AppStore appStore, AppStore.BTState btState);
}
