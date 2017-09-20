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
import ogp.com.gpstoggler3.results.RPCResult;


public class ExecuteWithTimeout extends WorkerThread {
    private static final int ATTEMPTS = 3;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static abstract class ExecuteParams {
    }

    private static class ExecuteMethod extends ExecuteParams {
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

    public RPCResult execute(final ExecuteParams params, int rpcTimeout) {
        Log.v(Constants.TAG, "ExecuteWithTimeout::execute. Entry...");

        Callable<RPCResult> rpcTask = new Callable<RPCResult>() {
            @Override
            public RPCResult call() throws Exception {
                Log.d(Constants.TAG, "ExecuteWithTimeout::execute::call. Entry...");

                RPCResult returned = executeWithResult(params);
                if (!returned.isError()) {
                    Log.d(Constants.TAG, "ExecuteWithTimeout::execute::call. Succeeded executing [" + params.getClass() + "]. Returned: " + returned.toString());
                } else {
                    Log.e(Constants.TAG, "ExecuteWithTimeout::execute::call. Exception when executing [" + params.getClass() + "].");
                }

                Log.v(Constants.TAG, "ExecuteWithTimeout::execute::call. Exit.");
                return returned;
            }
        };

        RPCResult result = null;
        Exception exception = null;

        for (int i = 0; i < ATTEMPTS; i++) {
            Future<RPCResult> future = executorService.submit(rpcTask);

            try {
                Log.i(Constants.TAG, "ExecuteWithTimeout::execute. Waiting for 'get' with timeout...");
                result = future.get(rpcTimeout, TimeUnit.MILLISECONDS);
                Log.d(Constants.TAG, "ExecuteWithTimeout::execute. Success.");
            } catch (TimeoutException e) {
                exception = new TimeoutException("Timeout reached");
                Log.w(Constants.TAG, "ExecuteWithTimeout::execute. TimeoutException accounted!");
            } catch (InterruptedException e) {
                Log.d(Constants.TAG, "ExecuteWithTimeout::execute. InterruptedException accounted and safely ignored.");
                continue;
            } catch (ExecutionException e) {
                exception = e;
                Log.e(Constants.TAG, "ExecuteWithTimeout::execute. ExecutionException accounted!", e);
            }

            break;
        }

        if (null != exception) {
            executorService.shutdownNow();
            executorService = Executors.newSingleThreadExecutor();

            result = new RPCResult(exception);
            executedError(exception);
        }

        Log.v(Constants.TAG, "ExecuteWithTimeout::execute. Exit.");
        return result;
    }


    public RPCResult executeWithResult(final ExecuteParams params) throws InvocationTargetException {
        Log.v(Constants.TAG, "ExecuteWithTimeout::executeWithResult. Entry...");

        RPCResult result;

        if (params instanceof ExecuteMethod) {
            ExecuteMethod executedMethod = (ExecuteMethod)params;
            try {
                Object output = executedMethod.method.invoke(executedMethod.instance, executedMethod.arguments);
                result = new RPCResult(output);
                Log.d(Constants.TAG, "ExecuteWithTimeout::executeWithResult. Succeeded. Result: " + result.toString());
            } catch (IllegalAccessException e) {
                Log.e(Constants.TAG, "ExecuteWithTimeout::executeWithResult. Exception: ", e);
                result = new RPCResult(e);
            }
        } else {
            Exception e = new InvalidParameterException();
            result = new RPCResult(e);
        }

        if (result.isError()) {
            executedError(result.getError());
        }

        Log.v(Constants.TAG, "ExecuteWithTimeout::executeWithResult. Exit.");
        return result;
    }


    public void executedError(Exception e) {
        Log.v(Constants.TAG, "ExecuteWithTimeout::executedError. Placeholder.");
    }
}
