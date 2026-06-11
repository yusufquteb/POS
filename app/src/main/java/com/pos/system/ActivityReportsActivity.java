package com.pos.system;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.appbar.MaterialToolbar;
import android.widget.Button;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import com.pos.system.databinding.ActivityReportsBinding;

/**
 * ActivityReportsActivity - التقارير والإحصائيات
 * النسخة 3.0 - مُصحَّحة الكاملة
 *
 * الإصلاحات:
 * ✅ تصحيح أسماء الحقول (final_total → total, product_name → name)
 * ✅ العملة من إعدادات المتجر بدلاً من ج.م الثابتة
 * ✅ دعم اللغتين
 * ✅ ربط البيانات الحقيقية من DBHelper
 * ✅ تصدير PDF صحيح
 * ✅ تفعيل date picker مخصص
 */
public class ActivityReportsActivity extends BaseActivity {

    private ActivityReportsBinding binding;


    private static final String TAG = "ReportsActivity";

    private DBHelper dbHelper;
    private String   currency = "ج.م";

    // Stats Views
    private TextView tvTotalSales, tvInvoiceCount, tvAverageInvoice;
    private TextView tvProductsCount, tvInventoryValue, tvLowStock;
    private TextView tvTotalExpenses, tvNetProfit, tvCogs, tvGrossProfit;

    // Top Products List
    private RecyclerView rvTopProducts;
    private TopProductsAdapter topAdapter;
    private List<Map<String,String>> topProducts = new ArrayList<>();
    private RecyclerView rvSalesByCategory;
    private SalesByCategoryAdapter salesByCatAdapter;
    private List<HashMap<String,String>> salesByCatList = new ArrayList<>();


    // Chart
    private BarChart barChartSales;

    // Buttons
    private Button btnExportPdf, btnShareReport, btnWhatsapp;
    private ChipGroup chipGroupPeriod;

    // Date range
    private Date startDate, endDate;
    private String selectedPeriod = "today";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReportsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets(binding.getRoot());

