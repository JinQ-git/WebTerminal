package dev.util.crypto;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.interfaces.RSAPrivateKey;

import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import dev.util.HexUtil;

import java.nio.file.Paths;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;

public class RSAUtil
{
    private final static String NAME = "RSA";

    private final static String PEM_PREFIX_PRIVATE      = "-----BEGIN PRIVATE KEY-----";
    //private final static String PEM_PREFIX_RSA_PRIVATE  = "-----BEGIN RSA PRIVATE KEY-----";
    private final static String PEM_POSTFIX_PRIVATE     = "-----END PRIVATE KEY-----";
    //private final static String PEM_POSTFIX_RSA_PRIVATE = "-----END RSA PRIVATE KEY-----";

    private final static String PEM_PREFIX_PUBLIC      = "-----BEGIN PUBLIC KEY-----";
    //private final static String PEM_PREFIX_RSA_PUBLIC  = "-----BEGIN RSA PUBLIC KEY-----";
    private final static String PEM_POSTFIX_PUBLIC     = "-----END PUBLIC KEY-----";
    //private final static String PEM_POSTFIX_RSA_PUBLIC = "-----END RSA PUBLIC KEY-----";

    private final static String PEM_PREFIX_PRIVATE_KEY_REGEX  = "-----BEGIN( RSA)? PRIVATE KEY-----";
    private final static String PEM_POSTFIX_PRIVATE_KEY_REGEX = "-----END( RSA)? PRIVATE KEY-----";

    private final static String PEM_PREFIX_PUBLIC_KEY_REGEX  = "-----BEGIN( RSA)? PUBLIC KEY-----";
    private final static String PEM_POSTFIX_PUBLIC_KEY_REGEX = "-----END( RSA)? PUBLIC KEY-----";

    public enum KEY_SIZE { 
        KS_512(512), KS_1024(1024), KS_2048(2048), KS_3072(3072);

        private final int mKeySize;
        KEY_SIZE(int keySize) { mKeySize = keySize; }

        public int getKeySize() { return mKeySize; }
        public int getKeySizeByte() { return mKeySize / 8; }
    }

    public static byte[] encryptData( PublicKey key, byte[] planeData ) {
        try {
            Cipher encoder = Cipher.getInstance(NAME);
            encoder.init(Cipher.ENCRYPT_MODE, key);
            return encoder.doFinal(planeData);
        }
        catch(NoSuchAlgorithmException nsae) {
            // Never Reach Here!!
        }
        catch(NoSuchPaddingException nspe) {
            // Never Reach Here!!
        }
        catch(InvalidKeyException ike) {
            // Fail
        }
        catch(IllegalBlockSizeException ibse) {
            // Fail
        }
        catch(BadPaddingException bpe) {
            // Fail
        }
        return null;
    }

    public static String encryptDataToBase64( PublicKey key, byte[] planeData ) {
        byte[] encData = encryptData( key, planeData );
        if( encData != null ) {
            return Base64.getEncoder().encodeToString(encData);
        }
        return null;
    }

    public static String encryptDataToHex( PublicKey key, byte[] planeData, boolean isUpperCase ) {
        return HexUtil.toHexString( encryptData(key, planeData), isUpperCase );
    }

    public static String encryptDataToHex( PublicKey key, byte[] planeData ) {
        return encryptDataToHex( key, planeData, true );
    }

    public static byte[] decryptData( PrivateKey key, byte[] encryptedData ) {
        try {
            Cipher decoder = Cipher.getInstance(NAME);
            decoder.init(Cipher.DECRYPT_MODE, key);
            return decoder.doFinal(encryptedData);
        }
        catch(NoSuchAlgorithmException nsae) {
            // Never Reach Here!!
        }
        catch(NoSuchPaddingException nspe) {
            // Never Reach Here!!
        }
        catch(InvalidKeyException ike) {
            // Fail
        }
        catch(IllegalBlockSizeException ibse) {
            // Fail
        }
        catch(BadPaddingException bpe) {
            // Fail
        }
        return null;
    }

    public static byte[] decryptDataFromBase64( PrivateKey key, String encryptedDataBase64 ) {
        return decryptData( key, Base64.getDecoder().decode(encryptedDataBase64) );
    }

    public static byte[] decryptDataFromHex( PrivateKey key, String encryptedDataHex ) {
        return decryptData( key, HexUtil.toByteArray(encryptedDataHex) );
    }

    public static KeyPair generateKeyPair( KEY_SIZE keySize ) {
        try {
            KeyPairGenerator keygen = KeyPairGenerator.getInstance(NAME);
            keygen.initialize(keySize.getKeySize());
            return keygen.generateKeyPair();
        }catch(NoSuchAlgorithmException e) {
            // Never Reach Here!!
        }
        return null;
    }

