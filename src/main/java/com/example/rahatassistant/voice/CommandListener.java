package com.example.rahatassistant.voice;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;

public class CommandListener {

    public interface Callback {
        void onCommandText(String cmdText);
        void onError(int errorCode);
    }

    private static final String TAG = "CommandListener";

    public static void listenOnce(Context ctx, Callback cb) {
        SpeechRecognizer sr = SpeechRecognizer.createSpeechRecognizer(ctx.getApplicationContext());

        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US");

        // Longer waiting window after wake word
        i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 4000);
        i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 4000);
        i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 800);

        sr.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { Log.d(TAG, "onReadyForSpeech"); }
            @Override public void onBeginningOfSpeech() { Log.d(TAG, "onBeginningOfSpeech"); }
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() { Log.d(TAG, "onEndOfSpeech"); }

            @Override
            public void onError(int error) {
                Log.e(TAG, "onError: " + error);
                try { sr.destroy(); } catch (Exception ignored) {}
                cb.onError(error);
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String text = (data != null && !data.isEmpty()) ? data.get(0) : "";
                Log.d(TAG, "heard cmd: " + text);
                try { sr.destroy(); } catch (Exception ignored) {}
                cb.onCommandText(text);
            }

            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        sr.startListening(i);
    }
}
