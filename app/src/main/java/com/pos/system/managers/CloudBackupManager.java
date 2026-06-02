package com.pos.system.managers;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;
import androidx.documentfile.provider.DocumentFile;
import com.pos.system.DBHelper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * CloudBackupManager — SAF-based backup to any folder the user picks
 * (Google Drive, local storage, Samsung Cloud, etc.).
 *
 * No OAuth, no SDK dependencies beyond androidx.documentfile.
 * The user selects a folder once; permissions are persisted.
 */
public class CloudBackupManager {

    private static final String TAG              = "CloudBackupManager";
    private static final String SETTING_FOLDER   = "cloud_backup_folder";
    private static final String SETTING_LAST_BK  = "cloud_backup_last";
    private static final int    MAX_BACKUPS_KEPT  = 5;
    private static final String BACKUP_PREFIX     = "SmartPOS_";
    private static final String BACKUP_EXT        = ".db";

    private final Context ctx;
    private final DBHelper db;

    public CloudBackupManager(Context context) {
        this.ctx = context.getApplicationContext();
        this.db  = new DBHelper(ctx);
    }

    // ── Folder management ────────────────────────────────────────────────────

    public boolean isFolderConfigured() {
        String saved = db.getStoreSetting(SETTING_FOLDER);
        if (saved == null || saved.isEmpty()) return false;
        DocumentFile folder = DocumentFile.fromTreeUri(ctx, Uri.parse(saved));
        return folder != null && folder.exists() && folder.isDirectory();
    }

    public void saveFolderUri(Uri treeUri) {
        try {
            ctx.getContentResolver().takePersistableUriPermission(
                treeUri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            );
        } catch (Exception e) {
            Log.w(TAG, "Could not persist URI permissions: " + e.getMessage());
        }
        db.saveStoreSetting(SETTING_FOLDER, treeUri.toString());
    }

    public Uri getSavedFolderUri() {
        String saved = db.getStoreSetting(SETTING_FOLDER);
        return (saved != null && !saved.isEmpty()) ? Uri.parse(saved) : null;
    }

    public void clearSavedFolder() {
        db.saveStoreSetting(SETTING_FOLDER, "");
        db.saveStoreSetting(SETTING_LAST_BK, "");
    }

    // ── Backup ───────────────────────────────────────────────────────────────

    public boolean backup() {
        Uri folderUri = getSavedFolderUri();
        if (folderUri == null) return false;
        return backupToFolder(folderUri);
    }

    public boolean backupToFolder(Uri folderUri) {
        try {
            db.close();

            File dbFile = ctx.getDatabasePath(DBHelper.DATABASE_NAME);
            if (!dbFile.exists()) {
                Log.e(TAG, "DB file not found: " + dbFile.getAbsolutePath());
                return false;
            }

            DocumentFile folder = DocumentFile.fromTreeUri(ctx, folderUri);
            if (folder == null || !folder.exists()) {
                Log.e(TAG, "Backup folder not accessible");
                return false;
            }

            // Rotate old backups: delete oldest if we already have MAX_BACKUPS_KEPT
            pruneOldBackups(folder);

            // Create new backup file
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
                .format(new Date());
            String fileName = BACKUP_PREFIX + timestamp + BACKUP_EXT;
            DocumentFile backupFile = folder.createFile("application/octet-stream", fileName);
            if (backupFile == null) {
                Log.e(TAG, "Cannot create backup file in folder");
                return false;
            }

            // Copy bytes
            try (InputStream  is = new FileInputStream(dbFile);
                 OutputStream os = ctx.getContentResolver().openOutputStream(backupFile.getUri())) {
                if (os == null) throw new Exception("Cannot open output stream");
                byte[] buf = new byte[8192];
                int len;
                while ((len = is.read(buf)) > 0) os.write(buf, 0, len);
                os.flush();
            }

            db.saveStoreSetting(SETTING_LAST_BK,
                new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date()));
            Log.d(TAG, "Cloud backup created: " + fileName);
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Backup failed: " + e.getMessage(), e);
            return false;
        } finally {
            // Re-open DB
            new DBHelper(ctx);
        }
    }

    // ── Restore ──────────────────────────────────────────────────────────────

    public boolean restoreFromUri(Uri fileUri) {
        InputStream  is = null;
        OutputStream os = null;
        try {
            db.close();

            File dbFile = ctx.getDatabasePath(DBHelper.DATABASE_NAME);
            ContentResolver cr = ctx.getContentResolver();

            is = cr.openInputStream(fileUri);
            os = new FileOutputStream(dbFile);
            if (is == null) throw new Exception("Cannot open input stream");

            byte[] buf = new byte[8192];
            int len;
            while ((len = is.read(buf)) > 0) os.write(buf, 0, len);
            os.flush();
            Log.d(TAG, "Restore complete from: " + fileUri);
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Restore failed: " + e.getMessage(), e);
            return false;
        } finally {
            try { if (is != null) is.close(); } catch (Exception ignored) {}
            try { if (os != null) os.close(); } catch (Exception ignored) {}
            new DBHelper(ctx);
        }
    }

    // ── List backups ─────────────────────────────────────────────────────────

    public List<DocumentFile> listBackups() {
        List<DocumentFile> result = new ArrayList<>();
        Uri folderUri = getSavedFolderUri();
        if (folderUri == null) return result;
        DocumentFile folder = DocumentFile.fromTreeUri(ctx, folderUri);
        if (folder == null || !folder.exists()) return result;

        DocumentFile[] files = folder.listFiles();
        if (files == null) return result;

        for (DocumentFile f : files) {
            String name = f.getName();
            if (name != null && name.startsWith(BACKUP_PREFIX) && name.endsWith(BACKUP_EXT)) {
                result.add(f);
            }
        }

        // Sort newest first
        result.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        return result;
    }

    public String getLastBackupDate() {
        String date = db.getStoreSetting(SETTING_LAST_BK);
        return (date != null && !date.isEmpty()) ? date : null;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void pruneOldBackups(DocumentFile folder) {
        try {
            List<DocumentFile> backups = new ArrayList<>();
            DocumentFile[] files = folder.listFiles();
            if (files == null) return;
            for (DocumentFile f : files) {
                String name = f.getName();
                if (name != null && name.startsWith(BACKUP_PREFIX) && name.endsWith(BACKUP_EXT)) {
                    backups.add(f);
                }
            }
            if (backups.size() < MAX_BACKUPS_KEPT) return;
            backups.sort((a, b) -> Long.compare(a.lastModified(), b.lastModified()));
            // Delete oldest
            for (int i = 0; i <= backups.size() - MAX_BACKUPS_KEPT; i++) {
                backups.get(i).delete();
            }
        } catch (Exception e) {
            Log.w(TAG, "Prune error: " + e.getMessage());
        }
    }

    public void close() {
        try { db.close(); } catch (Exception ignored) {}
    }
}
