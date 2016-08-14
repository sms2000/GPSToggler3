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

import ogp.com.gpstoggler3.debug.Constants;
import ogp.com.gpstoggler3.R;
import ogp.com.gpstoggler3.broadcasters.Broadcasters;


public class GPSIconWidget extends BaseWidget {
    @Override
    protected void createWidgetView(final Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.v(Constants.TAG, "GPSIconWidget::createWidgetView. Entry...");

        Context appContext = context.getApplicationContext();
        RemoteViews updateViews = new RemoteViews(appContext.getPackageName(), R.layout.layout_widget_icon);
        Drawable drawable = ContextCompat.getDrawable(appContext, getResIdByStatus());
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();

        updateViews.setImageViewBitmap(R.id.gpsPic, bitmap);

        Intent intent = new Intent(Broadcasters.GPS_PIC_CLICK);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(appContext, 0, intent, 0);

        updateViews.setOnClickPendingIntent(R.id.widget_icon, pendingIntent);

        Log.i(Constants.TAG, "GPSIconWidget::createWidgetView. setOnClickPendingIntent invoked.");

        ComponentName thisWidget = new ComponentName(context, GPSIconWidget.class);

        AppWidgetManager manager = AppWidgetManager.getInstance(appContext);
        manager.updateAppWidget(thisWidget, updateViews);

        Log.v(Constants.TAG, "GPSIconWidget::createWidgetView. Exit.");
    }


    protected void update(Context context) {
        Log.v(Constants.TAG, "GPSIconWidget::update. Entry...");

        Context appContext = context.getApplicationContext();

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(appContext);
        ComponentName thisAppWidget = new ComponentName(appContext.getPackageName(), GPSIconWidget.class.getName());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

        onUpdate(context, appWidgetManager, appWidgetIds);

        Log.v(Constants.TAG, "GPSIconWidget::update. Exit.");
    }


    private int getResIdByStatus() {
        try {
            boolean gpsStatus = togglerBinder.onGps().gpsOn;
            return gpsStatus ? R.drawable.active : R.drawable.inactive;
        } catch (RemoteException | NullPointerException e) {
            Log.w(Constants.TAG, "GPSIconWidget::createWidgetView. Not yet bound to the main service.");
            return R.drawable.disabled;
        }
    }
}
