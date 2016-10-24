package ogp.com.gpstoggler3.servlets;

import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.InvalidParameterException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import ogp.com.gpstoggler3.global.Constants;


public class ExecuteWithTimeout extends WorkerThread {
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static abstract class ExecuteParams {
    }

    public static class ExecuteMethod extends ExecuteParams {
        private Object instance;
        private Method method;
        private Object[] arguments;

        public ExecuteMethod(final Object instance, final Method method, final Object[] arguments) {
            this.instance = instance;
            this.method = method;
            this.arguments = arguments;
        }
    }


    public ExecuteWithTimeout() {
        super();
    }

    public Object execute(final ExecuteParams params, int rpcTimeout) {
        Log.v(Constants.TAG, "ExecuteWithTimeout::execute. Entry...");

        Callable<Object> rpcTask = new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                Log.d(Constants.TAG, "ExecuteWithTimeout::execute::call. Entry...");

                Object returned = executeWithResult(params);
                Log.i(Constants.TAG, "ExecuteWithTimeout::execute::call. Succeeded executing [" + params.getClass() + "]. Returned: " + returned.toString());

                Log.v(Constants.TAG, "ExecuteWithTimeout::execute::call. Exit.");
                return returned;
            }
        };

        Object result = null;
        Future<Object> future = executorService.submit(rpcTask);

        Exception exception = null;

        try {
            Log.i(Constants.TAG, "ExecuteWithTimeout::execute. Waiting for 'get' with timeout...");

            result = future.get(rpcTimeout, TimeUnit.MILLISECONDS);
            Log.d(Constants.TAG, "ExecuteWithTimeout::execute. Success.");
        } catch (TimeoutException e) {
            exception = e;
            Log.w(Constants.TAG, "ExecuteWithTimeout::execute. TimeoutException accounted!");
        } catch (InterruptedException e) {
            exception = e;
            Log.e(Constants.TAG, "ExecuteWithTimeout::execute. InterruptedException accounted!");
        } catch (ExecutionException e) {
            exception = e;
            Log.e(Constants.TAG, "ExecuteWithTimeout::execute. ExecutionException accounted!", e);
        }

        if (null != exception) {
            result = exception;
            executedError(exception);
        }

        Log.v(Constants.TAG, "ExecuteWithTimeout::execute. Exit.");
        return result;
    }


    public Object executeWithResult(final ExecuteParams params) throws InvocationTargetException {
        Log.v(Constants.TAG, "ExecuteWithTimeout::executeWithResult. Entry...");

        Object result = null;

        if (params instanceof ExecuteMethod) {
            ExecuteMethod executedMethod = (ExecuteMethod)params;
            try {
                result = executedMethod.method.invoke(executedMethod.instance, executedMethod.arguments);
                Log.d(Constants.TAG, "ExecuteWithTimeout::executeWithResult. Succeeded. Result: " + result.toString());
            } catch (IllegalAccessException e) {
                Log.e(Constants.TAG, "ExecuteWithTimeout::executeWithResult. Exception: ", e);
                executedError(e);
            }
        } else {
            executedError(new InvalidParameterException());
            result = null;
        }

        Log.v(Constants.TAG, "ExecuteWithTimeout::executeWithResult. Exit.");
        return result;
    }


    public void executedError(Exception e) {
        Log.v(Constants.TAG, "ExecuteWithTimeout::executedError. Placeholder.");
    }
}
