package ogp.com.gpstoggler3.servlets;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.lang.reflect.Method;

import ogp.com.gpstoggler3.ITogglerService;
import ogp.com.gpstoggler3.services.TogglerService;
import ogp.com.gpstoggler3.global.Constants;


public class ExecuteOnServiceWithTimeout extends ExecuteWithTimeout {
    private TogglerServiceConnection serviceConnection = new TogglerServiceConnection();
    private ITogglerService togglerBinder;
    private Context context;


    public static class ExecuteOnService extends ExecuteParams {
        private Method method;

        public ExecuteOnService(final Method method) {
            this.method = method;
        }
    }


    public ExecuteOnServiceWithTimeout(Context context) {
        super();

        this.context = context;
    }


    private class TogglerServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.v(Constants.TAG, "ExecuteOnServiceWithTimeout::TogglerServiceConnection::onServiceConnected. Entry...");
            togglerBinder = ITogglerService.Stub.asInterface(binder);

            Log.i(Constants.TAG, "ExecuteOnServiceWithTimeout. Bind succeeded.");

            synchronized (ExecuteOnServiceWithTimeout.this) {
                Log.d(Constants.TAG, "ExecuteOnServiceWithTimeout::transferWithResult::onServiceConnected. 'wait' interrupting...");

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


    @Override
    public Object executeWithResult(final ExecuteParams params) {
        Log.v(Constants.TAG, "ExecuteOnServiceWithTimeout::executeWithResult. Entry...");

        Object result = null;

        if (params instanceof ExecuteOnService) {
            Method method = ((ExecuteOnService)params).method;
            if (null != method) {
                boolean bindAgain = true;

                if (null != togglerBinder) {
                    try {
                        togglerBinder.getPid();
                        bindAgain = false;
                    } catch (RemoteException e) {
                        Log.w(Constants.TAG, "ExecuteOnServiceWithTimeout::executeWithResult. Need to rebind...");
                    }
                }

                if (bindAgain) {
                    post(new Runnable() {
                        @Override
                        public void run() {
                            Log.v(Constants.TAG, "ExecuteOnServiceWithTimeout::executeWithResult::run. Entry...");

                            TogglerService.startServiceAndBind(context, serviceConnection);

                            Log.v(Constants.TAG, "ExecuteOnServiceWithTimeout::executeWithResult::run. Exit.");
                        }
                    });

                    synchronized (this) {
                        try {
                            wait();
                            Log.i(Constants.TAG, "ExecuteOnServiceWithTimeout::executeWithResult. Rebind succeeded.");
                        } catch (InterruptedException ignored) {
                            Log.i(Constants.TAG, "ExecuteOnServiceWithTimeout::executeWithResult. 'wait' interrupted so the rebind is questionable.");
                        }
                    }
                }

                if (null != togglerBinder) {
                    try {
                        Log.i(Constants.TAG, String.format("ExecuteOnServiceWithTimeout::executeWithResult. Invoking [1] '%s'...", method.getName()));

                        result = method.invoke(togglerBinder);

                        if (null != result) {
                            Log.i(Constants.TAG, "ExecuteOnServiceWithTimeout::executeWithResult. Exchange succeeded [1].");
                        } else {
                            Log.e(Constants.TAG, "ExecuteOnServiceWithTimeout::executeWithResult. Exchange failed with 'null' result [1].");
                        }
                    } catch (Exception e) {
                        Log.e(Constants.TAG, "ExecuteOnServiceWithTimeout::executeWithResult. Exchange error! [1]. Reconnecting...");
                    }
                } else {
                    Log.e(Constants.TAG, "ExecuteOnServiceWithTimeout::executeWithResult. Error. Bind failed!");
                }
            } else {
                Log.e(Constants.TAG, "ExecuteOnServiceWithTimeout::executeWithResult. Bad method specified.");
            }
        } else {
            Log.e(Constants.TAG, "ExecuteOnServiceWithTimeout::executeWithResult. 'params' must be of 'ExceuteOnService' and not of " + params.getClass().getCanonicalName());
        }

        Log.v(Constants.TAG, "ExecuteOnServiceWithTimeout::executeWithResult. Exit [2].");
        return result;
    }
}
