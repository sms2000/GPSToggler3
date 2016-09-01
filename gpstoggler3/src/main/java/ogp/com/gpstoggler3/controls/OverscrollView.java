package ogp.com.gpstoggler3.controls;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ListView;

import ogp.com.gpstoggler3.broadcasters.Broadcasters;
import ogp.com.gpstoggler3.global.Constants;


public class OverscrollView extends ListView {
    private static final long OVERSCROLL_TIMEOUT = 3000;

    private long lastOverscrollTime = 0;
    private Context context;


    public OverscrollView(Context context) {
        super(context);

        this.context = context;
    }


    public OverscrollView(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.context = context;
    }


    public OverscrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        this.context = context;
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public OverscrollView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        this.context = context;
    }


    @Override
    public void onOverScrolled (int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        Log.v(Constants.TAG, "OverscrollView::onOverScrolled. Entry...");

        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);

        if (clampedY) {
            long timeNow = System.currentTimeMillis();
            if (timeNow - lastOverscrollTime >= OVERSCROLL_TIMEOUT) {
                lastOverscrollTime = timeNow;

                Intent intent = new Intent(Broadcasters.ENUMERATE_INSTALLED_APPS);
                context.sendBroadcast(intent);

                Log.e(Constants.TAG, "OverscrollView::onOverScrolled. Event!");
            }

        }

        Log.v(Constants.TAG, "OverscrollView::onOverScrolled. Exit.");
    }
}
