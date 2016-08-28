package ogp.com.gpstoggler3.controls;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ListView;

import ogp.com.gpstoggler3.global.Constants;


public class OverscrollView extends ListView {
    public OverscrollView(Context context) {
        super(context);
    }


    public OverscrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    public OverscrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public OverscrollView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }


    @Override
    public void onOverScrolled (int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);

        Log.e(Constants.TAG, "OverscrollView::onOverScrolled. Event!");
    }
}
