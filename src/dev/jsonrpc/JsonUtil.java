package dev.jsonrpc;

import com.google.gson.*;

public class JsonUtil {
    public static Gson gson = new Gson();

    public static JsonElement toJsonTree(Object src) { return gson.toJsonTree(src); }
    public static String toJson(Object src) { return gson.toJson(src); }

    public static <T> T fromJson(String src, Class<T> type) throws JsonSyntaxException { 
        return gson.fromJson(src, type);
    }
    public static <T> T fromJson(JsonElement src, Class<T> type) throws JsonSyntaxException { 
        return gson.fromJson(src, type);
    }
}