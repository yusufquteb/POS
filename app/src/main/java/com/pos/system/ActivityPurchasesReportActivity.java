package com.pos.system;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import java.text.SimpleDateFormat;
import java.util.*;

public class ActivityPurchasesReportActivity extends BaseActivity {

    private static final String TAG = "PurchasesReportActivity";

    private DBHelper dbHelper;
    private String currency = "ج.م";

    private RecyclerView rvPurchases;
    private TextView tvTotalPurchases, tvPendingCount, tvReceivedCount, tvEmpty;
    private Chip chipAll, chipPending, chipReceived;

    private List<HashMap<String, String>> allData = new ArrayList<>();
    private List<HashMap<String, String>> filteredData = new ArrayList<>();
    private PurchasesAdapter adapter;
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchases_report);
        applyWindowInsets(findViewById(R.id.root_layout));

        dbHelper = new DBHelper(this);
        try { currency = dbHelper.getStoreSettings().getOrDefault("currency", "ج.م"); } catch (Exception ignored) {}

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) { setSupportActionBar(toolbar); toolbar.setNavigationOnClickListener(v -> finish()); }

        tvTotalPurchases = findViewById(R.id.tv_total_purchases);
        tvPendingCount   = findViewById(R.id.tv_pending_count);
        tvReceivedCount  = findViewById(R.id.tv_received_count);
        tvEmpty          = findViewById(R.id.tv_empty);

        chipAll      = findViewById(R.id.chip_all);
        chipPending  = findViewById(R.id.chip_pending);
        chipReceived = findViewById(R.id.chip_received);

        if (chipAll != null) chipAll.setOnClickListener(v -> filterData("all"));
        if (chipPending != null) chipPending.setOnClickListener(v -> filterData("pending"));
        if (chipReceived != null) chipReceived.setOnClickListener(v -> filterData("received"));

        rvPurchases = findViewById(R.id.rv_purchases);
        adapter = new PurchasesAdapter();
        rvPurchases.setLayoutManager(new LinearLayoutManager(this));
        rvPurchases.setAdapter(adapter);

        loadData();
    }

    private void loadData() {
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<HashMap<String, String>> orders = dbHelper.getPurchaseOrders();
                double total = 0;
                int pending = 0, received = 0;
                for (HashMap<String, String> o : orders) {
                    total += safeDouble(o, "total");
                    String status = o.getOrDefault("status", "pending");
                    if ("received".equals(status)) received++;
                    else pending++;
                }
                final double fTotal = total;
                final int fPending = pending, fReceived = received;
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    allData.clear();
                    allData.addAll(orders);
                    filterData(currentFilter);
                    setText(tvTotalPurchases, fmtAmt(fTotal));
                    setText(tvPendingCount,   fPending + " " + getString(R.string.po_status_pending));
                    setText(tvReceivedCount,  fReceived + " " + getString(R.string.po_status_received));
                });
            } catch (Exception e) {
                android.util.Log.e(TAG, "loadData: " + e.getMessage(), e);
            }
        });
    }

    private void filterData(String filter) {
        currentFilter = filter;
        filteredData.clear();
        for (HashMap<String, String> o : allData) {
            String status = o.getOrDefault("status", "pending");
            if ("all".equals(filter) || filter.equals(status)) filteredData.add(o);
        }
        adapter.notifyDataSetChanged();
        if (tvEmpty != null) tvEmpty.setVisibility(filteredData.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private class PurchasesAdapter extends RecyclerView.Adapter<PurchasesAdapter.VH> {
        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_purchase_report_row, parent, false);
            return new VH(v);
        }
        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            HashMap<String, String> item = filteredData.get(pos);
            h.tvSupplier.setText(item.getOrDefault("supplier_name", getString(R.string.suppliers_title)));
            h.tvDate.setText(shortDate(item.getOrDefault("created_at", "")));
            h.tvTotal.setText(fmtAmt(safeDouble(item, "total")));
            String status = item.getOrDefault("status", "pending");
            h.tvStatus.setText("received".equals(status) ? getString(R.string.po_status_received) : getString(R.string.po_status_pending));
            int clr = "received".equals(status)
                ? androidx.core.content.ContextCompat.getColor(ActivityPurchasesReportActivity.this, R.color.color_success)
                : 0xFFFF9800;
            h.tvStatus.setTextColor(clr);
        }
        @Override public int getItemCount() { return filteredData.size(); }
        class VH extends RecyclerView.ViewHolder {
            TextView tvSupplier, tvDate, tvTotal, tvStatus;
            VH(@NonNull View v) {
                super(v);
                tvSupplier = v.findViewById(R.id.tv_supplier);
                tvDate     = v.findViewById(R.id.tv_date);
                tvTotal    = v.findViewById(R.id.tv_total);
                tvStatus   = v.findViewById(R.id.tv_status);
            }
        }
    }

    private String shortDate(String dt) {
        if (dt.length() >= 10) return dt.substring(0, 10);
        return dt;
    }

    private String fmtAmt(double v) { return String.format(Locale.US, "%.2f %s", v, currency); }
    private double safeDouble(HashMap<String, String> m, String key) {
        try { return Double.parseDouble(m.getOrDefault(key, "0")); } catch (Exception e) { return 0; }
    }
    private void setText(TextView tv, String t) { if (tv != null) tv.setText(t); }

    @Override protected void onDestroy() {
        super.onDestroy();
        try { if (dbHelper != null) dbHelper.close(); } catch (Exception ignored) {}
    }
}
