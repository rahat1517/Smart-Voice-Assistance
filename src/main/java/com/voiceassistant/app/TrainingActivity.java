package com.voiceassistant.app;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class TrainingActivity extends AppCompatActivity {

    private EditText etPhrase, etAction;
    private Button btnAdd;
    private LinearLayout savedCommandsList;
    private CommandEngine commandEngine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training);

        commandEngine     = new CommandEngine(this);
        etPhrase          = findViewById(R.id.etCommandPhrase);
        etAction          = findViewById(R.id.etCommandAction);
        btnAdd            = findViewById(R.id.btnAddCommand);
        savedCommandsList = findViewById(R.id.savedCommandsList);

        btnAdd.setOnClickListener(v -> addCommand());

        loadSavedCommands();
    }

    private void addCommand() {
        String phrase  = etPhrase.getText().toString().trim();
        String action  = etAction.getText().toString().trim();

        if (phrase.isEmpty() || action.isEmpty()) {
            Toast.makeText(this, "Phrase এবং Action দুটোই দিন", Toast.LENGTH_SHORT).show();
            return;
        }

        commandEngine.addCustomCommand(phrase, action);
        etPhrase.setText("");
        etAction.setText("");
        Toast.makeText(this, "✅ Command যোগ হয়েছে!", Toast.LENGTH_SHORT).show();
        loadSavedCommands();
    }

    private void loadSavedCommands() {
        savedCommandsList.removeAllViews();
        List<String[]> commands = commandEngine.getCustomCommands();

        if (commands.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("এখনো কোনো custom command নেই।");
            empty.setTextColor(Color.parseColor("#8B949E"));
            empty.setPadding(12, 12, 12, 12);
            savedCommandsList.addView(empty);
            return;
        }

        for (String[] cmd : commands) {
            if (cmd == null || cmd.length < 2) continue;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, 8, 0, 8);

            LinearLayout textBlock = new LinearLayout(this);
            textBlock.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            textBlock.setLayoutParams(params);

            TextView phrase = new TextView(this);
            phrase.setText("🗣️ " + cmd[0]);
            phrase.setTextColor(Color.parseColor("#E6EDF3"));
            phrase.setTextSize(14f);

            TextView action = new TextView(this);
            action.setText("⚡ " + cmd[1]);
            action.setTextColor(Color.parseColor("#58A6FF"));
            action.setTextSize(12f);

            textBlock.addView(phrase);
            textBlock.addView(action);

            Button btnDelete = new Button(this);
            btnDelete.setText("🗑️");
            btnDelete.setTextSize(16f);
            btnDelete.setBackgroundColor(Color.TRANSPARENT);
            btnDelete.setTextColor(Color.parseColor("#F85149"));
            btnDelete.setOnClickListener(v -> {
                commandEngine.deleteCustomCommand(cmd[0]);
                Toast.makeText(this, "Command মুছে গেছে", Toast.LENGTH_SHORT).show();
                loadSavedCommands();
            });

            row.addView(textBlock);
            row.addView(btnDelete);

            View line = new View(this);
            line.setBackgroundColor(Color.parseColor("#21262D"));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1);
            lp.setMargins(0, 4, 0, 4);
            line.setLayoutParams(lp);

            savedCommandsList.addView(row);
            savedCommandsList.addView(line);
        }
    }
}
