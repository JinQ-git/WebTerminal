// JsonRPC Request Method
const REQ_CONN_ID = 1

const REQ_CONN = "connect";
const REQ_RESP_DATA = "data";
const REQ_RESIZE = "resize";
const RESP_LOG  = "log"; // for Debugging?!

// CipherData Encoding
const ENC_BASE64 = "base64";
const ENC_HEX    = "hex";

class TerminalOptions {
    resize(cols, rows) {
        if( Number.isInteger(cols) && Number.isInteger(rows) ) {
            this.resize = { cols: cols, rows: rows };
        }
    }

    removeAll() {
        for( let key in this ) { delete this[key]; }
    }
}

class JsonRPCRequestMaker {
    connectRPC(id, userId, userPw, opts) { // typeof id === "number(int)", typeof userId === "string", typeof userPw === "string"
        let connInfo = { id: userId, pw: userPw };
        let request = { id: Number(id), method: REQ_CONN, params: { data: connInfo } };
        if( opts instanceof TerminalOptions ) {
            request.params.options = opts;
        }

        return request;
    }

    connectRemoteRPC(id, userId, userPw, hostAddr, opts) { // typeof id === "number(int)", typeof userId === "string", typeof userPw === "string", typeof hostAddr === "string"
        let connInfo = { id: userId, pw: userPw, host: hostAddr };
        let request = { id: Number(id), method: REQ_CONN, params: { data: connInfo } };
        if( opts instanceof TerminalOptions ) {
            request.params.options = opts;
        }
        return request;
    }

    dataRPC(data) { // typeof data === "string"
        let request = { method: REQ_RESP_DATA, params: { data: data } };
        return request;
    }

    resizeRPC(c, r) {
        let request = { method: REQ_RESIZE, params: { cols: c, rows: r } };
        return request;
    }

    parseDataRPC(rpc) {
        if( rpc.method == REQ_RESP_DATA ) {
            return rpc.params.data;
        }
        return "";
    }
}

class JsonRPCRequestMakerSecured extends JsonRPCRequestMaker {
    constructor( publicKeyPEM, signature ) {
        super();
        this.publicKey = publicKeyPEM; // must be PEM format string
        if( typeof signature === "string" ) {
            this.signature = signature;
        }

        // Generate SecretKey
        const KEY_SIZE = 256;
        let randomText = CryptoJS.lib.WordArray.random(128 / 8).toString(CryptoJS.enc.Base64);
        let salt = CryptoJS.lib.WordArray.random(128 / 8);
        let key = CryptoJS.PBKDF2(randomText, salt, { keySize: KEY_SIZE / 32, iterations: 1000, hasher: CryptoJS.algo.SHA256 });

        this.secretKey = key;
        this.secretKeyBase64 = key.toString(CryptoJS.enc.Base64);
    }

    _encryptConnInfo(connInfo) {
        let connInfoJson = JSON.stringify( connInfo );

        let rsa = new JSEncrypt();
        rsa.setPublicKey(this.publicKey);

        let cipherInfo = { cipherData: rsa.encrypt(connInfoJson), encoding: ENC_BASE64 };
        if( this.signature ) {
            cipherInfo.signature = this.signature;
        }

        return cipherInfo;
    }

    connectRPC(id, userId, userPw, opts) {
        let connInfo = { id: userId, pw: userPw, key: this.secretKeyBase64, encoding: ENC_BASE64 };
        let cipherInfo = this._encryptConnInfo(connInfo);
        let request = { id: Number(id), method: REQ_CONN, params: { isSecured: true, data: cipherInfo } };
        if( opts instanceof TerminalOptions ) {
            request.params.options = opts;
        }
        return request;
    }

    connectRemoteRPC(id, userId, userPw, hostAddr, opts) { // typeof id === "number(int)", typeof userId === "string", typeof userPw === "string", typeof hostAddr === "string"
        let connInfo = { id: userId, pw: userPw, host: hostAddr, key: this.secretKeyBase64, encoding: ENC_BASE64 };
        let cipherInfo = this._encryptConnInfo(connInfo);
        let request = { id: Number(id), method: REQ_CONN, params: { isSecured: true, data: cipherInfo } };
        if( opts instanceof TerminalOptions ) {
            request.params.options = opts;
        }
        return request;
    }

    dataRPC(data) {
        let randomIV = CryptoJS.lib.WordArray.random(128 / 8);
        let cipher   = CryptoJS.AES.encrypt( data, this.secretKey, { iv : randomIV } );

        let cipherData = { 
            iv: randomIV.toString(CryptoJS.enc.Base64), 
            cipherData: cipher.ciphertext.toString(CryptoJS.enc.Base64),
            encoding: ENC_BASE64
        };

        let request = { method: REQ_RESP_DATA, params: { isSecured: true, data: cipherData } };
        return request;
    }

    parseDataRPC(rpc) {
        if( rpc.method == REQ_RESP_DATA ) {
            if( !rpc.params.isSecured ) { return rpc.params.data; }
            let cipher = rpc.params.data;

            let decoder;
            if( cipher.encoding == ENC_BASE64 ) { decoder = CryptoJS.enc.Base64; }
            else if( cipher.encoding == ENC_HEX ) { decoder = CryptoJS.enc.Hex; }
            else { return ""; } // Never Reach Here!!

            let cipherOption = { iv: decoder.parse( cipher.iv ) };
            let cipherParam  = CryptoJS.lib.CipherParams.create({
                ciphertext: decoder.parse( cipher.cipherData )
            });

            let result = CryptoJS.AES.decrypt( cipherParam, this.secretKey, cipherOption );
            return result.toString( CryptoJS.enc.Utf8 );
        }
        return "";
    }
}