package ogp.com.gpstoggler3.results;

import java.util.List;
import java.util.Locale;


public class RPCResult {
    private Object result;
    private Exception error;


    public RPCResult(Exception e) {
        this.error = e;
        this.result = null;
    }


    public RPCResult(Object result) {
        this.result = result;
        this.error = null;
    }


    public boolean isError() {
        return null != error;
    }

    public boolean isList() {
        return (null == error) && (result instanceof List);
    }

    public Exception getError() {
        return error;
    }


    public int size() {
        if (isList()) {
            List list = (List)result;
            return list.size();
        } else {
            return -1;
        }
    }


    public Object get(int i) {
        try {
            return ((List)result).get(i);
        } catch (Exception ignored) {
            return null;
        }
    }


    public List getList() {
        try {
            return (List) result;
        } catch (Exception ignored) {
            return null;
        }
    }


    @Override
    public String toString() {
        return null == error ? String.format(Locale.ENGLISH,  "(OK) %s", result.toString()) : String.format(Locale.ENGLISH, "(EXC) %s", error.getLocalizedMessage());
    }
}
