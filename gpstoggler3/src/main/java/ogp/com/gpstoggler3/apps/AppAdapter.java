package ogp.com.gpstoggler3.apps;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
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
import ogp.com.gpstoggler3.controls.TriStateSwitch;
import ogp.com.gpstoggler3.global.Constants;
import ogp.com.gpstoggler3.interfaces.AppAdapterInterface;
import ogp.com.gpstoggler3.resources.IconStorage;


public class AppAdapter extends ArrayAdapter<AppStore> {
    private AppAdapterInterface appAdapterInterface;


    public AppAdapter(Context context, AppAdapterInterface appAdapterInterface) {
        super(context, 0, new ArrayList<AppStore>());

        this.appAdapterInterface = appAdapterInterface;

        IconStorage.getInstance(context);
    }


    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final AppStore appStore = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_list, parent, false);
        }

        final TextView appName = convertView.findViewById(R.id.appName);
        final TextView appPackage = convertView.findViewById(R.id.appPackage);
        final ImageView stateIcon = convertView.findViewById(R.id.appImage);
        final TriStateSwitch appLookup = convertView.findViewById(R.id.appLookup);
        final ImageView appIcon = convertView.findViewById(R.id.appIcon);

        appLookup.setFlashListener(new TriStateSwitch.TriStateListener() {
            @Override
            public void onStateChanged(AppStore.AppState appState) {
                onClickAppLookup(appStore, appLookup.getState());
            }
        });

        assert appStore != null;
        appName.setText(appStore.friendlyName);
        appPackage.setText(appStore.packageName);
        setStateImageDrawable(stateIcon, appStore);
        appLookup.setState(appStore.getAppState());
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


    public void uploadActivatedApps(ListWatched listActivated) {
        int activated = 0;
        int deactivated = 0;

        for (int i = 0; i < getCount(); i++) {
            AppStore appStore = getItem(i);

            assert appStore != null;
            if (null != listActivated && listActivated.containsPackage(appStore.packageName)) {
                activated++;
                appStore.setActive(true);
            } else {
                deactivated++;
                appStore.setActive(false);
            }
        }

        Log.d(Constants.TAG, String.format("AppAdapter::uploadActivatedApps. Found %d active and %d inactive applications.", activated, deactivated));

        notifyDataSetChanged();
    }


    private void onClickAppLookup(AppStore appStore, AppStore.AppState state) {
        appStore.setAppState(state);
        notifyDataSetChanged();

        appAdapterInterface.onClickAppLookup(appStore, state);
    }


    private void setStateImageDrawable(ImageView stateIcon, AppStore app) {
        Drawable drawable;

        if (AppStore.AppState.DISABLED == app.getAppState()) {
            drawable = IconStorage.getDisabled();
        } else if (app.getActive()) {
            drawable = IconStorage.getActive();
        } else {
            drawable = IconStorage.getInactive();
        }

        stateIcon.setImageDrawable(drawable);
    }


    private void setAppImageDrawable(ImageView appIcon, AppStore app) {
        appIcon.setImageDrawable(app.getAppIcon());
    }
}
