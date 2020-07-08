const LANG_OPT = { 
    LOCALE : { KR: "ko_KR", JP: "ja_JP", EN: "en_US" },
    ENCDOING: { UTF8: "UTF-8" },
    getEncodedURIString: function(locale, encoding) {
        if( typeof locale === "string" && locale.length > 0 ) {
            if( typeof encoding === "string" && encoding.length > 0 ) {
                return encodeURIComponent( locale + "." + encoding );
            }
            return encodeURIComponent( locale + "." + this.ENCDOING.UTF8 );
        }
        return '';
    }
};

function isSecuredProtocol() { return (location.protocol == "https:"); }
function getAppNameFromPathName() { // for tomcat URL
    let pathName = location.pathname; // begin with '/'
    let found = pathName.indexOf( '/', 1 );

    return found < 0 ? pathName : pathName.substring(0, found);
}
function getParentPathName() {
    let pathName = location.pathname; // begin with '/'
    let found = pathName.lastIndexOf('/');

    return found > 0 ? pathName.substring(0, found) : pathName;
}

function getWebScoketAddress() {
    let protocol = isSecuredProtocol() ? "wss://" : "ws://";
    let host     = location.host; // include port (if exist)
    let basePathName  = getAppNameFromPathName(); // containing an initial '/'

    return protocol + host + basePathName + "/ws/secureshell";
}

function selectLocaleByBrowserLanguage() {
    if( navigator && navigator.language ) {
        let filteredVal = $("#inputLocale option[value!='']").map( function() { if( $(this).val().startsWith( navigator.language ) ) return $(this).val(); } );
        $("#inputLocale").val( filteredVal.length > 0 ? filteredVal[0] : "Default" );
    }
    else {
        $("#inputLocale").val("Default");
    }
}

function createTerminal() { // Singleton Instance
    if( ! window.terminal ) {
        let term = new Terminal()
        let fitAddon = new FitAddon.FitAddon();
        term.loadAddon(fitAddon);
        term.fitAddon = fitAddon;
        term.listener = []; // Add IEvent<> Here!!
        term.open(document.getElementById("TERMINAL_AREA"));
        fitAddon.fit();

        window.terminal = term;
        window.onresize = function() { fitAddon.fit(); }
    }
}

function resetTerminal() {
    clearTerminalScreen();
    removeAllTerminalListener();
}

function clearTerminalScreen() {
    if( window.terminal ) {
        window.terminal.reset();
    }
}

function focusOnTerminal() {
    if( window.terminal ) {
        window.terminal.focus();
    }
}

function removeAllTerminalListener() {
    if( window.terminal ) {
        let term = window.terminal;
        if( term.listener ) {
            term.listener.forEach( e => e.dispose() );
            term.listener = [];
        }
    }
}

function addTerminalOnDataListener(callback) {
    if( window.terminal && typeof callback === "function" ) {
        let term = window.terminal;
        term.listener.push( term.onData( callback ) );
    }
}

function addTerminalOnResizeListener(callback) {
    if( window.terminal && typeof callback === "function" ) {
        let term = window.terminal;
        term.listener.push( term.onResize( callback ) );
    }
}

function openSigninDialog() { 
    $("#signinModal").modal( { backdrop: "static", keyboard: false } ); 
    updateSignInButton();
}
function closeSigninDialog() { 
    $("#signinModal").modal( "hide" ); 
    clearSigninInput();
}
function appendSigninAlert(message, type) {
    // type = [ primary | secondary | success | danger | warning | info | light | dark ]
    if( typeof type !== "string" ) { type = "danger"; }
    $("#AlertList").append( 
        $("<div></div>").addClass("alert alert-dismissible fade show").addClass("alert-" + type).text(message).append(
            '<button type="button" class="close" data-dismiss="alert">&times;</button>'
        )
    );
}
function clearSigninInput(withLocaleCharsetOption) {
    $("#inputId").val("");
    $("#inputPassword").val("");
    if(Boolean(withLocaleCharsetOption)) {
        $("#inputLocale").val("Default");
        $("#inputCharset").val("Default");
    }
} 
function clearSigninAlert() { $("#AlertList").empty(); }

function getLocaleCharsetOptionQuery() {
    let locale = $("#inputLocale").val().trim();
    let charset = $("#inputCharset").val().trim();

    let query = [];
    if( locale.length > 0 && locale.toLowerCase() != "default" ) {
        query.push("LOCALE=" + encodeURIComponent(locale) );
    }

    if(charset.length > 0 && charset.toLowerCase() != "default" ) {
        query.push("CHARSET=" + encodeURIComponent(charset) );
    }

    return query.length > 0 ? query.join('&') : "";
}

function updateSignInButton(toLoading) {
    const disabledList = "#inputId, #inputPassword, #inputHost, #chkRemoteHost";
    if( Boolean(toLoading) ) {
        $("#SignInBtn").attr("disabled", true).empty().html(
            '<span class="spinner-border" style="width: 1.5rem; height: 1.5rem;"></span> Connecting...'
        );
        $(disabledList).attr("disabled", true);
    }
    else {
        $("#SignInBtn").attr("disabled", false).empty().text("Sign In");
        $(disabledList).attr("disabled", false);
    }
}

