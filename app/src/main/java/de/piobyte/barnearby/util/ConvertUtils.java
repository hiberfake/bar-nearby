package de.piobyte.barnearby.util;

public class ConvertUtils {

//        String uuid = "E20A39F473F54BC4A12F17D1AD07A961";
//        String major = "0000";
//        String minor = "0000";
//        byte[] input = ConvertUtils.hexStringToByteArray(uuid + major + minor);
//        Log.d(TAG, Base64.encodeToString(input, Base64.DEFAULT));

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}