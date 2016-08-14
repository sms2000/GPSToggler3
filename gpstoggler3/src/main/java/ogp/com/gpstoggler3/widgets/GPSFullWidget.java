package ogp.com.gpstoggler3.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.RemoteException;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.RemoteViews;

import java.text.DateFormat;

import ogp.com.gpstoggler3.debug.Constants;
import ogp.com.gpstoggler3.R;
import ogp.com.gpstoggler3.broadcasters.Broadcasters;
import ogp.com.gpstoggler3.status.GPSStatus;


public class GPSFullWidget extends BaseWidget {
    protected void createWidgetView(final Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.v(Constants.TAG, "GPSFullWidget::createWidgetView. Entry...");

        String timestampChange = context.getResources().getString(R.string.no_timestamp);
        GPSStatus gpsStatus = new GPSStatus();
        try {
            gpsStatus = togglerBinder.onGps();
            if (null != gpsStatus.gpsOn && -1 != gpsStatus.gpsStatusTimestamp) {
                timestampChange = DateFormat.getDateTimeInstance().format(gpsStatus.gpsStatusTimestamp);
            }
        } catch (RemoteException | NullPointerException e) {
            Log.w(Constants.TAG, "GPSFullWidget::createWidgetView. Not yet bound to the main service.");
        }

        Context appContext = context.getApplicationContext();
        RemoteViews updateViews = new RemoteViews(appContext.getPackageName(), R.layout.layout_widget_full);

        // Icon
        Drawable drawable = ContextCompat.getDrawable(appContext, getResIdByStatus(gpsStatus.gpsOn));
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        updateViews.setImageViewBitmap(R.id.gpsPic, bitmap);

        // Icon click
        Intent intent = new Intent(Broadcasters.GPS_PIC_CLICK);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(appContext, 0, intent, 0);
        updateViews.setOnClickPendingIntent(R.id.gpsPic, pendingIntent);

        // List of apps
        Intent listIntent = new Intent(context, WidgetsService.class);
        listIntent.setData(Uri.parse(listIntent.toUri(Intent.URI_INTENT_SCHEME)));
        updateViews.setRemoteAdapter(R.id.listApps, listIntent);
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.listApps);

        // Text
        updateViews.setTextViewText(R.id.gpsStatus, timestampChange);

        Log.w(Constants.TAG, "GPSFullWidget::createWidgetView. setOnClickPendingIntent invoked.");

        ComponentName thisWidget = new ComponentName(context, GPSFullWidget.class);

        AppWidgetManager manager = AppWidgetManager.getInstance(appContext);
        manager.updateAppWidget(thisWidget, updateViews);

        Log.v(Constants.TAG, "GPSFullWidget::createWidgetView. Exit.");
    }


    protected void update(Context context) {
        Log.v(Constants.TAG, "GPSFullWidget::update. Entry...");

        Context appContext = context.getApplicationContext();

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(appContext);
        ComponentName thisAppWidget = new ComponentName(appContext.getPackageName(), GPSFullWidget.class.getName());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

        onUpdate(context, appWidgetManager, appWidgetIds);

        Log.v(Constants.TAG, "GPSFullWidget::update. Exit.");
    }


    private int getResIdByStatus(Boolean gpsOn) {
        if (null == gpsOn) {
            return R.drawable.disabled;
        } else {
            return gpsOn ? R.drawable.active : R.drawable.inactive;
        }
    }
}
