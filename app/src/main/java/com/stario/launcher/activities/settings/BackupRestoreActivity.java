package com.stario.launcher.activities.settings;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.stario.launcher.themes.ThemedActivity;

import java.io.*;
import java.util.zip.*;

public class BackupRestoreActivity extends ThemedActivity {
    private static final String[] EXCLUDED = {"cache", "code_cache", "lib", "oat"};
    private ActivityResultLauncher<Intent> createBackup;
    private ActivityResultLauncher<Intent> restoreBackup;

    @Override protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 32, 24, 24);
        root.setBackgroundColor(getColor(com.stario.launcher.R.color.transparent));
        TextView title = new TextView(this);
        title.setText("Backup & restore");
        title.setTextSize(26);
        root.addView(title);
        TextView description = new TextView(this);
        description.setText("Save all Stario settings, layouts, preferences and customizations into one portable ZIP file. Restore it later, including after reinstalling Stario.");
        description.setTextSize(15);
        description.setPadding(0, 16, 0, 24);
        root.addView(description);
        MaterialButton backup = new MaterialButton(this);
        backup.setText("Create full backup");
        root.addView(backup);
        MaterialButton restore = new MaterialButton(this);
        restore.setText("Restore from backup file");
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
        rp.topMargin = 12;
        root.addView(restore, rp);
        setContentView(root);

        createBackup = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null)
                writeBackup(result.getData().getData());
        });
        restoreBackup = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null)
                confirmRestore(result.getData().getData());
        });
        backup.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            i.setType("application/zip");
            i.putExtra(Intent.EXTRA_TITLE, "Stario-Full-Backup.zip");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            createBackup.launch(i);
        });
        restore.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.setType("application/zip");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            restoreBackup.launch(i);
        });
    }

    private boolean excluded(String name) {
        for (String item : EXCLUDED) if (item.equals(name)) return true;
        return false;
    }

    private void writeBackup(Uri uri) {
        if (uri == null) return;
        try (OutputStream raw = getContentResolver().openOutputStream(uri);
             ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(raw))) {
            zip.putNextEntry(new ZipEntry("stario-backup-v1\n"));
            zip.closeEntry();
            File[] roots = getDataDir().listFiles();
            if (roots != null) for (File file : roots) {
                if (!excluded(file.getName())) addFile(zip, file, file.getName());
            }
            zip.finish();
            Toast.makeText(this, "Full backup created.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Backup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void addFile(ZipOutputStream zip, File file, String path) throws IOException {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children == null) return;
            for (File child : children) addFile(zip, child, path + "/" + child.getName());
        } else {
            zip.putNextEntry(new ZipEntry(path));
            try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
                byte[] buffer = new byte[8192];
                int n;
                while ((n = in.read(buffer)) != -1) zip.write(buffer, 0, n);
            }
            zip.closeEntry();
        }
    }

    private void confirmRestore(Uri uri) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Restore Stario backup?")
                .setMessage("This replaces the current Stario settings and customizations with the contents of the selected backup. Stario will restart afterward.")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Restore", (d, w) -> restoreFrom(uri))
                .show();
    }

    private void restoreFrom(Uri uri) {
        try (InputStream raw = getContentResolver().openInputStream(uri);
             ZipInputStream zip = new ZipInputStream(new BufferedInputStream(raw))) {
            ZipEntry entry;
            boolean valid = false;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.equals("stario-backup-v1\n")) { valid = true; continue; }
                if (!valid) throw new IOException("This is not a valid Stario backup.");
                File target = new File(getDataDir(), name).getCanonicalFile();
                String base = getDataDir().getCanonicalPath() + File.separator;
                if (!target.getPath().startsWith(base) || excluded(name.split("/")[0]))
                    throw new IOException("Backup contains an invalid path.");
                if (entry.isDirectory()) target.mkdirs();
                else {
                    File parent = target.getParentFile();
                    if (parent != null) parent.mkdirs();
                    try (OutputStream out = new BufferedOutputStream(new FileOutputStream(target))) {
                        byte[] buffer = new byte[8192];
                        int n;
                        while ((n = zip.read(buffer)) != -1) out.write(buffer, 0, n);
                    }
                }
                zip.closeEntry();
            }
            if (!valid) throw new IOException("Not a Stario backup file.");
            Toast.makeText(this, "Restore complete. Restarting Stario…", Toast.LENGTH_LONG).show();
            getWindow().getDecorView().postDelayed(() -> {
                Process.killProcess(Process.myPid());
            }, 1200);
        } catch (Exception e) {
            Toast.makeText(this, "Restore failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
