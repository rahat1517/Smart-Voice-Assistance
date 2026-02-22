package com.example.rahatassistant.utils;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

public class PermissionUtils {

    public static boolean hasAll(Context ctx, String[] perms) {
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(ctx, p) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
