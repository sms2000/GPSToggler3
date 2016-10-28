package ogp.com.gpstoggler3.results;

import java.util.ArrayList;
import java.util.List;

public class RPCResult {
    private Object result;
    private Exception error;

    public RPCResult(Exception e) {
        this.error = e;
        this.result = null;
    }


    public RPCResult(String string) {
        String[] split = string.split("\n");

        List<String> result = new ArrayList<String>();
        for (String line : split) {
            result.add(line);
        }

        this.result = result;
        error = null;
    }


    public RPCResult(Object result) {
        this.result = result;
        this.error = null;
    }


    public boolean isError() {
        return null != error;
    }

    public boolean isList() {
        return (null != error) && (result instanceof List);
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
}
