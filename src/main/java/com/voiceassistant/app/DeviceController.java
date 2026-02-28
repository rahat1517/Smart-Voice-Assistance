package com.voiceassistant.app;
import android.content.Context;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

public class DeviceController {

    private static final String TAG = "DeviceController";
    private final Context ctx;

    public DeviceController(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    public void toggleFlash(boolean on) {
        try {
            CameraManager cm = (CameraManager) ctx.getSystemService(Context.CAMERA_SERVICE);
            if (cm == null) return;
            for (String id : cm.getCameraIdList()) {
                CameraCharacteristics cc = cm.getCameraCharacteristics(id);
                Boolean hasFlash = cc.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer facing = cc.get(CameraCharacteristics.LENS_FACING);
                if (hasFlash != null && hasFlash && facing != null &&
                        facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cm.setTorchMode(id, on);
                    return;
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Flash error: " + e.getMessage());
        }
    }

    // Android 10+ can't toggle programmatically for normal apps; open settings as fallback
    public void setWifi(boolean enabled) {
        try {
            Intent i = new Intent(Settings.ACTION_WIFI_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
        } catch (Exception ignored) {}
    }

    public void volumeUp() {
        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        if (am != null) am.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
    }

    public void volumeDown() {
        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        if (am != null) am.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
    }

    public void volumeMute() {
        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        if (am != null) am.adjustVolume(AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI);
    }

    public boolean openApp(String pkgOrAction) {
        try {
            if (pkgOrAction == null || pkgOrAction.trim().isEmpty()) return false;

            // action (settings etc)
            if (pkgOrAction.startsWith("android.")) {
                Intent i = new Intent(pkgOrAction);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(i);
                return true;
            }

            android.content.pm.PackageManager pm = ctx.getPackageManager();

            // Installed check
            try {
                pm.getPackageInfo(pkgOrAction, 0);
            } catch (PackageManager.NameNotFoundException e) {
                openPlayStoreDetails(pkgOrAction);
                return false;
            }

            // Launch intent (YouTube home included)
            Intent launch = pm.getLaunchIntentForPackage(pkgOrAction);
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(launch);
                return true;
            }

            // Installed but no launcher (rare)
            openPlayStoreDetails(pkgOrAction);
            return false;

        } catch (Exception e) {
            Log.e(TAG, "openApp error", e);
            return false;
        }
    }

    private void openPlayStoreDetails(String packageName) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + packageName));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
        } catch (ActivityNotFoundException e) {
            Intent i = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
        }
    }
//public void openApp(String pkgOrAction) {
//    try {
//        if (pkgOrAction == null || pkgOrAction.isEmpty()) return;
//
//        // 1) system action
//        if (pkgOrAction.startsWith("android.")) {
//            Intent i = new Intent(pkgOrAction);
//            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            ctx.startActivity(i);
//            return;
//        }
//
//        // 2) if it's NOT a package name, don't treat it as package
//        // package looks like: com.xxx.yyy
//        boolean looksLikePkg = pkgOrAction.contains(".") && !pkgOrAction.contains(" ");
//
//        if (looksLikePkg) {
//            Intent launch = ctx.getPackageManager().getLaunchIntentForPackage(pkgOrAction);
//            if (launch != null) {
//                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                ctx.startActivity(launch);
//                return;
//            }
//            // installed না থাকলে এখানেই থামাও (Play Store-এ না নিয়ে)
//            Log.e(TAG, "App not installed: " + pkgOrAction);
//            return;
//        }
//
//        // 3) only for plain text names -> play store search (optional)
//        Intent i = new Intent(Intent.ACTION_VIEW,
//                Uri.parse("market://search?q=" + Uri.encode(pkgOrAction)));
//        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        ctx.startActivity(i);
//
//    } catch (Exception e) {
//        Log.e(TAG, "openApp error: " + e.getMessage());
//    }
//}
    public void makeCall(String phone) {
        try {
            if (phone == null || phone.trim().isEmpty()) return;
            Intent i = new Intent(Intent.ACTION_CALL);
            i.setData(Uri.parse("tel:" + phone));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
        } catch (Exception e) {
            Log.e(TAG, "call error: " + e.getMessage());
        }
    }
}
