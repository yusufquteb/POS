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
import com.pos.system.DBHelper;
import com.pos.system.MainActivity;
import com.pos.system.R;
import java.util.List;
import java.util.HashMap;

public class LowStockWorker extends Worker {

    private static final String CHANNEL_ID = "low_stock_channel";
    private static final int NOTIFICATION_ID = 1001;

    public LowStockWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            DBHelper db = new DBHelper(getApplicationContext());
            List<HashMap<String, String>> lowStock = db.getLowStockProducts();
            db.close();

            if (lowStock != null && !lowStock.isEmpty()) {
                showLowStockNotification(lowStock.size());
            }
            return Result.success();
        } catch (Exception e) {
            return Result.failure();
        }
    }

    private void showLowStockNotification(int count) {
        Context ctx = getApplicationContext();
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "تنبيهات المخزون",
                NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("تنبيهات عند انخفاض المخزون");
            nm.createNotificationChannel(channel);
        }

        Intent intent = new Intent(ctx, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String msg = count + " منتج على وشك النفاد";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("⚠️ تنبيه المخزون")
            .setContentText(msg)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pi)
            .setAutoCancel(true);

        nm.notify(NOTIFICATION_ID, builder.build());
    }
}
