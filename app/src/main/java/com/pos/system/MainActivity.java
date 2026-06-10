package com.pos.system;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import java.util.HashMap;
import java.util.List;
import com.pos.system.managers.ReviewManager;
import com.google.android.material.snackbar.Snackbar;
import com.pos.system.databinding.ActivityMainBinding;

public class MainActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private ActivityMainBinding binding;
    private static final String TAG = "MainActivity";

    private DBHelper dbHelper;
    private ReviewManager reviewManager;
    private com.pos.system.managers.AppUpdateManager appUpdateManager;
    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private BottomNavigationView bottomNav;

    // Pages
    private View pageHome, pageSales, pageInventory, pageCustomers, pageMore;

    // Home page stats
    private MaterialCardView cardTodaySales;
    private MaterialCardView cardTodayInvoices;
    private MaterialCardView cardLowStock;
    private TextView tvTodaySales;
    private TextView tvTodayInvoices;
    private TextView tvLowStockCount;
    private MaterialCardView cardAlert;
    private TextView tvAlertMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        appUpdateManager = new com.pos.system.managers.AppUpdateManager(this);
        appUpdateManager.checkForFlexibleUpdate();
    }

    private void setupUI() {
        setupToolbar();
        initializeViews();
        setupBottomNav();
        setupCardClicks();
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
        navView = binding.navView;
        bottomNav = binding.bottomNav;

        pageHome      = binding.pageHome;
        pageSales     = binding.pageSales;
        pageInventory = binding.pageInventory;
        pageCustomers = binding.pageCustomers;
        pageMore      = binding.pageMore;

        cardTodaySales    = binding.cardTodaySales;
        cardTodayInvoices = binding.cardTodayInvoices;
        cardLowStock      = binding.cardLowStock;
        tvTodaySales      = binding.tvTodaySales;
        tvTodayInvoices   = binding.tvTodayInvoices;
        tvLowStockCount   = binding.tvLowStockCount;
        cardAlert         = binding.cardAlert;
        tvAlertMessage    = binding.tvAlertMessage;
    }

    private void setupBottomNav() {
        if (bottomNav == null) return;
        showPage(pageHome);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if      (id == R.id.tab_home)      { showPage(pageHome);      updateToolbarTitle(R.string.app_name); }
            else if (id == R.id.tab_sales)     { showPage(pageSales);     updateToolbarTitle(R.string.tab_sales); }
            else if (id == R.id.tab_inventory) { showPage(pageInventory); updateToolbarTitle(R.string.tab_inventory); }
            else if (id == R.id.tab_customers) { showPage(pageCustomers); updateToolbarTitle(R.string.tab_customers); }
            else if (id == R.id.tab_more)      { showPage(pageMore);      updateToolbarTitle(R.string.tab_more); }
            return true;
        });
    }

    private void showPage(View target) {
        View[] pages = {pageHome, pageSales, pageInventory, pageCustomers, pageMore};
        for (View p : pages) {
            if (p != null) p.setVisibility(p == target ? View.VISIBLE : View.GONE);
        }
    }

    private void updateToolbarTitle(int titleRes) {
        if (binding.toolbar != null) binding.toolbar.setTitle(getString(titleRes));
    }

    private void setupCardClicks() {
        // Home page
        if (cardTodaySales  != null) cardTodaySales.setOnClickListener(v -> openActivity(ActivityReportsActivity.class));
        if (cardLowStock    != null) cardLowStock.setOnClickListener(v -> showLowStockDialog());
        if (cardAlert       != null) cardAlert.setOnClickListener(v -> showLowStockDialog());

        setClick(binding.cardPos,      () -> openActivity(ActivityCartActivity.class));
        setClick(binding.cardProducts, () -> openActivity(ActivityProductsActivity.class));
        setClick(binding.cardInvoices, () -> openActivity(ActivityInvoicesActivity.class));
        setClick(binding.cardCustomers,() -> openActivity(ActivityCustomersActivity.class));
        setClick(binding.cardSuppliers,() -> openActivity(ActivitySuppliersActivity.class));
        setClick(binding.cardReports,  () -> openActivity(ActivityReportsActivity.class));
        setClick(binding.cardSettings, () -> openActivity(ActivitySettingsActivity.class));

        // Sales tab
        setClick(binding.cardPosSales,      () -> openActivity(ActivityCartActivity.class));
        setClick(binding.cardInvoicesSales, () -> openActivity(ActivityInvoicesActivity.class));
        setClick(binding.cardReturnsSales,  () -> openActivity(ActivityReturnActivity.class));
        setClick(binding.cardQuotesSales,   () -> openActivity(ActivityPriceQuotesActivity.class));
        setClick(binding.cardDebtsSales,    () -> openActivity(ActivityDebtActivity.class));

        // Inventory tab
        setClick(binding.cardProductsInv,  () -> openActivity(ActivityProductsActivity.class));
        setClick(binding.cardStockCountInv,() -> openActivity(ActivityStockCountActivity.class));
        setClick(binding.cardExpiryInv,    () -> openActivity(ActivityExpiryDashboardActivity.class));
        setClick(binding.cardInventoryHub, () -> openActivity(ActivityProductsActivity.class));

        // Customers tab
        setClick(binding.cardCustList,      () -> openActivity(ActivityCustomersActivity.class));
        setClick(binding.cardCustInvoices,  () -> openActivity(ActivityInvoicesActivity.class));
        setClick(binding.cardCustDebt,      () -> openActivity(ActivityDebtActivity.class));
        setClick(binding.cardCustInstall,   () -> openActivity(ActivityInstallmentsActivity.class));
        setClick(binding.cardCustChecks,    () -> openActivity(ActivityChecksActivity.class));
        setClick(binding.cardCustRemaining, () -> openActivity(ActivityCustomerRemainingActivity.class));
        setClick(binding.cardSuppList,      () -> openActivity(ActivitySuppliersActivity.class));
        setClick(binding.cardSuppOrders,    () -> openActivity(ActivityPurchaseOrderActivity.class));
        setClick(binding.cardSuppRemaining, () -> openActivity(ActivitySupplierRemainingActivity.class));

        // More tab
        setClick(binding.cardMoreWallet,   () -> openActivity(ActivityWalletActivity.class));
        setClick(binding.cardMoreCash,     () -> openActivity(ActivityCashDrawerActivity.class));
        setClick(binding.cardMoreExpenses, () -> openActivity(ActivityExpensesActivity.class));
        setClick(binding.cardMoreShifts,   () -> openActivity(ActivityShiftActivity.class));
        setClick(binding.cardMoreReports,  () -> openActivity(ActivityReportsActivity.class));
        setClick(binding.cardMoreInsights, () -> openActivity(ActivityBusinessInsightsActivity.class));
        setClick(binding.cardMoreSettings, () -> openActivity(ActivitySettingsActivity.class));
        setClick(binding.cardMoreUsers,    () -> openActivity(ActivityUsersActivity.class));
        setClick(binding.cardMoreBackup,   () -> openActivity(ActivityBackupActivity.class));
        setClick(binding.cardMorePrinter,  () -> openActivity(ActivityPrinterSettingsActivity.class));
    }

    private void setClick(View v, Runnable action) {
        if (v != null) v.setOnClickListener(x -> action.run());
    }

    private void setupDrawer() {
        if (navView != null) {
            navView.setNavigationItemSelectedListener(this);
            View headerView = navView.getHeaderView(0);
            if (headerView != null) updateDrawerHeader(headerView);
        }
    }

    private void updateDrawerHeader(View headerView) {
        TextView tvStoreName  = headerView.findViewById(R.id.tv_store_name);
        TextView tvStorePhone = headerView.findViewById(R.id.tv_store_phone);
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
                        double todaySales = 0; int todayInvoices = 0;
                        if (stats != null) {
                            Object s = stats.get("today_total"); Object c = stats.get("today_count");
                            if (s != null) todaySales    = ((Number) s).doubleValue();
                            if (c != null) todayInvoices = ((Number) c).intValue();
                        }
                        if (tvTodaySales    != null) tvTodaySales.setText(String.format("%.2f %s", todaySales, currency));
                        if (tvTodayInvoices != null) tvTodayInvoices.setText(String.valueOf(todayInvoices));
                        if (tvLowStockCount != null) tvLowStockCount.setText(lowStock != null ? String.valueOf(lowStock.size()) : "0");
                    } catch (Exception ignored) {}
                });
            } catch (Exception e) {
                android.util.Log.e(TAG, "loadDashboardData error", e);
            }
        });
    }

    private String getCurrencySymbol() {
        try {
            HashMap<String, String> settings = dbHelper.getStoreSettings();
            return settings.getOrDefault("currency", "ج.م");
        } catch (Exception e) { return "ج.م"; }
    }

    private void checkAlerts() {
        try {
            List<HashMap<String, String>> lowStock = dbHelper.getLowStockProducts(5);
            if (lowStock != null && !lowStock.isEmpty()) {
                if (cardAlert      != null) cardAlert.setVisibility(View.VISIBLE);
                if (tvAlertMessage != null)
                    tvAlertMessage.setText(getString(R.string.low_stock_alert_message, lowStock.size()));
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
                showToast(getString(R.string.no_low_stock)); return;
            }
            StringBuilder msg = new StringBuilder();
            for (HashMap<String, String> p : lowStock)
                msg.append("• ").append(p.get("name")).append(" (").append(p.get("qty")).append(")\n");
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.low_stock_title, lowStock.size()))
                    .setMessage(msg.toString())
                    .setPositiveButton(R.string.view_products, (d, w) -> openActivity(ActivityProductsActivity.class))
                    .setNegativeButton(R.string.close, null).show();
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
                .setTitle(R.string.about_app).setMessage(getString(R.string.about_message))
                .setPositiveButton(R.string.ok, null).show();
    }

    private void confirmLogout() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.logout).setMessage(R.string.logout_confirm)
                .setPositiveButton(R.string.yes, (d, w) -> { AuthActivity.logout(this); finish(); })
                .setNegativeButton(R.string.no, null).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            loadDashboardData(); checkAlerts();
            if (appUpdateManager != null) appUpdateManager.onResume();
            if (navView != null) {
                View headerView = navView.getHeaderView(0);
                if (headerView != null) updateDrawerHeader(headerView);
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "onResume error", e);
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (bottomNav != null && bottomNav.getSelectedItemId() != R.id.tab_home) {
            bottomNav.setSelectedItemId(R.id.tab_home);
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
