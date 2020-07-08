package dev.util.ssh;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import javax.websocket.Session;

// Secure Shell WebSocket Writer
public class SecureShellWsWriter extends OutputStream {
    private Session ws;
    private Object mutex = new Object();
    private final Charset inCharset;

    private SecureShell ssh;
    private boolean closeFlag = false;

    public SecureShellWsWriter(SecureShell target, Session session) { 
        ssh = target;
        ws = session;
        // Default Input Charset is "UTF-8"
        Charset utf8 = null;
        try { utf8 = Charset.forName("UTF-8"); } 
        catch(Exception ignore) { utf8 = null; } // Never Reach Here!!!

        inCharset = utf8;
    }

    public SecureShellWsWriter(SecureShell target, Session session, Charset inputCharset) {
        ssh = target;
        ws = session;
        inCharset = inputCharset;
    }

    private synchronized boolean needClose() {
        if( !closeFlag ) {
            closeFlag = true;
            return true;
        }
        return false;
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

    @Override // Input Bytes Must be UTF-8 Encoded String
    public void write(byte[] b, int off, int len) throws IOException {
        synchronized(mutex) {
            if( ws.isOpen() ) {
                ws.getBasicRemote().sendText(
                    String.format(
                        "{\"method\":\"data\",\"params\":{\"data\":\"%s\"}}",
                        new String( b, off, len, inCharset )
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