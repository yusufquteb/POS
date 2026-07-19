package com.pos.system;

import android.os.Bundle;
import com.pos.system.FeatureGate;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import com.pos.system.databinding.ActivityReturnBinding;

/**
 * ActivityReturnActivity - شاشة المرتجعات / الاسترداد
 *
 * يعرض قائمة بجميع الفواتير ويتيح للمستخدم اختيار فاتورة
 * ومن ثم تحديد المنتجات المراد إرجاعها وطريقة الاسترداد.
 *
 * @version 1.0
 */
public class ActivityReturnActivity extends BaseActivity {

    private ActivityReturnBinding binding;


    private static final String TAG = "ReturnActivity";

    // ─── Views ──────────────────────────────────────────────────
    private RecyclerView rvInvoices;
    private LinearLayout tvEmpty;
    private TextView tvReturnsCount;
    private TextView tvTotalRefunded;

    // ─── Data ────────────────────────────────────────────────────
    private DBHelper dbHelper;
    private InvoicesAdapter adapter;
    private String currency = "ج.م";
    private final List<HashMap<String, String>> invoicesList = new ArrayList<>();

    // ════════════════════════════════════════════════════════════
    // Lifecycle
    // ════════════════════════════════════════════════════════════

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReturnBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets(binding.getRoot());

        if (!FeatureGate.isUnlocked(this)) {
            FeatureGate.requirePremium(this, "إدارة المرتجعات", true);
            return;
        }

