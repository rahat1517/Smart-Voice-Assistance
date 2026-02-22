package com.example.rahatassistant.service;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.example.rahatassistant.nlu.CommandRouter;
import com.example.rahatassistant.utils.NotifUtils;
import com.example.rahatassistant.voice.HotwordListener;

public class VoiceAssistantService extends Service {

    private HotwordListener hotwordListener;
    private final Handler handler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();

        NotifUtils.ensureChannel(this);
        startForeground(NotifUtils.NOTIF_ID, NotifUtils.build(this, "Say: “hi/hey/he rahat <command>”"));

        hotwordListener = new HotwordListener(this, new HotwordListener.Callback() {
            @Override
            public void onHotwordDetected(String fullText, String afterWakeText) {
                cueBeep();

                // We require full command in same sentence (most reliable)
                if (afterWakeText == null || afterWakeText.trim().isEmpty()) {
                    NotifUtils.notify(VoiceAssistantService.this,
                            "Say full command: “hi rahat turn on flashlight”");
                    // Restart listening quickly
                    handler.postDelayed(() -> hotwordListener.start(), 400);
                    return;
                }

                // Execute inline command
                CommandRouter.execute(VoiceAssistantService.this, afterWakeText);
                NotifUtils.notify(VoiceAssistantService.this, "Done ✅ Say: “hi/hey/he rahat <command>”");

                handler.postDelayed(() -> hotwordListener.start(), 500);
            }

            @Override
            public void onHotwordState(String msg) {
                NotifUtils.notify(VoiceAssistantService.this, msg);
            }
        });

        hotwordListener.start();
    }

    private void cueBeep() {
        try {
            ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90);
            tg.startTone(ToneGenerator.TONE_PROP_BEEP, 120);
        } catch (Exception ignored) {}
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }

    @Override
    public void onDestroy() {
        if (hotwordListener != null) hotwordListener.stop();
        super.onDestroy();
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}
