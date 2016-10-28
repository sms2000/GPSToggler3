package ogp.com.gpstoggler3.results;


import java.util.List;

public class RPCResult {
    public List<String> result;
    public Exception    error;

    public RPCResult(List<String> result) {
        this.result = result;
        this.error = null;
    }


    public RPCResult(Exception e) {
        this.error = e;
        this.result = null;
    }


    public boolean isError() {
        return null != error;
    }
}
