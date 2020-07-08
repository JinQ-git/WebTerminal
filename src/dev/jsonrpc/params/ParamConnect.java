package dev.jsonrpc.params;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.PrivateKey;
import java.util.Base64;

import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;

import dev.jsonrpc.JsonUtil;
import dev.util.HexUtil;
import dev.util.crypto.RSAKeyManager;
import dev.util.crypto.RSAUtil;

public class ParamConnect { // ReadOnly
    public static class ConnectionInfo {
        @Expose public String host = null;
        public String id;
        public String pw;
        @Expose public String key; // secretKey of current user (secretKey is created on client side)
                                   // This field should not be null when parsed from [CipherInfo]
        @Expose public CipherEncoding encoding = CipherEncoding.BASE64; // encoding mehtod of `key`

        public byte[] getKeyBytes() throws UnsupportedEncodingException {
            if( key != null ) {
                switch(encoding) {
                    case BASE64:
                        return Base64.getDecoder().decode(key);
                    case HEX:
                        return HexUtil.toByteArray(key);
                    case UTF8STR:
                        return key.getBytes("UTF-8");
                    default: // Never Reach Here!!
                        throw new UnsupportedEncodingException( "Invalid encoding");
                }
            }
            return null;
        }
    }

    public static class CipherInfo {
        @Expose public String signature; // RSA PublicKey Signature that Used to Encrypt ParamConnect
        @Expose public CipherEncoding encoding = CipherEncoding.BASE64; // encoding method of `cipherData`
        public String cipherData; // encrypted `UserInfo` Data with RSA PublicKey
    }

    public static class TerminalOptions {
        @Expose public ParamResize resize = null;
    }

    @Expose public boolean isSecured = false; // If true, `data` is typeof `UserInfo`, else `data` is typeof `CipherInfo`
            public JsonElement data;
    @Expose public TerminalOptions options = null;
    
    public ConnectionInfo getConnectionInfoData() throws JsonSyntaxException, UnsupportedEncodingException
    {
        if ( !isSecured ) {
            return JsonUtil.fromJson(data, ConnectionInfo.class);
        }

        Charset UTF8 = null;
        try { UTF8 = Charset.forName("UTF-8"); } catch(Exception ignore) {}


        CipherInfo c = JsonUtil.fromJson(data, CipherInfo.class);

        // Get Private Key
        RSAKeyManager.RSAKeyInfo keyInfo = null;
        if( c.signature != null && !c.signature.isEmpty() ) {
            keyInfo = RSAKeyManager.getInstance().getKeyInfoWithSignature( c.signature );
            if( keyInfo == null ) {
                throw new JsonSyntaxException("Invalid signature");
            }
        }
        else {
            keyInfo = RSAKeyManager.getInstance().getDefaultKeyInfo();
        }

        PrivateKey privKey = keyInfo.keyPair.getPrivate();
        byte[] planeBytes = null;
        switch(c.encoding) {
            case BASE64: 
                planeBytes = RSAUtil.decryptDataFromBase64(privKey, c.cipherData);
                break;

            case HEX:
                planeBytes = RSAUtil.decryptDataFromHex(privKey, c.cipherData);
                break;

            case UTF8STR:
                planeBytes = RSAUtil.decryptData(privKey, c.cipherData.getBytes(UTF8));
                break;
            default: // Never Reach Here!!
                throw new UnsupportedEncodingException( "Invalid encoding");
        }

        if( planeBytes == null ) {
            throw new JsonSyntaxException("Invalid cipherData");
        }

        // NOTE!: planeBytes is UTF-8 encoded Json String of `ConnectionInfo`
        return JsonUtil.fromJson(new String(planeBytes, UTF8), ConnectionInfo.class);
    }

    public TerminalOptions getTerminalOptions() {
        return options;
    }
}