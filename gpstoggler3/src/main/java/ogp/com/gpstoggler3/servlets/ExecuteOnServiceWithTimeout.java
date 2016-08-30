package ogp.com.gpstoggler3.servlets;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import ogp.com.gpstoggler3.ITogglerService;
import ogp.com.gpstoggler3.services.TogglerService;
import ogp.com.gpstoggler3.global.Constants;


public class ExecuteOnServiceWithTimeout extends WorkerThread {
    private TogglerServiceConnection serviceConnection = new TogglerServiceConnection();
    private ITogglerService togglerBinder;


    public ExecuteOnServiceWithTimeout(Context context) {
        super(context);
    }


    private class TogglerServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.v(Constants.TAG, "ExecuteOnServiceWithTimeout::TogglerServiceConnection::onServiceConnected. Entry...");
            togglerBinder = ITogglerService.Stub.asInterface(binder);

            Log.i(Constants.TAG, "ExecuteOnServiceWithTimeout. Bind succeeded.");

            synchronized (ExecuteOnServiceWithTimeout.this) {
                ExecuteOnServiceWithTimeout.this.notify();
            }

            Log.v(Constants.TAG, "ExecuteOnServiceWithTimeout::TogglerServiceConnection::onServiceConnected. Exit.");
        }


        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.v(Constants.TAG, "ExecuteOnServiceWithTimeout::TogglerServiceConnection::onServiceDisconnected. Entry...");
            togglerBinder = null;
            Log.v(Constants.TAG, "ExecuteOnServiceWithTimeout::TogglerServiceConnection::onServiceDisconnected. Exit.");
        }
    }


    public Object execute(final String rpcMethod, int rpcTimeout) {
        Log.v(Constants.TAG, "ExecuteOnServiceWithTimeout::execute. Entry...");

        Callable<Object> rpcTask = new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                Log.d(Constants.TAG, "ExecuteOnServiceWithTimeout::execute. Waiting more...");

                Log.v(Constants.TAG, "ExecuteOnServiceWithTimeout::execute::call. Entry/Exit.");
                return transferWithResult(rpcMethod);
            }
        };

        Object result = null;
        Future<Object> future = Executors.newFixedThreadPool(1).submit(rpcTask);
        try {
            Log.d(Constants.TAG, "ExecuteOnServiceWithTimeout::execute. Waiting...");

            result = future.get(rpcTimeout, TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {
        }

        Log.v(Constants.TAG, "ExecuteOnServiceWithTimeout::execute. Exit.");
        return result;
    }


    private Object transferWithResult(final String rpcMethod) {
        Log.v(Constants.TAG, "ExecuteOnServiceWithTimeout::transferWithResult. Entry...");

        final Object[] result = {null};

        if (null != togglerBinder) {
            try {
                Log.i(Constants.TAG, String.format("ExecuteOnServiceWithTimeout::transferWithResult. Invoking [1] '%s'...", rpcMethod));

                Method method = togglerBinder.getClass().getMethod(rpcMethod);
                result[0] = method.invoke(togglerBinder);
                Log.i(Constants.TAG, "ExecuteOnServiceWithTimeout::transferWithResult. Exchange succeeded [1].");

                Log.v(Constants.TAG, "ExecuteOnServiceWithTimeout::transferWithResult. Exit [1].");
                return result[0];
            } catch (Exception e) {
                Log.e(Constants.TAG, "ExecuteOnServiceWithTimeout::transferWithResult. Exchange error! [1]. Reconnecting...");
            }
        } else {
            Log.d(Constants.TAG, "ExecuteOnServiceWithTimeout::transferWithResult. Needs to connect to the main service...");
        }

        post(new Runnable() {
            @Override
            public void run() {
                Log.v(Constants.TAG, "ExecuteOnServiceWithTimeout::transferWithResult::run. Entry...");

                TogglerService.startServiceAndBind(context, serviceConnection);

                Log.v(Constants.TAG, "ExecuteOnServiceWithTimeout::transferWithResult::run. Exit.");
            }
        });


        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException ignored) {
            }
        }

        if (null == togglerBinder) {
            Log.e(Constants.TAG, "ExecuteOnServiceWithTimeout::transferWithResult. Error. Bind failed!");
        } else {
            Log.i(Constants.TAG, "ExecuteOnServiceWithTimeout::transferWithResult. Bind confirmed.");

            try {
                Log.i(Constants.TAG, String.format("ExecuteOnServiceWithTimeout::transferWithResult. Invoking [2] '%s'...", rpcMethod));

                Method method = togglerBinder.getClass().getMethod(rpcMethod);
                result[0] = method.invoke(togglerBinder);
                Log.i(Constants.TAG, "ExecuteOnServiceWithTimeout::transferWithResult. Exchange succeeded [2].");
            } catch (Exception e) {
                Log.e(Constants.TAG, "ExecuteOnServiceWithTimeout. Exchange error! [2]");
            }

            Log.v(Constants.TAG, "ExecuteOnServiceWithTimeout::transferWithResult. Exit.");

            if (null != result[0]) {
                Log.w(Constants.TAG, "ExecuteOnServiceWithTimeout::transferWithResult. OK: " + result[0].toString());
            } else {
                Log.e(Constants.TAG, "ExecuteOnServiceWithTimeout::transferWithResult. Problem! result == null.");
            }
        }


        Log.v(Constants.TAG, "ExecuteOnServiceWithTimeout::transferWithResult. Exit [2].");
        return result[0];
    }
}
