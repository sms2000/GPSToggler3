package ogp.com.gpstoggler3.controls;

import android.content.Context;
import android.widget.ListView;


public class OverscrollView extends ListView {
    public OverscrollView(Context context) {
        super(context);
    }


    @Override
    public void onOverScrolled (int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);


    }
}
