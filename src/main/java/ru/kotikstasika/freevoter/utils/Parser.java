package ru.kotikstasika.freevoter.utils;

import lombok.experimental.UtilityClass;
import net.md_5.bungee.api.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class Parser {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([a-fA-F0-9]{6})");

    public static String color(String message) {
        if (message == null) return "";
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder buffer = new StringBuilder();

        while (matcher.find()) {
            String hex = matcher.group(1);
            try {
                String replacement = ChatColor.of("#" + hex).toString();
                matcher.appendReplacement(buffer, replacement);
            } catch (Exception e) {
                matcher.appendReplacement(buffer, "");
            }
        }

        matcher.appendTail(buffer);
        message = buffer.toString();

        return ChatColor.translateAlternateColorCodes('&', message);
    }
}