package dev.util.crypto;

import java.security.KeyPair;

import java.util.Map;
import java.util.HashMap;

import dev.util.crypto.RSAUtil.KEY_SIZE;

import com.google.gson.annotations.*;

public class RSAKeyManager
{
    private final static long NO_EXPIRE = Long.MAX_VALUE;
    private final static long ONE_HOUR = 1000 * 60 * 60;
    private final static long ONE_DAY  = ONE_HOUR * 24;

    public class RSAKeyInfo {
        @Expose(serialize = false) public KeyPair keyPair;
        @SerializedName("key")     public String  pubKeyPEM;
        @Expose                    public String  signature = null;
        @Expose @SerializedName("expire")  public Long expireDate = null;

        private RSAKeyInfo( KeyPair kp ) {
            keyPair    = kp;
            pubKeyPEM  = RSAUtil.publicKeyToPEMString(kp.getPublic());
        }

        public RSAKeyInfo( KeyPair kp, long expireDurationMillis ) {
            keyPair    = kp;
            pubKeyPEM  = RSAUtil.publicKeyToPEMString(kp.getPublic());
            signature  = RSAUtil.generateSignatureFromPublicKey(keyPair);
            expireDate = expireDurationMillis < 0 ? NO_EXPIRE : (System.currentTimeMillis() + expireDurationMillis);
        }
    }

    private Map<String, RSAKeyInfo> mapKeyInfo;
    private RSAKeyInfo defaultKeyInfo;
    private Object mutex = new Object();

    private RSAKeyManager() {
        mapKeyInfo = new HashMap<String, RSAKeyInfo>();
        defaultKeyInfo = new RSAKeyInfo( RSAUtil.generateKeyPair(KEY_SIZE.KS_2048) );
    }

    private static RSAKeyManager instance = null;
    public static synchronized RSAKeyManager getInstance() {
        if(instance == null) { instance = new RSAKeyManager(); }
        return instance;
    }

    private RSAKeyInfo _syncGet(String signature) {
        synchronized(mutex) {
            RSAKeyInfo info = mapKeyInfo.get(signature);
            if( info != null && info.expireDate < System.currentTimeMillis() ) {
                mapKeyInfo.remove(signature);
                info = null;
            }
            return info;
        }
    }

    public RSAKeyInfo generateNew() {
        return generateNew(ONE_DAY);
    }

    public RSAKeyInfo generateNew(long expireDurationMillis) {
        final long MIN_DURATION = ONE_HOUR;
        long duration = expireDurationMillis < MIN_DURATION ? MIN_DURATION : expireDurationMillis;

        RSAKeyInfo keyInfo = new RSAKeyInfo( RSAUtil.generateKeyPair(KEY_SIZE.KS_2048), duration );
        synchronized(mutex) { mapKeyInfo.put( keyInfo.signature, keyInfo ); }
        return keyInfo;
    }

    public RSAKeyInfo getDefaultKeyInfo() {
        return defaultKeyInfo;
    }

    public RSAKeyInfo getKeyInfoWithSignature(String signature) {
        return _syncGet(signature);
    }

    public void removeKey(String signature) {
        synchronized(mutex) { mapKeyInfo.remove(signature); }
    }

    public void removeExpiredKey() { 
        long curr = System.currentTimeMillis();
        synchronized(mutex) {
            mapKeyInfo.values().removeIf( info -> info.expireDate < curr );
        }
    }
}