        dbHelper = new DBHelper(this);
        loadCurrency();
        initViews();
        setupToolbar();
        setupListeners();
        setTodayDates();
        loadReports();
    }

    private void loadCurrency() {
        try {
            HashMap<String, String> s = dbHelper.getStoreSettings();
            currency = s.getOrDefault("currency", "ج.م");
        } catch (Exception e) { currency = "ج.م"; }
    }

    private void initViews() {
        tvTotalSales     = binding.tvTotalSales;
        tvInvoiceCount   = binding.tvInvoiceCount;
        tvAverageInvoice = binding.tvAverageInvoice;
        tvProductsCount  = binding.tvProductsCount;
        tvInventoryValue = binding.tvInventoryValue;
        tvLowStock       = binding.tvLowStock;
        tvTotalExpenses  = binding.tvTotalExpenses;
        tvNetProfit      = binding.tvNetProfit;
        tvCogs           = binding.tvCogs;
        tvGrossProfit    = binding.tvGrossProfit;

        rvTopProducts = binding.rvTopProducts;
        if (rvTopProducts != null) {
            topAdapter = new TopProductsAdapter();
            rvTopProducts.setLayoutManager(new LinearLayoutManager(this));
            rvTopProducts.setAdapter(topAdapter);
            rvTopProducts.setNestedScrollingEnabled(false);
        }

        rvSalesByCategory = binding.rvSalesByCategory;
        if (rvSalesByCategory != null) {
            salesByCatAdapter = new SalesByCategoryAdapter();
            rvSalesByCategory.setLayoutManager(new LinearLayoutManager(this));
            rvSalesByCategory.setAdapter(salesByCatAdapter);
            rvSalesByCategory.setNestedScrollingEnabled(false);
        }

        chipGroupPeriod = binding.chipGroupPeriod;
        btnExportPdf    = binding.btnExportPdf;
        btnShareReport  = binding.btnShareReport;
        btnWhatsapp     = binding.btnWhatsappReport;
        barChartSales   = binding.barChartSales;
        setupBarChart();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = binding.toolbar;
        if (toolbar != null) { setSupportActionBar(toolbar); toolbar.setNavigationOnClickListener(v -> finish()); }
    }

    private void setupListeners() {
        if (chipGroupPeriod != null) {
            chipGroupPeriod.setOnCheckedStateChangeListener((group, ids) -> {
                if (ids.isEmpty()) return;
                int id = ids.get(0);
                if      (id == R.id.chip_today)  { setTodayDates();  }
                else if (id == R.id.chip_week)   { setWeekDates();   }
                else if (id == R.id.chip_month)  { setMonthDates();  }
                else if (id == R.id.chip_custom) { showDatePicker(); return; }
                loadReports();
            });
        }
        if (btnExportPdf   != null) btnExportPdf.setOnClickListener(v -> exportToPDF());
        if (btnShareReport != null) btnShareReport.setOnClickListener(v -> shareReport());
        if (btnWhatsapp    != null) btnWhatsapp.setOnClickListener(v -> shareToWhatsApp());
        android.widget.Button btnSalesByEmp = binding.btnSalesByEmployee;
        if (btnSalesByEmp != null) btnSalesByEmp.setOnClickListener(v -> showSalesByEmployee());
    }

    // ═══════════════════════════════════════════════════════════
    // مبيعات الموظفين
    // ═══════════════════════════════════════════════════════════
    private void showSalesByEmployee() {
        try {
            java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US);
            String from = startDate != null ? fmt.format(startDate) : null;
            String to   = endDate   != null ? fmt.format(endDate)   : null;
            java.util.List<HashMap<String, String>> rows = dbHelper.getSalesByEmployee(from, to);
            if (rows == null || rows.isEmpty()) {
                showToast(getString(R.string.sales_by_employee_empty));
                return;
            }
            StringBuilder msg = new StringBuilder();
            for (HashMap<String, String> r : rows) {
                double sales = 0;
                try { sales = Double.parseDouble(r.getOrDefault("total_sales", "0")); } catch (Exception ignored) {}
                msg.append("👤 ").append(r.getOrDefault("employee", "admin"))
                   .append("\n   ").append(getString(R.string.sales_by_employee_invoices))
                   .append(": ").append(r.getOrDefault("invoice_count", "0"))
                   .append("   |   ").append(String.format(Locale.US, "%.2f %s", sales, currency))
                   .append("\n\n");
            }
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.sales_by_employee_title))
                .setMessage(msg.toString().trim())
                .setPositiveButton(R.string.close, null)
                .show();
        } catch (Exception e) {
            android.util.Log.e(TAG, "showSalesByEmployee: " + e.getMessage(), e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Date Setup
    // ═══════════════════════════════════════════════════════════
    private void setTodayDates() {
        Calendar c = Calendar.getInstance();
        startDate = dayStart(c);
        endDate   = dayEnd(c);
    }

    private void setWeekDates() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek());
        startDate = dayStart(c);
        c.add(Calendar.DAY_OF_WEEK, 6);
        endDate = dayEnd(c);
    }

    private void setMonthDates() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        startDate = dayStart(c);
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
        endDate = dayEnd(c);
    }

    private void showDatePicker() {
        // DateRangePicker from Material Design
        com.google.android.material.datepicker.MaterialDatePicker<
            androidx.core.util.Pair<Long, Long>> picker =
            com.google.android.material.datepicker.MaterialDatePicker
                .Builder.dateRangePicker()
                .setTitleText(getString(R.string.reports_custom))
                .build();
        picker.show(getSupportFragmentManager(), "date_picker");
        picker.addOnPositiveButtonClickListener(selection -> {
            startDate = new Date(selection.first);
            endDate   = new Date(selection.second);
            loadReports();
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

    // ═══════════════════════════════════════════════════════════
    // Bar Chart Setup
    // ═══════════════════════════════════════════════════════════
    private void setupBarChart() {
        if (barChartSales == null) return;
        barChartSales.setDrawBarShadow(false);
        barChartSales.setDrawValueAboveBar(true);
        barChartSales.getDescription().setEnabled(false);
        barChartSales.setMaxVisibleValueCount(10);
        barChartSales.setPinchZoom(false);
        barChartSales.setDrawGridBackground(false);
        barChartSales.getLegend().setEnabled(false);
        barChartSales.getAxisRight().setEnabled(false);
        barChartSales.getAxisLeft().setDrawGridLines(false);
        barChartSales.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChartSales.getXAxis().setDrawGridLines(false);
        barChartSales.getXAxis().setGranularity(1f);
        barChartSales.setNoDataText(getString(R.string.chart_no_data));
        android.util.TypedValue tcv = new android.util.TypedValue();
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, tcv, true);
        int textColor = tcv.data;
        barChartSales.getAxisLeft().setTextColor(textColor);
        barChartSales.getXAxis().setTextColor(textColor);
    }

    private void loadSalesChart(String start, String end) {
        if (barChartSales == null) return;
        try {
            List<HashMap<String, String>> rows = dbHelper.getSalesByPeriod(start, end);
            if (rows == null || rows.isEmpty()) {
                barChartSales.clear();
                return;
            }
            List<BarEntry> entries = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            for (int i = 0; i < rows.size(); i++) {
                HashMap<String, String> row = rows.get(i);
                float total = 0f;
                try { total = Float.parseFloat(row.getOrDefault("total", "0")); } catch (Exception ignored) {}
                entries.add(new BarEntry(i, total));
                String date = row.getOrDefault("date", "");
                if (date.length() >= 10) date = date.substring(5); // MM-DD
                labels.add(date);
            }
            android.util.TypedValue tv = new android.util.TypedValue();
            getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, tv, true);
            int primaryColor = tv.data;
            BarDataSet dataSet = new BarDataSet(entries, "");
            dataSet.setColor(primaryColor);
            dataSet.setDrawValues(rows.size() <= 14);
            BarData data = new BarData(dataSet);
            data.setBarWidth(0.7f);
            barChartSales.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
            barChartSales.setData(data);
            barChartSales.animateY(600);
        } catch (Exception e) {
            Log.e(TAG, "loadSalesChart: " + e.getMessage(), e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Load Reports
    // ═══════════════════════════════════════════════════════════
    private void loadReports() {
        try {
            String start = fmt(startDate);
            String end   = fmt(endDate);
            loadSalesStats(start, end);
            loadInventoryStats();
            loadTopProducts(start, end);
            loadSalesByCategory(start, end);
            loadSalesChart(start, end);
        } catch (Exception e) {
            Log.e(TAG, "loadReports: " + e.getMessage(), e);
            snack(getString(R.string.error_loading));
        }
    }

    private void loadSalesStats(String start, String end) {
        try {
            android.database.Cursor c = dbHelper.getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(total),0), COUNT(*), COALESCE(AVG(total),0) " +
                "FROM invoices WHERE DATE(created_at) BETWEEN ? AND ?",
                new String[]{start, end});
            if (c.moveToFirst()) {
                setText(tvTotalSales,     fmt(c.getDouble(0)));
                setText(tvInvoiceCount,   String.valueOf(c.getInt(1)));
                setText(tvAverageInvoice, fmt(c.getDouble(2)));
            }
            c.close();

            // تقرير الربح الكامل مع COGS
            HashMap<String, Double> profit = dbHelper.getFullProfitReport(start, end);
            double revenue      = profit.get("revenue");
            double cogs         = profit.get("cogs");
            double grossProfit  = profit.get("gross_profit");
            double expenses     = profit.get("expenses");
            double netProfit    = profit.get("net_profit");

            setText(tvTotalExpenses, fmt(expenses));
            int clrError   = androidx.core.content.ContextCompat.getColor(this, R.color.color_error);
            int clrInfo    = androidx.core.content.ContextCompat.getColor(this, R.color.color_info);
            int clrSuccess = androidx.core.content.ContextCompat.getColor(this, R.color.color_success);
            if (tvCogs != null)        { tvCogs.setText(fmt(cogs));        tvCogs.setTextColor(clrError); }
            if (tvGrossProfit != null) {
                tvGrossProfit.setText(fmt(grossProfit));
                tvGrossProfit.setTextColor(grossProfit >= 0 ? clrInfo : clrError);
            }
            if (tvNetProfit != null) {
                tvNetProfit.setText(fmt(netProfit));
                tvNetProfit.setTextColor(netProfit >= 0 ? clrSuccess : clrError);
            }
        } catch (Exception e) {
            Log.e(TAG, "loadSalesStats: " + e.getMessage(), e);
        }
    }

    private void loadInventoryStats() {
        try {
            setText(tvProductsCount,  String.valueOf(dbHelper.getTotalProductsCount()));
            setText(tvInventoryValue, fmt(dbHelper.getTotalInventoryValue()));
            List<HashMap<String,String>> low = dbHelper.getLowStockProducts(5);
            setText(tvLowStock, String.valueOf(low != null ? low.size() : 0));
        } catch (Exception e) {
            Log.e(TAG, "loadInventoryStats: " + e.getMessage(), e);
        }
    }

    private void loadTopProducts(String start, String end) {
        try {
            // ✅ الحقل الصحيح في invoice_items هو "name" وليس "product_name"
            //    وهو "qty" وليس "quantity"
            android.database.Cursor c = dbHelper.getReadableDatabase().rawQuery(
                "SELECT ii.name, SUM(ii.qty) as total_qty, SUM(ii.total) as total_rev " +
                "FROM invoice_items ii " +
                "JOIN invoices i ON ii.invoice_id = i.id " +
                "WHERE DATE(i.created_at) BETWEEN ? AND ? " +
                "GROUP BY ii.name ORDER BY total_qty DESC LIMIT 10",
                new String[]{start, end});

            topProducts.clear();
            while (c.moveToNext()) {
                HashMap<String, String> item = new HashMap<>();
                item.put("name",     c.getString(0)  != null ? c.getString(0) : "");
                item.put("quantity", String.valueOf(c.getInt(1)));
                item.put("revenue",  String.format(Locale.US, "%.2f", c.getDouble(2)));
                topProducts.add(item);
            }
            c.close();

            if (topAdapter != null) topAdapter.notifyDataSetChanged();

            View emptyTop = binding.tvNoTopProducts;
            if (emptyTop != null) emptyTop.setVisibility(topProducts.isEmpty() ? View.VISIBLE : View.GONE);
        } catch (Exception e) {
            Log.e(TAG, "loadTopProducts: " + e.getMessage(), e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Export / Share
    // ═══════════════════════════════════════════════════════════
    private void exportToPDF() {
        try {
            PdfDocument doc = new PdfDocument();
            PdfDocument.Page page = doc.startPage(
                new PdfDocument.PageInfo.Builder(595, 842, 1).create());
            Canvas canvas = page.getCanvas();
            Paint paint   = new Paint();
            paint.setAntiAlias(true);

            // Header
            paint.setColor(androidx.core.content.ContextCompat.getColor(this, R.color.color_info));
            canvas.drawRect(0, 0, 595, 80, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(22); paint.setFakeBoldText(true);
            canvas.drawText(getString(R.string.reports_title), 30, 50, paint);

            // Date
            paint.setColor(Color.DKGRAY);
            paint.setTextSize(12); paint.setFakeBoldText(false);
            String dateRange = fmt(startDate) + " → " + fmt(endDate);
            canvas.drawText(dateRange, 30, 100, paint);

            // Stats
            paint.setColor(Color.BLACK);
            int y = 140;
            paint.setTextSize(14); paint.setFakeBoldText(true);
            canvas.drawText("── " + getString(R.string.reports_sales) + " ──", 30, y, paint); y += 30;
            paint.setFakeBoldText(false); paint.setTextSize(12);
            canvas.drawText(getString(R.string.main_today_sales) + ": " + safe(tvTotalSales), 40, y, paint); y += 22;
            canvas.drawText(getString(R.string.tv_invoice_count_label) + ": " + safe(tvInvoiceCount), 40, y, paint); y += 22;
            canvas.drawText(getString(R.string.invoice_average) + ": " + safe(tvAverageInvoice), 40, y, paint); y += 40;

            paint.setTextSize(14); paint.setFakeBoldText(true);
            canvas.drawText("── " + getString(R.string.reports_products) + " ──", 30, y, paint); y += 30;
            paint.setFakeBoldText(false); paint.setTextSize(12);
            canvas.drawText(getString(R.string.products_count) + ": " + safe(tvProductsCount), 40, y, paint); y += 22;
            canvas.drawText(getString(R.string.tv_inventory_value_label) + ": " + safe(tvInventoryValue), 40, y, paint); y += 22;
            canvas.drawText(getString(R.string.low_stock) + ": " + safe(tvLowStock), 40, y, paint); y += 40;

            // Top Products table header
            paint.setTextSize(14); paint.setFakeBoldText(true);
            canvas.drawText("── " + getString(R.string.reports_products_top) + " ──", 30, y, paint); y += 30;
            paint.setFakeBoldText(false); paint.setTextSize(11);
            for (int i = 0; i < Math.min(topProducts.size(), 10); i++) {
                if (y > 800) break;
                Map<String,String> p = topProducts.get(i);
                String line = (i+1) + ". " + p.get("name") + "  qty:" + p.get("quantity") + "  " + p.get("revenue") + " " + currency;
                canvas.drawText(line, 40, y, paint); y += 20;
            }

            // Footer
            paint.setColor(Color.GRAY); paint.setTextSize(10);
            String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
            canvas.drawText("SmartPOS – " + ts, 30, 820, paint);

            doc.finishPage(page);

            String ts2 = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File file = new File(getExternalFilesDir(null), "Report_" + ts2 + ".pdf");
            FileOutputStream fos = new FileOutputStream(file);
            doc.writeTo(fos); doc.close(); fos.close();

            snack("✓ " + getString(R.string.export_pdf));
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "exportToPDF: " + e.getMessage(), e);
            snack("✗ " + e.getMessage());
        }
    }

    private void shareReport() {
        try {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.reports_title));
            i.putExtra(Intent.EXTRA_TEXT, buildReportText());
            startActivity(Intent.createChooser(i, getString(R.string.share_report)));
        } catch (Exception e) {
            Log.e(TAG, "shareReport: " + e.getMessage(), e);
        }
    }

    private void shareToWhatsApp() {
        try {
            String text = buildReportText();
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.setPackage("com.whatsapp");
            i.putExtra(Intent.EXTRA_TEXT, text);
            startActivity(i);
        } catch (android.content.ActivityNotFoundException e) {
            // WhatsApp not installed, fall back to generic share
            shareReport();
        } catch (Exception e) {
            Log.e(TAG, "shareToWhatsApp: " + e.getMessage(), e);
        }
    }

    private String buildReportText() {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 *").append(getString(R.string.reports_title)).append("*\n\n");
        sb.append("📅 ").append(fmt(startDate)).append(" → ").append(fmt(endDate)).append("\n\n");
        sb.append("💰 ").append(getString(R.string.reports_sales)).append(":\n");
        sb.append("• ").append(getString(R.string.main_today_sales)).append(": ").append(safe(tvTotalSales)).append("\n");
        sb.append("• ").append(getString(R.string.invoices_title)).append(": ").append(safe(tvInvoiceCount)).append("\n");
        sb.append("• ").append(getString(R.string.invoice_average)).append(": ").append(safe(tvAverageInvoice)).append("\n\n");
        sb.append("📦 ").append(getString(R.string.reports_products)).append(":\n");
        sb.append("• ").append(getString(R.string.products_count, dbHelper.getTotalProductsCount())).append("\n");
        sb.append("• ").append(getString(R.string.low_stock)).append(": ").append(safe(tvLowStock)).append("\n\n");
        sb.append("🔴 ").append(getString(R.string.expenses_title)).append(": ").append(safe(tvTotalExpenses)).append("\n");
        sb.append("✅ ").append(getString(R.string.reports_profit)).append(": ").append(tvNetProfit != null ? tvNetProfit.getText() : "").append("\n\n");
        if (!topProducts.isEmpty()) {
            sb.append("🏆 ").append(getString(R.string.reports_products_top)).append(":\n");
            for (int i = 0; i < Math.min(topProducts.size(), 5); i++) {
                Map<String, String> p = topProducts.get(i);
                sb.append(i + 1).append(". ").append(p.getOrDefault("name", ""))
                    .append(" – ").append(p.getOrDefault("quantity", "0")).append(" ").append(getString(R.string.items)).append("\n");
            }
            sb.append("\n");
        }
        sb.append("─────────────\nSmartPOS – ").append(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date()));
        return sb.toString();
    }

    // ═══════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════
    private String fmt(double amount) {
        return String.format(Locale.US, "%.2f %s", amount, currency);
    }

    private String fmt(Date date) {
        if (date == null) return "";
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date);
    }

    private String safe(TextView tv) { return tv != null ? tv.getText().toString() : "0"; }
    private void   setText(TextView tv, String text) { if (tv != null) tv.setText(text); }
    private double toDouble(Object o) {
        if (o == null) return 0;
        try { return ((Number) o).doubleValue(); } catch (Exception e) { return 0; }
    }

    private void snack(String msg) {
        View root = findViewById(android.R.id.content);
        if (root != null) Snackbar.make(root, msg, Snackbar.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) dbHelper.close();
    }

    // ═══════════════════════════════════════════════════════════
    // Adapter
    // ═══════════════════════════════════════════════════════════
    private class TopProductsAdapter extends RecyclerView.Adapter<TopProductsAdapter.VH> {
        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_top_product, parent, false);
            return new VH(v);
        }
        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Map<String,String> item = topProducts.get(pos);
            if (h.rank    != null) h.rank.setText(String.valueOf(pos + 1));
            if (h.name    != null) h.name.setText(item.getOrDefault("name", ""));
            if (h.qty     != null) h.qty.setText(item.getOrDefault("quantity", "0"));
            if (h.revenue != null) h.revenue.setText(item.getOrDefault("revenue", "0") + " " + currency);
        }
        @Override public int getItemCount() { return topProducts.size(); }
        class VH extends RecyclerView.ViewHolder {
            TextView rank, name, qty, revenue;
            VH(@NonNull View v) {
                super(v);
                rank    = v.findViewById(R.id.tv_rank);
                name    = v.findViewById(R.id.tv_product_name);
                qty     = v.findViewById(R.id.tv_quantity);
                revenue = v.findViewById(R.id.tv_revenue);
            }
        }
    }
    private void loadSalesByCategory(String start, String end) {
        try {
            salesByCatList.clear();
            List<HashMap<String, String>> data = dbHelper.getSalesByCategory(start, end);
            if (data != null) salesByCatList.addAll(data);
            if (salesByCatAdapter != null) salesByCatAdapter.notifyDataSetChanged();
            View emptyView = binding.tvNoCategoryData;
            if (emptyView != null)
                emptyView.setVisibility(salesByCatList.isEmpty() ? View.VISIBLE : View.GONE);
        } catch (Exception e) {
            Log.e(TAG, "loadSalesByCategory: " + e.getMessage(), e);
        }
    }

    private class SalesByCategoryAdapter extends RecyclerView.Adapter<SalesByCategoryAdapter.VH> {
        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_top_product, parent, false);
            return new VH(v);
        }
        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            HashMap<String, String> item = salesByCatList.get(pos);
            if (h.rank    != null) h.rank.setText(String.valueOf(pos + 1));
            if (h.name    != null) h.name.setText(item.getOrDefault("category", "غير مصنّف"));
            if (h.qty     != null) h.qty.setText(item.getOrDefault("total_qty", "0") + " قطعة");
            if (h.revenue != null) h.revenue.setText(
                String.format(Locale.US, "%.2f %s",
                    Double.parseDouble(item.getOrDefault("total_sales", "0")), currency));
        }
        @Override public int getItemCount() { return salesByCatList.size(); }
        class VH extends RecyclerView.ViewHolder {
            TextView rank, name, qty, revenue;
            VH(@NonNull View v) {
                super(v);
                rank    = v.findViewById(R.id.tv_rank);
                name    = v.findViewById(R.id.tv_product_name);
                qty     = v.findViewById(R.id.tv_quantity);
                revenue = v.findViewById(R.id.tv_revenue);
            }
        }
    }

}
