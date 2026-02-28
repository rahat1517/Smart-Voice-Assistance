package com.voiceassistant.app;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public class ContactMatcher {

    private final Context ctx;

    public static class MatchResult {
        public final String name;
        public final String phone;
        public MatchResult(String name, String phone) {
            this.name = name;
            this.phone = phone;
        }
    }

    public ContactMatcher(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    public MatchResult findContact(String query) {
        if (query == null) return null;
        String q = query.trim();
        if (q.isEmpty()) return null;

        // 1) direct try
        MatchResult m = findContactOnce(q);
        if (m != null) return m;

        // 2) if not found: try BN->EN candidates
        for (String cand : bnToEnCandidates(q)) {
            m = findContactOnce(cand);
            if (m != null) return m;
        }

        return null;
    }

    private MatchResult findContactOnce(String q) {
        Cursor cursor = null;
        try {
            String selection =
                    "UPPER(" + ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + ") LIKE UPPER(?)";

            cursor = ctx.getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                    },
                    selection,
                    new String[]{"%" + q + "%"},
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(0);
                String phone = cursor.getString(1);
                if (phone != null) phone = phone.replaceAll("\\s+", "");
                return new MatchResult(name, phone);
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    // --- Bangla spoken -> English name candidates (quick + practical) ---
    private List<String> bnToEnCandidates(String bn) {
        String s = bn.toLowerCase(Locale.ROOT).trim();
        LinkedHashSet<String> out = new LinkedHashSet<>();

        // If already latin, nothing to do
        if (s.matches(".*[a-z].*")) return new ArrayList<>();

        // ✅ common: সাকিব / শাকিব -> Sakib/Shakib/Saqib
        if (s.contains("সাকিব") || s.contains("শাকিব")) {
            out.add("sakib");
            out.add("shakib");
            out.add("saqib");
            out.add("shaqib");
        }

        // add more common patterns if you want
        // e.g. "রাহাত" -> rahat, "মাহি" -> mahi etc.

        return new ArrayList<>(out);
    }
}