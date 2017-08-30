package ogp.com.gpstoggler3.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.RemoteViews;

import ogp.com.gpstoggler3.R;
import ogp.com.gpstoggler3.broadcasters.Broadcasters;
import ogp.com.gpstoggler3.global.Constants;


public class AppStartWidget extends BaseWidget {
    @Override
    void createWidgetView(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.v(Constants.TAG, "AppStartWidget::createWidgetView. Entry...");

        Context appContext = context.getApplicationContext();
        RemoteViews updateViews = new RemoteViews(appContext.getPackageName(), R.layout.layout_app_icon);
        Drawable drawable = ContextCompat.getDrawable(appContext, getResIdByStatus());
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();

        updateViews.setImageViewBitmap(R.id.appPic, bitmap);

        Intent intent = new Intent(Broadcasters.GPS_PIC_CLICK);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(appContext, 0, intent, 0);

        updateViews.setOnClickPendingIntent(R.id.app_icon, pendingIntent);

        Log.d(Constants.TAG, "AppStartWidget::createWidgetView. setOnClickPendingIntent invoked.");

        ComponentName thisWidget = new ComponentName(context, AppStartWidget.class);

        AppWidgetManager manager = AppWidgetManager.getInstance(appContext);
        manager.updateAppWidget(thisWidget, updateViews);

        Log.v(Constants.TAG, "AppStartWidget::createWidgetView. Exit.");
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(Constants.TAG, "AppStartWidget::onReceive. Entry...");

        appContext = context.getApplicationContext();

        String action = intent.getAction();
        if (null == action) {
            Log.i(Constants.TAG, "AppStartWidget::onReceive. Zero action!");
            return;
        }

        Log.i(Constants.TAG, "BaseWidget::onReceive. Entry for action: " + action);

        switch (action) {
            case Broadcasters.APP_SELECTED:
                boolean auto = intent.getBooleanExtra(Broadcasters.GPS_STATE_CHANGED_AUTO, false);
                Log.v(Constants.TAG, String.format("BaseWidget::onReceive. GPS status changed (2nd stage). Widgets aware. Auto: [%s].", auto ? "ON" : "OFF"));

                bindIfNot(context);
                break;

            default:
                super.onReceive(context, intent);
        }


        Log.v(Constants.TAG, "AppStartWidget::onReceive. Exit.");
    }


    @Override
    void update(Context context) {
        Log.v(Constants.TAG, "AppStartWidget::update. Entry...");

        Context appContext = context.getApplicationContext();

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(appContext);
        ComponentName thisAppWidget = new ComponentName(appContext.getPackageName(), AppStartWidget.class.getName());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

        onUpdate(context, appWidgetManager, appWidgetIds);

        Log.v(Constants.TAG, "AppStartWidget::update. Exit.");
    }


    private int getResIdByStatus() {
        try {
            return R.drawable.app_select;
        } catch (Exception e) {
            Log.w(Constants.TAG, "AppStartWidget::createWidgetView. Not yet bound to the main service.");
            return R.drawable.app_select;
        }
    }
}
