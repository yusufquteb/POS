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
import java.text.SimpleDateFormat;
import java.util.*;

public class ActivitySupplierAgingActivity extends BaseActivity {

    private static final String TAG = "SupplierAgingActivity";

    private DBHelper dbHelper;
    private String currency = "ج.م";

    private RecyclerView rvSuppliers;
    private TextView tvTotal0_30, tvTotal31_60, tvTotal61_90, tvTotal90plus, tvEmpty;
    private TextView tvCount0_30, tvCount31_60, tvCount61_90, tvCount90plus;

    private List<AgingRow> data = new ArrayList<>();
    private AgingAdapter adapter;

    static class AgingRow {
        String name, phone;
        double debt;
        double bucket0_30, bucket31_60, bucket61_90, bucket90plus;
        AgingRow(String name, String phone, double debt) {
            this.name = name; this.phone = phone; this.debt = debt;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_supplier_aging);
        applyWindowInsets(findViewById(R.id.root_layout));

        dbHelper = new DBHelper(this);
        try { currency = dbHelper.getStoreSettings().getOrDefault("currency", "ج.م"); } catch (Exception ignored) {}

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) { setSupportActionBar(toolbar); toolbar.setNavigationOnClickListener(v -> finish()); }

        tvTotal0_30   = findViewById(R.id.tv_total_0_30);
        tvTotal31_60  = findViewById(R.id.tv_total_31_60);
        tvTotal61_90  = findViewById(R.id.tv_total_61_90);
        tvTotal90plus = findViewById(R.id.tv_total_90plus);
        tvCount0_30   = findViewById(R.id.tv_count_0_30);
        tvCount31_60  = findViewById(R.id.tv_count_31_60);
        tvCount61_90  = findViewById(R.id.tv_count_61_90);
        tvCount90plus = findViewById(R.id.tv_count_90plus);
        tvEmpty       = findViewById(R.id.tv_empty);

        rvSuppliers = findViewById(R.id.rv_suppliers);
        adapter = new AgingAdapter();
        rvSuppliers.setLayoutManager(new LinearLayoutManager(this));
        rvSuppliers.setAdapter(adapter);

        loadData();
    }

    private void loadData() {
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<HashMap<String, String>> suppliers = dbHelper.getSuppliersWithDebt();
                List<AgingRow> rows = new ArrayList<>();
                double sum0_30 = 0, sum31_60 = 0, sum61_90 = 0, sum90plus = 0;
                int cnt0_30 = 0, cnt31_60 = 0, cnt61_90 = 0, cnt90plus = 0;

                // For suppliers we use last purchase order date from purchase_orders table
                android.database.Cursor lastPoC = dbHelper.getReadableDatabase().rawQuery(
                    "SELECT supplier_id, MAX(created_at) as last_po FROM purchase_orders GROUP BY supplier_id", null);
                Map<String, String> lastPoMap = new HashMap<>();
                while (lastPoC.moveToNext()) {
                    lastPoMap.put(lastPoC.getString(0), lastPoC.getString(1));
                }
                lastPoC.close();

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                Date now = new Date();

                for (HashMap<String, String> s : suppliers) {
                    String supId = s.getOrDefault("id", "0");
                    double debt  = safeDouble(s, "debt");
                    AgingRow row = new AgingRow(
                        s.getOrDefault("name", ""),
                        s.getOrDefault("phone", ""),
                        debt);

                    String lastPo = lastPoMap.getOrDefault(supId, "");
                    long daysSince = 999;
                    if (!lastPo.isEmpty()) {
                        try {
                            Date d = sdf.parse(lastPo);
                            if (d != null)
                                daysSince = (now.getTime() - d.getTime()) / (1000L * 60 * 60 * 24);
                        } catch (Exception ignored) {}
                    }

                    if (daysSince <= 30) {
                        row.bucket0_30 = debt; sum0_30 += debt; cnt0_30++;
                    } else if (daysSince <= 60) {
                        row.bucket31_60 = debt; sum31_60 += debt; cnt31_60++;
                    } else if (daysSince <= 90) {
                        row.bucket61_90 = debt; sum61_90 += debt; cnt61_90++;
                    } else {
                        row.bucket90plus = debt; sum90plus += debt; cnt90plus++;
                    }
                    rows.add(row);
                }

                rows.sort((a, b) -> Double.compare(b.debt, a.debt));

                final List<AgingRow> finalRows = rows;
                final double f0_30 = sum0_30, f31_60 = sum31_60, f61_90 = sum61_90, f90p = sum90plus;
                final int c0 = cnt0_30, c1 = cnt31_60, c2 = cnt61_90, c3 = cnt90plus;
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    data.clear();
                    data.addAll(finalRows);
                    adapter.notifyDataSetChanged();
                    setText(tvTotal0_30,   fmtAmt(f0_30));
                    setText(tvTotal31_60,  fmtAmt(f31_60));
                    setText(tvTotal61_90,  fmtAmt(f61_90));
                    setText(tvTotal90plus, fmtAmt(f90p));
                    setText(tvCount0_30,   c0 + " " + getString(R.string.suppliers_title));
                    setText(tvCount31_60,  c1 + " " + getString(R.string.suppliers_title));
                    setText(tvCount61_90,  c2 + " " + getString(R.string.suppliers_title));
                    setText(tvCount90plus, c3 + " " + getString(R.string.suppliers_title));
                    if (tvEmpty != null) tvEmpty.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
                });
            } catch (Exception e) {
                android.util.Log.e(TAG, "loadData: " + e.getMessage(), e);
            }
        });
    }

    private class AgingAdapter extends RecyclerView.Adapter<AgingAdapter.VH> {
        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_aging_row, parent, false);
            return new VH(v);
        }
        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            if (pos < 0 || pos >= data.size()) return;
            AgingRow row = data.get(pos);
            h.tvName.setText(row.name);
            h.tvDebt.setText(fmtAmt(row.debt));
            h.tvBucket.setText(getBucketLabel(row));
            int clr = getBucketColor(row);
            h.tvDebt.setTextColor(clr);
            h.tvBucket.setTextColor(clr);
        }
        @Override public int getItemCount() { return data.size(); }
        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvDebt, tvBucket;
            VH(@NonNull View v) {
                super(v);
                tvName   = v.findViewById(R.id.tv_name);
                tvDebt   = v.findViewById(R.id.tv_debt);
                tvBucket = v.findViewById(R.id.tv_bucket);
            }
        }
    }

    private String getBucketLabel(AgingRow row) {
        if (row.bucket0_30 > 0)  return "0-30 " + getString(R.string.aging_days);
        if (row.bucket31_60 > 0) return "31-60 " + getString(R.string.aging_days);
        if (row.bucket61_90 > 0) return "61-90 " + getString(R.string.aging_days);
        return "+90 " + getString(R.string.aging_days);
    }

    private int getBucketColor(AgingRow row) {
        if (row.bucket0_30 > 0)  return androidx.core.content.ContextCompat.getColor(this, R.color.color_success);
        if (row.bucket31_60 > 0) return androidx.core.content.ContextCompat.getColor(this, R.color.color_info);
        if (row.bucket61_90 > 0) return 0xFFFF9800;
        return androidx.core.content.ContextCompat.getColor(this, R.color.color_error);
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
