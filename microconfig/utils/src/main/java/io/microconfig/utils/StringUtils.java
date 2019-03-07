package io.microconfig.utils;

import java.util.regex.Pattern;

import static java.lang.Character.*;
import static java.util.stream.Collectors.joining;

public class StringUtils {
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static String unixLikePath(String path) {
        return path.replace('\\', '/');
    }

    public static String replaceMultipleSpaces(String line) {
        return line.replaceAll("\\s+", " ").trim();
    }

    public static boolean like(String value, String like) {
        if (value == null || like == null) return false;

        String pattern = Pattern.quote(like);
        pattern = pattern.replace("_", "\\E.\\Q");
        pattern = pattern.replace("%", "\\E.*\\Q");
        return value.matches(pattern);
    }

    public static String toLowerHyphen(String name) {
        return name.codePoints()
                .mapToObj(c -> isUpperCase((char) c) ? "-" + toLowerCase((char) c) : Character.toString((char) c))
                .collect(joining());
    }

    public static int indexOfFirstDigitOr(String line, int defaultIndex) {
        for (int i = 0; i < line.length(); i++) {
            if (isDigit(line.charAt(i))) return i;
        }
        return defaultIndex;
    }

    public static String addOffsets(String value, int spacesCount) {
        StringBuilder result = new StringBuilder();
        result.append(value);
        for (int i = 0; i < spacesCount; i++) {
            result.append(' ');
        }
        return result.toString();
    }
}