        dbHelper = new DBHelper(this);
        try { currency = dbHelper.getStoreSettings().getOrDefault("currency", "ج.م"); } catch (Exception ignored) {}
        initViews();
        setupToolbar();
        setupRecyclerView();
        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) dbHelper.close();
    }

    // ════════════════════════════════════════════════════════════
    // Initialisation
    // ════════════════════════════════════════════════════════════

    private void initViews() {
        rvInvoices      = binding.rvInvoices;
        tvEmpty         = binding.tvEmpty;
        tvReturnsCount  = binding.tvReturnsCount;
        tvTotalRefunded = binding.tvTotalRefunded;
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = binding.toolbar;
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void setupRecyclerView() {
        if (rvInvoices == null) return;
        adapter = new InvoicesAdapter(invoicesList, this::showReturnDialog, currency);
        rvInvoices.setLayoutManager(new LinearLayoutManager(this));
        rvInvoices.setAdapter(adapter);
    }

    // ════════════════════════════════════════════════════════════
    // Data loading
    // ════════════════════════════════════════════════════════════

    private void loadData() {
        loadInvoices();
        loadReturnStats();
    }

    private void loadInvoices() {
        invoicesList.clear();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.rawQuery(
                "SELECT id, invoice_number, customer_name, total, created_at, payment_method " +
                "FROM invoices ORDER BY created_at DESC", null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    HashMap<String, String> row = new HashMap<>();
                    for (int i = 0; i < cursor.getColumnCount(); i++) {
                        String col = cursor.getColumnName(i);
                        row.put(col, cursor.isNull(i) ? "" : cursor.getString(i));
                    }
                    invoicesList.add(row);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "loadInvoices: " + e.getMessage(), e);
        } finally {
            if (cursor != null) cursor.close();
        }
        updateEmptyState();
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void loadReturnStats() {
        try {
            List<HashMap<String, String>> returns = dbHelper.getReturns();
            int count = (returns != null) ? returns.size() : 0;
            double totalRefunded = 0;
            if (returns != null) {
                for (HashMap<String, String> r : returns) {
                    try {
                        totalRefunded += Double.parseDouble(r.getOrDefault("total_refund", "0"));
                    } catch (NumberFormatException ignored) {}
                }
            }
            if (tvReturnsCount != null)
                tvReturnsCount.setText(String.valueOf(count));
            if (tvTotalRefunded != null)
                tvTotalRefunded.setText(String.format(Locale.getDefault(), "%.2f %s", totalRefunded, currency));
        } catch (Exception e) {
            Log.e(TAG, "loadReturnStats: " + e.getMessage(), e);
        }
    }

    private void updateEmptyState() {
        boolean empty = invoicesList.isEmpty();
        if (rvInvoices != null) rvInvoices.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (tvEmpty    != null) tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    // ════════════════════════════════════════════════════════════
    // Return dialog
    // ════════════════════════════════════════════════════════════

    /**
     * يفتح حوار تفاصيل الفاتورة ويتيح اختيار المنتجات المراد إرجاعها.
     */
    private void showReturnDialog(HashMap<String, String> invoice) {
        long invoiceId;
        try {
            invoiceId = Long.parseLong(invoice.getOrDefault("id", "0"));
        } catch (NumberFormatException e) {
            showSnackbar("فاتورة غير صالحة", true);
            return;
        }

        // Load invoice items
        List<HashMap<String, String>> items = new ArrayList<>();
        try {
            List<HashMap<String, String>> raw = dbHelper.getInvoiceItems(String.valueOf(invoiceId));
            if (raw != null) items.addAll(raw);
        } catch (Exception e) {
            Log.e(TAG, "getInvoiceItems: " + e.getMessage(), e);
        }

        if (items.isEmpty()) {
            showToast("لا توجد منتجات في هذه الفاتورة");
            return;
        }

        // Build dialog view programmatically (no extra layout file needed)
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int dp16 = dp(16);
        int dp8  = dp(8);
        int dp4  = dp(4);
        root.setPadding(dp16, dp8, dp16, dp8);
        scrollView.addView(root);

        // ── Invoice header ──────────────────────────────────────
        String invoiceNumber = invoice.getOrDefault("invoice_number", "-");
        String customerName  = invoice.getOrDefault("customer_name", getString(R.string.general_customer));
        String customerId    = invoice.getOrDefault("customer_id",   "0");
        String invoiceTotal  = invoice.getOrDefault("total",         "0");

        TextView tvHeader = new TextView(this);
        tvHeader.setText("فاتورة: " + invoiceNumber + "\nالعميل: " + customerName
                + "\nالإجمالي: " + formatAmount(invoiceTotal) + " " + currency);
        tvHeader.setTextSize(14);
        tvHeader.setPadding(0, 0, 0, dp8);
        tvHeader.setTextColor(getAttrColor(com.google.android.material.R.attr.colorOnSurface));
        root.addView(tvHeader);

        // ── Divider ─────────────────────────────────────────────
        View divider = new View(this);
        divider.setBackgroundColor(getAttrColor(com.google.android.material.R.attr.colorOutlineVariant));
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        divParams.setMargins(0, dp4, 0, dp8);
        divider.setLayoutParams(divParams);
        root.addView(divider);

        // ── Section label ────────────────────────────────────────
        TextView tvItemsLabel = new TextView(this);
        tvItemsLabel.setText("اختر المنتجات المراد إرجاعها:");
        tvItemsLabel.setTextSize(13);
        tvItemsLabel.setTextColor(getAttrColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
        tvItemsLabel.setPadding(0, 0, 0, dp8);
        root.addView(tvItemsLabel);

        // ── Item rows ────────────────────────────────────────────
        // Keep parallel lists: checkboxes + qty edit texts
        List<CheckBox>  checkBoxes = new ArrayList<>();
        List<EditText>  qtyEdits   = new ArrayList<>();
        List<Integer>   maxQties   = new ArrayList<>();

        for (HashMap<String, String> item : items) {
            String name     = item.getOrDefault("name",  "منتج");
            String priceStr = item.getOrDefault("price", "0");
            String qtyStr   = item.getOrDefault("qty",   "1");
            int originalQty = safeInt(qtyStr, 1);

            // Row container
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowParams.setMargins(0, 0, 0, dp8);
            row.setLayoutParams(rowParams);

            // CheckBox
            CheckBox cb = new CheckBox(this);
            cb.setChecked(false);
            cb.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            row.addView(cb);

            // Name + price label
            LinearLayout infoLayout = new LinearLayout(this);
            infoLayout.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            infoParams.setMarginStart(dp8);
            infoLayout.setLayoutParams(infoParams);

            TextView tvName = new TextView(this);
            tvName.setText(name);
            tvName.setTextSize(14);
            tvName.setTextColor(getAttrColor(com.google.android.material.R.attr.colorOnSurface));
            infoLayout.addView(tvName);

            TextView tvPrice = new TextView(this);
            tvPrice.setText(formatAmount(priceStr) + " " + currency + " × " + originalQty);
            tvPrice.setTextSize(12);
            tvPrice.setTextColor(getAttrColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
            infoLayout.addView(tvPrice);

            row.addView(infoLayout);

            // Qty EditText
            EditText etQty = new EditText(this);
            etQty.setText(String.valueOf(originalQty));
            etQty.setInputType(InputType.TYPE_CLASS_NUMBER);
            etQty.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
            etQty.setEnabled(false);
            etQty.setTextSize(14);
            LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(dp(56), dp(40));
            etParams.setMarginStart(dp8);
            etQty.setLayoutParams(etParams);
            etQty.setGravity(android.view.Gravity.CENTER);
            row.addView(etQty);

            // Enable/disable qty based on checkbox
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                etQty.setEnabled(isChecked);
                if (!isChecked) etQty.setText(String.valueOf(originalQty));
            });

            root.addView(row);
            checkBoxes.add(cb);
            qtyEdits.add(etQty);
            maxQties.add(originalQty);
        }

        // ── Refund method ────────────────────────────────────────
        TextView tvRefundMethod = new TextView(this);
        tvRefundMethod.setText("طريقة الاسترداد:");
        tvRefundMethod.setTextSize(13);
        tvRefundMethod.setPadding(0, dp8, 0, dp4);
        tvRefundMethod.setTextColor(getAttrColor(com.google.android.material.R.attr.colorOnSurface));
        root.addView(tvRefundMethod);

        ChipGroup chipGroup = new ChipGroup(this);
        chipGroup.setSingleSelection(true);
        chipGroup.setSelectionRequired(true);

        Chip chipCash = new Chip(this);
        chipCash.setText("نقداً");
        chipCash.setCheckable(true);
        chipCash.setChecked(true);
        chipGroup.addView(chipCash);

        Chip chipCard = new Chip(this);
        chipCard.setText("بطاقة");
        chipCard.setCheckable(true);
        chipGroup.addView(chipCard);

        Chip chipBank = new Chip(this);
        chipBank.setText("تحويل بنكي");
        chipBank.setCheckable(true);
        chipGroup.addView(chipBank);

        LinearLayout.LayoutParams cgParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cgParams.setMargins(0, 0, 0, dp8);
        chipGroup.setLayoutParams(cgParams);
        root.addView(chipGroup);

        // ── Reason field ─────────────────────────────────────────
        TextView tvReasonLabel = new TextView(this);
        tvReasonLabel.setText("سبب الإرجاع (اختياري):");
        tvReasonLabel.setTextSize(13);
        tvReasonLabel.setPadding(0, dp4, 0, dp4);
        tvReasonLabel.setTextColor(getAttrColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
        root.addView(tvReasonLabel);

        TextInputEditText etReason = new TextInputEditText(this);
        etReason.setHint("أدخل سبب الإرجاع...");
        etReason.setLines(2);
        etReason.setMaxLines(3);
        etReason.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        LinearLayout.LayoutParams reasonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        reasonParams.setMargins(0, 0, 0, dp8);
        etReason.setLayoutParams(reasonParams);
        root.addView(etReason);

        // ── Build & show dialog ──────────────────────────────────
        final long finalInvoiceId = invoiceId;

        new MaterialAlertDialogBuilder(this)
                .setTitle("مرتجع فاتورة: " + invoiceNumber)
                .setView(scrollView)
                .setPositiveButton("تنفيذ المرتجع", (dialog, which) -> {
                    // Collect selected items
                    List<HashMap<String, String>> selectedItems = new ArrayList<>();
                    double totalRefund = 0;

                    for (int i = 0; i < items.size(); i++) {
                        if (!checkBoxes.get(i).isChecked()) continue;

                        String qtyText = qtyEdits.get(i).getText() != null
                                ? qtyEdits.get(i).getText().toString().trim() : "";
                        int qty = safeInt(qtyText, 0);
                        int maxQty = maxQties.get(i);

                        if (qty <= 0 || qty > maxQty) {
                            showToast("كمية غير صحيحة للمنتج: " + items.get(i).getOrDefault("name", ""));
                            return;
                        }

                        HashMap<String, String> orig = items.get(i);
                        double price = safeDouble(orig.getOrDefault("price", "0"));
                        double itemTotal = price * qty;
                        totalRefund += itemTotal;

                        HashMap<String, String> sel = new HashMap<>();
                        sel.put("product_id", orig.getOrDefault("product_id", ""));
                        sel.put("barcode",    orig.getOrDefault("barcode",    ""));
                        sel.put("name",       orig.getOrDefault("name",       ""));
                        sel.put("price",      String.valueOf(price));
                        sel.put("qty",        String.valueOf(qty));
                        sel.put("total",      String.valueOf(itemTotal));
                        selectedItems.add(sel);
                    }

                    if (selectedItems.isEmpty()) {
                        showToast("يرجى تحديد منتج واحد على الأقل");
                        return;
                    }

                    // Refund method from chip group
                    String refundMethod = "cash";
                    int checkedChipId = chipGroup.getCheckedChipId();
                    if (checkedChipId == chipCard.getId())       refundMethod = "card";
                    else if (checkedChipId == chipBank.getId())  refundMethod = "bank_transfer";

                    String reason = etReason.getText() != null
                            ? etReason.getText().toString().trim() : "";

                    processReturn(
                            finalInvoiceId,
                            invoiceNumber,
                            customerId,
                            customerName,
                            selectedItems,
                            totalRefund,
                            reason,
                            refundMethod
                    );
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // ════════════════════════════════════════════════════════════
    // Return processing
    // ════════════════════════════════════════════════════════════

    private void processReturn(long originalInvoiceId,
                               String originalInvoiceNumber,
                               String customerId,
                               String customerName,
                               List<HashMap<String, String>> selectedItems,
                               double totalRefund,
                               String reason,
                               String refundMethod) {
        String returnNumber = generateReturnNumber();
        try {
            long returnId = dbHelper.createReturn(
                    returnNumber,
                    originalInvoiceId,
                    originalInvoiceNumber,
                    customerId,
                    customerName,
                    selectedItems,
                    totalRefund,
                    reason,
                    refundMethod
            );

            if (returnId > 0) {
                // Success
                loadData();
                showSuccessSnackbar(returnNumber, totalRefund, refundMethod);
            } else {
                showSnackbar("فشل في تسجيل المرتجع، يرجى المحاولة مرة أخرى", true);
            }
        } catch (Exception e) {
            Log.e(TAG, "processReturn error: " + e.getMessage(), e);
            showToast("خطأ في تسجيل المرتجع: " + e.getMessage());
        }
    }

    private void showSuccessSnackbar(String returnNumber, double totalRefund, String refundMethod) {
        View rootView = findViewById(android.R.id.content);
        if (rootView == null) return;

        String methodLabel;
        switch (refundMethod) {
            case "card":          methodLabel = "بطاقة";        break;
            case "bank_transfer": methodLabel = "تحويل بنكي";   break;
            default:              methodLabel = "نقداً";         break;
        }

        String msg = "✓ " + getString(R.string.refund_success)
                + " | " + returnNumber
                + " | " + String.format(Locale.getDefault(), "%.2f %s", totalRefund, currency)
                + " | " + methodLabel;

        Snackbar.make(rootView, msg, Snackbar.LENGTH_LONG)
                .setAction("حسناً", v -> { /* dismiss */ })
                .show();
    }

    // ════════════════════════════════════════════════════════════
    // Helpers
    // ════════════════════════════════════════════════════════════

    private String generateReturnNumber() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault());
        return "RET-" + sdf.format(new Date());
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private int getAttrColor(int attr) {
        int[] attrs = {attr};
        android.content.res.TypedArray ta = obtainStyledAttributes(attrs);
        int color = ta.getColor(0, 0xFF808080);
        ta.recycle();
        return color;
    }

    private static int safeInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    private static double safeDouble(String s) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0; }
    }

    private static String formatAmount(String s) {
        try {
            return String.format(Locale.getDefault(), "%.2f", Double.parseDouble(s));
        } catch (Exception e) {
            return s;
        }
    }

    // ════════════════════════════════════════════════════════════
    // Adapter
    // ════════════════════════════════════════════════════════════

    interface OnInvoiceClickListener {
        void onInvoiceClick(HashMap<String, String> invoice);
    }

    private static class InvoicesAdapter
            extends RecyclerView.Adapter<InvoicesAdapter.ViewHolder> {

        private final List<HashMap<String, String>> data;
        private final OnInvoiceClickListener listener;
        private final String currency;

        InvoicesAdapter(List<HashMap<String, String>> data, OnInvoiceClickListener listener, String currency) {
            this.data     = data;
            this.listener = listener;
            this.currency = currency;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_invoice, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HashMap<String, String> invoice = data.get(position);
            holder.bind(invoice, currency);
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onInvoiceClick(invoice);
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView tvNumber;
            final TextView tvCustomer;
            final TextView tvTotal;
            final TextView tvDate;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvNumber   = itemView.findViewById(R.id.tv_invoice_number);
                tvCustomer = itemView.findViewById(R.id.tv_customer_name);
                tvTotal    = itemView.findViewById(R.id.tv_total);
                tvDate     = itemView.findViewById(R.id.tv_date);
            }

            void bind(HashMap<String, String> inv, String currency) {
                if (tvNumber != null)
                    tvNumber.setText(inv.getOrDefault("invoice_number", "-"));
                if (tvCustomer != null)
                    tvCustomer.setText(inv.getOrDefault("customer_name",
                            itemView.getContext().getString(R.string.general_customer)));
                if (tvTotal != null) {
                    String raw = inv.getOrDefault("total", "0");
                    try {
                        tvTotal.setText(String.format(Locale.getDefault(),
                                "%.2f %s", Double.parseDouble(raw), currency));
                    } catch (NumberFormatException e) {
                        tvTotal.setText(raw);
                    }
                }
                if (tvDate != null)
                    tvDate.setText(inv.getOrDefault("created_at", ""));
            }
        }
    }
}
