package ogp.com.gpstoggler3.widgets;


import android.content.Context;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService.RemoteViewsFactory;

import java.util.ArrayList;

import ogp.com.gpstoggler3.ExecuteOnServiceWithTimeout;
import ogp.com.gpstoggler3.R;
import ogp.com.gpstoggler3.apps.AppStore;
import ogp.com.gpstoggler3.debug.Constants;


class AppListProvider implements RemoteViewsFactory {
    private static final String RPC_METHOD = "listActivatedApps";
    private static final int RPC_TIMEOUT = 5000;            // 5 second

    private ArrayList<String> listItemList = new ArrayList<>();
    private Context context = null;


    AppListProvider(Context context) {
        this.context = context;
    }



    @Override
    public void onCreate() {
    }


    @Override
    public void onDataSetChanged() {
        ArrayList<AppStore> list = (ArrayList<AppStore>) new ExecuteOnServiceWithTimeout(context).execute(RPC_METHOD, RPC_TIMEOUT);

        listItemList.clear();
        if (null != list) {
            for (AppStore app : list) {
                listItemList.add(app.friendlyName);
            }
        }

        if (0 == listItemList.size()) {
            String noActiveApps = context.getResources().getString(R.string.no_active_apps_found);
            listItemList.add(noActiveApps);
        }

        Log.i(Constants.TAG, String.format("AppListProvider::onDataSetChanged. Loaded %d app(s).", listItemList.size()));
    }


    @Override
    public void onDestroy() {
    }


    @Override
    public int getCount() {
        return listItemList.size();
    }


    @Override
    public long getItemId(int position) {
        return position;
    }


    @Override
    public boolean hasStableIds() {
        return false;
    }


    @Override
    public RemoteViews getViewAt(int position) {
        final RemoteViews remoteView = new RemoteViews(context.getPackageName(), R.layout.list_row);
        String listItem = listItemList.get(position);
        remoteView.setTextViewText(R.id.appName, listItem);
        return remoteView;
    }


    @Override
    public RemoteViews getLoadingView() {
        return null;
    }


    @Override
    public int getViewTypeCount() {
        return 1;
    }
}
