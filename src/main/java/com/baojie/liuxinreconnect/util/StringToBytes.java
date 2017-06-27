package com.baojie.liuxinreconnect.util;

import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.binary.Base64;

public final class StringToBytes {

    private static final String NullString = "NULL";

    private StringToBytes() {

    }

    public static final byte[] protoBytesToBase64Bytes(final byte[] protoBytes) {
        CheckNull.checkByteArrayNull(protoBytes, "byte[]");
        byte[] base64Bytes = null;
        try {
            base64Bytes = Base64.encodeBase64(protoBytes);
        } catch (Throwable throwable) {
            base64Bytes = null;
            throwable.printStackTrace();
        }
        if (null == base64Bytes) {
            base64Bytes = new byte[0];
        }
        return base64Bytes;
    }

    public static final byte[] base64BytesToProtoBytes(final byte[] base64Bytes) {
        CheckNull.checkByteArrayNull(base64Bytes, "byte[]");
        byte[] protoBytes = null;
        try {
            protoBytes = Base64.decodeBase64(base64Bytes);
        } catch (Throwable throwable) {
            protoBytes = null;
            throwable.printStackTrace();
        }
        if (null == protoBytes) {
            protoBytes = new byte[0];
        }
        return protoBytes;
    }


    public static final byte[] stringToBytesByUTF8(final String string) {
        CheckNull.checkStringNull(string, "string");
        byte[] bytes = null;
        try {
            bytes = Base64.encodeBase64(getBytesFromString(string));
        } catch (Throwable throwable) {
            bytes = null;
            throwable.printStackTrace();
        }
        if (null == bytes) {
            bytes = new byte[0];
        }
        return bytes;
    }

    private static final byte[] getBytesFromString(final String string) {
        byte[] bytes = null;
        try {
            bytes = string.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            bytes = null;
            e.printStackTrace();
        }
        if (null == bytes) {
            bytes = new byte[0];
        }
        return bytes;
    }

    public static final String bytesToStringByUTF8(final byte[] base64Bytes) {
        byte[] bytes = null;
        try {
            bytes = Base64.decodeBase64(base64Bytes);
        } catch (Throwable throwable) {
            bytes = null;
            throwable.printStackTrace();
        }
        if (null == bytes) {
            return NullString;
        } else {
            return makeNewString(bytes);
        }
    }


    private static final String makeNewString(final byte[] bytes) {
        String string = null;
        try {
            string = new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            string = null;
            e.printStackTrace();
        }
        if (null == string) {
            string = NullString;
        }
        return string;
    }

}
