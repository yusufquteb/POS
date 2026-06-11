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
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import androidx.core.util.Pair;
import java.text.SimpleDateFormat;
import java.util.*;

public class ActivityProductProfitabilityActivity extends BaseActivity {

    private static final String TAG = "ProfitabilityActivity";

    private DBHelper dbHelper;
    private String currency = "ج.م";

    private RecyclerView rvProducts;
    private TextView tvTotalRevenue, tvTotalCogs, tvTotalProfit, tvEmpty;
    private ChipGroup chipGroupPeriod;

    private List<HashMap<String, String>> data = new ArrayList<>();
    private ProfitAdapter adapter;

    private Date startDate, endDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_profitability);
        applyWindowInsets(findViewById(R.id.root_layout));

        dbHelper = new DBHelper(this);
        try { currency = dbHelper.getStoreSettings().getOrDefault("currency", "ج.م"); } catch (Exception ignored) {}

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) { setSupportActionBar(toolbar); toolbar.setNavigationOnClickListener(v -> finish()); }

        tvTotalRevenue = findViewById(R.id.tv_total_revenue);
        tvTotalCogs    = findViewById(R.id.tv_total_cogs);
        tvTotalProfit  = findViewById(R.id.tv_total_profit);
        tvEmpty        = findViewById(R.id.tv_empty);

        rvProducts = findViewById(R.id.rv_products);
        adapter    = new ProfitAdapter();
        rvProducts.setLayoutManager(new LinearLayoutManager(this));
        rvProducts.setAdapter(adapter);

        chipGroupPeriod = findViewById(R.id.chip_group_period);
        if (chipGroupPeriod != null) {
            chipGroupPeriod.setOnCheckedStateChangeListener((g, ids) -> {
                if (ids.isEmpty()) return;
                int id = ids.get(0);
                if      (id == R.id.chip_today)  setTodayDates();
                else if (id == R.id.chip_week)   setWeekDates();
                else if (id == R.id.chip_month)  setMonthDates();
                else if (id == R.id.chip_custom) { showDatePicker(); return; }
                loadData();
            });
        }

        setMonthDates();
        loadData();
    }

    private void loadData() {
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String start = fmt(startDate);
                String end   = fmt(endDate);
                List<HashMap<String, String>> rows = dbHelper.getProfitByProduct(start, end);
                double totalRev = 0, totalCogs = 0, totalProfit = 0;
                for (HashMap<String, String> r : rows) {
                    totalRev    += safeDouble(r, "revenue");
                    totalCogs   += safeDouble(r, "cogs");
                    totalProfit += safeDouble(r, "profit");
                }
                final List<HashMap<String, String>> finalRows = rows;
                final double fRev = totalRev, fCogs = totalCogs, fProfit = totalProfit;
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    data.clear();
                    data.addAll(finalRows);
                    adapter.notifyDataSetChanged();
                    setText(tvTotalRevenue, fmtAmt(fRev));
                    setText(tvTotalCogs,    fmtAmt(fCogs));
                    setText(tvTotalProfit,  fmtAmt(fProfit));
                    if (tvTotalProfit != null) {
                        int clr = fProfit >= 0
                            ? androidx.core.content.ContextCompat.getColor(this, R.color.color_success)
                            : androidx.core.content.ContextCompat.getColor(this, R.color.color_error);
                        tvTotalProfit.setTextColor(clr);
                    }
                    if (tvEmpty != null) tvEmpty.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
                });
            } catch (Exception e) {
                android.util.Log.e(TAG, "loadData: " + e.getMessage(), e);
            }
        });
    }

    private class ProfitAdapter extends RecyclerView.Adapter<ProfitAdapter.VH> {
        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_profit_row, parent, false);
            return new VH(v);
        }
        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            HashMap<String, String> item = data.get(pos);
            h.tvName.setText(item.getOrDefault("name", ""));
            h.tvQty.setText(item.getOrDefault("total_qty", "0") + " " + getString(R.string.items));
            h.tvRevenue.setText(fmtAmt(safeDouble(item, "revenue")));
            double profit = safeDouble(item, "profit");
            double margin = safeDouble(item, "margin_pct");
            h.tvProfit.setText(fmtAmt(profit));
            h.tvMargin.setText(String.format(Locale.US, "%.1f%%", margin));
            int clr = profit >= 0
                ? androidx.core.content.ContextCompat.getColor(ActivityProductProfitabilityActivity.this, R.color.color_success)
                : androidx.core.content.ContextCompat.getColor(ActivityProductProfitabilityActivity.this, R.color.color_error);
            h.tvProfit.setTextColor(clr);
            h.tvMargin.setTextColor(clr);
        }
        @Override public int getItemCount() { return data.size(); }
        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvQty, tvRevenue, tvProfit, tvMargin;
            VH(@NonNull View v) {
                super(v);
                tvName    = v.findViewById(R.id.tv_product_name);
                tvQty     = v.findViewById(R.id.tv_qty);
                tvRevenue = v.findViewById(R.id.tv_revenue);
                tvProfit  = v.findViewById(R.id.tv_profit);
                tvMargin  = v.findViewById(R.id.tv_margin);
            }
        }
    }

    private void setTodayDates() {
        Calendar c = Calendar.getInstance();
        startDate = dayStart(c); endDate = dayEnd(c);
    }
    private void setWeekDates() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek());
        startDate = dayStart(c);
        c.add(Calendar.DAY_OF_WEEK, 6); endDate = dayEnd(c);
    }
    private void setMonthDates() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1); startDate = dayStart(c);
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH)); endDate = dayEnd(c);
    }
    private void showDatePicker() {
        MaterialDatePicker<Pair<Long, Long>> picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(R.string.reports_custom).build();
        picker.show(getSupportFragmentManager(), "dp");
        picker.addOnPositiveButtonClickListener(s -> {
            startDate = new Date(s.first); endDate = new Date(s.second); loadData();
        });
    }
    private Date dayStart(Calendar c) {
        Calendar s = (Calendar) c.clone();
        s.set(Calendar.HOUR_OF_DAY, 0); s.set(Calendar.MINUTE, 0);
        s.set(Calendar.SECOND, 0); s.set(Calendar.MILLISECOND, 0);
        return s.getTime();
    }
    private Date dayEnd(Calendar c) {
        Calendar e = (Calendar) c.clone();
        e.set(Calendar.HOUR_OF_DAY, 23); e.set(Calendar.MINUTE, 59);
        e.set(Calendar.SECOND, 59); e.set(Calendar.MILLISECOND, 999);
        return e.getTime();
    }
    private String fmt(Date d) {
        if (d == null) return "";
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(d);
    }
    private String fmtAmt(double v) {
        return String.format(Locale.US, "%.2f %s", v, currency);
    }
    private double safeDouble(HashMap<String, String> m, String key) {
        try { return Double.parseDouble(m.getOrDefault(key, "0")); } catch (Exception e) { return 0; }
    }
    private void setText(TextView tv, String t) { if (tv != null) tv.setText(t); }

    @Override protected void onDestroy() {
        super.onDestroy();
        try { if (dbHelper != null) dbHelper.close(); } catch (Exception ignored) {}
    }
}
