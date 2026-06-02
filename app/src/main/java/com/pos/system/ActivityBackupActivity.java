package com.pos.system;

import com.pos.system.BaseActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import com.pos.system.managers.CloudBackupManager;
import com.pos.system.FeatureGate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

/**
 * ActivityBackupActivity - محسّنة
 *
 * ✅ الإصلاحات:
 * - استبدال startActivityForResult/onActivityResult بـ ActivityResultLauncher
 * - نقل النصوص المضمّنة إلى strings.xml
 * - استبدال e.printStackTrace() بـ Log.e()
 * - إصلاح إغلاق الـ Streams
 */
public class ActivityBackupActivity extends BaseActivity {

    private static final String TAG                    = "BackupActivity";
    private static final int    PERMISSION_REQUEST_CODE = 100;

    private DBHelper         dbHelper;
    private ListView         listBackups;
    private View             emptyState;
    private final ArrayList<File> backupFiles = new ArrayList<>();
    private CloudBackupManager cloudBackupManager;
    private Button  btnSelectFolder, btnCloudBackupNow;
    private TextView tvCloudStatus, tvCloudLastBackup;

    private final androidx.activity.result.ActivityResultLauncher<Uri> folderPickerLauncher =
        registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), uri -> {
            if (uri != null) {
                cloudBackupManager.saveFolderUri(uri);
                updateCloudUI();
                performCloudBackup();
            }
        });


    // ✅ ActivityResultLauncher لإدارة صلاحية الملفات (Android 11+)
    private final ActivityResultLauncher<Intent> manageStorageLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        loadBackupFiles();
                    } else {
                        showSnackbar(getString(R.string.storage_permission_required), true);
                    }
                }
            }
        );

    // ✅ ActivityResultLauncher لاختيار ملف النسخة الاحتياطية
    private final ActivityResultLauncher<Intent> pickBackupLauncher =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK
                        && result.getData() != null
                        && result.getData().getData() != null) {
                    confirmRestore(result.getData().getData());
                }
            }
        );

    // ─────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);

        dbHelper = new DBHelper(this);
        cloudBackupManager = new CloudBackupManager(this);
        initViews();
        setupToolbar();
        setupButtons();
        setupCloudBackup();

        if (checkPermissions()) {
            loadBackupFiles();
        } else {
            requestStoragePermission();
        }
    }

    // ─────────────────────────────────────────────
    private void initViews() {
        listBackups = findViewById(R.id.list_backups);
        if (listBackups == null) listBackups = findViewById(android.R.id.list);
        emptyState = findViewById(R.id.empty_state);
    }

    private void setupToolbar() {
        View toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) toolbar.setOnClickListener(v -> finish());
    }

    private void setupButtons() {
        View btnCreate = findViewById(R.id.btn_create_backup);
        if (btnCreate != null) btnCreate.setOnClickListener(v -> createBackup());

        View btnRestore = findViewById(R.id.btn_restore_backup);
        if (btnRestore != null) btnRestore.setOnClickListener(v -> pickBackupFile());
    }

    // ─────────────────────────────────────────────

    // ─────────────────────────────────────────────
    // Cloud Backup (SAF-based)
    // ─────────────────────────────────────────────

    private void setupCloudBackup() {
        btnSelectFolder    = findViewById(R.id.btn_select_cloud_folder);
        btnCloudBackupNow  = findViewById(R.id.btn_cloud_backup_now);
        tvCloudStatus      = findViewById(R.id.tv_cloud_status);
        tvCloudLastBackup  = findViewById(R.id.tv_cloud_last_backup);

        if (btnSelectFolder != null)
            btnSelectFolder.setOnClickListener(v -> {
                if (!FeatureGate.isUnlocked(this)) {
                    FeatureGate.requirePremium(this, "النسخ الاحتياطي على السحابة");
                    return;
                }
                folderPickerLauncher.launch(null);
            });

        if (btnCloudBackupNow != null)
            btnCloudBackupNow.setOnClickListener(v -> performCloudBackup());

        updateCloudUI();
    }

    private void updateCloudUI() {
        if (cloudBackupManager == null) return;
        boolean configured = cloudBackupManager.isFolderConfigured();
        if (tvCloudStatus != null) {
            tvCloudStatus.setText(configured ? "مُفعّل ✓" : "غير مُفعّل");
            tvCloudStatus.setTextColor(configured ? 0xFF388E3C : 0xFFF57C00);
        }
        if (btnCloudBackupNow != null)
            btnCloudBackupNow.setVisibility(configured ? View.VISIBLE : View.GONE);
        if (tvCloudLastBackup != null) {
            String last = cloudBackupManager.getLastBackupDate();
            if (last != null) {
                tvCloudLastBackup.setText("آخر نسخة: " + last);
                tvCloudLastBackup.setVisibility(View.VISIBLE);
            } else {
                tvCloudLastBackup.setVisibility(View.GONE);
            }
        }
    }

    private void performCloudBackup() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("نسخ احتياطي على السحابة")
            .setMessage("هل تريد إنشاء نسخة احتياطية في المجلد المحدد؟")
            .setPositiveButton("نعم", (d, w) -> {
                boolean ok = cloudBackupManager.backup();
                showSnackbar(ok ? "✓ تم النسخ الاحتياطي بنجاح" : "فشل النسخ الاحتياطي", !ok);
                updateCloudUI();
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                manageStorageLauncher.launch(intent);
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                manageStorageLauncher.launch(intent);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadBackupFiles();
            } else {
                showSnackbar(getString(R.string.storage_permission_required), true);
            }
        }
    }

    // ─────────────────────────────────────────────
    private File getBackupDirectory() {
        File dir;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            dir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), "SmartPOS_Backups");
            if (!dir.exists() || !dir.canWrite()) {
                dir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), "SmartPOS_Backups");
            }
        } else {
            dir = new File(Environment.getExternalStorageDirectory(), "SmartPOS_Backups");
        }
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    // ─────────────────────────────────────────────
    private void loadBackupFiles() {
        backupFiles.clear();
        try {
            File backupDir = getBackupDirectory();
            File[] files = backupDir.listFiles(
                (d, name) -> name.endsWith(".db") || name.endsWith(".backup"));
            if (files != null && files.length > 0) {
                backupFiles.addAll(Arrays.asList(files));
                backupFiles.sort((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
            }
        } catch (Exception e) {
            Log.e(TAG, "loadBackupFiles error: " + e.getMessage(), e);
            showSnackbar(getString(R.string.unknown_error), true);
        }
        updateUI();
    }

    private void updateUI() {
        if (emptyState != null)
            emptyState.setVisibility(backupFiles.isEmpty() ? View.VISIBLE : View.GONE);
        if (listBackups != null)
            listBackups.setAdapter(new BackupsAdapter());
    }

    // ─────────────────────────────────────────────
    private void createBackup() {
        new MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.create_backup))
            .setMessage(getString(R.string.backup_title) + "?")
            .setPositiveButton(getString(R.string.yes), (d, w) -> performBackup())
            .setNegativeButton(getString(R.string.no), null)
            .show();
    }

    private void performBackup() {
        try {
            if (dbHelper != null) dbHelper.close();

            String dbPath = getDatabasePath(DBHelper.DATABASE_NAME).getAbsolutePath();
            File dbFile = new File(dbPath);
            if (!dbFile.exists()) {
                showSnackbar(getString(R.string.file_not_found), true);
                dbHelper = new DBHelper(this);
                return;
            }

            File backupDir = getBackupDirectory();
            if (!backupDir.exists() && !backupDir.mkdirs()) {
                showSnackbar(getString(R.string.operation_failed), true);
                dbHelper = new DBHelper(this);
                return;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
            File backupFile = new File(backupDir, "backup_" + sdf.format(new Date()) + ".db");

            copyFile(dbFile, backupFile);

            showSnackbar(getString(R.string.backup_created) + "\n" + backupDir.getAbsolutePath(),
                false);
            loadBackupFiles();

        } catch (Exception e) {
            Log.e(TAG, "performBackup error: " + e.getMessage(), e);
            showSnackbar(getString(R.string.backup_failed), true);
        } finally {
            dbHelper = new DBHelper(this);
        }
    }

    // ─────────────────────────────────────────────
    private void pickBackupFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        pickBackupLauncher.launch(
            Intent.createChooser(intent, getString(R.string.restore_backup))
        );
    }

    private void confirmRestore(Uri uri) {
        new MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.restore_backup))
            .setMessage(getString(R.string.backup_restored) + " ?")
            .setPositiveButton(getString(R.string.yes), (d, w) -> performRestore(uri))
            .setNegativeButton(getString(R.string.cancel), null)
            .show();
    }

    private void performRestore(Uri uri) {
        InputStream   inputStream  = null;
        FileOutputStream outputStream = null;
        try {
            if (dbHelper != null) dbHelper.close();

            String dbPath = getApplicationContext()
                .getDatabasePath(DBHelper.DATABASE_NAME).getAbsolutePath();
            File dbFile = new File(dbPath);

            inputStream  = getContentResolver().openInputStream(uri);
            outputStream = new FileOutputStream(dbFile);

            if (inputStream == null) {
                showSnackbar(getString(R.string.operation_failed), true);
                return;
            }

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.flush();

            showSnackbar(getString(R.string.backup_restored), false);

            // إعادة تشغيل
            new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.restart_app))
                .setMessage(getString(R.string.restart_required))
                .setPositiveButton(getString(R.string.restart_app), (d, w) -> {
                    Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }
                })
                .setCancelable(false)
                .show();

        } catch (Exception e) {
            Log.e(TAG, "performRestore error: " + e.getMessage(), e);
            showSnackbar(getString(R.string.restore_failed), true);
        } finally {
            // ✅ إغلاق Streams مع Log بدل printStackTrace
            try { if (inputStream  != null) inputStream.close();  } catch (Exception e) {
                Log.w(TAG, "Error closing inputStream: " + e.getMessage());
            }
            try { if (outputStream != null) outputStream.close(); } catch (Exception e) {
                Log.w(TAG, "Error closing outputStream: " + e.getMessage());
            }
            dbHelper = new DBHelper(this);
        }
    }

    // ─────────────────────────────────────────────
    private void copyFile(File src, File dst) throws Exception {
        try (FileInputStream fis  = new FileInputStream(src);
             FileOutputStream fos = new FileOutputStream(dst)) {
            byte[] buf = new byte[1024];
            int    len;
            while ((len = fis.read(buf)) > 0) fos.write(buf, 0, len);
            fos.flush();
        }
    }

    // ─────────────────────────────────────────────
    private void showSnackbar(String msg, boolean error) {
        try {
            Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG)
                .setBackgroundTint(error ? 0xFFB3261E : 0xFF2E7D32)
                .show();
        } catch (Exception e) {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }
    }

    // ─────────────────────────────────────────────
    // Adapter
    // ─────────────────────────────────────────────
    private class BackupsAdapter extends BaseAdapter {

        @Override public int    getCount()     { return backupFiles.size(); }
        @Override public Object getItem(int i) { return backupFiles.get(i); }
        @Override public long   getItemId(int i) { return i; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = LayoutInflater.from(ActivityBackupActivity.this)
                    .inflate(android.R.layout.simple_list_item_2, parent, false);

            File     file  = backupFiles.get(position);
            TextView text1 = convertView.findViewById(android.R.id.text1);
            TextView text2 = convertView.findViewById(android.R.id.text2);

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
            text1.setText(file.getName());
            text2.setText(sdf.format(new Date(file.lastModified()))
                + " • " + String.format(Locale.getDefault(), "%.2f KB", file.length() / 1024.0));

            convertView.setOnClickListener(v ->
                new MaterialAlertDialogBuilder(ActivityBackupActivity.this)
                    .setTitle(file.getName())
                    .setItems(new String[]{
                        getString(R.string.restore_backup),
                        getString(R.string.delete)
                    }, (d, which) -> {
                        if (which == 0) confirmRestore(Uri.fromFile(file));
                        else deleteBackup(file, position);
                    })
                    .show()
            );
            return convertView;
        }
    }

    private void deleteBackup(File file, int position) {
        new MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete))
            .setMessage(getString(R.string.delete_product_message))
            .setPositiveButton(getString(R.string.yes), (d, w) -> {
                if (file.delete()) {
                    backupFiles.remove(position);
                    updateUI();
                    showSnackbar(getString(R.string.deleted_successfully), false);
                } else {
                    showSnackbar(getString(R.string.operation_failed), true);
                }
            })
            .setNegativeButton(getString(R.string.no), null)
            .show();
    }

    // ─────────────────────────────────────────────
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) dbHelper.close();
    }
}
