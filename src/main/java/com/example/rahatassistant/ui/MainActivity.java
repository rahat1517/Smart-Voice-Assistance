package com.example.rahatassistant.ui;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.rahatassistant.R;
import com.example.rahatassistant.service.VoiceAssistantService;
import com.example.rahatassistant.utils.PermissionUtils;

import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private TextView statusTv;

    private final String[] REQ_PERMS = new String[]{
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
    };

    private ActivityResultLauncher<String[]> permsLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusTv = findViewById(R.id.statusTv);
        Button startBtn = findViewById(R.id.startBtn);
        Button stopBtn = findViewById(R.id.stopBtn);
        Button testVibeBtn = findViewById(R.id.testVibeBtn);

        permsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                (Map<String, Boolean> result) -> {
                    boolean ok = true;
                    for (Boolean b : result.values()) ok = ok && Boolean.TRUE.equals(b);

                    if (ok) {
                        startAssistantService();
                        statusTv.setText("Service started. Say: “hey/hi/he rahat …” 🎤");
                    } else {
                        statusTv.setText("Permissions needed ❌ (Mic + Camera)");
                    }
                }
        );

        startBtn.setOnClickListener(view -> {
            if (PermissionUtils.hasAll(this, REQ_PERMS)) {
                startAssistantService();
                statusTv.setText("Service started. Say: “hey/hi/he rahat …” 🎤");
            } else {
                permsLauncher.launch(REQ_PERMS);
            }
        });

        stopBtn.setOnClickListener(view -> {
            stopAssistantService();
            statusTv.setText("Service stopped.");
        });

        testVibeBtn.setOnClickListener(view -> {
            Vibrator vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vib == null) {
                statusTv.setText("No vibrator hardware ❌");
                return;
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vib.vibrate(VibrationEffect.createOneShot(200, 255));
                } else {
                    vib.vibrate(200);
                }
                statusTv.setText("Vibrate test triggered ✅");
            } catch (Exception e) {
                statusTv.setText("Vibrate failed: " + e.getMessage());
            }
        });
    }

    private void startAssistantService() {
        Intent i = new Intent(this, VoiceAssistantService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, i);
        } else {
            startService(i);
        }
    }

    private void stopAssistantService() {
        stopService(new Intent(this, VoiceAssistantService.class));
    }
}
