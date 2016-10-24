package ogp.com.gpstoggler3.apps;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;

import ogp.com.gpstoggler3.global.Constants;

public class ListAppStore extends ArrayList<AppStore> implements Parcelable {
    public static final Parcelable.Creator<ListAppStore> CREATOR = new Parcelable.Creator<ListAppStore>() {
        public ListAppStore createFromParcel(Parcel in) {
            return new ListAppStore(in);
        }


        public ListAppStore[] newArray(int size) {
            return new ListAppStore[size];
        }
    };


    public ListAppStore() {
        super();
    }


    private ListAppStore(Parcel in) {
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
            Log.d(Constants.TAG, "ListAppStore::readFromParcel. Exception [1]: ", e);
            return;
        }

        Log.d(Constants.TAG, String.format("ListAppStore::readFromParcel. Should read [%d] apps.", toRead));

        for (int i = 0; i < toRead; i++) {
            try {
                String friendlyName = in.readString();
                if (null == friendlyName) {
                    break;
                }

                String packageName = in.readString();
                ////////////Log.v(TogglerService.TAG, String.format("ListAppStore::readFromParcel. Read package [%s]", packageName));

                AppStore app = new AppStore(friendlyName, packageName);
                app.setAppState(AppStore.AppState.values()[in.readInt()]);
                app.setActive(1 == in.readInt());

                add(app);
                counted++;
            } catch (Exception e) {
                Log.d(Constants.TAG, "ListAppStore::readFromParcel. Exception [2]: ", e);
                break;
            }
        }

        Log.d(Constants.TAG, String.format("ListAppStore::readFromParcel. Read from Parcel: [%d] apps.", counted));
    }


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        int counted = 0;

        dest.writeInt(size());

        for (AppStore app : this) {
            dest.writeString(app.friendlyName);
            dest.writeString(app.packageName);

            dest.writeInt(app.getAppState().ordinal());
            dest.writeInt(app.getActive() ? 1 : 0);
            counted++;
        }

        Log.d(Constants.TAG, String.format("ListAppStore::writeToParcel. Written to Parcel: [%d] apps.", counted));
    }
}
