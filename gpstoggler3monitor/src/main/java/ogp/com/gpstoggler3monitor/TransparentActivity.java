package ogp.com.gpstoggler3monitor;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;


public class TransparentActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceBundle) {
        Log.v(MonitorService.TAG, "TransparentActivity::onCreate. Entry...");
        super.onCreate(savedInstanceBundle);

        Intent intent = new Intent(this, MonitorService.class);
        startService(intent);

        Log.v(MonitorService.TAG, "TransparentActivity::onCreate. Exit.");
    }


    @Override
    public void onResume() {
        Log.v(MonitorService.TAG, "TransparentActivity::onResume. Entry...");
        super.onResume();

        new Handler().postDelayed(new Runnable() {
                                      @Override
                                      public void run() {
                                          Log.v(MonitorService.TAG, "TransparentActivity::onResume::run. Finishing...");
                                          TransparentActivity.this.finish();
                                      }
                                  },
                50);

        Log.v(MonitorService.TAG, "TransparentActivity::onResume. Exit.");
    }


    @Override
    public void onDestroy() {
        Log.v(MonitorService.TAG, "TransparentActivity::onDestroy. Entry...");
        super.onDestroy();

        Log.v(MonitorService.TAG, "TransparentActivity::onDestroy. Exit.");
    }
}
