package com.voiceassistant.app;

import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.provider.Settings;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CommandRouter {

    public static class ActionResult {
        public final boolean handled;
        public final String response;
        public ActionResult(boolean handled, String response) {
            this.handled = handled;
            this.response = response;
        }
        public static ActionResult ok(String r){ return new ActionResult(true, r); }
        public static ActionResult no(){ return new ActionResult(false, ""); }
    }

    private final Context ctx;
    private final DeviceController device;
    private final ContactMatcher contactMatcher;
    private final CommandEngine commandEngine;

    private final Map<String, String> appMap = new HashMap<>();

    public CommandRouter(Context ctx,
                         DeviceController device,
                         ContactMatcher contactMatcher,
                         CommandEngine commandEngine) {
        this.ctx = ctx.getApplicationContext();
        this.device = device;
        this.contactMatcher = contactMatcher;
        this.commandEngine = commandEngine;

        // ===== Google Apps =====
        appMap.put("chrome", "com.android.chrome");
        appMap.put("google", "com.google.android.googlequicksearchbox");
        appMap.put("gmail", "com.google.android.gm");
        appMap.put("youtube", "com.google.android.youtube");
        appMap.put("yt music", "com.google.android.apps.youtube.music");
        appMap.put("maps", "com.google.android.apps.maps");
        appMap.put("drive", "com.google.android.apps.docs");
        appMap.put("photos", "com.google.android.apps.photos");
        appMap.put("play store", "com.android.vending");
        appMap.put("calendar", "com.google.android.calendar");
        appMap.put("meet", "com.google.android.apps.meetings");
        appMap.put("classroom", "com.google.android.apps.classroom");

// ===== Meta Apps =====
        appMap.put("facebook", "com.facebook.katana");
        appMap.put("messenger", "com.facebook.orca");
        appMap.put("instagram", "com.instagram.android");
        appMap.put("whatsapp", "com.whatsapp");

// ===== Communication =====
        appMap.put("telegram", "org.telegram.messenger");
        appMap.put("truecaller", "com.truecaller");
        appMap.put("zoom", "us.zoom.videomeetings");
        appMap.put("phone", "com.android.dialer");
        appMap.put("messages", "com.google.android.apps.messaging");

// ===== Bangladeshi Apps =====
        appMap.put("bkash", "com.bKash.customerapp");
        appMap.put("nagad", "com.konasl.nagad");
        appMap.put("mygp", "com.portonics.mygp");
        appMap.put("mybl", "com.arena.banglalinkmela.app");
        appMap.put("rail sheba", "com.shohoz.tracerail");

// ===== Ride & Delivery =====
        appMap.put("uber", "com.ubercab");
        appMap.put("foodpanda", "com.global.foodpanda.android");

// ===== Utility Apps =====
        appMap.put("calculator", "com.android.calculator2");
        appMap.put("clock", "com.android.deskclock");
        appMap.put("camera", "com.android.camera");
        appMap.put("settings", "com.android.settings");
        appMap.put("files", "com.android.documentsui");
        appMap.put("weather", "com.miui.weather2"); // device অনুযায়ী ভিন্ন হতে পারে
        appMap.put("compass", "com.miui.compass");

// ===== Others =====
        appMap.put("linkedin", "com.linkedin.android");
        appMap.put("w3schools", "com.w3schools.app");
        appMap.put("wps", "cn.wps.moffice_eng");
        appMap.put("duolingo", "com.duolingo");
        appMap.put("daraz", "com.daraz.android");
        appMap.put("efootball", "jp.konami.pesam");
        appMap.put("fitbit", "com.fitbit.FitbitMobile");
        appMap.put("music", "com.miui.player");
        // ===== Google Apps =====
        appMap.put("ক্রোম", "com.android.chrome");
        appMap.put("গুগল", "com.google.android.googlequicksearchbox");
        appMap.put("জিমেইল", "com.google.android.gm");
        appMap.put("ইউটিউব", "com.google.android.youtube");
        appMap.put("ইউটিউব মিউজিক", "com.google.android.apps.youtube.music");
        appMap.put("ম্যাপস", "com.google.android.apps.maps");
        appMap.put("ড্রাইভ", "com.google.android.apps.docs");
        appMap.put("ফটোস", "com.google.android.apps.photos");
        appMap.put("প্লে স্টোর", "com.android.vending");
        appMap.put("ক্যালেন্ডার", "com.google.android.calendar");
        appMap.put("মিট", "com.google.android.apps.meetings");
        appMap.put("ক্লাসরুম", "com.google.android.apps.classroom");

// ===== Meta Apps =====
        appMap.put("ফেসবুক", "com.facebook.katana");
        appMap.put("মেসেঞ্জার", "com.facebook.orca");
        appMap.put("ইনস্টাগ্রাম", "com.instagram.android");
        appMap.put("হোয়াটসঅ্যাপ", "com.whatsapp");

// ===== Communication =====
        appMap.put("টেলিগ্রাম", "org.telegram.messenger");
        appMap.put("ট্রুকলার", "com.truecaller");
        appMap.put("জুম", "us.zoom.videomeetings");
        appMap.put("ফোন", "com.android.dialer");
        appMap.put("মেসেজ", "com.google.android.apps.messaging");

// ===== Bangladeshi Apps =====
        appMap.put("বিকাশ", "com.bKash.customerapp");
        appMap.put("নগদ", "com.konasl.nagad");
        appMap.put("মাই জিপি", "com.portonics.mygp");
        appMap.put("মাই বিএল", "com.arena.banglalinkmela.app");
        appMap.put("রেল সেবা", "com.shohoz.tracerail");

// ===== Ride & Delivery =====
        appMap.put("উবার", "com.ubercab");
        appMap.put("ফুডপান্ডা", "com.global.foodpanda.android");

// ===== Utility =====
        appMap.put("ক্যালকুলেটর", "com.android.calculator2");
        appMap.put("ঘড়ি", "com.android.deskclock");
        appMap.put("ক্যামেরা", "com.android.camera");
        appMap.put("সেটিংস", "com.android.settings");
        appMap.put("ফাইলস", "com.android.documentsui");
        appMap.put("আবহাওয়া", "com.miui.weather2"); // device অনুযায়ী আলাদা হতে পারে
        appMap.put("কম্পাস", "com.miui.compass");

// ===== Others =====
        appMap.put("লিংকডইন", "com.linkedin.android");
        appMap.put("ডব্লিউ থ্রি স্কুলস", "com.w3schools.app");
        appMap.put("ডব্লিউপিএস", "cn.wps.moffice_eng");
        appMap.put("ডুওলিংগো", "com.duolingo");
        appMap.put("দারাজ", "com.daraz.android");
        appMap.put("ই ফুটবল", "jp.konami.pesam");
        appMap.put("ফিটবিট", "com.fitbit.FitbitMobile");
        appMap.put("মিউজিক", "com.miui.player");
    }

    public ActionResult route(String rawText) {
        String text = normalize(rawText);
        if (text.isEmpty()) return ActionResult.ok("কিছু শুনতে পেলাম না। আবার বলুন।");

        // 1) custom
        String custom = matchCustom(text);
        if (custom != null) return executeCustom(custom);

        // 2) rules
        ActionResult r;
        r = handleGreeting(text); if (r.handled) return r;
        r = handleTimeDate(text); if (r.handled) return r;
        r = handleBattery(text); if (r.handled) return r;
        r = handleFlash(text); if (r.handled) return r;
        r = handleWifi(text); if (r.handled) return r;
        r = handleVolume(text); if (r.handled) return r;
        r = handleOpenApp(text); if (r.handled) return r;
        r = handleCall(text); if (r.handled) return r;
        r = handleSettings(text); if (r.handled) return r;

        return ActionResult.no();
    }

    private String normalize(String t) {
        if (t == null) return "";
        t = t.toLowerCase(Locale.ROOT).trim();
        t = t.replace("।", "").replace("?", "").replace("!", "")
                .replace(",", " ").replace(";", " ");
        String[] fillers = {"একটু", "তো", "প্লিজ", "please", "দয়া করে", "করে দাও", "দাও"};
        for (String f : fillers) t = t.replace(f, " ");
        return t.replaceAll("\s+", " ").trim();
    }

    private boolean hasAny(String text, String... keys) {
        for (String k : keys) if (text.contains(k)) return true;
        return false;
    }

    private String matchCustom(String text) {
        List<String[]> cmds = commandEngine.getCustomCommands();
        for (String[] c : cmds) {
            if (c == null || c.length < 2) continue;
            String phrase = c[0] == null ? "" : normalize(c[0]);
            String action = c[1] == null ? "" : c[1].trim();
            if (!phrase.isEmpty() && (text.equals(phrase) || text.contains(phrase))) return action;
        }
        return null;
    }

    private ActionResult executeCustom(String action) {
        String a = action.trim();
        if (a.startsWith("SAY:")) return ActionResult.ok(a.substring(4).trim());
        if (a.startsWith("OPEN_APP:")) {
            String pkg = a.substring("OPEN_APP:".length()).trim();
            device.openApp(pkg);
            return ActionResult.ok("অ্যাপ খুলছি ✅");
        }
        switch (a) {
            case "FLASH_ON": device.toggleFlash(true); return ActionResult.ok("ফ্ল্যাশ চালু 🔦");
            case "FLASH_OFF": device.toggleFlash(false); return ActionResult.ok("ফ্ল্যাশ বন্ধ");
            case "WIFI_ON": device.setWifi(true); return ActionResult.ok("WiFi সেটিংস খুলছি 📶");
            case "WIFI_OFF": device.setWifi(false); return ActionResult.ok("WiFi সেটিংস খুলছি");
            case "VOLUME_UP": device.volumeUp(); return ActionResult.ok("ভলিউম বাড়াচ্ছি 🔊");
            case "VOLUME_DOWN": device.volumeDown(); return ActionResult.ok("ভলিউম কমাচ্ছি 🔉");
            case "VOLUME_MUTE": device.volumeMute(); return ActionResult.ok("নীরব মোড 🔇");
            default: return ActionResult.ok("✅ Custom: " + a);
        }
    }

    private ActionResult handleGreeting(String t) {
        if (hasAny(t, "হ্যালো", "হাই", "hello", "hi", "আসসালামু", "সালাম")) {
            return ActionResult.ok("হ্যালো! বলুন—আমি কী করতে পারি? 😊");
        }
        return ActionResult.no();
    }

    private ActionResult handleTimeDate(String t) {
        if (hasAny(t, "সময়", "টাইম", "কয়টা", "time")) {
            return ActionResult.ok("এখন সময় " + new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date()));
        }
        if (hasAny(t, "তারিখ", "ডেট", "date", "আজকে কত")) {
            return ActionResult.ok("আজ " + new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(new Date()));
        }
        return ActionResult.no();
    }

    private ActionResult handleBattery(String t) {
        if (!hasAny(t, "ব্যাটারি", "চার্জ", "battery")) return ActionResult.no();
        BatteryManager bm = (BatteryManager) ctx.getSystemService(Context.BATTERY_SERVICE);
        if (bm == null) return ActionResult.ok("ব্যাটারি তথ্য পাচ্ছি না।");
        int pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        return ActionResult.ok("ব্যাটারি আছে " + pct + " শতাংশ 🔋");
    }

    private ActionResult handleFlash(String t) {
        if (!hasAny(t, "ফ্ল্যাশ", "টর্চ", "আলো", "torch")) return ActionResult.no();
        if (hasAny(t, "চালু", "অন", "জ্বাল", "জাল")) {
            device.toggleFlash(true);
            return ActionResult.ok("ফ্ল্যাশ চালু 🔦");
        }
        if (hasAny(t, "বন্ধ", "অফ", "নিভ")) {
            device.toggleFlash(false);
            return ActionResult.ok("ফ্ল্যাশ বন্ধ");
        }
        return ActionResult.ok("ফ্ল্যাশ অন নাকি অফ করতে চান?");
    }

    private ActionResult handleWifi(String t) {
        if (!hasAny(t, "ওয়াইফাই", "wifi", "নেট")) return ActionResult.no();
        device.setWifi(true);
        return ActionResult.ok("WiFi সেটিংস খুলছি 📶");
    }

    private ActionResult handleVolume(String t) {
        if (!hasAny(t, "ভলিউম", "সাউন্ড", "আওয়াজ", "volume")) return ActionResult.no();
        if (hasAny(t, "বাড়", "উচ্চ", "বেশি")) {
            device.volumeUp();
            return ActionResult.ok("ভলিউম বাড়াচ্ছি 🔊");
        }
        if (hasAny(t, "কম", "নিচু")) {
            device.volumeDown();
            return ActionResult.ok("ভলিউম কমাচ্ছি 🔉");
        }
        if (hasAny(t, "মিউট", "চুপ", "নীরব")) {
            device.volumeMute();
            return ActionResult.ok("নীরব মোড 🔇");
        }
        return ActionResult.ok("ভলিউম বাড়াব নাকি কমাব?");
    }
    private ActionResult handleOpenApp(String t) {
        if (!hasAny(t, "খোল", "ওপেন", "open", "launch", "চালু")) return ActionResult.no();
        for (Map.Entry<String, String> e : appMap.entrySet()) {
            if (t.contains(e.getKey())) {
                String target = e.getValue();
                if (target.startsWith("android.")) {
                    Intent i = new Intent(target);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(i);
                    return ActionResult.ok("খুলছি ✅");
                } else {
                    device.openApp(target);
                    return ActionResult.ok("অ্যাপ খুলছি ✅");
                }
            }
        }
        return ActionResult.no();
    }

    // Google-assistant style: call ammy/mom/dad support
    private ActionResult handleCall(String t) {
        if (!hasAny(t, "কল", "ফোন", "call", "dial")) return ActionResult.no();

        if (hasAny(t, "আম্মু", "মা", "mom", "ammy", "mummy")) {
            return callByAlias("মা");
        }
        if (hasAny(t, "আব্বু", "বাবা", "dad", "abbu")) {
            return callByAlias("বাবা");
        }

        String name = t;
        String[] rm = {"কল", "ফোন", "call", "dial", "কর", "করে", "দাও", "কে"};
        for (String r : rm) name = name.replace(r, " ");
        name = name.replaceAll("\s+", " ").trim();

        if (name.isEmpty()) return ActionResult.ok("কাকে কল করব? নাম বলুন।");

        ContactMatcher.MatchResult match = contactMatcher.findContact(name);
        if (match == null) return ActionResult.ok("“" + name + "” নামের কনট্যাক্ট পাইনি।");

        device.makeCall(match.phone);
        return ActionResult.ok(match.name + " কে কল করছি 📞");
    }

    private ActionResult callByAlias(String alias) {
        ContactMatcher.MatchResult match = contactMatcher.findContact(alias);
        if (match == null) return ActionResult.ok(alias + " নামের কনট্যাক্ট পাইনি।");
        device.makeCall(match.phone);
        return ActionResult.ok(match.name + " কে কল করছি 📞");
    }

    private ActionResult handleSettings(String t) {
        if (!hasAny(t, "সেটিংস", "settings")) return ActionResult.no();
        try {
            Intent i = new Intent(Settings.ACTION_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            return ActionResult.ok("সেটিংস খুলছি");
        } catch (Exception e) {
            return ActionResult.ok("সেটিংস খুলতে পারিনি।");
        }
    }
}
