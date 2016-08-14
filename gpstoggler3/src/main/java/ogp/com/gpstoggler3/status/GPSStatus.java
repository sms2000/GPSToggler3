package ogp.com.gpstoggler3.status;

import android.os.Parcel;
import android.os.Parcelable;


public class GPSStatus implements Parcelable {
    public Boolean gpsOn;
    public long gpsStatusTimestamp;


    public static final Parcelable.Creator<GPSStatus> CREATOR = new Parcelable.Creator<GPSStatus>() {
        public GPSStatus createFromParcel(Parcel in) {
            return new GPSStatus(in);
        }


        public GPSStatus[] newArray(int size) {
            return new GPSStatus[size];
        }
    };


    public GPSStatus() {
        super();
    }


    private GPSStatus(Parcel in) {
        super();

        readFromParcel(in);
    }


    @Override
    public int describeContents() {
        return 0;
    }


    private void readFromParcel(Parcel in) {
        try {
            int _gpsOn = in.readInt();
            if (-1 == _gpsOn) {
                gpsOn = null;
            } else {
                gpsOn = 1 == _gpsOn;
            }
        } catch (Exception e) {
            gpsOn = null;
            gpsStatusTimestamp = -1;
            return;
        }

        try {
            gpsStatusTimestamp = in.readLong();
        } catch (Exception e) {
            gpsStatusTimestamp = -1;
        }

    }


    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(gpsOn == null ? -1 : (gpsOn ? 1 : 0));
        dest.writeLong(gpsStatusTimestamp);
    }
}
