package com.gathin.keyremap;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class KeyRemapService extends AccessibilityService {
    private static final int KEY_YOUTUBE = 4065;
    private static final int KEY_TCL_CHANNEL = 4020;
    private static final String PKG_TIZENTUBE = "io.gh.reisxd.tizentube.cobalt";
    private static final String PKG_TCL_TV = "com.tcl.tv";

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        if (event == null) {
            return super.onKeyEvent(event);
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int code = event.getKeyCode();
            if (code == KEY_YOUTUBE) {
                launch(PKG_TIZENTUBE);
                return true;
            } else if (code == KEY_TCL_CHANNEL) {
                launch(PKG_TCL_TV);
                return true;
            }
        }
        return super.onKeyEvent(event);
    }

    private void launch(String pkg) {
        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage(pkg);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                startActivity(intent);
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }
}
