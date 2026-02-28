package com.voiceassistant.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CommandEngine {

    private static final String PREF_NAME = "voice_assistant_commands";
    private static final String KEY_PREFIX = "cmd_";

    private final SharedPreferences prefs;

    public CommandEngine(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Save a custom command: phrase -> action
    public void addCustomCommand(String phrase, String action) {
        if (phrase == null || action == null) return;

        phrase = phrase.trim();
        action = action.trim();
        if (phrase.isEmpty() || action.isEmpty()) return;

        prefs.edit().putString(KEY_PREFIX + phrase, action).apply();
    }

    // Get all saved commands as List of [phrase, action]
    public List<String[]> getCustomCommands() {
        Map<String, ?> all = prefs.getAll();
        List<String[]> result = new ArrayList<>();

        for (Map.Entry<String, ?> e : all.entrySet()) {
            String key = e.getKey();
            if (key != null && key.startsWith(KEY_PREFIX)) {
                String phrase = key.substring(KEY_PREFIX.length());
                Object value = e.getValue();
                if (value instanceof String) {
                    result.add(new String[]{phrase, (String) value});
                }
            }
        }
        return result;
    }

    // Delete by phrase
    public void deleteCustomCommand(String phrase) {
        if (phrase == null) return;
        prefs.edit().remove(KEY_PREFIX + phrase.trim()).apply();
    }

    // Clear all saved commands
    public void clearAllCustomCommands() {
        Map<String, ?> all = prefs.getAll();
        SharedPreferences.Editor ed = prefs.edit();
        for (String key : all.keySet()) {
            if (key != null && key.startsWith(KEY_PREFIX)) ed.remove(key);
        }
        ed.apply();
    }
}
