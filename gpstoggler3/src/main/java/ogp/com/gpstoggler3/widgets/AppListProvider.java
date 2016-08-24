package ogp.com.gpstoggler3.widgets;


import android.content.Context;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService.RemoteViewsFactory;

import java.util.ArrayList;

import ogp.com.gpstoggler3.ExecuteOnServiceWithTimeout;
import ogp.com.gpstoggler3.R;
import ogp.com.gpstoggler3.apps.AppStore;
import ogp.com.gpstoggler3.global.Constants;


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
        Log.v(Constants.TAG, "AppListProvider::onCreate. Invoked.");
    }


    @Override
    public void onDataSetChanged() {
        Log.v(Constants.TAG, "AppListProvider::onDataSetChanged. Entry...");

        listItemList.clear();

        ArrayList   list;

        try {
            list = (ArrayList) new ExecuteOnServiceWithTimeout(context).execute(RPC_METHOD, RPC_TIMEOUT);
        } catch (Exception e) {
            Log.e(Constants.TAG, "AppListProvider::onDataSetChanged. Error with 'ExecuteOnServiceWithTimeout::execute' returning not expected type [1].");

            Log.v(Constants.TAG, "AppListProvider::onDataSetChanged. Exit [1].");
            return;
        }

        for (int i = 0; null != list && i < list.size(); i++) {
            try {
                AppStore app = (AppStore) list.get(i);
                listItemList.add(app.friendlyName);
            } catch (Exception e) {
                Log.e(Constants.TAG, "AppListProvider::onDataSetChanged. Error with 'ExecuteOnServiceWithTimeout::execute' returning not expected type [2].");

                Log.v(Constants.TAG, "AppListProvider::onDataSetChanged. Exit [2].");
                return;
            }
        }

        if (0 == listItemList.size()) {
            String noActiveApps = context.getResources().getString(R.string.no_active_apps_found);
            listItemList.add(noActiveApps);
        }

        Log.i(Constants.TAG, String.format("AppListProvider::onDataSetChanged. Loaded %d app(s).", listItemList.size()));

        Log.v(Constants.TAG, "AppListProvider::onDataSetChanged. Exit [3].");
    }


    @Override
    public void onDestroy() {
        Log.v(Constants.TAG, "AppListProvider::onDestroy. Invoked.");
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
