package dev.jsonrpc.params;

import com.google.gson.annotations.SerializedName;
//import com.google.gson.annotations.Expose;

public class ParamLog {
    public static enum LogType {
        @SerializedName("info")  INFO,
        @SerializedName("warn")  WARN,
        @SerializedName("error") ERROR
    }

    public LogType type;
    public String  msg;

    public ParamLog( LogType type, String message ) {
        this.type = type;
        this.msg  = message;
    }
}

