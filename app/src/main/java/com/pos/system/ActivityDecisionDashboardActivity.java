package com.pos.system;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * لوحة القرارات الذكية — تجيب على 4 أسئلة رئيسية:
 * 1. ماذا أشتري الآن؟   (low stock → reorder)
 * 2. ماذا سيتلف؟        (expiring in 7 days)
 * 3. ما الذي لا يتحرك؟  (dead stock 30 days)
 * 4. أضعف المنتجات ربحاً (low margin last 30 days)
 */
public class ActivityDecisionDashboardActivity extends BaseActivity {

    private DBHelper dbHelper;

    private TextView tvReorderCount, tvExpiryRiskCount, tvDeadCount;
    private RecyclerView rvReorder, rvExpiryRisk, rvDeadStock, rvLowMargin;
    private MaterialButton btnCreatePO, btnViewExpiry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decision_dashboard);

        dbHelper = new DBHelper(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvReorderCount    = findViewById(R.id.tv_reorder_count);
        tvExpiryRiskCount = findViewById(R.id.tv_expiry_risk_count);
        tvDeadCount       = findViewById(R.id.tv_dead_count);

        rvReorder    = findViewById(R.id.rv_reorder);
        rvExpiryRisk = findViewById(R.id.rv_expiry_risk);
        rvDeadStock  = findViewById(R.id.rv_dead_stock);
        rvLowMargin  = findViewById(R.id.rv_low_margin);

        setupRecyclers();

        btnCreatePO   = findViewById(R.id.btn_create_po);
        btnViewExpiry = findViewById(R.id.btn_view_expiry);

        btnCreatePO.setOnClickListener(v ->
            startActivity(new Intent(this, ActivityPurchaseOrderActivity.class)));
        btnViewExpiry.setOnClickListener(v ->
            startActivity(new Intent(this, ActivityExpiryDashboardActivity.class)));

        loadAllSections();
    }

    private void setupRecyclers() {
        rvReorder.setLayoutManager(new LinearLayoutManager(this));
        rvReorder.setNestedScrollingEnabled(false);

        rvExpiryRisk.setLayoutManager(new LinearLayoutManager(this));
        rvExpiryRisk.setNestedScrollingEnabled(false);

        rvDeadStock.setLayoutManager(new LinearLayoutManager(this));
        rvDeadStock.setNestedScrollingEnabled(false);

        rvLowMargin.setLayoutManager(new LinearLayoutManager(this));
        rvLowMargin.setNestedScrollingEnabled(false);
    }

    private void loadAllSections() {
        try {
            // 1. ماذا أشتري؟
            List<HashMap<String, String>> lowStock = dbHelper.getLowStockProducts();
            if (lowStock == null) lowStock = new ArrayList<>();
            tvReorderCount.setText(String.valueOf(lowStock.size()));
            rvReorder.setAdapter(new SimpleProductAdapter(lowStock.subList(0, Math.min(5, lowStock.size())), "reorder"));

            // 2. ماذا سيتلف؟
            List<HashMap<String, String>> expiring = dbHelper.getExpiringProducts(7);
            if (expiring == null) expiring = new ArrayList<>();
            tvExpiryRiskCount.setText(String.valueOf(expiring.size()));
            rvExpiryRisk.setAdapter(new SimpleProductAdapter(expiring.subList(0, Math.min(5, expiring.size())), "expiry"));

            // 3. ما الذي لا يتحرك؟
            List<HashMap<String, String>> dead = dbHelper.getDeadStockProducts(30);
            if (dead == null) dead = new ArrayList<>();
            tvDeadCount.setText(String.valueOf(dead.size()));
            rvDeadStock.setAdapter(new SimpleProductAdapter(dead.subList(0, Math.min(5, dead.size())), "dead"));

            // 4. أضعف المنتجات ربحاً
            String end   = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -30);
            String start = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());
            List<HashMap<String, String>> lowMargin = dbHelper.getProfitByProduct(start, end);
            if (lowMargin == null) lowMargin = new ArrayList<>();
            rvLowMargin.setAdapter(new SimpleProductAdapter(
                lowMargin.subList(0, Math.min(5, lowMargin.size())), "margin"));

        } catch (Exception e) {
            showSnackbar("خطأ في تحميل البيانات", true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) dbHelper.close();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Simple list adapter for all 4 sections
    // ─────────────────────────────────────────────────────────────────────────

    private static class SimpleProductAdapter
            extends RecyclerView.Adapter<SimpleProductAdapter.VH> {

        private final List<HashMap<String, String>> data;
        private final String type;

        SimpleProductAdapter(List<HashMap<String, String>> data, String type) {
            this.data = new ArrayList<>(data);
            this.type = type;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.two_line_list_item, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            HashMap<String, String> p = data.get(pos);
            switch (type) {
                case "reorder":
                    h.text1.setText(p.getOrDefault("name", "—"));
                    h.text2.setText("المخزون: " + p.getOrDefault("qty", "0")
                        + " · حد الطلب: " + p.getOrDefault("reorder_level", "5"));
                    break;
                case "expiry":
                    h.text1.setText(p.getOrDefault("name", "—"));
                    h.text2.setText("ينتهي: " + p.getOrDefault("expiry", "—")
                        + " · الكمية: " + p.getOrDefault("qty", "0"));
                    break;
                case "dead":
                    h.text1.setText(p.getOrDefault("name", "—"));
                    h.text2.setText("المخزون: " + p.getOrDefault("qty", "0")
                        + " · لم يُباع منذ 30 يوم");
                    break;
                case "margin":
                    h.text1.setText(p.getOrDefault("name", "—"));
                    String margin = p.getOrDefault("margin_pct", "0");
                    String profit = p.getOrDefault("profit", "0");
                    h.text2.setText("هامش: " + margin + "% · الربح: " + profit);
                    break;
            }
        }

        @Override public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView text1, text2;
            VH(@NonNull View v) {
                super(v);
                text1 = v.findViewById(android.R.id.text1);
                text2 = v.findViewById(android.R.id.text2);
                if (text1 != null) {
                    text1.setTextSize(13f);
                    text1.setMaxLines(1);
                    text1.setEllipsize(android.text.TextUtils.TruncateAt.END);
                }
                if (text2 != null) {
                    text2.setTextSize(11f);
                }
            }
        }
    }
}
