package com.pos.system;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import java.util.HashMap;
import java.util.List;
import com.pos.system.managers.ReviewManager;
import com.google.android.material.snackbar.Snackbar;
import com.pos.system.databinding.ActivityMainBinding;

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

    private ActivityMainBinding binding;

    private static final String TAG = "MainActivity";

    private DBHelper      dbHelper;
    private ReviewManager reviewManager;
    private com.pos.system.managers.AppUpdateManager appUpdateManager;
    private DrawerLayout  drawerLayout;
    private NavigationView navView;

    // Dashboard Cards
    private MaterialCardView cardTodaySales;
    private MaterialCardView cardTodayInvoices;
    private MaterialCardView cardLowStock;

    // Stats TextViews
    private TextView tvTodaySales;
    private TextView tvTodayInvoices;
    private TextView tvLowStockCount;

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
    private MaterialCardView cardChecks;
    private MaterialCardView cardInstallments;
    private MaterialCardView cardCashDrawer;
    private MaterialCardView cardStockCount;
    private MaterialCardView cardUsers;
    private MaterialCardView cardWallet;
    private MaterialCardView cardPriceQuotes;
    private MaterialCardView cardCustomerRemaining;
    private MaterialCardView cardSupplierRemaining;

    // Alert Card
    private MaterialCardView cardAlert;
    private TextView         tvAlertMessage;

    // FAB
    private ExtendedFloatingActionButton fabQuickSale;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // BaseActivity يطبق الثيم واللغة تلقائياً
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        try {
            initializeComponents();
            setupUI();
            loadDashboardData();
            checkAlerts();
        } catch (Exception e) {
            android.util.Log.e(TAG, "MainActivity init error", e);
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
        MaterialToolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            if (drawerLayout != null) drawerLayout.openDrawer(GravityCompat.START);
        });
    }

    private void initializeViews() {
        drawerLayout = binding.drawerLayout;
        navView      = binding.navView;

        cardTodaySales    = binding.cardTodaySales;
        cardTodayInvoices = binding.cardTodayInvoices;
        cardLowStock      = binding.cardLowStock;

        tvTodaySales    = binding.tvTodaySales;
        tvTodayInvoices = binding.tvTodayInvoices;
        tvLowStockCount = binding.tvLowStockCount;

        cardPOS            = binding.cardPos;
        cardProducts       = binding.cardProducts;
        cardInvoices       = binding.cardInvoices;
        cardCustomers      = binding.cardCustomers;
        cardSuppliers      = binding.cardSuppliers;
        cardReports        = binding.cardReports;
        cardExpenses       = binding.cardExpenses;
        cardSettings       = binding.cardSettings;
        cardReturns        = binding.cardReturns;
        cardShifts         = binding.cardShifts;
        cardDebts          = binding.cardDebts;
        cardPurchaseOrders = binding.cardPurchaseOrders;
        cardChecks        = binding.cardChecks;
        cardInstallments  = binding.cardInstallments;
        cardCashDrawer    = binding.cardCashDrawer;
        cardStockCount    = binding.cardStockCount;
        cardUsers         = binding.cardUsers;
        cardWallet            = binding.cardWallet;
        cardPriceQuotes       = binding.cardPriceQuotes;
        cardCustomerRemaining = binding.cardCustomerRemaining;
        cardSupplierRemaining = binding.cardSupplierRemaining;

        cardAlert      = binding.cardAlert;
        tvAlertMessage = binding.tvAlertMessage;

        fabQuickSale = binding.fabQuickSale;
    }

    private void setupCardClicks() {
        if (cardTodaySales  != null) cardTodaySales.setOnClickListener(v -> openActivity(ActivityReportsActivity.class));
        if (cardLowStock    != null) cardLowStock.setOnClickListener(v -> showLowStockDialog());

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
        setCardClick(cardChecks,        ActivityChecksActivity.class);
        setCardClick(cardInstallments,  ActivityInstallmentsActivity.class);
        setCardClick(cardWallet,            ActivityWalletActivity.class);
        setCardClick(cardPriceQuotes,       ActivityPriceQuotesActivity.class);
        setCardClick(cardCustomerRemaining, ActivityCustomerRemainingActivity.class);
        setCardClick(cardSupplierRemaining, ActivitySupplierRemainingActivity.class);
        setCardClick(cardCashDrawer,    ActivityCashDrawerActivity.class);
        setCardClick(cardStockCount,    ActivityStockCountActivity.class);
        setCardClick(cardUsers,         ActivityUsersActivity.class);
        setCardClick(binding.cardInsights, ActivityBusinessInsightsActivity.class);
    }

    private void setCardClick(MaterialCardView card, Class<?> cls) {
        if (card != null) {
            card.setOnClickListener(v -> openActivity(cls));
        }
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
        } catch (Exception e) {
            android.util.Log.e(TAG, "updateDrawerHeader error", e);
        }
    }

    private void loadDashboardData() {
        java.util.concurrent.ExecutorService exec =
            java.util.concurrent.Executors.newSingleThreadExecutor();
        exec.execute(() -> {
            try {
                final HashMap<String, Object> stats = dbHelper.getInvoicesStatistics();
                final java.util.List<HashMap<String, String>> lowStock = dbHelper.getLowStockProducts(5);
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
                        if (tvLowStockCount != null) tvLowStockCount.setText(lowStock != null ? String.valueOf(lowStock.size()) : "0");
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
            android.util.Log.e(TAG, "checkAlerts error", e);
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
                    .setBackgroundTint(androidx.core.content.ContextCompat.getColor(this, R.color.color_info))
                    .setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.white))
                    .setActionTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.color_gold))
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
            else if (id == R.id.nav_checks)          openActivity(ActivityChecksActivity.class);
            else if (id == R.id.nav_installments)    openActivity(ActivityInstallmentsActivity.class);
            else if (id == R.id.nav_cash_drawer)     openActivity(ActivityCashDrawerActivity.class);
            else if (id == R.id.nav_stock_count)     openActivity(ActivityStockCountActivity.class);
            else if (id == R.id.nav_users)           openActivity(ActivityUsersActivity.class);
            else if (id == R.id.nav_printer)         openActivity(ActivityPrinterSettingsActivity.class);
            else if (id == R.id.nav_about)           showAboutDialog();
            else if (id == R.id.nav_rate)            rateApp();
            else if (id == R.id.nav_share)           shareApp();
            else if (id == R.id.nav_logout)          confirmLogout();
        } catch (Exception e) {
            android.util.Log.e(TAG, "Navigation error", e);
        }
        if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void rateApp() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                android.net.Uri.parse("market://details?id=" + getPackageName())));
        } catch (android.content.ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                android.net.Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
        }
    }

    private void shareApp() {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT,
            getString(R.string.about_message) + "\nhttps://play.google.com/store/apps/details?id=" + getPackageName());
        startActivity(Intent.createChooser(share, getString(R.string.nav_share)));
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
            if (appUpdateManager != null) appUpdateManager.onResume();
            // Refresh drawer header so store name changes appear immediately
            if (navView != null) {
                View headerView = navView.getHeaderView(0);
                if (headerView != null) updateDrawerHeader(headerView);
            }
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
