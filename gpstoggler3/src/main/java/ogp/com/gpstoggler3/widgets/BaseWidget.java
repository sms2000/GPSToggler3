package ogp.com.gpstoggler3.widgets;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import ogp.com.gpstoggler3.ITogglerService;
import ogp.com.gpstoggler3.TogglerService;
import ogp.com.gpstoggler3.broadcasters.Broadcasters;
import ogp.com.gpstoggler3.global.Constants;


public abstract class BaseWidget extends AppWidgetProvider {
    ITogglerService togglerBinder = null;
    private TogglerServiceConnection serviceConnection = new TogglerServiceConnection();
    private Context appContext = null;


    private class TogglerServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.v(Constants.TAG, "BaseWidget::TogglerServiceConnection::onServiceConnected. Entry...");

            togglerBinder = ITogglerService.Stub.asInterface(binder);

            int myPID = android.os.Process.myPid();
            int serverPID = 0;
            try {
                serverPID = togglerBinder.getPid();
            } catch (RemoteException e) {
                Log.e(Constants.TAG, "BaseWidget::onServiceConnected. Exception: ", e);
            }

            if (0 != serverPID) {
                Log.d(Constants.TAG, String.format("BaseWidget::onServiceConnected. App PID = %d, Server PID = %d.", myPID, serverPID));
            } else {
                Log.e(Constants.TAG, String.format("BaseWidget::onServiceConnected. Failed to connect the server. App PID = %d.", myPID));
            }

            if (null != appContext) {
                update(appContext);
            }

            Log.v(Constants.TAG, "BaseWidget::TogglerServiceConnection::onServiceConnected. Exit.");
        }


        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.v(Constants.TAG, "BaseWidget::TogglerServiceConnection::onServiceDisconnected. Entry...");
            togglerBinder = null;
            Log.v(Constants.TAG, "BaseWidget::TogglerServiceConnection::onServiceDisconnected. Exit.");
        }
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        appContext = context.getApplicationContext();

        String action = intent.getAction();
        if (null == action) {
            Log.i(Constants.TAG, "BaseWidget::onReceive. Zero action!");
            return;
        }

        Log.i(Constants.TAG, "BaseWidget::onReceive. Entry for action: " + action);

        switch (action) {
            case AppWidgetManager.ACTION_APPWIDGET_UPDATE:
                bindIfNot(context);
                break;


            case Broadcasters.GPS_STATE_CHANGED:
                boolean auto = intent.getBooleanExtra(Broadcasters.GPS_STATE_CHANGED_AUTO, false);
                boolean gpsStatus = intent.getBooleanExtra(Broadcasters.GPS_STATE_CHANGED, false);
                Log.v(Constants.TAG, String.format("BaseWidget::onReceive. GPS status changed (2nd stage). Widgets aware. Auto: [%s].", auto ? "ON" : "OFF"));

                bindIfNot(context);
                break;

            /*
            case Broadcasters.RETRIVE_WIDGETS:
                Log.v(Constants.TAG, "BaseWidget::onReceive. Widgets retrived.");
                bindIfNot(context);
                break;
            */
        }

        Log.v(Constants.TAG, "BaseWidget::onReceive. Exit.");
    }


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.v(Constants.TAG, "BaseWidget::onUpdate. Entry...");

        createWidgetView(context, appWidgetManager, appWidgetIds);

        super.onUpdate(context, appWidgetManager, appWidgetIds);

        Log.v(Constants.TAG, "BaseWidget::onUpdate. Exit.");
    }


    private void bindIfNot(Context context) {
        Log.v(Constants.TAG, "BaseWidget::bindIfNot. Entry...");

        if (null == togglerBinder) {
            if (TogglerService.startServiceAndBind(context.getApplicationContext(), serviceConnection)) {
                if (null != togglerBinder) {
                    Log.i(Constants.TAG, "BaseWidget::bindIfNot. Bind succeeded. Should be ready soon.");
                } else {
                    Log.i(Constants.TAG, "BaseWidget::bindIfNot. Bind succeeded. Should be ready soon.");
                }
            } else {
                Log.e(Constants.TAG, "BaseWidget::bindIfNot. Bind failed. This widget is useless.");
            }
        } else {
            update(context);
        }

        Log.v(Constants.TAG, "BaseWidget::bindIfNot. Exit.");
    }


    abstract void createWidgetView(final Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds);


    abstract void update(Context context);
}
