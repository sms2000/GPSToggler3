package ogp.com.gpstoggler3.apps;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import ogp.com.gpstoggler3.R;
import ogp.com.gpstoggler3.global.Constants;
import ogp.com.gpstoggler3.resources.IconStorage;


public class AppAdapter extends ArrayAdapter<AppStore> {
    private AppAdapterInterface appAdapterInterface;


    public AppAdapter(Context context, AppAdapterInterface appAdapterInterface) {
        super(context, 0, new ArrayList<AppStore>());

        this.appAdapterInterface = appAdapterInterface;

        IconStorage.getInstance(context);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final AppStore appStore = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_list, parent, false);
        }

        TextView appName = (TextView) convertView.findViewById(R.id.appName);
        TextView appPackage = (TextView) convertView.findViewById(R.id.appPackage);
        ImageView icon = (ImageView) convertView.findViewById(R.id.appImage);
        final CheckBox appLookup = (CheckBox) convertView.findViewById(R.id.appLookup);

        appLookup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickAppLookup(appStore, appLookup.isChecked());
            }
        });

        appName.setText(appStore.friendlyName);
        appPackage.setText(appStore.packageName);
        setImageDrawable(icon, appStore);
        appLookup.setChecked(appStore.getLookup());
        return convertView;
    }


    public void uploadActivatedApps(ListWatched listActivated) {
        int activated = 0;
        int deactivated = 0;

        for (int i = 0; i < getCount(); i++) {
            AppStore appStore = getItem(i);

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


    private void onClickAppLookup(AppStore appStore, boolean checked) {
        appStore.setLookup(checked);
        notifyDataSetChanged();

        appAdapterInterface.onClickAppLookup(appStore, checked);
    }


    private void setImageDrawable(ImageView icon, AppStore app) {
        Drawable drawable;

        if (!app.getLookup()) {
            drawable = IconStorage.getDisabled();
        } else if (app.getActive()) {
            drawable = IconStorage.getActive();
        } else {
            drawable = IconStorage.getInactive();
        }

        icon.setImageDrawable(drawable);
    }
}
