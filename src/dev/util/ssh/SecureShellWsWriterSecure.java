package dev.util.ssh;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Base64;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;

import javax.websocket.Session;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import dev.util.crypto.AESUtil;

public class SecureShellWsWriterSecure extends OutputStream {
    private Session ws;
    private SecretKeySpec key;
    private final Charset inCharset;
    private final Charset utf8Charset;

    private SecureRandom sr;
    private byte[] ivBytes = new byte[16];
    private Object mutex = new Object();

    private SecureShell ssh;
    private boolean closeFlag = false;

    public SecureShellWsWriterSecure(SecureShell target, Session session, SecretKeySpec secretKey ) { 
        ssh = target;
        ws = session;
        key = secretKey;
        inCharset = null;
        utf8Charset = null;

        try { sr = SecureRandom.getInstanceStrong(); }
        catch(NoSuchAlgorithmException nase) { sr = new SecureRandom(); }
    }

    public SecureShellWsWriterSecure(SecureShell target, Session session, SecretKeySpec secretKey, Charset inputCharset ) { 
        ssh = target;
        ws = session;
        key = secretKey;
        inCharset = inputCharset;

        Charset utf8 = null;
        try{ utf8 = Charset.forName("UTF-8"); } 
        catch(Exception e) { utf8 = null; } // Never Reach Here!!!
        utf8Charset = utf8;

        try { sr = SecureRandom.getInstanceStrong(); }
        catch(NoSuchAlgorithmException nase) { sr = new SecureRandom(); }
    }

    private synchronized boolean needClose() {
        if( !closeFlag ) {
            closeFlag = true;
            return true;
        }
        return false;
    }

    private IvParameterSpec getNextIv() {
        sr.nextBytes(ivBytes);
        return new IvParameterSpec(ivBytes);
    }

    @Override
    public void close() throws IOException {
        if( needClose() ) {
            super.close();

            // Close WebSocket Here
            synchronized(mutex) {
                if( ws.isOpen() ) {
                    try{ ws.close(); } catch(Exception ignore) {}
                }
            }

            // Close (Disconnect) Shell if needed
            ssh.close();
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        synchronized(mutex) {
            if( ws.isOpen() ) {
                if( inCharset != null ) {
                    String planeText = new String(b, off, len, inCharset);
                    b = planeText.getBytes(utf8Charset);
                    off = 0;
                    len = b.length;
                }

                IvParameterSpec iv = getNextIv();
                String encPartBase64 = AESUtil.encryptDataToBase64(key, iv, b, off, len);
                String ivPartBase64  = Base64.getEncoder().encodeToString(iv.getIV());

                ws.getBasicRemote().sendText(
                    String.format(
                        "{\"method\":\"data\",\"params\":{\"isSecured\":true,\"data\":{\"iv\":\"%s\",\"cipherData\":\"%s\",\"encoding\":\"base64\"}}}",
                        ivPartBase64,
                        encPartBase64
                    )
                );
            }
        }
    }

    @Override
    public void write(int b) throws IOException {
        // Dummy Method: Generally Never Called
        write( new byte[] { (byte)b }, 0, 1 );
    }
}