function tryConnect() {
    event.preventDefault(); // Prevent Default Submit Event

    if(isCheckedRemoteHost()) {
        const HOST_VALIDATOR = /^[a-zA-Z0-9\-\.]{1,63}(:\d{1,5})?$/;
        let target = $("#inputHost");
        if( !HOST_VALIDATOR.test(target.val()) ) {
            target.addClass("is-invalid");
            return;
        }
    }

    let requireSecured = !isSecuredProtocol();
    if( requireSecured ) { // Check PublicKey Exist?
        // Request Public Key Again!!
        if( !window.publicKey ) {
            // error
            alert("No Publick Key!!");
            return;
        }
    }

    clearSigninAlert();
    updateSignInButton(true);

    // Connect WebSocket
    let targetURL = getWebScoketAddress();
    let query = getLocaleCharsetOptionQuery();
    if(query.length > 0) { targetURL += ("?" + query); } // Append Query to URL

    let ws = new WebSocket(targetURL);
    // ws.binaryType = "arraybuffer";
    ws.makeRequest = requireSecured 
                ? new JsonRPCRequestMakerSecured(window.publicKey.key, window.publicKey.signature) 
                : new JsonRPCRequestMaker();

    ws.onopen = wsHandleOpen;
    ws.onmessage = wsHandleConnect;
    ws.onclose = wsHandleClose;
}

function wsHandleOpen(ev) {
    let webSock = ev.target;

    let termOpt = null;
    if(window.terminal) {
        termOpt = new TerminalOptions();
        termOpt.resize( window.terminal.cols, window.terminal.rows );
    }

    let request =  isCheckedRemoteHost() 
                ? webSock.makeRequest.connectRemoteRPC( REQ_CONN_ID, $("#inputId").val(), $("#inputPassword").val(), $("#inputHost").val(), termOpt )
                : webSock.makeRequest.connectRPC( REQ_CONN_ID, $("#inputId").val(), $("#inputPassword").val(), termOpt );

    webSock.send( JSON.stringify(request) );
}

function wsHandleConnect(ev) {
    let webSock = ev.target;

    let resp = JSON.parse( ev.data );
    if( resp.id == REQ_CONN_ID ) { // Response of Connect RPC
        if( typeof resp.result === "string" && resp.result.toLowerCase() == "success" ) {
            // onSuccess!!
            closeSigninDialog();
            connectWebsocketWithTerminal(webSock);
            webSock.onmessage = wsHandleMessage;
            focusOnTerminal();
            return;
        }
        else if( resp.error ) {
            appendSigninAlert( resp.error, "danger" );
            updateSignInButton();
        }
        else {
            // Unknown Error
            appendSigninAlert( "Unknown Response From Server", "danger" );
            console.log(ev.data); // debug
        }
        webSock.close(); // should close current WebSocket Session!!
    }
    else if( resp.method == RESP_LOG ) {
        let logInfo = resp.params;
        switch( logInfo.type ) {
            case "info":  appendSigninAlert( logInfo.message, "info" ); break;
            case "warn":  appendSigninAlert( logInfo.message, "warning" ); break;
            case "error": appendSigninAlert( logInfo.message, "danger" ); break;
            default: appendSigninAlert( logInfo.message ); break;
        }
        console.log(logInfo);
    }
}

function wsHandleMessage(ev) {
    let webSock = ev.target;

    let resp = JSON.parse(ev.data);
    if( resp.method == REQ_RESP_DATA ) {
        let data = webSock.makeRequest.parseDataRPC( resp );
        if(window.terminal) {
            window.terminal.write( data );
        }
    }
    else if( resp.method == RESP_LOG ) {
        let logInfo = resp.params;
        console.log(logInfo); // Toast?!
    }
}

function wsHandleClose(ev) {
    removeAllTerminalListener();
    openSigninDialog();

    if( ev.code != 1000 ) { // 1000 : Normal Closure
        let message = "Socket Closed (" + ev.code + ")";
        if( typeof ev.reason === "string" && ev.reason.length > 0 ) { message += (": " + ev.reason); }
        console.log(ev);
        appendSigninAlert(message, "danger");
    }
}

function connectWebsocketWithTerminal(websock) {
    resetTerminal();
    addTerminalOnDataListener(function(data){
        websock.send( JSON.stringify(websock.makeRequest.dataRPC( data )) );
    });
    addTerminalOnResizeListener(function(size){
        websock.send( JSON.stringify(websock.makeRequest.resizeRPC( size.cols, size.rows )) );
    });
}

function requestPublicKey() {
    $.ajax({
        url: "crypto/pubkey.json",
        dataType: "json",
        success: function(data, textStatus, jqXHR) {
            window.publicKey = data;
        }
    });
}

function isCheckedRemoteHost() {
    return $("#chkRemoteHost").prop("checked");
}

function onChangeRemoteHost() {
    if(isCheckedRemoteHost()) {
        $("#inputHostHolder").show();
        $("#inputHost").attr("required", "required");
    }
    else {
        $("#inputHostHolder").hide();
        $("#inputHost").removeAttr("required");
    }
}

window.addEventListener("load", function(){
    if( !isSecuredProtocol() ) { requestPublicKey(); }
    createTerminal();
    openSigninDialog();
});
