package dev.jsonrpc;

import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;

public class JsonRPCRequest {
    // Constant of Method Name
    public static final String METHOD_CONNECT = "connect";
    public static final String METHOD_ON_DATA = "data";
    public static final String METHOD_RESIZE  = "resize";
    public static final String METHOD_LOG     = "log";

    // Local Property
    @Expose private Integer     id = null;
            private String      method;
            private JsonElement params;

    public boolean     isNotification() { return id == null || id < 0; }
    public int         getId()          { return id == null ? -1 : id; }
    public String      getMethod()      { return method; }

    private JsonRPCRequest() {}
    private JsonRPCRequest(int id, String method, Object params) {
        this.id = id;
        this.method = method;
        this.params = JsonUtil.toJsonTree(params);
    }
    private JsonRPCRequest(String method, Object params) {
        this.id = null;
        this.method = method;
        this.params = JsonUtil.toJsonTree(params);
    }

    public <T> T getParamAsClass(Class<T> type) throws JsonSyntaxException { 
        return JsonUtil.fromJson( params, type ); 
    }
    
    public static JsonRPCRequest fromJSON(String jsonString) throws JsonSyntaxException {
        return JsonUtil.fromJson(jsonString, JsonRPCRequest.class);
    }

    public static JsonRPCRequest createRequest(int id, String method, Object params) {
        return new JsonRPCRequest(id, method, params);
    }
    
    public static JsonRPCRequest createNotify(String method, Object params) {
        return new JsonRPCRequest(method, params);
    }
}