    public static PublicKey generatePublicKeyFromPrivateKey(PrivateKey privKey) {
        try {
            KeyFactory keygen = KeyFactory.getInstance(NAME);
            RSAPrivateCrtKey rsaPrivKey = (RSAPrivateCrtKey)privKey;

            return keygen.generatePublic( new RSAPublicKeySpec(rsaPrivKey.getModulus(), rsaPrivKey.getPublicExponent()) );
        } catch(NoSuchAlgorithmException nsae) {
            // Never Reach Here!!
        } catch(InvalidKeySpecException ikse ) {
            // Never Reach Here!!
        }

        return null;
    }

    public static boolean checkKeyPair(PrivateKey privKey, PublicKey pubKey) 
    {
        if( privKey instanceof RSAPrivateKey && pubKey instanceof RSAPublicKey ) {
            RSAPrivateKey rsaPrivKey = (RSAPrivateKey)privKey;
            RSAPublicKey  rsaPubKey  = (RSAPublicKey)pubKey;

            if( rsaPrivKey.getModulus().equals( rsaPubKey.getModulus() ) ) {
                return BigInteger.valueOf(2).modPow( 
                    rsaPubKey.getPublicExponent().multiply( rsaPrivKey.getPrivateExponent() ).subtract( BigInteger.ONE ),
                    rsaPubKey.getModulus()
                ).equals( BigInteger.ONE );
            }
        }

        return false;
    }

    public static String generateSignatureFromPublicKey(KeyPair key) {
        return generateSignatureFromPublicKey(key.getPublic());
    }
    public static String generateSignatureFromPublicKey(PublicKey pubKey) {
        byte[] keyBytes = pubKey.getEncoded();
        MessageDigest digest = null;
        try { digest = MessageDigest.getInstance("SHA-1"); } catch(Exception ignore) {}
        digest.reset();
        digest.update(keyBytes);
        return Base64.getEncoder().encodeToString( digest.digest() );
    }

    public static String privateKeyToPEMString(PrivateKey privKey) {
        return _makePEMString( Base64.getEncoder().encodeToString( privKey.getEncoded() ), true );
        // return PEM_PREFIX_PRIVATE_RSA + "\n" + Base64.getEncoder().encodeToString( privKey.getEncoded() ) + "\n" + PEM_POSTFIX_PRIVATE_RSA + "\n";
    }

    public static String publicKeyToPEMString(PublicKey pubKey) {
        return _makePEMString( Base64.getEncoder().encodeToString( pubKey.getEncoded() ), false );
        // return PEM_PREFIX_PUBLIC_RSA+ "\n" + Base64.getEncoder().encodeToString( pubKey.getEncoded() )+ "\n" + PEM_POSTFIX_PUBLIC_RSA + "\n";
    }

    public static PublicKey generatePublicKeyFromPEMFile( String fileName ) throws InvalidKeySpecException  {
        String fileContent = _readAsString(fileName);
        if( fileContent == null ) { return null; } // File Not Found or No Permission Exception
        if(!fileContent.startsWith("-----BEGIN")) { return null; } // Wrong File Format Exception (Not PEM File)

        byte[] encodedBytes = _PEMStringToByteArray(fileContent, false);
        
        KeyFactory keygen = null; 
        try { keygen = KeyFactory.getInstance(NAME); } catch(Exception ignore) {}

        return keygen.generatePublic(new X509EncodedKeySpec(encodedBytes));
    }

    public static PrivateKey generatePrivateKeyFromPEMFile( String fileName ) throws InvalidKeySpecException {
        String fileContent = _readAsString(fileName);
        if( fileContent == null ) { return null; } // File Not Found or No Permission Exception
        if(!fileContent.startsWith("-----BEGIN")) { return null; } // Wrong File Format Exception (Not PEM File)

        byte[] encodedBytes = _PEMStringToByteArray(fileContent, true);

        KeyFactory keygen = null; 
        try { keygen = KeyFactory.getInstance(NAME); } catch(Exception ignore) {}

        if( _isSequenceEncode(encodedBytes, 0) ) {
            try {
                int offsetToVersion = _getASN1EncodeDataOffset(encodedBytes, 0);
                int offsetToModule  = offsetToVersion + _getASN1EncodeTotalLength(encodedBytes, offsetToVersion);
                if( _isIntegerEncode(encodedBytes, offsetToModule) ) {
                    int offsetToPublicExp   = offsetToModule + _getASN1EncodeTotalLength(encodedBytes, offsetToModule);
                    int offsetToPrivateExp  = offsetToPublicExp + _getASN1EncodeTotalLength(encodedBytes, offsetToPublicExp);

                    if( !_isIntegerEncode(encodedBytes, offsetToPublicExp) || !_isIntegerEncode(encodedBytes, offsetToPrivateExp) ) {

                    }

                    BigInteger modules    = _getASN1IntegerData(encodedBytes, offsetToModule);
                    BigInteger privateExp = _getASN1IntegerData(encodedBytes, offsetToPrivateExp);

                    return keygen.generatePrivate( new RSAPrivateKeySpec(modules, privateExp) );
                }
            }
            catch(IndexOutOfBoundsException ioobe) {
                throw new InvalidKeySpecException("Unexpected Encoded Format");
            }
        }

        return keygen.generatePrivate( new PKCS8EncodedKeySpec(encodedBytes) );
    }

