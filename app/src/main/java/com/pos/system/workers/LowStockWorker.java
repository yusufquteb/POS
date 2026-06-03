package com.pos.system.workers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.pos.system.ActivityPurchaseOrderActivity;
import com.pos.system.DBHelper;
import com.pos.system.MainActivity;
import com.pos.system.R;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LowStockWorker extends Worker {

    private static final String CHANNEL_ID      = "low_stock_channel";
    private static final String REORDER_CHANNEL = "reorder_channel";
    private static final int    NOTIF_LOW_STOCK = 1001;
    private static final int    NOTIF_REORDER   = 1002;

    public LowStockWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            DBHelper db = new DBHelper(getApplicationContext());

            List<HashMap<String, String>> lowStock = db.getLowStockProducts();
            if (lowStock != null && !lowStock.isEmpty()) {
                showLowStockNotification(lowStock.size());
                int poCount = createSmartReorderSuggestions(db, lowStock);
                if (poCount > 0) showReorderNotification(poCount);
            }

            db.close();
            return Result.success();
        } catch (Exception e) {
            return Result.failure();
        }
    }

    /**
     * يجمع المنتجات منخفضة المخزون حسب المورد ويُنشئ أوامر شراء مسودة تلقائياً
     * يتجنب تكرار PO إذا كان هناك أمر شراء pending لنفس المورد
     */
    private int createSmartReorderSuggestions(DBHelper db, List<HashMap<String, String>> lowStock) {
        try {
            // جمع المنتجات حسب المورد
            HashMap<String, List<HashMap<String, String>>> bySupplier = new HashMap<>();
            for (HashMap<String, String> p : lowStock) {
                String supplier = p.getOrDefault("supplier", "").trim();
                if (supplier.isEmpty()) supplier = "غير محدد";
                if (!bySupplier.containsKey(supplier))
                    bySupplier.put(supplier, new ArrayList<>());
                bySupplier.get(supplier).add(p);
            }

            // للتحقق من POs الموجودة
            List<HashMap<String, String>> existingPOs = db.getPurchaseOrders();
            java.util.Set<String> pendingSuppliers = new java.util.HashSet<>();
            for (HashMap<String, String> po : existingPOs) {
                if ("pending".equals(po.get("status"))) {
                    pendingSuppliers.add(po.getOrDefault("supplier_name", "").trim());
                }
            }

            int created = 0;
            for (java.util.Map.Entry<String, List<HashMap<String, String>>> entry : bySupplier.entrySet()) {
                String supplierName = entry.getKey();
                // تجنب التكرار
                if (pendingSuppliers.contains(supplierName)) continue;

                List<HashMap<String, String>> items = new ArrayList<>();
                double total = 0;
                for (HashMap<String, String> p : entry.getValue()) {
                    HashMap<String, String> item = new HashMap<>();
                    item.put("name", p.getOrDefault("name", ""));
                    int reorderLevel = safeInt(p.get("reorder_level"), 5);
                    int currentQty   = safeInt(p.get("qty"), 0);
                    int suggestQty   = Math.max(reorderLevel - currentQty, reorderLevel);
                    item.put("qty",  String.valueOf(suggestQty));
                    double cost = safeDouble(p.get("cost"));
                    item.put("cost",  String.valueOf(cost));
                    item.put("total", String.valueOf(cost * suggestQty));
                    total += cost * suggestQty;
                    items.add(item);
                }

                long poId = db.addPurchaseOrder(supplierName, "", items, total,
                    "طلب تلقائي - " + new java.text.SimpleDateFormat("yyyy-MM-dd",
                        java.util.Locale.US).format(new java.util.Date()));
                if (poId > 0) created++;
            }
            return created;
        } catch (Exception e) {
            return 0;
        }
    }

    private void showLowStockNotification(int count) {
        Context ctx = getApplicationContext();
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        createChannel(nm, CHANNEL_ID, "تنبيهات المخزون");

        Intent intent = new Intent(ctx, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String msg = count + " منتج على وشك النفاد";
        nm.notify(NOTIF_LOW_STOCK, new NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("⚠️ تنبيه المخزون")
            .setContentText(msg)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build());
    }

    private void showReorderNotification(int poCount) {
        Context ctx = getApplicationContext();
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        createChannel(nm, REORDER_CHANNEL, "طلبات الشراء التلقائية");

        Intent intent = new Intent(ctx, ActivityPurchaseOrderActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(ctx, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String msg = "تم إنشاء " + poCount + " طلب شراء تلقائي — اضغط للمراجعة";
        nm.notify(NOTIF_REORDER, new NotificationCompat.Builder(ctx, REORDER_CHANNEL)
            .setSmallIcon(R.drawable.ic_shopping_cart)
            .setContentTitle("🛒 اقتراح إعادة الطلب")
            .setContentText(msg)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build());
    }

    private void createChannel(NotificationManager nm, String id, String name) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(id, name,
                NotificationManager.IMPORTANCE_DEFAULT);
            nm.createNotificationChannel(ch);
        }
    }

    private int safeInt(String s, int def) {
        try { return s != null && !s.isEmpty() ? Integer.parseInt(s) : def; }
        catch (Exception e) { return def; }
    }

    private double safeDouble(String s) {
        try { return s != null && !s.isEmpty() ? Double.parseDouble(s) : 0.0; }
        catch (Exception e) { return 0.0; }
    }
}
