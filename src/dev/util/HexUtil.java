package dev.util;

public class HexUtil {
    private final static char[] HEX_LOOKUP_U = "0123456789ABCDEF".toCharArray();
    private final static char[] HEX_LOOKUP_L = "0123456789abcdef".toCharArray();

    public static String toHexString(byte[] data, boolean isUpperCase) {
        if( data != null) {
            final char[] lookup = isUpperCase ? HEX_LOOKUP_U : HEX_LOOKUP_L;
            StringBuilder sb = new StringBuilder(data.length * 2);
            for( byte b : data ) {
                sb.append( lookup[ (b >> 4) & 0xF ] );
                sb.append( lookup[ b & 0xF ] );
            }
            return sb.toString();
        }
        return null;
    }

    public static byte[] toByteArray(String hexString) {
        byte[] data = new byte[ (hexString.length() / 2) + (hexString.length() % 2) ];
        int strIdx = 0;
        int idx = 0;

        if( (hexString.length() % 2) > 0 ) {
            data[idx] = (byte)(Character.digit( hexString.charAt(0), 16 ) & 0x0F);
            idx = 1;
            strIdx = 2;
        }

        for( ; strIdx < hexString.length(); strIdx += 2, idx++ ) {
            data[idx] = (byte)(((Character.digit( hexString.charAt(strIdx), 16 ) << 4) & 0xF0)
                      | (Character.digit( hexString.charAt(strIdx + 1), 16 ) & 0x0F));
        }

        return data;
    }
    
}