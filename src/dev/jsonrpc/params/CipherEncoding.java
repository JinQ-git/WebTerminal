package dev.jsonrpc.params;

import com.google.gson.annotations.SerializedName;

public enum CipherEncoding {
    @SerializedName("base64") BASE64,
    @SerializedName("hex")    HEX,
    @SerializedName("utf8")   UTF8STR
}