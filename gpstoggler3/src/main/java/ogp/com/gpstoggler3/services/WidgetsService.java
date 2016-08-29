package ogp.com.gpstoggler3.services;

import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViewsService;

import ogp.com.gpstoggler3.global.Constants;
import ogp.com.gpstoggler3.widgets.AppListProvider;


public class WidgetsService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        Log.i(Constants.TAG, "TogglerService::onGetViewFactory. Invoked...");

        return new AppListProvider(this.getApplicationContext());
    }
}
