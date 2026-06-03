package com.pos.system;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import com.pos.system.managers.ReviewManager;
import com.google.android.material.snackbar.Snackbar;

/**
 * MainActivity - الصفحة الرئيسية
 *
 * يرث من BaseActivity الذي يتولى تطبيق الثيم واللغة تلقائياً.
 *
 * @author POS System
 * @version 2.0
 * @since 2026-02-17
 */
public class MainActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";

    private DBHelper      dbHelper;
    private ReviewManager reviewManager;
    private com.pos.system.managers.AppUpdateManager appUpdateManager;
    private DrawerLayout  drawerLayout;
    private NavigationView navView;

    // Dashboard Cards
    private MaterialCardView cardTodaySales;
    private MaterialCardView cardTodayInvoices;
    private MaterialCardView cardTotalProducts;
    private MaterialCardView cardLowStock;

    // Stats TextViews
    private TextView tvTodaySales;
    private TextView tvTodayInvoices;
    private TextView tvTotalProducts;
    private TextView tvLowStockCount;

    // Decision Cards
    private MaterialCardView cardExpiryAlert;
    private MaterialCardView cardDeadStock;
    private MaterialCardView cardBestSeller;
    private MaterialCardView cardBestCustomer;
    private TextView tvExpiryCount;
    private TextView tvDeadStockCount;
    private TextView tvBestSellerName;
    private TextView tvBestCustomerName;

    // Action Cards
    private MaterialCardView cardPOS;
    private MaterialCardView cardProducts;
    private MaterialCardView cardInvoices;
    private MaterialCardView cardCustomers;
    private MaterialCardView cardSuppliers;
    private MaterialCardView cardReports;
    private MaterialCardView cardExpenses;
    private MaterialCardView cardSettings;
    private MaterialCardView cardReturns;
    private MaterialCardView cardShifts;
    private MaterialCardView cardDebts;
    private MaterialCardView cardPurchaseOrders;

    // Alert Card
    private MaterialCardView cardAlert;
    private TextView         tvAlertMessage;

    // FAB
    private ExtendedFloatingActionButton fabQuickSale;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // BaseActivity يطبق الثيم واللغة تلقائياً
        super.onCreate(savedInstanceState);

        GlobalExceptionHandler.setup(this);
        // إصلاح: تأكد من اسم الـ layout الصحيح (activity_main وليس main)
        setContentView(R.layout.activity_main);

        try {
            initializeComponents();
            setupUI();
            loadDashboardData();
            checkAlerts();
        } catch (Exception e) {
            GlobalExceptionHandler.handle(this, e);
        }
    }

    private void initializeComponents() {
        dbHelper = new DBHelper(this);
        reviewManager = new ReviewManager(this);
        reviewManager.onAppLaunched();
        // ✅ In-App Update – فحص تحديثات Google Play
        appUpdateManager = new com.pos.system.managers.AppUpdateManager(this);
        appUpdateManager.checkForFlexibleUpdate();
    }

    private void setupUI() {
        setupToolbar();
        initializeViews();
        setupCardClicks();
        setupFAB();
        setupDrawer();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            if (drawerLayout != null) drawerLayout.openDrawer(GravityCompat.START);
        });
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navView      = findViewById(R.id.nav_view);

        cardTodaySales    = findViewById(R.id.card_today_sales);
        cardTodayInvoices = findViewById(R.id.card_today_invoices);
        cardTotalProducts = findViewById(R.id.card_total_products);
        cardLowStock      = findViewById(R.id.card_low_stock);

        tvTodaySales    = findViewById(R.id.tv_today_sales);
        tvTodayInvoices = findViewById(R.id.tv_today_invoices);
        tvTotalProducts = findViewById(R.id.tv_total_products);
        tvLowStockCount = findViewById(R.id.tv_low_stock_count);

        cardPOS            = findViewById(R.id.card_pos);
        cardProducts       = findViewById(R.id.card_products);
        cardInvoices       = findViewById(R.id.card_invoices);
        cardCustomers      = findViewById(R.id.card_customers);
        cardSuppliers      = findViewById(R.id.card_suppliers);
        cardReports        = findViewById(R.id.card_reports);
        cardExpenses       = findViewById(R.id.card_expenses);
        cardSettings       = findViewById(R.id.card_settings);
        cardReturns        = findViewById(R.id.card_returns);
        cardShifts         = findViewById(R.id.card_shifts);
        cardDebts          = findViewById(R.id.card_debts);
        cardPurchaseOrders = findViewById(R.id.card_purchase_orders);

        cardAlert      = findViewById(R.id.card_alert);
        tvAlertMessage = findViewById(R.id.tv_alert_message);

        cardExpiryAlert   = findViewById(R.id.card_expiry_alert);
        cardDeadStock     = findViewById(R.id.card_dead_stock);
        cardBestSeller    = findViewById(R.id.card_best_seller);
        cardBestCustomer  = findViewById(R.id.card_best_customer);
        tvExpiryCount     = findViewById(R.id.tv_expiry_count);
        tvDeadStockCount  = findViewById(R.id.tv_dead_stock_count);
        tvBestSellerName  = findViewById(R.id.tv_best_seller_name);
        tvBestCustomerName = findViewById(R.id.tv_best_customer_name);

        fabQuickSale = findViewById(R.id.fab_quick_sale);
    }

    private void setupCardClicks() {
        if (cardTodaySales  != null) cardTodaySales.setOnClickListener(v -> openActivity(ActivityReportsActivity.class));
        if (cardLowStock    != null) cardLowStock.setOnClickListener(v -> showLowStockDialog());
        if (cardExpiryAlert != null) cardExpiryAlert.setOnClickListener(v -> openActivity(ActivityProductsActivity.class));
        if (cardDeadStock   != null) cardDeadStock.setOnClickListener(v -> openActivity(ActivityProductsActivity.class));
        if (cardBestSeller  != null) cardBestSeller.setOnClickListener(v -> openActivity(ActivityReportsActivity.class));
        if (cardBestCustomer != null) cardBestCustomer.setOnClickListener(v -> openActivity(ActivityCustomersActivity.class));

        setCardClick(cardPOS,            ActivityCartActivity.class);
        setCardClick(cardProducts,       ActivityProductsActivity.class);
        setCardClick(cardInvoices,       ActivityInvoicesActivity.class);
        setCardClick(cardCustomers,      ActivityCustomersActivity.class);
        setCardClick(cardSuppliers,      ActivitySuppliersActivity.class);
        setCardClick(cardReports,        ActivityReportsActivity.class);
        setCardClick(cardExpenses,       ActivityExpensesActivity.class);
        setCardClick(cardSettings,       ActivitySettingsActivity.class);
        setCardClick(cardReturns,        ActivityReturnActivity.class);
        setCardClick(cardShifts,         ActivityShiftActivity.class);
        setCardClick(cardDebts,          ActivityDebtActivity.class);
        setCardClick(cardPurchaseOrders, ActivityPurchaseOrderActivity.class);
    }

    private void setCardClick(MaterialCardView card, Class<?> cls) {
        if (card != null) {
            card.setOnClickListener(v -> openActivity(cls));
        }
    }

    private void openActivity(Class<?> cls) {
        startActivity(new Intent(this, cls));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void setupFAB() {
        if (fabQuickSale != null) {
            fabQuickSale.setOnClickListener(v -> openActivity(ActivityCartActivity.class));
        }
    }

    private void setupDrawer() {
        if (navView != null) {
            navView.setNavigationItemSelectedListener(this);
            View headerView = navView.getHeaderView(0);
            if (headerView != null) updateDrawerHeader(headerView);
        }
    }

    private void updateDrawerHeader(View headerView) {
        TextView  tvStoreName  = headerView.findViewById(R.id.tv_store_name);
        TextView  tvStorePhone = headerView.findViewById(R.id.tv_store_phone);
        try {
            HashMap<String, String> settings = dbHelper.getStoreSettings();
            if (settings != null) {
                if (tvStoreName  != null) tvStoreName.setText(settings.getOrDefault("name", "متجري"));
                if (tvStorePhone != null) tvStorePhone.setText(settings.getOrDefault("phone", ""));
            }
        } catch (Exception ignored) {}
    }

    private void loadDashboardData() {
        java.util.concurrent.ExecutorService exec =
            java.util.concurrent.Executors.newSingleThreadExecutor();
        exec.execute(() -> {
            try {
                final HashMap<String, Object> stats = dbHelper.getInvoicesStatistics();
                final int totalProducts = dbHelper.getProductsCount();
                final java.util.List<HashMap<String, String>> lowStock = dbHelper.getLowStockProducts(5);
                final java.util.List<HashMap<String, String>> expiring = dbHelper.getExpiringProducts(30);
                final java.util.List<HashMap<String, String>> deadStock = dbHelper.getDeadStockProducts(60);
                final HashMap<String, String> topSeller   = dbHelper.getTopSellerThisWeek();
                final HashMap<String, String> topCustomer = dbHelper.getTopCustomerThisMonth();
                final String currency = getCurrencySymbol();

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    try {
                        double todaySales    = 0;
                        int    todayInvoices = 0;
                        if (stats != null) {
                            Object s = stats.get("today_total");
                            Object c = stats.get("today_count");
                            if (s != null) todaySales    = ((Number) s).doubleValue();
                            if (c != null) todayInvoices = ((Number) c).intValue();
                        }
                        if (tvTodaySales    != null) tvTodaySales.setText(String.format("%.2f %s", todaySales, currency));
                        if (tvTodayInvoices != null) tvTodayInvoices.setText(String.valueOf(todayInvoices));
                        if (tvTotalProducts != null) tvTotalProducts.setText(String.valueOf(totalProducts));
                        if (tvLowStockCount != null) tvLowStockCount.setText(lowStock != null ? String.valueOf(lowStock.size()) : "0");
                        if (tvExpiryCount   != null) tvExpiryCount.setText(expiring  != null ? String.valueOf(expiring.size())  : "0");
                        if (tvDeadStockCount != null) tvDeadStockCount.setText(deadStock != null ? String.valueOf(deadStock.size()) : "0");
                        if (tvBestSellerName  != null) tvBestSellerName.setText(topSeller   != null ? topSeller.getOrDefault("name",   "—") : "—");
                        if (tvBestCustomerName != null) tvBestCustomerName.setText(topCustomer != null ? topCustomer.getOrDefault("name", "—") : "—");
                    } catch (Exception ignored) {}
                });
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "loadDashboardData bg: " + e.getMessage(), e);
            }
        });
    }

    private String getCurrencySymbol() {
        try {
            HashMap<String, String> settings = dbHelper.getStoreSettings();
            return settings.getOrDefault("currency", "ج.م");
        } catch (Exception e) {
            return "ج.م";
        }
    }

    private void checkAlerts() {
        try {
            List<HashMap<String, String>> lowStock = dbHelper.getLowStockProducts(5);
            if (lowStock != null && !lowStock.isEmpty()) {
                if (cardAlert    != null) cardAlert.setVisibility(View.VISIBLE);
                if (tvAlertMessage != null)
                    tvAlertMessage.setText(getString(R.string.low_stock_alert_message, lowStock.size()));
                if (cardAlert != null) cardAlert.setOnClickListener(v -> showLowStockDialog());
            } else {
                if (cardAlert != null) cardAlert.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        showTrialBannerIfNeeded();
    }

    private void showTrialBannerIfNeeded() {
        try {
            int days = FeatureGate.remainingTrialDays(this);
            // days == -1 means premium; days == 0 means trial expired; days > 0 means active trial
            if (days > 0) {
                View root = findViewById(android.R.id.content);
                if (root == null) return;
                String msg = days == 1
                    ? "آخر يوم في الفترة التجريبية — اشترك الآن"
                    : "تبقّى " + days + " يوم في الفترة التجريبية";
                Snackbar.make(root, msg, Snackbar.LENGTH_LONG)
                    .setAction("ترقية", v -> openActivity(ActivitySettingsActivity.class))
                    .setBackgroundTint(0xFF1565C0)
                    .setTextColor(0xFFFFFFFF)
                    .setActionTextColor(0xFFFFD54F)
                    .show();
            }
        } catch (Exception ignored) {}
    }

    private void showLowStockDialog() {
        try {
            List<HashMap<String, String>> lowStock = dbHelper.getLowStockProducts(5);
            if (lowStock == null || lowStock.isEmpty()) {
                showToast(getString(R.string.no_low_stock));
                return;
            }
            StringBuilder msg = new StringBuilder();
            for (HashMap<String, String> p : lowStock) {
                msg.append("• ").append(p.get("name")).append(" (").append(p.get("qty")).append(")\n");
            }
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.low_stock_title, lowStock.size()))
                    .setMessage(msg.toString())
                    .setPositiveButton(R.string.view_products, (d, w) -> openActivity(ActivityProductsActivity.class))
                    .setNegativeButton(R.string.close, null)
                    .show();
        } catch (Exception e) {
            showToast(getString(R.string.error_unknown));
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        try {
            if      (id == R.id.nav_pos)             openActivity(ActivityCartActivity.class);
            else if (id == R.id.nav_products)        openActivity(ActivityProductsActivity.class);
            else if (id == R.id.nav_customers)       openActivity(ActivityCustomersActivity.class);
            else if (id == R.id.nav_suppliers)       openActivity(ActivitySuppliersActivity.class);
            else if (id == R.id.nav_invoices)        openActivity(ActivityInvoicesActivity.class);
            else if (id == R.id.nav_reports)         openActivity(ActivityReportsActivity.class);
            else if (id == R.id.nav_expenses)        openActivity(ActivityExpensesActivity.class);
            else if (id == R.id.nav_settings)        openActivity(ActivitySettingsActivity.class);
            else if (id == R.id.nav_backup)          openActivity(ActivityBackupActivity.class);
            else if (id == R.id.nav_shifts)          openActivity(ActivityShiftActivity.class);
            else if (id == R.id.nav_returns)         openActivity(ActivityReturnActivity.class);
            else if (id == R.id.nav_debts)           openActivity(ActivityDebtActivity.class);
            else if (id == R.id.nav_purchase_orders) openActivity(ActivityPurchaseOrderActivity.class);
            else if (id == R.id.nav_about)           showAboutDialog();
            else if (id == R.id.nav_logout)          confirmLogout();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showAboutDialog() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.about_app)
                .setMessage(getString(R.string.about_message))
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    private void confirmLogout() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.logout)
                .setMessage(R.string.logout_confirm)
                .setPositiveButton(R.string.yes, (d, w) -> {
                    AuthActivity.logout(this);
                    finish();
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            loadDashboardData();
            checkAlerts();
            // ✅ استئناف تحديث كامل التنزيل
            if (appUpdateManager != null) appUpdateManager.onResume();
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "onResume: " + e.getMessage(), e);
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            try { dbHelper.close(); } catch (Exception ignored) {}
        }
    }
}
