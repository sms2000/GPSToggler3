package ogp.com.gpstoggler3.apps;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;

import ogp.com.gpstoggler3.global.Constants;
import ogp.com.gpstoggler3.settings.Settings;

public class ListWatched extends ArrayList<AppStore> implements Parcelable {
    public static final Parcelable.Creator<ListWatched> CREATOR = new Parcelable.Creator<ListWatched>() {
        public ListWatched createFromParcel(Parcel in) {
            return new ListWatched(in);
        }

        public ListWatched[] newArray(int size) {
            return new ListWatched[size];
        }
    };


    public ListWatched() {
        super();
    }


    private ListWatched(Parcel in) {
        super();

        readFromParcel(in);
    }


    @Override
    public int describeContents() {
        return 0;
    }


    private void readFromParcel(Parcel in) {
        int counted = 0;
        int toRead;

        try {
            toRead = in.readInt();
        } catch (Throwable e) {
            Log.d(Constants.TAG, "ListWatched::readFromParcel. Exception [1]: ", e);
            return;
        }

        Log.d(Constants.TAG, String.format("ListWatched::readFromParcel. Should read [%d] apps.", toRead));

        for (int i = 0; i < toRead; i++) {
            try {
                String packageName = in.readString();
                String friendlyName = in.readString();
                if (null == packageName || null == friendlyName) {
                    break;
                } else if (0 == packageName.length() || 0 == friendlyName.length()) {
                    continue;
                }

                AppStore.AppState state = AppStore.AppState.values()[in.readInt()];

                Log.v(Constants.TAG, String.format("ListWatched::readFromParcel. Read package [%s]", packageName));

                AppStore appStore = new AppStore(friendlyName, packageName, null);
                appStore.setAppState(state);
                add(appStore);
                counted++;
            } catch (Exception e) {
                Log.d(Constants.TAG, "ListWatched::readFromParcel. Exception [2]: ", e);
                break;
            }
        }

        Log.d(Constants.TAG, String.format("ListWatched::readFromParcel. Read from Parcel: [%d] apps.", counted));
    }


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        int counted = 0;

        dest.writeInt(size());

        for (AppStore app : this) {
            dest.writeString(app.packageName);
            dest.writeString(app.friendlyName);
            dest.writeInt(app.getAppState().ordinal());
            Log.v(Constants.TAG, String.format("ListWatched::writeToParcel. Written [%s] package.", app.packageName));
            counted++;
        }

        Log.d(Constants.TAG, String.format("ListWatched::writeToParcel. Written to Parcel: [%d] apps.", counted));
    }


    public boolean containsPackage(String packageName) {
        for (AppStore app : this) {
            if (app.packageName.equals(packageName)) {
                return true;
            }
        }

        return false;
    }


    public AppStore.AppState getState(String packageName) {
        for (AppStore app : this) {
            if (app.packageName.equals(packageName)) {
                return app.getAppState();
            }
        }
        return AppStore.AppState.DISABLED;
    }


    private boolean packageExists(String packageName) {
        for (AppStore app : this) {
            if (app.packageName.equals(packageName)) {
                return true;
            }
        }

        return false;
    }


    boolean packageExists(AppStore appStore) {
        return packageExists(appStore.packageName);
    }
}

