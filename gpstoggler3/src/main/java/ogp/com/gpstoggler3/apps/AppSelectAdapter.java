package ogp.com.gpstoggler3.apps;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import ogp.com.gpstoggler3.R;
import ogp.com.gpstoggler3.global.Constants;
import ogp.com.gpstoggler3.interfaces.AppAdapterInterface;
import ogp.com.gpstoggler3.resources.IconStorage;


public class AppSelectAdapter extends ArrayAdapter<AppStore> {
    private AppAdapterInterface appAdapterInterface;


    public AppSelectAdapter(Context context, AppAdapterInterface appAdapterInterface) {
        super(context, 0, new ArrayList<AppStore>());

        this.appAdapterInterface = appAdapterInterface;

        IconStorage.getInstance(context);
    }


    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final AppStore appStore = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.app_list, parent, false);
        }

        final TextView appName = convertView.findViewById(R.id.appName);
        final TextView appPackage = convertView.findViewById(R.id.appPackage);
        final ImageView appIcon = convertView.findViewById(R.id.appIcon);

        assert appStore != null;
        appName.setText(appStore.friendlyName);
        appPackage.setText(appStore.packageName);
        setAppImageDrawable(appIcon, appStore);
        return convertView;
    }


    public boolean updateCollection(ListAppStore appList) {
        Log.v(Constants.TAG, "AppAdapter::updateCollection. Entry...");

        boolean updateRequired = false;

        if (getCount() != appList.size()) {
            updateRequired = true;
            Log.i(Constants.TAG, "AppAdapter::updateCollection. Update required: different length of apps.");
        } else {
            for (int i = 0; i < getCount(); i++) {
                AppStore existedApp = getItem(i);
                AppStore newApp = appList.get(i);
                assert existedApp != null;
                if (!existedApp.packageName.equals(newApp.packageName)) {
                    updateRequired = true;
                    Log.i(Constants.TAG, "AppAdapter::updateCollection. Update required: different list of apps.");
                    break;
                }
            }
        }

        if (updateRequired) {
            clear();
            addAll(appList);
        } else {
            Log.i(Constants.TAG, "AppAdapter::updateCollection. Update not required: same list of apps.");
        }

        Log.v(Constants.TAG, "AppAdapter::updateCollection. Exit.");
        return updateRequired;
    }


    private void setAppImageDrawable(ImageView appIcon, AppStore app) {
        appIcon.setImageDrawable(app.getAppIcon());
    }
}
