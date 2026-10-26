package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

final class AppCustom {
    private static final String PREFS = "app_custom";
    private static final String KEY_LABEL = "label_";
    private static final String KEY_ICON = "icon_";
    private static final String DIR_ICONS = "custom_icons";

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static String getLabel(Context ctx, String pkg) {
        if (pkg == null) return null;
        String v = prefs(ctx).getString(KEY_LABEL + pkg, null);
        if (v == null || v.trim().isEmpty()) return null;
        return v;
    }

    static void setLabel(Context ctx, String pkg, String label) {
        if (pkg == null) return;
        SharedPreferences.Editor e = prefs(ctx).edit();
        if (label == null || label.trim().isEmpty()) {
            e.remove(KEY_LABEL + pkg);
        } else {
            e.putString(KEY_LABEL + pkg, label.trim());
        }
        e.apply();
    }

    static Drawable getIcon(Context ctx, String pkg) {
        if (pkg == null) return null;
        String name = prefs(ctx).getString(KEY_ICON + pkg, null);
        if (name == null) return null;
        File f = new File(new File(ctx.getFilesDir(), DIR_ICONS), name);
        if (!f.exists()) return null;
        Bitmap bmp = BitmapFactory.decodeFile(f.getAbsolutePath());
        if (bmp == null) return null;
        return new BitmapDrawable(ctx.getResources(), bmp);
    }

    static boolean setIconFromUri(Context ctx, String pkg, Uri uri) {
        if (pkg == null || uri == null) return false;
        try {
            File dir = new File(ctx.getFilesDir(), DIR_ICONS);
            if (!dir.exists()) dir.mkdirs();
            String name = pkg.replaceAll("[^a-zA-Z0-9._-]", "_") + ".png";
            File out = new File(dir, name);
            // A TV has little memory: a photo picked from storage can be 12 MP, so decode it down to at most
            // 512 px before it reaches the heap (a tile never draws it bigger).
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            InputStream probe = ctx.getContentResolver().openInputStream(uri);
            if (probe == null) return false;
            BitmapFactory.decodeStream(probe, null, bounds);
            probe.close();
            // Halve until the longer side is under 512 px, so a wide banner is shrunk as well as a square photo.
            int sample = 1;
            int longest = Math.max(bounds.outWidth, bounds.outHeight);
            while (longest / (sample * 2) >= 512) sample *= 2;
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            InputStream in = ctx.getContentResolver().openInputStream(uri);
            if (in == null) return false;
            Bitmap bmp = BitmapFactory.decodeStream(in, null, opts);
            in.close();
            if (bmp == null) return false;
            FileOutputStream fos = new FileOutputStream(out);
            boolean ok = bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            bmp.recycle();
            if (!ok) return false;
            prefs(ctx).edit().putString(KEY_ICON + pkg, name).apply();
            return true;
        } catch (Exception | OutOfMemoryError e) {
            // A huge photo on a small TV must not take the launcher down: the pick just fails.
            return false;
        }
    }

    /** The picked image, shrunk the same way as {@link #setIconFromUri}, for the dialog's preview. */
    static Drawable drawableFromUri(Context ctx, Uri uri) {
        if (uri == null) return null;
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            InputStream probe = ctx.getContentResolver().openInputStream(uri);
            if (probe == null) return null;
            BitmapFactory.decodeStream(probe, null, bounds);
            probe.close();
            int sample = 1;
            int longest = Math.max(bounds.outWidth, bounds.outHeight);
            while (longest / (sample * 2) >= 512) sample *= 2;
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            InputStream in = ctx.getContentResolver().openInputStream(uri);
            if (in == null) return null;
            Bitmap bmp = BitmapFactory.decodeStream(in, null, opts);
            in.close();
            return bmp == null ? null : new BitmapDrawable(ctx.getResources(), bmp);
        } catch (Exception | OutOfMemoryError e) {
            return null;
        }
    }

    static void reset(Context ctx, String pkg) {
        if (pkg == null) return;
        SharedPreferences.Editor e = prefs(ctx).edit();
        e.remove(KEY_LABEL + pkg);
        String name = prefs(ctx).getString(KEY_ICON + pkg, null);
        if (name != null) {
            File f = new File(new File(ctx.getFilesDir(), DIR_ICONS), name);
            if (f.exists()) f.delete();
            e.remove(KEY_ICON + pkg);
        }
        e.apply();
    }

    static boolean hasCustom(Context ctx, String pkg) {
        if (pkg == null) return false;
        SharedPreferences sp = prefs(ctx);
        return sp.contains(KEY_LABEL + pkg) || sp.contains(KEY_ICON + pkg);
    }
}
