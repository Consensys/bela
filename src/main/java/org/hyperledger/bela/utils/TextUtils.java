package org.hyperledger.bela.utils;

import org.apache.tuweni.bytes.Bytes;

import java.util.Arrays;
import java.util.stream.Collectors;

public class TextUtils {

    public static String wrapBytesForDisplayAtCols(byte[] bytes, int cols) {
            String hex =  Bytes.wrap(bytes).toHexString();
        return Arrays.asList(hex.split("(?<=\\G.{"+cols+"})"))
                .stream()
                .collect(Collectors.joining("\\\n"));
    }

    public static byte[] unWrapDisplayBytes(String wrappedBytes) {
        return Bytes.fromHexString(wrappedBytes.replaceAll("\\\\\\n", "")).toArrayUnsafe();
    }

    public static String abbreviateForDisplay(String s) {
        if (s.length() > 20) {
            return s.substring(0,8) + " ... " + s.substring(s.length()-8);
        } else {
            return s;
        }
    }

    public static String abbreviateForDisplay(byte [] s) {
        return abbreviateForDisplay(Bytes.wrap(s).toHexString());
    }
}
