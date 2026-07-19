package com.pos.system;

import android.os.Bundle;
import android.widget.TextView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import com.pos.system.databinding.ActivityBusinessInsightsBinding;

public class ActivityBusinessInsightsActivity extends BaseActivity {

    private ActivityBusinessInsightsBinding binding;
    private DBHelper dbHelper;

    private TextView tvBestSellerName;
    private TextView tvBestCustomerName;
    private TextView tvExpiryCount;
    private TextView tvDeadStockCount;
    private TextView tvCustomerDebtTotal;
    private TextView tvSupplierDebtTotal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBusinessInsightsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets(binding.getRoot());

        dbHelper = new DBHelper(this);

        MaterialToolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvBestSellerName    = binding.tvBestSellerName;
        tvBestCustomerName  = binding.tvBestCustomerName;
        tvExpiryCount       = binding.tvExpiryCount;
        tvDeadStockCount    = binding.tvDeadStockCount;
        tvCustomerDebtTotal = binding.tvCustomerDebtTotal;
        tvSupplierDebtTotal = binding.tvSupplierDebtTotal;

        binding.cardGoExpiry.setOnClickListener(v -> openActivity(ActivityExpiryDashboardActivity.class));
        binding.cardGoDecision.setOnClickListener(v -> openActivity(ActivityDecisionDashboardActivity.class));
        binding.cardGoDebts.setOnClickListener(v -> openActivity(ActivityDebtActivity.class));
        binding.cardBestSeller.setOnClickListener(v -> openActivity(ActivityReportsActivity.class));
        binding.cardBestCustomer.setOnClickListener(v -> openActivity(ActivityCustomersActivity.class));
        binding.cardExpiryAlert.setOnClickListener(v -> openActivity(ActivityExpiryDashboardActivity.class));
        binding.cardDeadStock.setOnClickListener(v -> openActivity(ActivityDecisionDashboardActivity.class));
        binding.cardCustomerDebt.setOnClickListener(v -> openActivity(ActivityDebtActivity.class));
        binding.cardSupplierDebt.setOnClickListener(v -> openActivity(ActivityDebtActivity.class));

        loadData();
    }

    private void loadData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                final HashMap<String, String> topSeller   = dbHelper.getTopSellerThisWeek();
                final HashMap<String, String> topCustomer = dbHelper.getTopCustomerThisMonth();
                final List<HashMap<String, String>> expiring  = dbHelper.getExpiringProducts(30);
                final List<HashMap<String, String>> deadStock = dbHelper.getDeadStockProducts(60);
                final double customerDebt  = dbHelper.getTotalCustomerDebt();
                final double supplierDebt  = dbHelper.getTotalSupplierDebt();
                final String currency = getCurrency();

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    tvBestSellerName.setText(topSeller   != null ? topSeller.getOrDefault("name",   "—") : "—");
                    tvBestCustomerName.setText(topCustomer != null ? topCustomer.getOrDefault("name", "—") : "—");
                    tvExpiryCount.setText(expiring  != null ? String.valueOf(expiring.size())  : "0");
                    tvDeadStockCount.setText(deadStock != null ? String.valueOf(deadStock.size()) : "0");
                    tvCustomerDebtTotal.setText(String.format("%.2f %s", customerDebt, currency));
                    tvSupplierDebtTotal.setText(String.format("%.2f %s", supplierDebt, currency));
                });
            } catch (Exception ignored) {}
        });
    }

    private String getCurrency() {
        try {
            return dbHelper.getStoreSettings().getOrDefault("currency", "ج.م");
        } catch (Exception e) {
            return "ج.م";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) dbHelper.close();
    }
}
