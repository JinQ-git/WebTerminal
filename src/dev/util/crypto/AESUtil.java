package dev.util.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import dev.util.HexUtil;

public class AESUtil {
    private final static String NAME = "AES";
    private final static String NAME_FULL = "AES/CBC/PKCS5Padding";

    public enum KEY_SIZE { 
        KS_128(128), KS_192(192), KS_256(256);

        private final int mKeySize;
        KEY_SIZE(int keySize) { mKeySize = keySize; }

        public int getKeySize() { return mKeySize; }
        public int getKeySizeByte() { return mKeySize / 8; }
    }

    public enum PseudoRandomFunction {
        HmacMD5("HmacMD5"), HmacSHA1("HmacSHA1"), HmacSHA256("HmacSHA256"), HmacSHA512("HmacSHA512");
        private final String name;
        private PseudoRandomFunction(String s) { name = s; }

        public String getName() { return name; }
    }

    public static byte[] encryptData( SecretKeySpec key, IvParameterSpec iv, byte[] planeData ) {
        try {
            Cipher encoder = Cipher.getInstance(NAME_FULL);
            if(iv == null) throw new InvalidAlgorithmParameterException("IvParameterSepc: NullPointerException");
            encoder.init(Cipher.ENCRYPT_MODE, key, iv);
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
        catch(InvalidAlgorithmParameterException iape) {
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

    public static byte[] encryptData( SecretKeySpec key, IvParameterSpec iv, byte[] planeData, int off, int len ) {
        try {
            Cipher encoder = Cipher.getInstance(NAME_FULL);
            if(iv == null) throw new InvalidAlgorithmParameterException("IvParameterSepc: NullPointerException");
            encoder.init(Cipher.ENCRYPT_MODE, key, iv);
            return encoder.doFinal(planeData, off, len);
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
        catch(InvalidAlgorithmParameterException iape) {
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

    public static String encryptDataToBase64( SecretKeySpec key, IvParameterSpec iv, byte[] planeData ) {
        byte[] encData = encryptData( key, iv, planeData );
        if( encData != null ) {
            return Base64.getEncoder().encodeToString(encData);
        }
        return null;
    }

    public static String encryptDataToBase64( SecretKeySpec key, IvParameterSpec iv, byte[] planeData, int off, int len ) {
        byte[] encData = encryptData( key, iv, planeData, off, len );
        if( encData != null ) {
            return Base64.getEncoder().encodeToString(encData);
        }
        return null;
    }

    public static String encryptDataToHex( SecretKeySpec key, IvParameterSpec iv, byte[] planeData, boolean isUpperCase ) {
        return HexUtil.toHexString( encryptData( key, iv, planeData ), isUpperCase );
    }

    public static String encryptDataToHex( SecretKeySpec key, IvParameterSpec iv, byte[] planeData, int off, int len, boolean isUpperCase ) {
        return HexUtil.toHexString( encryptData( key, iv, planeData, off, len ), isUpperCase );
    }

    public static String encryptDataToHex( SecretKeySpec key, IvParameterSpec iv, byte[] planeData ) {
        return encryptDataToHex( key, iv, planeData, true );
    }

    public static String encryptDataToHex( SecretKeySpec key, IvParameterSpec iv, byte[] planeData, int off, int len ) {
        return encryptDataToHex( key, iv, planeData, off, len, true );
    }

    public static byte[] decryptData( SecretKeySpec key, IvParameterSpec iv, byte[] encryptedData ) {
        try {
            Cipher decoder = Cipher.getInstance(NAME_FULL);
            decoder.init(Cipher.DECRYPT_MODE, key, iv);
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
        catch(InvalidAlgorithmParameterException iape) {
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

    public static byte[] decryptData( SecretKeySpec key, IvParameterSpec iv, byte[] encryptedData, int off, int len ) {
        try {
            Cipher decoder = Cipher.getInstance(NAME_FULL);
            decoder.init(Cipher.DECRYPT_MODE, key, iv);
            return decoder.doFinal(encryptedData, off, len);
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
        catch(InvalidAlgorithmParameterException iape) {
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

    public static byte[] decryptDataFromBase64( SecretKeySpec key, IvParameterSpec iv, String encryptedDataBase64 ) {
        return decryptData( key, iv, Base64.getDecoder().decode(encryptedDataBase64) );
    }

    public static byte[] decryptDataFromHex( SecretKeySpec key, IvParameterSpec iv, String encryptedDataHex ) {
        return decryptData( key, iv, HexUtil.toByteArray(encryptedDataHex) );
    }

    public static SecretKeySpec generateSecretKey(KEY_SIZE keySize)
    {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(NAME);
            keyGen.init(keySize.getKeySize(), SecureRandom.getInstanceStrong());
            SecretKey rawKey = keyGen.generateKey();
            if(rawKey != null) {
                return new SecretKeySpec(rawKey.getEncoded(), NAME);
            }
        }
        catch(NoSuchAlgorithmException nsae) {
            // KeyGenerator.getInstance(), SecureRandom.getInstanceStrong()
        }
        return null;
    }

    public static SecretKeySpec generateSecretKeyWithPBKDF2(String password, KEY_SIZE keySize) {
        SecureRandom sr = null;
        try { SecureRandom.getInstanceStrong(); }
        catch(NoSuchAlgorithmException nsae) { sr = new SecureRandom(); }

        byte[] salt = new byte[16];
        sr.nextBytes(salt);

        return generateSecretKeyWithPBKDF2(password, salt, PseudoRandomFunction.HmacSHA1, 1000, keySize);
    }

    public static SecretKeySpec generateSecretKeyWithPBKDF2(String password, byte[] salt, PseudoRandomFunction prf, int iteration, KEY_SIZE keySize )
    {
        // NOTE: salt.length == 16
        try {
            SecretKeyFactory keyGen = SecretKeyFactory.getInstance("PBKDF2With" + prf.getName());
            PBEKeySpec spec = new PBEKeySpec( password.toCharArray(), salt, iteration, keySize.getKeySize() );

            SecretKey rawKey = keyGen.generateSecret(spec);
            if(rawKey != null) {
                return new SecretKeySpec(rawKey.getEncoded(), NAME);
            }
        }
        catch(NoSuchAlgorithmException nsae) {
            // Fail
        }
        catch(InvalidKeySpecException ikse) {
            // Fail
        }
        return null;
    }

    public static IvParameterSpec generateInitializationVector()
    {
        byte[] iv = new byte[16];
        SecureRandom sr = null;
        try { sr = SecureRandom.getInstanceStrong(); }
        catch(NoSuchAlgorithmException nase) { sr = new SecureRandom(); }

        sr.nextBytes(iv);
        return new IvParameterSpec(iv);
    }
}