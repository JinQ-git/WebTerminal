package dev.util.ssh;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

import com.jcraft.jsch.*;

public class SecureShell
{
    // JSch Context, Session and Channel
    private JSch jsCtx = null;
    private com.jcraft.jsch.Session jsSession = null;
    private ChannelShell jsChannel = null;

    private OutputStream jsCommandOut = null;
    private OutputStream jsTerminalOut = System.out;

    private Object stateMutex = new Object();
    private boolean closeFlag = false;

    private void _openShell(String userId, String userPw, String host, String encOpt, int cols, int rows) throws IllegalArgumentException, JSchException, IOException, IllegalThreadStateException
    {
        if(userId == null || userId.isEmpty() || userPw == null || userPw.isEmpty() ) {
            throw new IllegalArgumentException("Both userID and userPW never be null or empty");
        }

        if(isConnected()) { _closeShell(); }
        
        JSch ctx = new JSch();
        com.jcraft.jsch.Session js = null;
        ChannelShell ch = null;
        int port = -1;

        if(encOpt == null || encOpt.isEmpty()) encOpt = getDefaultLanguageOption();
        if(host == null || host.isEmpty()) {
            host = "localhost";
        }
        else {
            int colonPos = host.indexOf(':');
            if(colonPos > 0) {
                String hostPart = host.substring(0, colonPos);
                String portPart = host.substring(colonPos + 1 );

                try { 
                    port = Integer.parseInt(portPart); 
                    if( port < 1 || port > 65535 ) {
                        throw new IllegalArgumentException("port number range is invalid");
                    }
                }
                catch(NumberFormatException nfe) { 
                    throw new IllegalArgumentException("port number of hostname is invalid");
                }

                host = hostPart;
            }
        }

        try {
            if( port < 0 ) {
                js = ctx.getSession(userId, host);
            }
            else {
                js = ctx.getSession(userId, host, port);
            }

            js.setPassword(userPw);
            js.setConfig("StrictHostKeyChecking", "no");
            
            //js.setUserInfo(new UserInfo() {
            //    public String getPassphrase() { return null; }
            //    public String getPassword() { return null; }
            //    public boolean promptPassphrase(String msg) { return false; }
            //    public boolean promptPassword(String msg) { return false; }
            //    public boolean promptYesNo(String str) { return false; }
            //    public void showMessage(String message) { LocalShell.this.sendMessageToClient(message); }
            //});

            js.connect(5000); // try 5 sec

            ch = (ChannelShell)js.openChannel("shell");

            ch.setInputStream(null);
            ch.setOutputStream(jsTerminalOut);
            
            // Should call Set/Get Input/OutputStream before call connect()!!
            jsCommandOut = ch.getOutputStream();

            ch.setEnv("LANG", encOpt); // Language Setting if needed

            if( cols > 0 && rows > 0) {
                ch.setPtySize(cols, rows, cols * 8, rows * 8);
            }

            ch.connect();
        }
        catch(Exception e) {
            if(js != null && js.isConnected()) { js.disconnect(); }
            throw e;
        }

        jsCtx = ctx;
        jsSession = js;
        jsChannel = ch;
    }

    private void _closeShell() {
        synchronized(stateMutex) {
            if( closeFlag ) return; // Prevent Recursive Close Call
            closeFlag = true;
            if( jsSession != null ) {
                if( jsSession.isConnected() ) {
                    if(jsChannel != null) {
                        if( jsChannel.isConnected() ) {
                            jsChannel.setOutputStream(null);
                            jsChannel.disconnect();
                        }
                        jsChannel = null;
                    }
                    jsSession.disconnect();
                }
                jsSession = null;
            }
            if(jsCtx != null) { jsCtx = null; }

            jsCommandOut = null;
            closeFlag = false;
        }
    }

    ////> Public Method
    public String getDefaultLanguageOption() {
        // return Default Options (By ServerSide Locale)
        Locale current = Locale.getDefault();
        return current.getLanguage() + "_" + current.getCountry() + ".UTF-8";
    }

    public boolean isConnected() {
        synchronized(stateMutex) {
            return jsChannel != null && jsChannel.isConnected();
        }
    }

    public void setOutputStream(OutputStream out) { // Call Before OpenRemote!!
        jsTerminalOut = out;
    }

    public void openRemote(String userId, String userPw, String host ) throws JSchException, IOException {
        openRemote(userId, userPw, host, null, -1, -1);
    }
    public void openRemote(String userId, String userPw, String host, String encOpt) throws JSchException, IOException {
        openRemote(userId, userPw, host, encOpt, -1, -1);
    }
    public void openRemote(String userId, String userPw, String host, String encOpt, int cols, int rows) throws JSchException, IOException {
        _openShell(userId, userPw, host, encOpt, cols, rows);
    }

    public void open(String userId, String userPw ) throws JSchException, IOException {
        open(userId, userPw, null, -1, -1);
    }
    public void open(String userId, String userPw, String encOpt) throws JSchException, IOException {
        open(userId, userPw, encOpt, -1, -1);
    }
    public void open(String userId, String userPw, String encOpt, int cols, int rows) throws JSchException, IOException {
        _openShell(userId, userPw, null, encOpt, cols, rows);
    }

    public void resize(int cols, int rows) {
        if( rows < 1 || cols < 1 ) return;
        if( isConnected() ) {
            jsChannel.setPtySize(cols, rows, cols * 8, rows * 8);
        }
    }

    public void close() { // force close
        _closeShell();
    }

    public void write(byte[] msg) throws IOException{
        synchronized(stateMutex) {
            if( jsCommandOut != null ) { jsCommandOut.write(msg); jsCommandOut.flush(); }
        }
    }
}