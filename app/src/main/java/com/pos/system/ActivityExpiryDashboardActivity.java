package com.pos.system;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import com.pos.system.databinding.ActivityExpiryDashboardBinding;

/**
 * لوحة إدارة الصلاحية — ثلاثة تبويبات:
 * - منتهية الصلاحية (أحمر)
 * - تنتهي خلال 7 أيام (برتقالي)
 * - تنتهي خلال 30 يوم (أصفر)
 */
public class ActivityExpiryDashboardActivity extends BaseActivity {

    private ActivityExpiryDashboardBinding binding;


    private static final int TAB_EXPIRED    = 0;
    private static final int TAB_CRITICAL   = 1;  // 7 days
    private static final int TAB_WARNING    = 2;  // 30 days

    private int COLOR_EXPIRED;
    private int COLOR_CRITICAL;
    private int COLOR_WARNING;
    private int COLOR_OK;

    private DBHelper      dbHelper;
    private TabLayout     tabLayout;
    private RecyclerView  rvExpiry;
    private LinearLayout  layoutEmpty;
    private TextView      tvSummary;
    private ExpiryAdapter adapter;
    private int           currentTab = TAB_EXPIRED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExpiryDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets(binding.getRoot());

        dbHelper = new DBHelper(this);
        COLOR_EXPIRED  = androidx.core.content.ContextCompat.getColor(this, R.color.color_error);
        COLOR_CRITICAL = androidx.core.content.ContextCompat.getColor(this, R.color.color_warning);
        COLOR_WARNING  = androidx.core.content.ContextCompat.getColor(this, R.color.color_gold);
        COLOR_OK       = androidx.core.content.ContextCompat.getColor(this, R.color.color_success);

        MaterialToolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tabLayout    = binding.tabLayout;
        rvExpiry     = binding.rvExpiry;
        layoutEmpty  = binding.layoutEmpty;
        tvSummary    = binding.tvSummary;

        tabLayout.addTab(tabLayout.newTab().setText(R.string.expiry_tab_expired));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.expiry_tab_7days));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.expiry_tab_30days));

        adapter = new ExpiryAdapter();
        rvExpiry.setLayoutManager(new LinearLayoutManager(this));
        rvExpiry.setAdapter(adapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                loadCurrentTab();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        binding.btnMarkAllReviewed.setOnClickListener(v -> markAllReviewed());

        loadSummary();
        loadCurrentTab();
    }

    private void loadSummary() {
        try {
            int expired   = dbHelper.getExpiredProducts().size();
            int critical  = dbHelper.getExpiringProducts(7).size();
            int warning   = dbHelper.getExpiringProducts(30).size() - critical;
            tvSummary.setText(
                "منتهية: " + expired + " · خطر: " + critical + " · تحذير: " + warning);
        } catch (Exception ignored) {}
    }

    private void loadCurrentTab() {
        try {
            List<HashMap<String, String>> products;
            int barColor;
            switch (currentTab) {
                case TAB_CRITICAL:
                    products = dbHelper.getExpiringProducts(7);
                    barColor = COLOR_CRITICAL;
                    break;
                case TAB_WARNING:
                    List<HashMap<String, String>> all30  = dbHelper.getExpiringProducts(30);
                    List<HashMap<String, String>> all7   = dbHelper.getExpiringProducts(7);
                    products = new ArrayList<>(all30);
                    products.removeAll(all7);
                    barColor = COLOR_WARNING;
                    break;
                default:
                    products = dbHelper.getExpiredProducts();
                    barColor = COLOR_EXPIRED;
                    break;
            }
            adapter.setData(products, barColor, currentTab);
            boolean empty = products.isEmpty();
            rvExpiry.setVisibility(empty ? View.GONE : View.VISIBLE);
            layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        } catch (Exception e) {
            showSnackbar("خطأ في تحميل البيانات", true);
        }
    }

    private void markAllReviewed() {
        List<String> ids = adapter.getProductIds();
        if (ids.isEmpty()) {
            showSnackbar(getString(R.string.expiry_nothing_to_review), false);
            return;
        }
        int count = ids.size();
        new MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.expiry_mark_reviewed_title))
            .setMessage(getString(R.string.expiry_mark_reviewed_msg, count))
            .setPositiveButton(getString(R.string.ok), (d, w) -> {
                int marked = dbHelper.markExpiryReviewed(ids);
                loadSummary();
                loadCurrentTab();
                showSnackbar(getString(R.string.expiry_marked_done, marked), false);
            })
            .setNegativeButton(getString(R.string.cancel), null)
            .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) dbHelper.close();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Adapter
    // ─────────────────────────────────────────────────────────────────────────

    private static class ExpiryAdapter extends RecyclerView.Adapter<ExpiryAdapter.VH> {

        private final List<HashMap<String, String>> data = new ArrayList<>();
        private int barColor = 0;
        private int tabType  = TAB_EXPIRED;

        void setData(List<HashMap<String, String>> list, int color, int tab) {
            data.clear();
            if (list != null) data.addAll(list);
            barColor = color;
            tabType  = tab;
            notifyDataSetChanged();
        }

        List<String> getProductIds() {
            List<String> ids = new ArrayList<>();
            for (HashMap<String, String> p : data) {
                String id = p.get("id");
                if (id != null && !id.isEmpty()) ids.add(id);
            }
            return ids;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expiry_product, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            if (pos < 0 || pos >= data.size()) return;
            HashMap<String, String> p = data.get(pos);
            String name    = p.getOrDefault("name", "—");
            String expiry  = p.getOrDefault("expiry", "");
            String batch   = p.getOrDefault("batch_number", "");
            String supplier= p.getOrDefault("supplier", "");
            String qty     = p.getOrDefault("qty", "0");

            h.tvName.setText(name);
            h.tvBatch.setText("دفعة: " + (batch.isEmpty() ? "—" : batch));
            h.tvSupplier.setText(supplier.isEmpty() ? "" : "المورد: " + supplier);
            h.tvQty.setText(qty + " وحدة");

            // Status bar color
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(barColor);
            bg.setCornerRadius(4f);
            h.viewBar.setBackground(bg);

            // Expiry badge
            if (!expiry.isEmpty()) {
                long days = daysUntilExpiry(expiry);
                if (tabType == TAB_EXPIRED) {
                    long ago = Math.abs(days);
                    h.tvBadge.setText("منتهية منذ " + ago + " يوم");
                    h.tvBadge.setBackgroundColor(barColor);
                } else {
                    h.tvBadge.setText("تنتهي: " + expiry + " (" + days + " يوم)");
                    h.tvBadge.setBackgroundColor(barColor);
                }
            } else {
                h.tvBadge.setText(expiry.isEmpty() ? "—" : expiry);
            }
        }

        private long daysUntilExpiry(String expiryStr) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                Date expDate = sdf.parse(expiryStr);
                if (expDate == null) return 0;
                long diff = expDate.getTime() - System.currentTimeMillis();
                return TimeUnit.MILLISECONDS.toDays(diff);
            } catch (ParseException e) {
                return 0;
            }
        }

        @Override public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            View     viewBar;
            TextView tvName, tvBatch, tvSupplier, tvQty, tvBadge;
            VH(@NonNull View v) {
                super(v);
                viewBar    = v.findViewById(R.id.view_status_bar);
                tvName     = v.findViewById(R.id.tv_product_name);
                tvBatch    = v.findViewById(R.id.tv_batch);
                tvSupplier = v.findViewById(R.id.tv_supplier);
                tvQty      = v.findViewById(R.id.tv_qty);
                tvBadge    = v.findViewById(R.id.tv_expiry_badge);
            }
        }
    }
}
