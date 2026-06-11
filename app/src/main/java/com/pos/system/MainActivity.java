package com.pos.system;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.pos.system.managers.ReviewManager;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * MainActivity — Photo-card grid navigation (inspired by Cashier competitor design).
 * 2-column grid, each card = gradient background + icon + label.
 * Material Expressive: large corners (20dp), spring-like press animation.
 */
public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";

    private DBHelper dbHelper;
    private ReviewManager reviewManager;
    private com.pos.system.managers.AppUpdateManager appUpdateManager;

    private RecyclerView rvGrid;
    private NavAdapter adapter;

    // Header stat views (set after RecyclerView inflates the header)
    private TextView tvHeaderSales;
    private TextView tvHeaderInvoices;
    private TextView tvHeaderLowStock;
    private String currency = "ج.م";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        applyWindowInsets(findViewById(R.id.root_layout));

        try {
            dbHelper = new DBHelper(this);
            currency = dbHelper.getStoreSettings().getOrDefault("currency", "ج.م");

            reviewManager = new ReviewManager(this);
            reviewManager.onAppLaunched();

            appUpdateManager = new com.pos.system.managers.AppUpdateManager(this);
            appUpdateManager.checkForFlexibleUpdate();

            setupToolbar();
            setupGrid();
            loadDashboardData();
            showTrialBannerIfNeeded();
        } catch (Exception e) {
            android.util.Log.e(TAG, "onCreate error", e);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Toolbar
    // ─────────────────────────────────────────────────────────────

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar == null) return;
        setSupportActionBar(toolbar);
        try {
            String storeName = dbHelper.getStoreSettings().getOrDefault("name", getString(R.string.app_name));
            toolbar.setTitle(storeName);
        } catch (Exception e) {
            toolbar.setTitle(R.string.app_name);
        }
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_settings) {
                openActivity(ActivitySettingsActivity.class);
                return true;
            } else if (id == R.id.action_search) {
                openActivity(ActivityProductsActivity.class);
                return true;
            }
            return false;
        });
    }

    // ─────────────────────────────────────────────────────────────
    // Grid setup
    // ─────────────────────────────────────────────────────────────

    private void setupGrid() {
        rvGrid = findViewById(R.id.rv_nav_grid);
        if (rvGrid == null) return;

        GridLayoutManager glm = new GridLayoutManager(this, 2);
        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override public int getSpanSize(int position) {
                // Header item spans both columns
                return adapter.getItemViewType(position) == NavAdapter.TYPE_HEADER ? 2 : 1;
            }
        });
        rvGrid.setLayoutManager(glm);

        adapter = new NavAdapter(buildNavItems());
        rvGrid.setAdapter(adapter);
    }

    // ─────────────────────────────────────────────────────────────
    // Nav items definition
    // ─────────────────────────────────────────────────────────────

    private List<NavItem> buildNavItems() {
        List<NavItem> items = new ArrayList<>();
        // null = header placeholder
        items.add(null);

        items.add(new NavItem(R.string.tab_sales,       R.drawable.ic_pos,            R.drawable.bg_card_sales,      ActivityCartActivity.class));
        items.add(new NavItem(R.string.purchase_orders_title, R.drawable.ic_local_shipping, R.drawable.bg_card_purchase, ActivityPurchaseOrderActivity.class));
        items.add(new NavItem(R.string.products_title,  R.drawable.ic_inventory,      R.drawable.bg_card_inventory,  ActivityProductsActivity.class));
        items.add(new NavItem(R.string.customers_title, R.drawable.ic_customers,      R.drawable.bg_card_customers,  ActivityCustomersActivity.class));
        items.add(new NavItem(R.string.suppliers_title, R.drawable.ic_suppliers,      R.drawable.bg_card_suppliers,  ActivitySuppliersActivity.class));
        items.add(new NavItem(R.string.reports_title,   R.drawable.ic_bar_chart,      R.drawable.bg_card_reports,    ActivityReportsActivity.class));
        items.add(new NavItem(R.string.expenses_title,  R.drawable.ic_expenses,       R.drawable.bg_card_expenses,   ActivityExpensesActivity.class));
        items.add(new NavItem(R.string.invoices_title,  R.drawable.ic_invoices,       R.drawable.bg_card_accounts,   ActivityInvoicesActivity.class));
        items.add(new NavItem(R.string.hub_treasury,    R.drawable.ic_treasury,       R.drawable.bg_card_treasury,   ActivityCashDrawerActivity.class));
        items.add(new NavItem(R.string.print_barcodes_title, R.drawable.ic_print,    R.drawable.bg_card_barcode,    ActivityBarcodeLabelActivity.class));
        items.add(new NavItem(R.string.settings_title,  R.drawable.ic_settings,       R.drawable.bg_card_settings,   ActivitySettingsActivity.class));
        items.add(new NavItem(R.string.nav_users,       R.drawable.ic_people,         R.drawable.bg_card_users,      ActivityUsersActivity.class));
        return items;
    }

    static class NavItem {
        int titleRes;
        int iconRes;
        int bgRes;
        Class<?> destination;
        NavItem(int t, int i, int b, Class<?> d) { titleRes=t; iconRes=i; bgRes=b; destination=d; }
    }

    // ─────────────────────────────────────────────────────────────
    // Adapter
    // ─────────────────────────────────────────────────────────────

    class NavAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        static final int TYPE_HEADER = 0;
        static final int TYPE_CARD   = 1;

        private final List<NavItem> items;

        NavAdapter(List<NavItem> items) { this.items = items; }

        @Override public int getItemViewType(int position) {
            return items.get(position) == null ? TYPE_HEADER : TYPE_CARD;
        }

        @NonNull @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inf = LayoutInflater.from(parent.getContext());
            if (viewType == TYPE_HEADER) {
                View v = inf.inflate(R.layout.item_dashboard_header, parent, false);
                return new HeaderVH(v);
            }
            View v = inf.inflate(R.layout.item_nav_card, parent, false);
            return new CardVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof HeaderVH) {
                ((HeaderVH) holder).bind();
            } else {
                ((CardVH) holder).bind(items.get(position));
            }
        }

        @Override public int getItemCount() { return items.size(); }
    }

    // Header ViewHolder
    class HeaderVH extends RecyclerView.ViewHolder {
        HeaderVH(@NonNull View itemView) {
            super(itemView);
            tvHeaderSales     = itemView.findViewById(R.id.tv_header_sales);
            tvHeaderInvoices  = itemView.findViewById(R.id.tv_header_invoices);
            tvHeaderLowStock  = itemView.findViewById(R.id.tv_header_low_stock);
            View btnPos = itemView.findViewById(R.id.btn_quick_pos);
            if (btnPos != null) btnPos.setOnClickListener(v -> openActivity(ActivityCartActivity.class));
        }
        void bind() { /* stats loaded by loadDashboardData */ }
    }

    // Card ViewHolder
    class CardVH extends RecyclerView.ViewHolder {
        MaterialCardView card;
        View viewBg;
        android.widget.ImageView imgIcon;
        TextView tvLabel;

        CardVH(@NonNull View itemView) {
            super(itemView);
            card    = itemView.findViewById(R.id.card_root);
            viewBg  = itemView.findViewById(R.id.view_bg);
            imgIcon = itemView.findViewById(R.id.img_icon);
            tvLabel = itemView.findViewById(R.id.tv_label);
        }

        void bind(NavItem item) {
            if (item == null) return;
            viewBg.setBackgroundResource(item.bgRes);
            imgIcon.setImageResource(item.iconRes);
            // Tint icon white
            imgIcon.setColorFilter(0xFFFFFFFF);
            tvLabel.setText(getString(item.titleRes));

            card.setOnClickListener(v -> {
                animatePress(card);
                openActivity(item.destination);
            });
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Press animation (Material Expressive)
    // ─────────────────────────────────────────────────────────────

    private void animatePress(View v) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(v, "scaleX", 1f, 0.93f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(v, "scaleY", 1f, 0.93f, 1f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY);
        set.setDuration(200);
        set.setInterpolator(new android.view.animation.OvershootInterpolator(2f));
        set.start();
    }

    // ─────────────────────────────────────────────────────────────
    // Dashboard data
    // ─────────────────────────────────────────────────────────────

    private void loadDashboardData() {
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                HashMap<String, Object> stats = dbHelper.getInvoicesStatistics();
                List<HashMap<String, String>> lowStock = dbHelper.getLowStockProducts(5);
                double todaySales = 0; int todayInvoices = 0;
                if (stats != null) {
                    Object s = stats.get("today_total"); Object c = stats.get("today_count");
                    if (s != null) todaySales    = ((Number) s).doubleValue();
                    if (c != null) todayInvoices = ((Number) c).intValue();
                }
                int lowCount = lowStock != null ? lowStock.size() : 0;
                final double fSales = todaySales;
                final int fInvoices = todayInvoices, fLow = lowCount;
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (tvHeaderSales    != null) tvHeaderSales.setText(
                        String.format(java.util.Locale.US, "%.2f %s", fSales, currency));
                    if (tvHeaderInvoices != null) tvHeaderInvoices.setText(String.valueOf(fInvoices));
                    if (tvHeaderLowStock != null) tvHeaderLowStock.setText(String.valueOf(fLow));
                    showLowStockAlertIfNeeded(fLow);
                });
            } catch (Exception e) {
                android.util.Log.e(TAG, "loadDashboardData error", e);
            }
        });
    }

    private void showLowStockAlertIfNeeded(int lowCount) {
        if (lowCount > 0) {
            View root = findViewById(R.id.root_layout);
            if (root != null) {
                Snackbar.make(root, getString(R.string.low_stock_alert_message, lowCount), Snackbar.LENGTH_LONG)
                    .setAction(R.string.view_products, v -> openActivity(ActivityProductsActivity.class))
                    .show();
            }
        }
    }

    private void showTrialBannerIfNeeded() {
        try {
            int days = FeatureGate.remainingTrialDays(this);
            if (days > 0) {
                View root = findViewById(R.id.root_layout);
                if (root == null) return;
                String msg = days == 1
                    ? "آخر يوم في الفترة التجريبية — اشترك الآن"
                    : "تبقّى " + days + " يوم في الفترة التجريبية";
                Snackbar.make(root, msg, Snackbar.LENGTH_LONG)
                    .setAction("ترقية", v -> openActivity(ActivitySettingsActivity.class))
                    .show();
            }
        } catch (Exception ignored) {}
    }

    // ─────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────

    @Override
    protected void onResume() {
        super.onResume();
        try {
            loadDashboardData();
            if (appUpdateManager != null) appUpdateManager.onResume();
            // Refresh toolbar title with store name
            MaterialToolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                String storeName = dbHelper.getStoreSettings().getOrDefault("name", getString(R.string.app_name));
                toolbar.setTitle(storeName);
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "onResume error", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { if (dbHelper != null) dbHelper.close(); } catch (Exception ignored) {}
    }

    protected void openActivity(Class<?> cls) {
        try { startActivity(new Intent(this, cls)); } catch (Exception e) {
            android.util.Log.e(TAG, "openActivity error: " + cls.getSimpleName(), e);
        }
    }
}
