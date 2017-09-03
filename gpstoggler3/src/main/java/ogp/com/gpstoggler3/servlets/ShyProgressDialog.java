package ogp.com.gpstoggler3.servlets;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;


public class ShyProgressDialog extends ProgressDialog {
    private long shown = 0;
    private long minimumDelay;
    private Handler handler = new Handler();


    public ShyProgressDialog(Context context, long minimumDelay) {
        super(context);

        this.minimumDelay = minimumDelay;
    }


    @Override
    public void show() {
        shown = System.currentTimeMillis();

        super.show();
    }


    @Override
    public void dismiss() {
        if (0 < shown) {
            long now = System.currentTimeMillis();
            if (now - shown < minimumDelay) {
                long delay = minimumDelay - (now - shown);

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ShyProgressDialog.super.dismiss();
                    }
                }, delay);

            } else {
                super.dismiss();
            }

            shown = 0;
        }
    }
}
