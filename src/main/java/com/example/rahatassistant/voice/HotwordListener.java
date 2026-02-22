package com.example.rahatassistant.voice;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HotwordListener {

    public interface Callback {
        void onHotwordDetected(String fullText, String commandText);
        void onHotwordState(String msg);
    }

    private static final String TAG = "HotwordListener";

    // Wakeword (optional)
    private static final Pattern WAKE_PATTERN =
            Pattern.compile("\\b(?:hey|hi|he)?\\s*(rahat|raha|raat|chahat)\\b\\s*(.*)$",
                    Pattern.CASE_INSENSITIVE);

    // ✅ Direct flashlight command pattern (wakeword not needed)
    // Supports:
    // "flashlight on/off", "flash light on/off"
    // "turn on/off flashlight", "turn on/off the flashlight"
    // "torch on/off", "turn on/off torch"
    private static final Pattern FLASHLIGHT_PATTERN =
            Pattern.compile("\\b(?:turn\\s+)?(on|off)\\s+(?:the\\s+)?(?:flash\\s*light|torch)\\b|\\b(?:flash\\s*light|torch)\\s+(on|off)\\b",
                    Pattern.CASE_INSENSITIVE);

    private final Context ctx;
    private final Callback cb;

    private SpeechRecognizer sr;
    private Intent intent;

    private volatile boolean running = false;

    private boolean isListening = false;
    private boolean restartScheduled = false;

    private boolean beganSpeech = false;
    private long readyAtMs = 0;

    private final Handler handler = new Handler(Looper.getMainLooper());

    public HotwordListener(Context ctx, Callback cb) {
        this.ctx = ctx.getApplicationContext();
        this.cb = cb;
        buildIntent();
        createRecognizer();
    }

    private void buildIntent() {
        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, ctx.getPackageName());

        // Force English
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US");

        // Try 5 seconds window (device dependent)
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 5000);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000);
    }

    private void createRecognizer() {
        if (sr != null) return;

        sr = SpeechRecognizer.createSpeechRecognizer(ctx);
        sr.setRecognitionListener(new RecognitionListener() {

            @Override
            public void onReadyForSpeech(Bundle params) {
                isListening = true;
                restartScheduled = false;

                beganSpeech = false;
                readyAtMs = SystemClock.uptimeMillis();

                cb.onHotwordState("Listening… say: “flashlight on/off” or “turn on flashlight”");
                Log.d(TAG, "onReadyForSpeech");

                // Give user up to 5 seconds to start speaking
                handler.postDelayed(() -> {
                    if (!running) return;
                    if (!isListening) return;
                    if (beganSpeech) return;

                    Log.d(TAG, "No speech within 5s -> restarting");
                    isListening = false;
                    try { if (sr != null) sr.stopListening(); } catch (Exception ignored) {}
                    scheduleRestart(300);
                }, 5000);
            }

            @Override
            public void onBeginningOfSpeech() {
                beganSpeech = true;
                Log.d(TAG, "onBeginningOfSpeech");
            }

            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                isListening = false;
                Log.d(TAG, "onEndOfSpeech");
            }

            @Override
            public void onError(int error) {
                isListening = false;
                Log.e(TAG, "onError: " + error);

                if (!running) return;

                long aliveMs = SystemClock.uptimeMillis() - readyAtMs;
                if (aliveMs < 900) {
                    scheduleRestart(2000);
                    return;
                }

                if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                    scheduleRestart(1500);
                } else if (error == SpeechRecognizer.ERROR_CLIENT) {
                    recreateRecognizer();
                    scheduleRestart(1800);
                } else {
                    scheduleRestart(1200);
                }
            }

            @Override
            public void onResults(Bundle results) {
                isListening = false;
                if (!running) return;

                handleResults(results);

                if (running) scheduleRestart(600);
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                if (!running) return;
                handleResults(partialResults);
            }

            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    private void handleResults(Bundle bundle) {
        ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (data == null || data.isEmpty()) return;

        String text = data.get(0);
        if (text == null) return;

        String norm = normalize(text);
        if (norm.isEmpty()) return;

        Log.d(TAG, "heard: " + text + " | norm=" + norm);

        // ✅ 1) Direct flashlight command (no wakeword needed)
        String cmd = extractFlashlightCommand(norm);
        if (!cmd.isEmpty()) {
            Log.d(TAG, "✅ FLASHLIGHT COMMAND: " + cmd);
            cb.onHotwordDetected(norm, cmd);
            return;
        }

        // ✅ 2) Wakeword-based fallback (optional)
        Matcher m = WAKE_PATTERN.matcher(norm);
        if (m.find()) {
            String after = (m.group(2) != null) ? m.group(2).trim() : "";
            Log.d(TAG, "🔥 WAKE DETECTED! after=" + after);

            if (!after.isEmpty()) {
                cb.onHotwordDetected(norm, after);
            } else {
                cb.onHotwordState("Wake detected. Say a command.");
            }
        }
    }

    private String extractFlashlightCommand(String norm) {
        Matcher fm = FLASHLIGHT_PATTERN.matcher(norm);
        if (!fm.find()) return "";

        // pattern has 2 possible groups for on/off
        String state = fm.group(1);
        if (state == null) state = fm.group(2);

        if ("on".equalsIgnoreCase(state)) return "turn on flashlight";
        if ("off".equalsIgnoreCase(state)) return "turn off flashlight";
        return "";
    }

    private String normalize(String s) {
        s = s.toLowerCase().trim();
        s = s.replaceAll("[\\p{Punct}]+", " ");
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }

    public void start() {
        running = true;
        createRecognizer();
        scheduleRestart(0);
    }

    private void safeStart() {
        if (!running || sr == null) return;
        if (isListening) return;

        try {
            sr.startListening(intent);
        } catch (Exception e) {
            Log.e(TAG, "safeStart exception: " + e);
            recreateRecognizer();
            scheduleRestart(1500);
        }
    }

    private void scheduleRestart(long delayMs) {
        if (!running) return;
        if (restartScheduled) return;

        restartScheduled = true;
        handler.postDelayed(() -> {
            restartScheduled = false;
            safeStart();
        }, delayMs);
    }

    private void recreateRecognizer() {
        try {
            if (sr != null) {
                sr.cancel();
                sr.destroy();
            }
        } catch (Exception ignored) {}
        sr = null;
        isListening = false;
        createRecognizer();
    }

    public void stop() {
        running = false;
        restartScheduled = false;
        isListening = false;

        try {
            if (sr != null) {
                sr.cancel();
                sr.destroy();
            }
        } catch (Exception ignored) {}
        sr = null;
    }
}