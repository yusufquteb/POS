package com.pos.system;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import com.pos.system.managers.ReviewManager;
import com.google.android.material.snackbar.Snackbar;
import com.pos.system.databinding.ActivityMainBinding;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

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
    private TextView tvGreeting;
    private BarChart chartProfit;
    private TextView tvProfitPeriodTotal;
    private RecyclerView recyclerRecentInvoices;
    private TextView tvNoRecentInvoices;
    private RecentInvoicesAdapter recentInvoicesAdapter;

    // Inventory tab stats
    private TextView tvInvTotal;
    private TextView tvInvLow;
    private TextView tvInvExpiry;

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
        setupGreeting();
        setupBottomNav();
        setupCardClicks();
        setupDrawer();
        setupProfitChart();
        setupRecentInvoices();
    }

    private void setupGreeting() {
        if (tvGreeting == null) return;
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int resId = hour < 12 ? R.string.greeting_morning
                  : hour < 18 ? R.string.greeting_afternoon
                              : R.string.greeting_evening;
        tvGreeting.setText(resId);
    }

    private void setupRecentInvoices() {
        if (recyclerRecentInvoices == null) return;
        recyclerRecentInvoices.setLayoutManager(new LinearLayoutManager(this));
        recyclerRecentInvoices.setNestedScrollingEnabled(false);
        recentInvoicesAdapter = new RecentInvoicesAdapter();
        recyclerRecentInvoices.setAdapter(recentInvoicesAdapter);
        View tvViewAll = binding.tvViewAllInvoices;
        if (tvViewAll != null) tvViewAll.setOnClickListener(v -> openActivity(ActivityInvoicesActivity.class));
    }

    private void loadRecentInvoices() {
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                final List<HashMap<String, String>> invoices = dbHelper.getRecentInvoices(5);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    boolean empty = invoices == null || invoices.isEmpty();
                    if (recyclerRecentInvoices != null) recyclerRecentInvoices.setVisibility(empty ? View.GONE : View.VISIBLE);
                    if (tvNoRecentInvoices     != null) tvNoRecentInvoices.setVisibility(empty ? View.VISIBLE : View.GONE);
                    if (!empty && recentInvoicesAdapter != null) recentInvoicesAdapter.setData(invoices);
                });
            } catch (Exception e) {
                android.util.Log.e(TAG, "loadRecentInvoices error", e);
            }
        });
    }

    private class RecentInvoicesAdapter extends RecyclerView.Adapter<RecentInvoicesAdapter.VH> {
        private List<HashMap<String, String>> invoices = new ArrayList<>();

        void setData(List<HashMap<String, String>> data) {
            this.invoices = data != null ? data : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_home_invoice, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            if (position < 0 || position >= invoices.size()) return;
            HashMap<String, String> inv = invoices.get(position);
            String customerName = inv.getOrDefault("customer_name", "");
            if (customerName == null || customerName.trim().isEmpty()) {
                customerName = getString(R.string.general_customer);
            }
            String initial = customerName.isEmpty() ? "?" : customerName.substring(0, 1).toUpperCase(Locale.getDefault());

            if (holder.tvAvatar       != null) holder.tvAvatar.setText(initial);
            if (holder.tvCustomer     != null) holder.tvCustomer.setText(customerName);
            if (holder.tvInvoiceNumber!= null) holder.tvInvoiceNumber.setText(inv.getOrDefault("invoice_number", "-"));

            double total = 0;
            try { total = Double.parseDouble(inv.getOrDefault("total", "0")); } catch (Exception ignored) {}
            if (holder.tvAmount != null) {
                holder.tvAmount.setText(String.format(Locale.getDefault(), "%.2f %s", total, getCurrencySymbol()));
            }

            String status = inv.getOrDefault("status", "completed");
            int statusColor;
            int statusPillBg;
            String statusLabel;
            if ("returned".equalsIgnoreCase(status)) {
                statusLabel = getString(R.string.status_returned);
                statusColor = R.color.color_error;
                statusPillBg = R.drawable.bg_status_pill_error;
            } else if ("partial".equalsIgnoreCase(status)) {
                statusLabel = getString(R.string.status_partial);
                statusColor = R.color.color_info;
                statusPillBg = R.drawable.bg_status_pill_info;
            } else {
                statusLabel = getString(R.string.status_paid);
                statusColor = R.color.color_success;
                statusPillBg = R.drawable.bg_status_pill_success;
            }
            if (holder.tvStatus != null) {
                holder.tvStatus.setText(statusLabel);
                holder.tvStatus.setTextColor(androidx.core.content.ContextCompat.getColor(MainActivity.this, statusColor));
                holder.tvStatus.setBackgroundResource(statusPillBg);
            }

            holder.itemView.setOnClickListener(v -> {
                try {
                    long id = Long.parseLong(inv.getOrDefault("id", "0"));
                    Intent intent = new Intent(MainActivity.this, ActivityInvoiceDetailsActivity.class);
                    intent.putExtra("invoice_id", id);
                    startActivity(intent);
                } catch (Exception ignored) {}
            });
        }

        @Override public int getItemCount() { return invoices.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvAvatar, tvCustomer, tvInvoiceNumber, tvAmount, tvStatus;
            VH(@NonNull View v) {
                super(v);
                tvAvatar        = v.findViewById(R.id.tv_avatar);
                tvCustomer      = v.findViewById(R.id.tv_customer_name);
                tvInvoiceNumber = v.findViewById(R.id.tv_invoice_number);
                tvAmount        = v.findViewById(R.id.tv_amount);
                tvStatus        = v.findViewById(R.id.tv_status);
            }
        }
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
        tvGreeting        = binding.tvGreeting;
        chartProfit         = binding.chartProfit;
        tvProfitPeriodTotal = binding.tvProfitPeriodTotal;
        recyclerRecentInvoices = binding.recyclerRecentInvoices;
        tvNoRecentInvoices     = binding.tvNoRecentInvoices;

        tvInvTotal  = binding.tvInvTotal;
        tvInvLow    = binding.tvInvLow;
        tvInvExpiry = binding.tvInvExpiry;
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
        if (cardTodayInvoices != null) cardTodayInvoices.setOnClickListener(v -> openActivity(ActivityInvoicesActivity.class));
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
            updateLogoutVisibility();
        }
    }

    /** "تسجيل الخروج" لا يظهر إلا لمستخدم سجّل دخوله فعلياً */
    private void updateLogoutVisibility() {
        if (navView == null) return;
        try {
            boolean isLoggedIn = AuthActivity.getCurrentUser(this) != null;
            android.view.MenuItem logoutItem = navView.getMenu().findItem(R.id.nav_logout);
            if (logoutItem != null) logoutItem.setVisible(isLoggedIn);
        } catch (Exception ignored) {}
    }

    private void updateDrawerHeader(View headerView) {
        TextView tvStoreName  = headerView.findViewById(R.id.tv_store_name);
        TextView tvStorePhone = headerView.findViewById(R.id.tv_store_phone);
        try {
            HashMap<String, String> settings = dbHelper.getStoreSettings();
            if (settings != null) {
                String name = settings.get("name");
                // "متجري" هو الاسم الافتراضي المزروع عند أول تشغيل — إن لم
                // يغيّره المستخدم بعد، نعرضه بلغة الواجهة الحالية بدل تثبيته
                // بالعربية دائمًا.
                boolean isDefaultSeedName = name == null || name.isEmpty() || "متجري".equals(name);
                if (tvStoreName != null) {
                    tvStoreName.setText(isDefaultSeedName ? getString(R.string.str_b530ab) : name);
                }
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
                final int totalProducts = dbHelper.getProductsCount();
                final int expiryCount   = dbHelper.getExpiringProducts(30) != null
                                         ? dbHelper.getExpiringProducts(30).size() : 0;
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    try {
                        double todaySales = 0; int todayInvoices = 0;
                        if (stats != null) {
                            Object s = stats.get("today_total"); Object c = stats.get("today_count");
                            if (s != null) todaySales    = ((Number) s).doubleValue();
                            if (c != null) todayInvoices = ((Number) c).intValue();
                        }
                        int lowCount = lowStock != null ? lowStock.size() : 0;
                        if (tvTodaySales    != null) tvTodaySales.setText(String.format("%.2f %s", todaySales, currency));
                        if (tvTodayInvoices != null) tvTodayInvoices.setText(String.valueOf(todayInvoices));
                        if (tvLowStockCount != null) tvLowStockCount.setText(String.valueOf(lowCount));
                        // Inventory tab stats
                        if (tvInvTotal  != null) tvInvTotal.setText(String.valueOf(totalProducts));
                        if (tvInvLow    != null) tvInvLow.setText(String.valueOf(lowCount));
                        if (tvInvExpiry != null) tvInvExpiry.setText(String.valueOf(expiryCount));
                    } catch (Exception ignored) {}
                });
            } catch (Exception e) {
                android.util.Log.e(TAG, "loadDashboardData error", e);
            }
        });
        loadProfitChart();
    }

    // ═══════════════════════════════════════════════════════════
    // Profit trend chart (home page) — X: day, Y: net profit
    // ═══════════════════════════════════════════════════════════
    private static final int PROFIT_CHART_DAYS = 7;

    private void setupProfitChart() {
        if (chartProfit == null) return;
        chartProfit.setDrawBarShadow(false);
        chartProfit.setDrawValueAboveBar(true);
        chartProfit.getDescription().setEnabled(false);
        chartProfit.setPinchZoom(false);
        chartProfit.setDrawGridBackground(false);
        chartProfit.getLegend().setEnabled(false);
        chartProfit.getAxisRight().setEnabled(false);
        chartProfit.getAxisLeft().setDrawGridLines(true);
        chartProfit.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chartProfit.getXAxis().setDrawGridLines(false);
        chartProfit.getXAxis().setGranularity(1f);
        chartProfit.setNoDataText(getString(R.string.chart_no_data));
        chartProfit.setExtraBottomOffset(4f);

        android.util.TypedValue tcv = new android.util.TypedValue();
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, tcv, true);
        int textColor = tcv.data;
        chartProfit.getAxisLeft().setTextColor(textColor);
        chartProfit.getXAxis().setTextColor(textColor);
    }

    private void loadProfitChart() {
        if (chartProfit == null) return;
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                SimpleDateFormat sqlFmt   = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                SimpleDateFormat dayLabel = new SimpleDateFormat("MM-dd", Locale.US);

                Calendar cal = Calendar.getInstance();
                List<String> dates  = new ArrayList<>();
                List<String> labels = new ArrayList<>();
                cal.add(Calendar.DAY_OF_YEAR, -(PROFIT_CHART_DAYS - 1));
                for (int i = 0; i < PROFIT_CHART_DAYS; i++) {
                    dates.add(sqlFmt.format(cal.getTime()));
                    labels.add(dayLabel.format(cal.getTime()));
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                }
                String startDate = dates.get(0);
                String endDate   = dates.get(dates.size() - 1);

                final List<HashMap<String, String>> rows = dbHelper.getNetProfitByPeriod(startDate, endDate);
                final HashMap<String, Double> byDate = new HashMap<>();
                if (rows != null) {
                    for (HashMap<String, String> row : rows) {
                        try {
                            byDate.put(row.get("date"), Double.parseDouble(row.getOrDefault("net_profit", "0")));
                        } catch (Exception ignored) {}
                    }
                }

                final List<Float> values = new ArrayList<>();
                double periodTotal = 0;
                for (String d : dates) {
                    double v = byDate.containsKey(d) ? byDate.get(d) : 0.0;
                    values.add((float) v);
                    periodTotal += v;
                }
                final double finalTotal = periodTotal;
                final String currency = getCurrencySymbol();

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    try {
                        renderProfitChart(labels, values);
                        if (tvProfitPeriodTotal != null) {
                            int color = androidx.core.content.ContextCompat.getColor(this,
                                finalTotal >= 0 ? R.color.color_success : R.color.color_error);
                            String sign = finalTotal >= 0 ? "+" : "";
                            tvProfitPeriodTotal.setTextColor(color);
                            tvProfitPeriodTotal.setText(String.format(Locale.getDefault(),
                                "%s%.2f %s", sign, finalTotal, currency));
                        }
                    } catch (Exception ignored) {}
                });
            } catch (Exception e) {
                android.util.Log.e(TAG, "loadProfitChart error", e);
            }
        });
    }

    private void renderProfitChart(List<String> labels, List<Float> values) {
        if (chartProfit == null) return;
        int successColor = androidx.core.content.ContextCompat.getColor(this, R.color.color_success);
        int errorColor    = androidx.core.content.ContextCompat.getColor(this, R.color.color_error);

        List<BarEntry> entries = new ArrayList<>();
        List<Integer> barColors = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            float v = values.get(i);
            entries.add(new BarEntry(i, v));
            barColors.add(v >= 0 ? successColor : errorColor);
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColors(barColors);
        dataSet.setDrawValues(false);
        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);

        chartProfit.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chartProfit.setData(data);
        chartProfit.animateY(500);
        chartProfit.invalidate();
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
                updateLogoutVisibility();
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
