package ogp.com.gpstoggler3.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

import ogp.com.gpstoggler3.R;
import ogp.com.gpstoggler3.broadcasters.Broadcasters;
import ogp.com.gpstoggler3.global.Constants;
import ogp.com.gpstoggler3.settings.Settings;


public class AppStartWidget extends BaseWidget {
    @Override
    void createWidgetView(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.v(Constants.TAG, "AppStartWidget::createWidgetView. Entry...");

        Context appContext = context.getApplicationContext();
        AppWidgetManager manager = AppWidgetManager.getInstance(appContext);

        for (int index : appWidgetIds) {
            RemoteViews updateViews = new RemoteViews(appContext.getPackageName(), R.layout.layout_app_icon);
            Drawable drawable = getResIdByStatus(index);
            Bitmap bitmap = Settings.drawableToBitmap(drawable);

            updateViews.setImageViewBitmap(R.id.appPic, bitmap);

            Intent intent = new Intent(Broadcasters.APP_PIC_CLICK);
            intent.putExtra(Broadcasters.APP_PIC_CLICK_EXTRA_INDEX, index);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(appContext, 0, intent, 0);

            updateViews.setOnClickPendingIntent(R.id.app_icon, pendingIntent);

            Log.w(Constants.TAG, "AppStartWidget::createWidgetView. setOnClickPendingIntent invoked.");
            manager.updateAppWidget(index, updateViews);
        }

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

        Log.w(Constants.TAG, "BaseWidget::onReceive. Entry for action: " + action);

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


    private Drawable getResIdByStatus(int index) {
        String packageName = Settings.getPackageForWidget(index);
        Drawable drawable = Settings.getPackageIcon(appContext, packageName);
        if (null != drawable) {
            return drawable;
        } else {
            return appContext.getResources().getDrawable(R.drawable.app_new_widget);
        }
    }
}
