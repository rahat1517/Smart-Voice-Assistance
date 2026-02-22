package com.example.rahatassistant.nlu;

public class CommandParser {

    public enum IntentType { FLASH_ON, FLASH_OFF, VOL_UP, VOL_DOWN, UNKNOWN }

    public static String normalize(String s) {
        if (s == null) return "";
        s = s.toLowerCase().trim();
        s = s.replaceAll("[\\p{Punct}]+", " ");
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }

    private static String stripWake(String t) {
        // Remove optional wake prefix if present (but NOT required)
        // hey/hi/he + rahat/raha/raat/chahat
        t = t.replaceAll("^\\s*(?:hey|hi|he)?\\s*(?:rahat|raha|raat|chahat)\\b\\s*", "");
        return t.trim();
    }

    public static IntentType detect(String raw) {
        String t = stripWake(normalize(raw));

        // --- FLASHLIGHT / TORCH ---
        boolean hasFlashWord =
                t.contains("flashlight") || t.contains("torch") || t.contains("flash") || t.contains("light");

        boolean isOn =
                t.matches(".*\\b(on|enable|start|open)\\b.*") ||
                t.matches(".*\\bturn on\\b.*") ||
                t.matches(".*\\bswitch on\\b.*");

        boolean isOff =
                t.matches(".*\\b(off|disable|stop|close)\\b.*") ||
                t.matches(".*\\bturn off\\b.*") ||
                t.matches(".*\\bswitch off\\b.*");

        if (hasFlashWord) {
            if (isOn) return IntentType.FLASH_ON;
            if (isOff) return IntentType.FLASH_OFF;

            // "flashlight" alone → treat as ON (optional)
            // return IntentType.FLASH_ON;
        }

        // --- VOLUME ---
        boolean hasVolWord = t.contains("volume") || t.contains("sound");

        boolean volUp =
                t.matches(".*\\b(up|increase|raise|louder|max)\\b.*");

        boolean volDown =
                t.matches(".*\\b(down|decrease|lower|quieter|min)\\b.*");

        if (hasVolWord) {
            if (volUp) return IntentType.VOL_UP;
            if (volDown) return IntentType.VOL_DOWN;
        }

        return IntentType.UNKNOWN;
    }
}
