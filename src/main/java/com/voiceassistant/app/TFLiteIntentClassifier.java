package com.voiceassistant.app;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TFLiteIntentClassifier {

    private static final String TAG = "TFLiteAI";
    private static final String MODEL_FILE  = "voice_intent_model.tflite";
    private static final String VOCAB_FILE  = "vocab.json";
    private static final String LABELS_FILE = "label_encoder.json";

    private static final int MAX_LEN = 20;
    private static final float CONFIDENCE_THRESHOLD = 0.55f;

    private Interpreter interpreter;
    private Map<String, Integer> word2idx = new HashMap<>();
    private Map<Integer, String> idx2label = new HashMap<>();
    private int numClasses = 0;
    private boolean ready = false;

    public TFLiteIntentClassifier(Context context) {
        try {
            // Helpful debug
            String[] root = context.getAssets().list("");
            Log.i(TAG, "ASSETS ROOT = " + java.util.Arrays.toString(root));

            loadModel(context);
            loadVocabulary(context);
            loadLabels(context);
            ready = true;
            Log.i(TAG, "✅ AI Model loaded. classes=" + numClasses);
        } catch (Exception e) {
            Log.e(TAG, "❌ Model load failed: " + e.getMessage(), e);
            ready = false;
        }
    }

    public boolean isReady() { return ready; }

    public IntentResult classify(String text) {
        if (!ready) {
            return new IntentResult("UNKNOWN", "", 0f,
                    "AI প্রস্তুত নয়। Custom/Rule কমান্ড ব্যবহার করুন।");
        }
        int[] encoded = encodeText(text);

        float[][] output = new float[1][numClasses];
        int[][] input = new int[1][MAX_LEN];
        input[0] = encoded;

        try {
            interpreter.run(input, output);
        } catch (Exception e) {
            return new IntentResult("UNKNOWN", "", 0f, "AI error: " + e.getMessage());
        }

        int best = 0;
        float bestP = 0f;
        for (int i = 0; i < output[0].length; i++) {
            if (output[0][i] > bestP) {
                bestP = output[0][i];
                best = i;
            }
        }
        String intent = idx2label.getOrDefault(best, "UNKNOWN");
        float conf = bestP;

        if (conf < CONFIDENCE_THRESHOLD) {
            return new IntentResult("UNKNOWN", "", conf, "বুঝতে পারিনি। আবার বলুন।");
        }
        return new IntentResult(intent, "", conf, "AI: " + intent);
    }

    private int[] encodeText(String text) {
        String normalized = (text == null ? "" : text.toLowerCase())
                .replace("।", "").replace("?", "").replace("!", "").trim();
        String[] tokens = normalized.isEmpty() ? new String[0] : normalized.split("\s+");

        int[] ids = new int[MAX_LEN];
        for (int i = 0; i < Math.min(tokens.length, MAX_LEN); i++) {
            ids[i] = word2idx.getOrDefault(tokens[i], 1);
        }
        return ids;
    }

    private void loadModel(Context context) throws IOException {
        AssetFileDescriptor fd = context.getAssets().openFd(MODEL_FILE);
        FileInputStream fis = new FileInputStream(fd.getFileDescriptor());
        FileChannel channel = fis.getChannel();
        MappedByteBuffer buffer = channel.map(
                FileChannel.MapMode.READ_ONLY,
                fd.getStartOffset(),
                fd.getDeclaredLength()
        );
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(2);
        interpreter = new Interpreter(buffer, options);
        Log.i(TAG, "Model loaded from assets/" + MODEL_FILE);
    }

    private void loadVocabulary(Context context) throws IOException, JSONException {
        String json = readAsset(context, VOCAB_FILE);
        JSONObject obj = new JSONObject(json);
        JSONObject w2i = obj.getJSONObject("word2idx");
        Iterator<String> keys = w2i.keys();
        while (keys.hasNext()) {
            String word = keys.next();
            word2idx.put(word, w2i.getInt(word));
        }
        Log.i(TAG, "Vocabulary loaded: " + word2idx.size());
    }

    private void loadLabels(Context context) throws IOException, JSONException {
        String json = readAsset(context, LABELS_FILE);
        JSONObject obj = new JSONObject(json);
        JSONObject i2l = obj.getJSONObject("idx2label");
        Iterator<String> keys = i2l.keys();
        while (keys.hasNext()) {
            String idxStr = keys.next();
            idx2label.put(Integer.parseInt(idxStr), i2l.getString(idxStr));
        }
        numClasses = idx2label.size();
        Log.i(TAG, "Labels loaded: " + idx2label);
    }

    private String readAsset(Context context, String filename) throws IOException {
        InputStream is = context.getAssets().open(filename);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        return sb.toString();
    }

    public void close() {
        if (interpreter != null) interpreter.close();
    }

    public static class IntentResult {
        public final String intent;
        public final String param;
        public final float confidence;
        public final String response;

        public IntentResult(String intent, String param, float confidence, String response) {
            this.intent = intent;
            this.param = param;
            this.confidence = confidence;
            this.response = response;
        }
    }
}
