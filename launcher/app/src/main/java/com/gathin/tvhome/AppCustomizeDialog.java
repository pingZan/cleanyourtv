package com.gathin.tvhome;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * "Sesuaikan": rename an app's tile and give it a picture picked from the TV's storage. The label and the
 * icon are saved per package (AppCustom); Reset puts both back to the app's own.
 */
final class AppCustomizeDialog {
    static final int REQUEST_PICK_ICON = 9001;

    static void show(Activity activity, String pkg, String currentLabel, Runnable onChanged) {
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View v = activity.getLayoutInflater().inflate(R.layout.dialog_app_customize, null);
        dialog.setContentView(v);
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setLayout((int) (460 * activity.getResources().getDisplayMetrics().density),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        EditText edit = v.findViewById(R.id.editLabel);
        ImageView preview = v.findViewById(R.id.iconPreview);
        Button pick = v.findViewById(R.id.btnPickIcon);
        Button reset = v.findViewById(R.id.btnReset);
        Button cancel = v.findViewById(R.id.btnCancel);
        Button save = v.findViewById(R.id.btnSave);

        String base = AppCustom.getLabel(activity, pkg);
        if (base == null) base = currentLabel;
        edit.setText(base == null ? "" : base);
        Drawable ic = AppCustom.getIcon(activity, pkg);
        if (ic == null) {
            // Show what the tile looks like now, so the preview is not an empty box.
            try {
                ic = activity.getPackageManager().getApplicationIcon(pkg);
            } catch (Exception ignored) {
            }
        }
        if (ic != null) preview.setImageDrawable(ic);

        Holder.lastPkg = pkg;
        Holder.lastPreview = preview;
        Holder.lastDialog = dialog;
        Holder.lastOnChanged = onChanged;
        Holder.pendingUri = null;

        pick.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (intent.resolveActivity(activity.getPackageManager()) == null) {
                // Some TVs only ship the older picker; a third-party file manager answers this one.
                intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            try {
                activity.startActivityForResult(intent, REQUEST_PICK_ICON);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(activity, R.string.customize_no_file_manager, Toast.LENGTH_LONG).show();
            }
        });

        reset.setOnClickListener(view -> {
            AppCustom.reset(activity, pkg);
            Holder.pendingUri = null;
            Toast.makeText(activity, R.string.customize_reset_done, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            if (onChanged != null) onChanged.run();
        });
        cancel.setOnClickListener(view -> dialog.dismiss());
        save.setOnClickListener(view -> {
            AppCustom.setLabel(activity, pkg, edit.getText().toString());
            boolean iconOk = true;
            if (Holder.pendingUri != null) iconOk = AppCustom.setIconFromUri(activity, pkg, Holder.pendingUri);
            Holder.pendingUri = null;
            Toast.makeText(activity, iconOk ? R.string.customize_saved : R.string.customize_icon_failed,
                    Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            if (onChanged != null) onChanged.run();
        });
        dialog.setOnDismissListener(d -> {
            if (Holder.lastDialog == dialog) {
                Holder.lastPkg = null;
                Holder.lastPreview = null;
                Holder.lastDialog = null;
                Holder.lastOnChanged = null;
                Holder.pendingUri = null;
            }
        });
        dialog.show();
        edit.requestFocus();
    }

    /** The file manager returned an image: keep it for Save and show it in the preview. */
    static void onIconPicked(Activity activity, Uri uri) {
        if (uri == null) return;
        try {
            activity.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {
        }
        Holder.pendingUri = uri;
        if (Holder.lastPreview != null) {
            Drawable picked = AppCustom.drawableFromUri(activity, uri);
            if (picked != null) Holder.lastPreview.setImageDrawable(picked);
        }
        if (Holder.lastDialog != null && Holder.lastDialog.isShowing()) {
            Toast.makeText(activity, R.string.customize_icon_picked, Toast.LENGTH_SHORT).show();
        }
    }

    private static final class Holder {
        static String lastPkg;
        static ImageView lastPreview;
        static Dialog lastDialog;
        static Runnable lastOnChanged;
        static Uri pendingUri;
    }
}
