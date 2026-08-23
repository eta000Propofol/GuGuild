package com.guguild.util;

import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtil {
    private static final Pattern HEX = Pattern.compile("&#([0-9a-fA-F]{6})");

    private ColorUtil() {
    }

    public static String colorize(String text) {
        if (text == null) {
            return "";
        }
        Matcher matcher = HEX.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    public static String stripColor(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '§') {
                if (i + 1 < text.length() && text.charAt(i + 1) == 'x') {
                    i += 14;
                } else {
                    i += 2;
                }
            } else if (c == '&' && i + 1 < text.length() && isLegacyColor(text.charAt(i + 1))) {
                i += 2;
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    public static String normalizeName(String rawName) {
        return stripColor(colorize(rawName)).trim().toLowerCase();
    }

    public static int visibleLength(String text) {
        return stripColor(colorize(text)).length();
    }

    private static boolean isLegacyColor(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')
                || c == 'k' || c == 'K' || c == 'l' || c == 'L' || c == 'm' || c == 'M'
                || c == 'n' || c == 'N' || c == 'o' || c == 'O' || c == 'r' || c == 'R';
    }
}
