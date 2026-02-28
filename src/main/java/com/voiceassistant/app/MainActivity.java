package com.voiceassistant.app;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ImageButton btnMic;
    private Button btnTraining, btnFlash, btnWifi, btnVolUp, btnVolDown;
    private TextView tvStatus, tvRecognized, tvResponse, tvAIStatus;
    private LinearLayout historyContainer;
    private View ringOuter, ringMiddle;

    private TFLiteIntentClassifier aiClassifier;  // optional
    private ContactMatcher contactMatcher;
    private DeviceController deviceController;
    private TextToSpeech tts;
    private CommandEngine commandEngine;
    private CommandRouter router;

    private SpeechRecognizer recognizer;

    private boolean isListening = false;
    private boolean isFlashOn = false;
    private Handler handler = new Handler();
    private ObjectAnimator pulseOuter, pulseMiddle;
    private static final int PERM_REQUEST = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnMic = findViewById(R.id.btnMic);
        btnTraining = findViewById(R.id.btnTraining);
        btnFlash = findViewById(R.id.btnFlash);
        btnWifi = findViewById(R.id.btnWifi);
        btnVolUp = findViewById(R.id.btnVolUp);
        btnVolDown = findViewById(R.id.btnVolDown);

        tvStatus = findViewById(R.id.tvStatus);
        tvRecognized = findViewById(R.id.tvRecognized);
        tvResponse = findViewById(R.id.tvResponse);
        tvAIStatus = findViewById(R.id.tvAIStatus);

        historyContainer = findViewById(R.id.historyContainer);
        ringOuter = findViewById(R.id.ringOuter);
        ringMiddle = findViewById(R.id.ringMiddle);

        contactMatcher = new ContactMatcher(this);
        deviceController = new DeviceController(this);
        commandEngine = new CommandEngine(this);
        router = new CommandRouter(this, deviceController, contactMatcher, commandEngine);

        // Disable mic until permissions granted (and AI optional)
        btnMic.setEnabled(true);

        // Load AI (optional)
        tvAIStatus.setText("⏳ AI Model loading (optional)...");
        new Thread(() -> {
            aiClassifier = new TFLiteIntentClassifier(this);
            runOnUiThread(() -> {
                if (aiClassifier != null && aiClassifier.isReady()) {
                    tvAIStatus.setText("🧠 AI Ready");
                    tvAIStatus.setTextColor(Color.parseColor("#3FB950"));
                } else {
                    tvAIStatus.setText("⚠️ AI not ready (Rules + Custom works)");
                    tvAIStatus.setTextColor(Color.parseColor("#F85149"));
                }
            });
        }).start();

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("bn", "BD"));
                if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.ENGLISH);
                }
            }
        });

        setupButtons();
        setupPulseAnimation();
        initSpeechRecognizer();
        checkAndRequestPermissions();
    }

    private void initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            showToast("Speech recognition নেই");
            return;
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle b) {
                isListening = true;
                setListeningUI(true);
            }
            @Override public void onBeginningOfSpeech() {
                tvStatus.setText("শুনছি... 🎙️");
            }
            @Override public void onRmsChanged(float rms) {
                float scale = 1f + Math.max(0, rms / 25f);
                ringOuter.setScaleX(scale);
                ringOuter.setScaleY(scale);
            }
            @Override public void onEndOfSpeech() {
                tvStatus.setText("প্রসেস হচ্ছে...");
            }
            @Override public void onError(int error) {
                isListening = false;
                setListeningUI(false);
                tvStatus.setText("আবার বলুন");
                handler.postDelayed(() -> tvStatus.setText("বলুন..."), 1200);
            }
            @Override public void onResults(Bundle results) {
                isListening = false;
                setListeningUI(false);
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String recognized = matches.get(0);
                    tvRecognized.setText("\"" + recognized + "\"");
                    processInput(recognized);
                }
            }
            @Override public void onPartialResults(Bundle b) {}
            @Override public void onBufferReceived(byte[] b) {}
            @Override public void onEvent(int i, Bundle b) {}
        });
    }

    private void startListening() {
        if (recognizer == null) {
            showToast("Speech recognizer init হয়নি");
            return;
        }
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        recognizer.startListening(intent);
    }

    private void processInput(String input) {
        String raw = input == null ? "" : input.trim();
        tvStatus.setText("Processing...");

        // 1) Router (custom + rules)
        CommandRouter.ActionResult r = router.route(raw);
        if (r.handled) {
            showResponse(r.response);
            speak(r.response);
            addToHistory(raw, "ROUTER", 1.0f);
            handler.postDelayed(() -> tvStatus.setText("বলুন..."), 1500);
            return;
        }

        // 2) AI if ready
        if (aiClassifier != null && aiClassifier.isReady()) {
            processWithAI(raw);
            return;
        }

        // 3) not handled + no AI
        String msg = "AI এখনো প্রস্তুত নয়। Training এ গিয়ে custom command যোগ করতে পারেন ✅";
        showResponse(msg);
        speak(msg);
        addToHistory(raw, "NO_AI", 0.0f);
        handler.postDelayed(() -> tvStatus.setText("বলুন..."), 1500);
    }

    private void processWithAI(String input) {
        tvStatus.setText("🧠 AI চিন্তা করছে...");
        new Thread(() -> {
            TFLiteIntentClassifier.IntentResult result = aiClassifier.classify(input);
            runOnUiThread(() -> {
                showResponse(result.response);
                speak(result.response);
                addToHistory(input, result.intent, result.confidence);
                handler.postDelayed(() -> tvStatus.setText("বলুন..."), 1500);
            });
        }).start();
    }

    private void setupButtons() {
        btnMic.setOnClickListener(v -> {
            if (!isListening) startListening();
        });

        btnTraining.setOnClickListener(v ->
                startActivity(new Intent(this, TrainingActivity.class)));

        btnFlash.setOnClickListener(v -> {
            isFlashOn = !isFlashOn;
            deviceController.toggleFlash(isFlashOn);
            showResponse(isFlashOn ? "ফ্ল্যাশ চালু 🔦" : "ফ্ল্যাশ বন্ধ");
        });

        btnWifi.setOnClickListener(v -> {
            deviceController.setWifi(true);
            showResponse("WiFi settings খুলছি...");
        });

        btnVolUp.setOnClickListener(v -> {
            deviceController.volumeUp();
            showResponse("ভলিউম বাড়ছে 🔊");
        });

        btnVolDown.setOnClickListener(v -> {
            deviceController.volumeDown();
            showResponse("ভলিউম কমছে 🔉");
        });
    }

    private void setListeningUI(boolean listening) {
        if (listening) {
            tvStatus.setText("শুনছি... 🎙️");
            tvStatus.setTextColor(Color.parseColor("#3FB950"));
            startPulse();
        } else {
            ringOuter.setScaleX(1f);
            ringOuter.setScaleY(1f);
            stopPulse();
            tvStatus.setTextColor(Color.parseColor("#58A6FF"));
        }
    }

    private void showResponse(String text) { tvResponse.setText(text); }

    private void speak(String text) {
        if (tts != null) {
            String clean = text.replaceAll("[🔦📶🔊🔉🔇📞✅😊⏳🧠⚠️]", "").trim();
            tts.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "tts1");
        }
    }

    private void addToHistory(String command, String intent, float confidence) {
        View item = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, null);
        TextView t1 = item.findViewById(android.R.id.text1);
        TextView t2 = item.findViewById(android.R.id.text2);

        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        t1.setText(time + "  " + command);
        t1.setTextColor(Color.parseColor("#E6EDF3"));
        t2.setText(String.format(Locale.getDefault(), "🧠 %s (%.0f%%)", intent, confidence * 100));
        t2.setTextColor(Color.parseColor("#58A6FF"));

        historyContainer.addView(item, 0);
        if (historyContainer.getChildCount() > 20)
            historyContainer.removeViewAt(historyContainer.getChildCount() - 1);
    }

    private void setupPulseAnimation() {
        pulseOuter = ObjectAnimator.ofFloat(ringOuter, "alpha", 0.3f, 1f);
        pulseOuter.setDuration(900);
        pulseOuter.setRepeatMode(ValueAnimator.REVERSE);
        pulseOuter.setRepeatCount(ValueAnimator.INFINITE);

        pulseMiddle = ObjectAnimator.ofFloat(ringMiddle, "alpha", 0.5f, 1f);
        pulseMiddle.setDuration(700);
        pulseMiddle.setRepeatMode(ValueAnimator.REVERSE);
        pulseMiddle.setRepeatCount(ValueAnimator.INFINITE);
    }

    private void startPulse() {
        if (pulseOuter != null) pulseOuter.start();
        if (pulseMiddle != null) pulseMiddle.start();
    }

    private void stopPulse() {
        if (pulseOuter != null) pulseOuter.cancel();
        if (pulseMiddle != null) pulseMiddle.cancel();
        ringOuter.setAlpha(1f);
        ringMiddle.setAlpha(1f);
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void checkAndRequestPermissions() {
        List<String> needed = new ArrayList<>();
        String[] perms = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.CAMERA
        };
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                needed.add(p);
            }
        }
        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toArray(new String[0]), PERM_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }
        if (aiClassifier != null) aiClassifier.close();
        if (tts != null) { tts.stop(); tts.shutdown(); }
        if (pulseOuter != null) pulseOuter.cancel();
        if (pulseMiddle != null) pulseMiddle.cancel();
    }
}
