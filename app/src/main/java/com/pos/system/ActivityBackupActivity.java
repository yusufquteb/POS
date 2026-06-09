package com.pos.system;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.pos.system.managers.CloudBackupManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import com.pos.system.databinding.ActivityBackupBinding;

public class ActivityBackupActivity extends BaseActivity {

    private ActivityBackupBinding binding;


    private static final String TAG = "BackupActivity";

    private DBHelper              dbHelper;
    private ListView              listBackups;
    private View                  emptyState;
    private final ArrayList<File> backupFiles = new ArrayList<>();
    private CloudBackupManager    cloudBackupManager;
    private Button                btnSelectFolder, btnCloudBackupNow;
    private TextView              tvCloudStatus, tvCloudLastBackup;

    private final ActivityResultLauncher<Uri> folderPickerLauncher =
        registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), uri -> {
            if (uri != null) {
                cloudBackupManager.saveFolderUri(uri);
                updateCloudUI();
                performCloudBackup();
            }
        });

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBackupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets(binding.getRoot());

        dbHelper = new DBHelper(this);
        cloudBackupManager = new CloudBackupManager(this);
        initViews();
        setupToolbar();
        setupButtons();
        setupCloudBackup();
        loadBackupFiles();
    }

    private void initViews() {
        listBackups = binding.listBackups;
        if (listBackups == null) listBackups = findViewById(android.R.id.list);
        emptyState = binding.emptyState;
    }

    private void setupToolbar() {
        View toolbar = binding.toolbar;
        if (toolbar != null) toolbar.setOnClickListener(v -> finish());
    }

    private void setupButtons() {
        View btnCreate = binding.btnCreateBackup;
        if (btnCreate != null) btnCreate.setOnClickListener(v -> createBackup());

        View btnRestore = binding.btnRestoreBackup;
        if (btnRestore != null) btnRestore.setOnClickListener(v -> pickBackupFile());
    }

    // ── Cloud Backup (SAF-based) ──────────────────────────────────────

    private void setupCloudBackup() {
        btnSelectFolder   = binding.btnSelectCloudFolder;
        btnCloudBackupNow = binding.btnCloudBackupNow;
        tvCloudStatus     = binding.tvCloudStatus;
        tvCloudLastBackup = binding.tvCloudLastBackup;

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

            TypedValue successVal = new TypedValue();
            TypedValue warningVal = new TypedValue();
            getTheme().resolveAttribute(R.attr.colorSuccess, successVal, true);
            getTheme().resolveAttribute(R.attr.colorWarning, warningVal, true);
            tvCloudStatus.setTextColor(configured ? successVal.data : warningVal.data);
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
            .setPositiveButton(R.string.yes, (d, w) -> {
                boolean ok = cloudBackupManager.backup();
                showSnackbar(ok ? "✓ تم النسخ الاحتياطي بنجاح" : "فشل النسخ الاحتياطي", !ok);
                updateCloudUI();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    // ── Local Backup ──────────────────────────────────────────────────

    private File getBackupDirectory() {
        File dir = new File(getExternalFilesDir(null), "SmartPOS_Backups");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private void loadBackupFiles() {
        backupFiles.clear();
        try {
            File   backupDir = getBackupDirectory();
            File[] files     = backupDir.listFiles(
                (d, name) -> name.endsWith(".db") || name.endsWith(".backup"));
            if (files != null && files.length > 0) {
                backupFiles.addAll(Arrays.asList(files));
                backupFiles.sort((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
            }
        } catch (Exception e) {
            Log.e(TAG, "loadBackupFiles error", e);
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
            File   dbFile = new File(dbPath);
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

            SimpleDateFormat sdf        = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
            File             backupFile = new File(backupDir, "backup_" + sdf.format(new Date()) + ".db");

            copyFile(dbFile, backupFile);

            showSnackbar(getString(R.string.backup_created) + "\n" + backupDir.getAbsolutePath(), false);
            loadBackupFiles();

        } catch (Exception e) {
            Log.e(TAG, "performBackup error", e);
            showSnackbar(getString(R.string.backup_failed), true);
        } finally {
            dbHelper = new DBHelper(this);
        }
    }

    // ── Restore ───────────────────────────────────────────────────────

    private void pickBackupFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        pickBackupLauncher.launch(Intent.createChooser(intent, getString(R.string.restore_backup)));
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
        InputStream      inputStream  = null;
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
            int    length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.flush();

            showSnackbar(getString(R.string.backup_restored), false);

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
            Log.e(TAG, "performRestore error", e);
            showSnackbar(getString(R.string.restore_failed), true);
        } finally {
            try { if (inputStream  != null) inputStream.close();  } catch (Exception e) {
                Log.w(TAG, "Error closing inputStream", e);
            }
            try { if (outputStream != null) outputStream.close(); } catch (Exception e) {
                Log.w(TAG, "Error closing outputStream", e);
            }
            dbHelper = new DBHelper(this);
        }
    }

    private void copyFile(File src, File dst) throws Exception {
        try (FileInputStream fis  = new FileInputStream(src);
             FileOutputStream fos = new FileOutputStream(dst)) {
            byte[] buf = new byte[1024];
            int    len;
            while ((len = fis.read(buf)) > 0) fos.write(buf, 0, len);
            fos.flush();
        }
    }

    // ── Adapter ───────────────────────────────────────────────────────

    private class BackupsAdapter extends BaseAdapter {

        @Override public int    getCount()        { return backupFiles.size(); }
        @Override public Object getItem(int i)    { return backupFiles.get(i); }
        @Override public long   getItemId(int i)  { return i; }

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) dbHelper.close();
    }
}
