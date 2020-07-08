package dev.util.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

import com.jcraft.jsch.*;

public class SecureShell extends Thread
{
    public interface SecureShellListener { public void onMessage(String msg); public void onLogOut(); }

    // JSch Context, Session and Channel
    private JSch jsCtx = null;
    private com.jcraft.jsch.Session jsSession = null;
    private ChannelShell jsChannel = null;

    private OutputStream jsCommandOut = null;
    private InputStream  jsTerminalIn = null;

    private Object stateMutex = new Object();
    private boolean closeExplicit = false;

    private SecureShellListener listener = null;

    private void _openShell(String userId, String userPw, String host, String encOpt, int cols, int rows) throws IllegalArgumentException, JSchException, IOException, IllegalThreadStateException
    {
        if(userId == null || userId.isEmpty() || userPw == null || userPw.isEmpty() ) {
            throw new IllegalArgumentException("Both userID and userPW never be null or empty");
        }
        if(this.getState() != State.NEW) {
            throw new IllegalThreadStateException();
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
            
            // Should call Set/Get Input/OutputStream before call connect()!!
            jsCommandOut = ch.getOutputStream();
            jsTerminalIn = ch.getInputStream();

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
            if( jsSession != null ) {
                if( jsSession.isConnected() ) {
                    if(jsChannel != null) {
                        jsChannel.disconnect();
                        jsChannel = null;
                    }
                    jsSession.disconnect();
                }
                jsSession = null;
            }
            if(jsCtx != null) { jsCtx = null; }

            jsCommandOut = null;
            jsTerminalIn = null;
        }
    }

    ////> Public Method
    public InputStream getTerminalInStream() { return jsTerminalIn; }
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

    public void openRemote(String userId, String userPw, String host, SecureShellListener _l ) throws IllegalArgumentException, JSchException, IOException, IllegalThreadStateException {
        openRemote(userId, userPw, host, null, -1, -1, _l);
    }
    public void openRemote(String userId, String userPw, String host, String encOpt, SecureShellListener _l) throws JSchException, IOException, IllegalThreadStateException {
        openRemote(userId, userPw, host, encOpt, -1, -1, _l);
    }
    public void openRemote(String userId, String userPw, String host, String encOpt, int cols, int rows, SecureShellListener _l) throws JSchException, IOException, IllegalThreadStateException {
        if( _l == null ) throw new IllegalArgumentException("LocalShellListener Interface never be null");
        listener = _l;
        _openShell(userId, userPw, host, encOpt, cols, rows);
        super.start();
    }

    public void open(String userId, String userPw, SecureShellListener _l ) throws IllegalArgumentException, JSchException, IOException, IllegalThreadStateException {
        open(userId, userPw, null, -1, -1, _l);
    }
    public void open(String userId, String userPw, String encOpt, SecureShellListener _l) throws JSchException, IOException, IllegalThreadStateException {
        open(userId, userPw, encOpt, -1, -1, _l);
    }
    public void open(String userId, String userPw, String encOpt, int cols, int rows, SecureShellListener _l) throws JSchException, IOException, IllegalThreadStateException {
        if( _l == null ) throw new IllegalArgumentException("LocalShellListener Interface never be null");
        listener = _l;
        _openShell(userId, userPw, null, encOpt, cols, rows);
        super.start();
    }

    public void resize(int cols, int rows) {
        if( rows < 1 || cols < 1 ) return;
        if( isConnected() ) {
            jsChannel.setPtySize(cols, rows, cols * 8, rows * 8);
        }
    }

    public void close() { // force close
        if( isConnected() ) {
            closeExplicit = true;
            _closeShell();
            try { super.join(); } catch(Exception ignore){}
        }
        else {
            _closeShell(); // Not Actual Close Action But CleanUp all Reference to null
        }
    }

    public void write(byte[] msg) throws IOException{
        if( jsCommandOut != null ) { jsCommandOut.write(msg); jsCommandOut.flush(); }
    }

    @Override
    public void start() throws IllegalAccessError {
        // Prevent Default Start Method
        throw new IllegalAccessError("Do not call start() method directly, instead call open() method");
    }

    @Override
    public void run() 
    {
        StringBuilder sb = new StringBuilder();
        byte[] buffer = new byte[1024];
        InputStream termIn = getTerminalInStream();

        try {
            while( isConnected() ) 
            {
                if( termIn.available() <= 0 ) {
                    if(sb.length() > 0 && listener != null) {
                        listener.onMessage(sb.toString());
                        sb.setLength(0);
                    }
                }
                int nRead = termIn.read(buffer, 0, buffer.length);
                if(nRead < 0) break;
                sb.append( new String(buffer, 0, nRead) );
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        
        if(!closeExplicit) { // Shell Closed via logout
            _closeShell(); // cleanUp Shell
            if(listener != null) {
                listener.onLogOut();
            }
        }
    }
}