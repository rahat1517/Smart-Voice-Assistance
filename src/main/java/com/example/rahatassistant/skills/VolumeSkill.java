package com.example.rahatassistant.skills;

import android.content.Context;
import android.media.AudioManager;

public class VolumeSkill {

    public static void raise(Context ctx) {
        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
    }

    public static void lower(Context ctx) {
        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
    }
}
