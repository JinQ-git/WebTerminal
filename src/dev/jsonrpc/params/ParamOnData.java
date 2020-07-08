package dev.jsonrpc.params;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Base64.Decoder;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;

import dev.util.crypto.AESUtil;
import dev.jsonrpc.JsonUtil;
import dev.util.HexUtil;

public class ParamOnData {
    public static class CipherData {
        @Expose public CipherEncoding encoding = CipherEncoding.BASE64; // encoding method of both iv and cipherData
        public String iv; // Initial Vector
        public String cipherData;

        public CipherData(String iv, String cipherData) {
            this.iv = iv;
            this.cipherData = cipherData;
        }

        public CipherData(String iv, String cipherData, CipherEncoding encoding) {
            this.iv = iv;
            this.cipherData = cipherData;
            this.encoding = encoding;
        }
    }

    @Expose public boolean isSecured = false;
    public JsonElement data; // if `isSecured` is true, data is typeof CipherData, else data is utf-8 encoded String
    
    public ParamOnData(String message) {
        data = JsonUtil.toJsonTree(message);
    }

    public ParamOnData(CipherData data) {
        this.isSecured = true;
        this.data = JsonUtil.toJsonTree(data);
    }
    
    public byte[] getBytes() throws NullPointerException {
        if( !isSecured ) {
            try { return data.getAsString().getBytes("UTF-8"); }
            catch(UnsupportedEncodingException ignore) {}
        }
        throw new NullPointerException("Secret Key is not provided");
    }

    public byte[] getBytes(SecretKeySpec key) throws JsonSyntaxException, NullPointerException
    {
        if( !isSecured ) {
            try { return data.getAsString().getBytes("UTF-8"); }
            catch(UnsupportedEncodingException ignore) {}
        }

        if( key == null ) {
            throw new NullPointerException("Secret Key is not provided");
        }

        CipherData c = JsonUtil.fromJson(data, CipherData.class);

        IvParameterSpec iv = null;
        byte[] cipherBytes = null;

        switch( c.encoding ) {
            case BASE64:
            {
                Decoder d = Base64.getDecoder();

                cipherBytes = d.decode( c.cipherData );
                iv = new IvParameterSpec( d.decode( c.iv ) );
            }
            break;

            case HEX:
            {
                iv = new IvParameterSpec( HexUtil.toByteArray( c.iv ) );
                cipherBytes = HexUtil.toByteArray( c.cipherData );
            }
            break;

            case UTF8STR:
            {
                Charset UTF8 = null;
                try{ UTF8 = Charset.forName("UTF-8"); } catch(Exception ignore) {}

                iv = new IvParameterSpec( c.iv.getBytes(UTF8) );
                cipherBytes = c.cipherData.getBytes(UTF8);
            }
            break;
        }
        
        if( iv == null ) {
            throw new JsonSyntaxException("Invlaid Initial Vector (iv)");
        }

        byte[] decryptData = AESUtil.decryptData( key, iv, cipherBytes );
        if( decryptData == null ) {
            throw new JsonSyntaxException("Invalid cipherData");
        }

        return decryptData;
    }
}