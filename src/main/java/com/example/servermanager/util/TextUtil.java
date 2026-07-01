package com.example.servermanager.util;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class TextUtil {

    /**
     * Converts a string with standard Minecraft legacy color codes (e.g. &a, &e)
     * into a styled {@link Text} component.
     *
     * @param text The input string containing color codes (using '&')
     * @return A styled MutableText component representing the formatted string
     */
    public static Text format(String text) {
        if (text == null) {
            return Text.literal("");
        }

        MutableText result = Text.literal("");
        String[] parts = text.split("&");

        // The first part has no color code preceding it
        if (parts.length > 0) {
            result.append(Text.literal(parts[0]));
        }

        // Active styling states
        Formatting currentColor = null;
        boolean bold = false;
        boolean italic = false;
        boolean underline = false;
        boolean strikethrough = false;
        boolean obfuscated = false;

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) {
                // This means there was a double "&&", representing a literal "&"
                result.append(Text.literal("&"));
                continue;
            }

            char code = part.charAt(0);
            String content = part.substring(1);

            Formatting format = getFormattingByChar(code);
            if (format != null) {
                if (format.isColor()) {
                    currentColor = format;
                    bold = false;
                    italic = false;
                    underline = false;
                    strikethrough = false;
                    obfuscated = false;
                } else if (format == Formatting.RESET) {
                    currentColor = null;
                    bold = false;
                    italic = false;
                    underline = false;
                    strikethrough = false;
                    obfuscated = false;
                } else {
                    switch (code) {
                        case 'l': bold = true; break;
                        case 'o': italic = true; break;
                        case 'n': underline = true; break;
                        case 'm': strikethrough = true; break;
                        case 'k': obfuscated = true; break;
                    }
                }
            } else {
                // Not a valid color code, append the original text with '&'
                result.append(Text.literal("&" + part));
                continue;
            }

            if (!content.isEmpty()) {
                MutableText textPart = Text.literal(content);
                if (currentColor != null) {
                    textPart.formatted(currentColor);
                }
                if (bold) textPart.formatted(Formatting.BOLD);
                if (italic) textPart.formatted(Formatting.ITALIC);
                if (underline) textPart.formatted(Formatting.UNDERLINE);
                if (strikethrough) textPart.formatted(Formatting.STRIKETHROUGH);
                if (obfuscated) textPart.formatted(Formatting.OBFUSCATED);

                result.append(textPart);
            }
        }

        return result;
    }

    private static Formatting getFormattingByChar(char code) {
        switch (code) {
            case '0': return Formatting.BLACK;
            case '1': return Formatting.DARK_BLUE;
            case '2': return Formatting.DARK_GREEN;
            case '3': return Formatting.DARK_AQUA;
            case '4': return Formatting.DARK_RED;
            case '5': return Formatting.DARK_PURPLE;
            case '6': return Formatting.GOLD;
            case '7': return Formatting.GRAY;
            case '8': return Formatting.DARK_GRAY;
            case '9': return Formatting.BLUE;
            case 'a': return Formatting.GREEN;
            case 'b': return Formatting.AQUA;
            case 'c': return Formatting.RED;
            case 'd': return Formatting.LIGHT_PURPLE;
            case 'e': return Formatting.YELLOW;
            case 'f': return Formatting.WHITE;
            case 'k': return Formatting.OBFUSCATED;
            case 'l': return Formatting.BOLD;
            case 'm': return Formatting.STRIKETHROUGH;
            case 'n': return Formatting.UNDERLINE;
            case 'o': return Formatting.ITALIC;
            case 'r': return Formatting.RESET;
            default: return null;
        }
    }
}
