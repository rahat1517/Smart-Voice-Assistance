package com.example.rahatassistant.nlu;

import android.content.Context;
import android.widget.Toast;

import com.example.rahatassistant.skills.FlashlightSkill;
import com.example.rahatassistant.skills.VolumeSkill;

public class CommandRouter {

    public static void execute(Context ctx, String rawText) {
        CommandParser.IntentType intent = CommandParser.detect(rawText);

        switch (intent) {
            case FLASH_ON:
                FlashlightSkill.setTorch(ctx, true);
                toast(ctx, "Flashlight ON");
                break;
            case FLASH_OFF:
                FlashlightSkill.setTorch(ctx, false);
                toast(ctx, "Flashlight OFF");
                break;
            case VOL_UP:
                VolumeSkill.raise(ctx);
                toast(ctx, "Volume UP");
                break;
            case VOL_DOWN:
                VolumeSkill.lower(ctx);
                toast(ctx, "Volume DOWN");
                break;
            default:
                toast(ctx, "Command not recognized: " + rawText);
        }
    }

    private static void toast(Context ctx, String msg) {
        Toast.makeText(ctx.getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