    private static boolean _isSequenceEncode(byte[] encodedData, int offset) {
        return (encodedData[offset] & 0x1F) == 16;
    }

    private static boolean _isIntegerEncode(byte[] encodedData, int offset) {
        return (encodedData[offset] & 0x1F) == 2;
    }

    private static int _getASN1EncodeDataOffset(byte[] encodedData, int offset) {
        return (offset + 2) + ( ( (encodedData[offset + 1] & 0x80) != 0 ) ? (encodedData[offset + 1] & 0x7F) : 0 );
    }

    private static int _getASN1EncodeTotalLength(byte[] encodedData, int offset) throws InvalidKeySpecException, IndexOutOfBoundsException {
        int len = _getASN1EncodeDataLength(encodedData, offset);
        if( (encodedData[offset + 1] & 0x80) != 0 ) {
            len += ((encodedData[offset + 1] & 0x7F));
        }
        return len + 2;
    }

    private static int _getASN1EncodeDataLength(byte[] encodedData, int offset) throws InvalidKeySpecException, IndexOutOfBoundsException {
        int len = (encodedData[offset + 1] & 0x7F);
        
        if( (encodedData[offset + 1] & 0x80) != 0 ) {
            byte[] buf = { 0, 0, 0, 0 };
            if( len > buf.length ) { throw new InvalidKeySpecException("Encoded Length is too Huge"); }
            for(int i = 0, j = buf.length - len; i < len ; i++, j++) {
                buf[j] = encodedData[ offset + 2 + i ];
            }
            len = ByteBuffer.wrap(buf).order(ByteOrder.BIG_ENDIAN).getInt();
        }

        return len;
    }

    private static BigInteger _getASN1IntegerData(byte[] encodedData, int offset) throws InvalidKeySpecException, IndexOutOfBoundsException {
        int newOffset = _getASN1EncodeDataOffset(encodedData, offset);
        int len = _getASN1EncodeDataLength(encodedData, offset);
        return new BigInteger( encodedData, newOffset, len);
    }

    private static String _makePEMString( String base64EncodeStr, boolean isPrivate ) {
        String prefix = isPrivate ? PEM_PREFIX_PRIVATE : PEM_PREFIX_PUBLIC;
        String postfix = isPrivate ? PEM_POSTFIX_PRIVATE : PEM_POSTFIX_PUBLIC;

        int baseSplitCount = base64EncodeStr.length() / 64;
        int beginIndex = 0;
        StringBuilder sb = new StringBuilder( prefix.length() + postfix.length() + base64EncodeStr.length() + baseSplitCount + 3 );

        sb.append(prefix).append("\n");
        for(int i = 0; i < baseSplitCount; i++) {
            sb.append( base64EncodeStr.substring(beginIndex, beginIndex + 64) ).append("\n");
            beginIndex += 64;
        }
        if( base64EncodeStr.length() % 64 > 0 ) {
            sb.append( base64EncodeStr.substring(beginIndex) ).append("\n");
        }
        sb.append(postfix).append("\n");

        return sb.toString();
    }

    private static byte[] _PEMStringToByteArray(String pemContent, boolean isPrivate) {
        if( pemContent != null ) {
            return Base64.getDecoder().decode(
                pemContent.replaceFirst(isPrivate ? PEM_PREFIX_PRIVATE_KEY_REGEX : PEM_PREFIX_PUBLIC_KEY_REGEX, "")
                          .replaceFirst(isPrivate ? PEM_POSTFIX_PRIVATE_KEY_REGEX : PEM_POSTFIX_PUBLIC_KEY_REGEX, "")
                          .replaceAll("\\s+", "")
            );
        }
        return null;
    }

    private static byte[] _readAsBytes(String fileName) {
        try { return Files.readAllBytes( Paths.get(fileName) ); } catch(Exception e) { }
        return null;
    }

    private static String _readAsString(String fileName) {
        byte[] bytes = _readAsBytes(fileName);
        return bytes != null ? new String(bytes) : null;
    }
}