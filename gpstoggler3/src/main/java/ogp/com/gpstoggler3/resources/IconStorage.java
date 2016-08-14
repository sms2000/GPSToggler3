package ogp.com.gpstoggler3.resources;

import android.content.Context;
import android.graphics.drawable.Drawable;

import ogp.com.gpstoggler3.R;

public class IconStorage {
    private static Drawable disabled;
    private static Drawable inactive;
    private static Drawable active;

    private static IconStorage  instance    = null;
    private static final Object lock        = new Object();

    public static IconStorage getInstance(Context context) {
        synchronized (lock) {
            if (null == instance && null != context) {
                instance = new IconStorage(context.getApplicationContext());
            }
        }

        return instance;
    }


    public static Drawable getDisabled() {
        return disabled;
    }


    public static Drawable getActive() {
        return active;
    }


    public static Drawable getInactive() {
        return inactive;
    }


    @SuppressWarnings("deprecation")
    private IconStorage(Context context) {
        disabled = context.getResources().getDrawable(R.drawable.disabled);
        inactive = context.getResources().getDrawable(R.drawable.inactive);
        active   = context.getResources().getDrawable(R.drawable.active);
    }
}
