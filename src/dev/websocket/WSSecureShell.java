package dev.websocket;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.websocket.OnOpen;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.server.ServerEndpoint;

import dev.jsonrpc.JsonUtil;
import dev.jsonrpc.JsonRPCRequest;
import dev.jsonrpc.params.*;
import dev.jsonrpc.params.ParamLog.LogType;
import dev.util.crypto.AESUtil;
import dev.util.ssh.SecureShell;

import com.jcraft.jsch.*;
import com.google.gson.*;

// Create Instance Per Connection

@ServerEndpoint(value="/ws/secureshell")
public class WSSecureShell implements SecureShell.SecureShellListener
{
    // Request Parameter
    private final static String PARAM_LOCALE  = "LOCALE";
    private final static String PARAM_CHARSET = "CHARSET";

    // Member Variables
    private SecretKeySpec encKey   = null;
    private SecureShell   sh       = null;
    private String        shEncOpt = null;

    // WebSocket Session
    private javax.websocket.Session wsSession = null;    
    private Object wsMutex = new Object();

    public void _closeAll() { _closeAll(null); }
    public void _closeAll(CloseReason reason) {
        synchronized(wsMutex) {
            if(wsSession != null) {
                if(wsSession.isOpen()) {
                    try { 
                        if( reason != null ) { wsSession.close(reason); }
                        else { wsSession.close(); }
                    }
                    catch(Exception ignore) {}
                }
                wsSession = null;
            }
        }

        if(sh != null) {
            sh.close();
            sh = null;
        }
    }

    // Private Methods
    private String _parseLocaleCharsetOption(javax.websocket.Session ws) {
        // Options From Parameter
        String locale = null;
        String charset = null;

        if( ws != null ) {
            List<String> values = null;
            Map<String, List<String>> reqParam = ws.getRequestParameterMap();
            
            values = reqParam.get( PARAM_LOCALE );
            if(values != null && !values.isEmpty()) {
                locale = values.get(0); // ex> ko_KR, ja_JP, en_US
            }

            values = reqParam.get( PARAM_CHARSET );
            if(values != null && !values.isEmpty()) {
                charset = values.get(0); // ex> UTF-8
            }
        }

        // if one of option is not null
        if( locale != null || charset != null ) {
            if( locale == null ) {
                Locale current = Locale.getDefault();
                locale = current.getLanguage() + "_" + current.getCountry();
            }

            if( charset == null ) {
                charset = "UTF-8";
            }

            // Combine Options
            return locale + "." + charset;
        }

        return null;
    }

    // Implementation - LocalShell.MessageWriter

    @Override
    public void onMessage(String message) {
        synchronized(wsMutex) {
            if( wsSession != null && wsSession.isOpen() ) {
                try {
                    ParamOnData params = null;
                    if(encKey != null)
                    {
                        IvParameterSpec iv   = AESUtil.generateInitializationVector();
                        String encPartBase64 = AESUtil.encryptDataToBase64(encKey, iv, message.getBytes("UTF-8"));
                        String ivPartBase64  = Base64.getEncoder().encodeToString(iv.getIV());

                        ParamOnData.CipherData data =  new ParamOnData.CipherData(ivPartBase64, encPartBase64, CipherEncoding.BASE64);
                        params = new ParamOnData(data);
                    }
                    else {
                        params = new ParamOnData(message);
                    }

                    wsSession.getBasicRemote().sendText( JsonUtil.toJson( JsonRPCRequest.createNotify(JsonRPCRequest.METHOD_ON_DATA, params) ) );
                }
                catch( IOException ioe ) {
                    ioe.printStackTrace();
                    _closeAll(new CloseReason(CloseCodes.UNEXPECTED_CONDITION, ioe.getMessage()));
                }
                catch(Exception e) {
                    e.printStackTrace();
                    _closeAll(new CloseReason(CloseCodes.UNEXPECTED_CONDITION, e.getMessage()));
                }
            }
        }
    }

    @Override
    public void onLogOut() { _closeAll(); }

    // WebSocket Implementation

    @OnOpen
    public void onOpenWebsocket(javax.websocket.Session ws) {
        wsSession = ws;
        shEncOpt = _parseLocaleCharsetOption(ws);
    }

    @OnClose
    public void onCloseWebsocket(javax.websocket.Session ws) {
        _closeAll(); // Clean Up
    }

    // @OnMessage // BinaryVersion
    // public void onMessageFromClient(byte[] msg) {
    // }

