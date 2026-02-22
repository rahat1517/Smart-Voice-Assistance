package com.example.rahatassistant.skills;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;

public class FlashlightSkill {

    public static void setTorch(Context ctx, boolean on) {
        CameraManager cm = (CameraManager) ctx.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String id : cm.getCameraIdList()) {
                CameraCharacteristics cc = cm.getCameraCharacteristics(id);
                Boolean hasFlash = cc.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer facing = cc.get(CameraCharacteristics.LENS_FACING);
                if (Boolean.TRUE.equals(hasFlash)
                        && facing != null
                        && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cm.setTorchMode(id, on);
                    return;
                }
            }
        } catch (CameraAccessException ignored) {}
    }
}