    @OnMessage // TextVersion
    public void onMessageFromClient(String msg) {
        JsonRPCRequest request = null;
        try { request = JsonRPCRequest.fromJSON(msg); }
        catch(JsonSyntaxException jse) {
            // Critical Error
            _closeAll(new CloseReason(CloseCodes.UNEXPECTED_CONDITION, "Invalid Request Json Format: " + jse.getMessage()));
            return;
        }

        switch( request.getMethod() )
        {
            case JsonRPCRequest.METHOD_ON_DATA: // Notification -> No Response
            {
                if( sh != null && sh.isConnected() ) {
                    ParamOnData dataParam = null;
                    try {
                        dataParam = request.getParamAsClass(ParamOnData.class);
                        sh.write( dataParam.getBytes(encKey) );
                    }
                    catch(JsonSyntaxException jse) {
                        sendJsonRPCLogNotify(new ParamLog(LogType.ERROR, jse.getMessage()));
                        return;
                    }
                    catch(NullPointerException npe) {
                        // SecertKey is not specified (Async Message)
                        sendJsonRPCLogNotify(new ParamLog(LogType.ERROR, npe.getMessage()));
                        return;
                    }
                    catch(IOException ioe) {
                        // Critical Error -> Disconnect
                        _closeAll( new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, ioe.getMessage()) );
                        return;
                    }
                }
            }
            return;

            case JsonRPCRequest.METHOD_RESIZE:
            {
                if( sh != null && sh.isConnected() ) {
                    ParamResize resizeParam = null;
                    try {
                        resizeParam = request.getParamAsClass(ParamResize.class);
                        sh.resize( resizeParam.cols, resizeParam.rows );
                    }
                    catch(JsonSyntaxException jse) {
                        sendJsonRPCLogNotify(new ParamLog(LogType.ERROR, jse.getMessage()));
                        return;
                    }
                }
            }
            return;

            case JsonRPCRequest.METHOD_CONNECT: // Response Required!!
            {
                if( sh != null && sh.isConnected() ) {
                    // Already Connected -> Reject Connection Request
                    sendJsonRPCResponse(request.getId(), "Already Connected", true);
                    return;
                }

                ParamConnect connParam     = null;
                ParamConnect.ConnectionInfo info = null;
                ParamConnect.TerminalOptions options = null;
                try {
                    connParam = request.getParamAsClass(ParamConnect.class);
                    info      = connParam.getConnectionInfoData();
                    options   = connParam.getTerminalOptions();

                    if(connParam.isSecured) {
                        byte[] keyBytes = info.getKeyBytes();
                        SecretKeySpec keySpec = null;

                        if( keyBytes != null ) {
                            keySpec = new SecretKeySpec( keyBytes, "AES" );
                        }

                        if( keySpec == null ) {
                            throw new JsonSyntaxException("\"key\" field is not specified or invalid format");
                        }

                        encKey = keySpec;
                    }
                }
                catch(Exception e) { // catch JsonSyntaxException, UnsupportedEncodingException
                    sendJsonRPCResponse(request.getId(), "Invalid JsonRPC Format: " + e.getMessage(), true);
                    return;
                }

                SecureShell _sh = new SecureShell();
                try { 
                    int cols = -1;
                    int rows = -1;

                    if(options != null && options.resize != null) {
                        cols = options.resize.cols;
                        rows = options.resize.rows;
                    }

                    if( info.host != null ) {
                        _sh.openRemote( info.id, info.pw, info.host, shEncOpt, cols, rows, this );
                    }
                    else {
                        _sh.open( info.id, info.pw, shEncOpt, cols, rows, this ); 
                    }
                }
                catch(JSchException je) { // Connection Error
                    sendJsonRPCResponse(request.getId(), je.getMessage(), true);
                    return;
                }
                catch(Exception e) {
                    e.printStackTrace();
                    // Critical Error -> Close WebSocket
                    CloseReason cr = new CloseReason(CloseCodes.UNEXPECTED_CONDITION, e.getMessage());
                    try { wsSession.close(cr); } catch(Exception ignore) {}
                    return;
                }
                sh = _sh;

                // Success Message to Client
                sendJsonRPCResponse(request.getId(), "Success", false);
            }
            return;

            default: // Invalid Method
                if( !request.isNotification() ) {
                    sendJsonRPCResponse(request.getId(), "Unknown Method: " + request.getMethod(), true);
                }
                return;
        }
    }

    @OnError
    public void onError(Throwable e)
    {
        e.printStackTrace();
    }

    private void _sendMessageToClientSync(String message) {
        synchronized(wsMutex) { 
            if( wsSession != null && wsSession.isOpen() ) {
                try { wsSession.getBasicRemote().sendText( message ); }
                catch(IOException ioe) {
                    _closeAll( new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, ioe.getMessage()) );
                }
            }
        }
    }

    private void sendJsonRPCResponse(int id, Object content, boolean isError) {
        _sendMessageToClientSync( String.format("{\"id\":%d,\"%s\":%s}", id, isError ? "error" : "result", JsonUtil.toJson(content)) );
    }

    private void sendJsonRPCLogNotify(ParamLog log) {
        _sendMessageToClientSync( JsonUtil.toJson( JsonRPCRequest.createNotify(JsonRPCRequest.METHOD_LOG, log) ) );
    }

    // public void sendMessageToClientAsync(String message) {
    //     if( wsSession != null && wsSession.isOpen() ) {
    //         wsSession.getAsyncRemote().sendText( message );
    //     }
    // }
}