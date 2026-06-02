package com.pos.system;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * ActivityShiftActivity — إدارة الوردية (الشيفت)
 *
 * الميزات:
 * - عرض حالة الوردية الحالية (مفتوحة / مغلقة)
 * - فتح وردية جديدة بإدخال النقد الافتتاحي
 * - إغلاق الوردية مع ملخص المبيعات
 * - عرض سجل الورديات السابقة
 */
public class ActivityShiftActivity extends BaseActivity {

    // ─── Views ───────────────────────────────────────────────
    private MaterialToolbar    toolbar;
    private MaterialCardView   cardShiftStatus;
    private TextView           tvShiftStatusLabel;
    private TextView           tvShiftStatusBadge;
    private TextView           tvOpeningCash;
    private TextView           tvOpenedAt;
    private TextView           tvCurrentSales;
    private TextView           tvInvoiceCount;
    private View               layoutShiftOpen;
    private View               layoutNoShift;
    private TextInputEditText  etOpeningCash;
    private MaterialButton     btnOpenShift;
    private MaterialButton     btnCloseShift;
    private RecyclerView       rvShifts;
    private TextView           tvNoShifts;

    // ─── Data ─────────────────────────────────────────────────
    private DBHelper           dbHelper;
    private String             currency = "ر.س";
    private ShiftsAdapter      adapter;
    private List<HashMap<String, String>> shiftsList = new ArrayList<>();

    // ═══════════════════════════════════════════════════════════
    //  Lifecycle
    // ═══════════════════════════════════════════════════════════
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shift);

        dbHelper = new DBHelper(this);

        // Read currency from store settings
        HashMap<String, String> storeSettings = dbHelper.getStoreSettings();
        if (storeSettings != null && storeSettings.containsKey("currency")) {
            String c = storeSettings.get("currency");
            if (!TextUtils.isEmpty(c)) currency = c;
        }

        bindViews();
        setupToolbar();
        setupRecyclerView();
        applyWindowInsets(findViewById(R.id.coordinator_shift));
        refreshUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUI();
    }

    // ═══════════════════════════════════════════════════════════
    //  Setup
    // ═══════════════════════════════════════════════════════════
    private void bindViews() {
        toolbar           = findViewById(R.id.toolbar);
        cardShiftStatus   = findViewById(R.id.card_shift_status);
        tvShiftStatusLabel = findViewById(R.id.tv_shift_status_label);
        tvShiftStatusBadge = findViewById(R.id.tv_shift_status_badge);
        tvOpeningCash     = findViewById(R.id.tv_opening_cash);
        tvOpenedAt        = findViewById(R.id.tv_opened_at);
        tvCurrentSales    = findViewById(R.id.tv_current_sales);
        tvInvoiceCount    = findViewById(R.id.tv_invoice_count);
        layoutShiftOpen   = findViewById(R.id.layout_shift_open);
        layoutNoShift     = findViewById(R.id.layout_no_shift);
        etOpeningCash     = findViewById(R.id.et_opening_cash);
        btnOpenShift      = findViewById(R.id.btn_open_shift);
        btnCloseShift     = findViewById(R.id.btn_close_shift);
        rvShifts          = findViewById(R.id.rv_shifts);
        tvNoShifts        = findViewById(R.id.tv_no_shifts);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        adapter = new ShiftsAdapter(shiftsList);
        rvShifts.setLayoutManager(new LinearLayoutManager(this));
        rvShifts.setAdapter(adapter);
        rvShifts.setNestedScrollingEnabled(false);
    }

    // ═══════════════════════════════════════════════════════════
    //  UI Refresh
    // ═══════════════════════════════════════════════════════════
    private void refreshUI() {
        refreshCurrentShiftCard();
        refreshShiftHistory();
    }

    private void refreshCurrentShiftCard() {
        HashMap<String, String> current = dbHelper.getCurrentShift();

        if (current != null && "open".equals(current.get("status"))) {
            // ── Shift is OPEN ──────────────────────────────────
            cardShiftStatus.setCardBackgroundColor(
                    getResources().getColor(R.color.shift_open_bg, getTheme()));
            tvShiftStatusBadge.setText(getString(R.string.shift_status_open));
            tvShiftStatusBadge.setTextColor(
                    getResources().getColor(R.color.shift_open_text, getTheme()));
            tvShiftStatusLabel.setText(getString(R.string.shift_current_status));

            String openingCash = current.getOrDefault("opening_cash", "0");
            String openedAt    = current.getOrDefault("opened_at",    "-");
            String totalSales  = current.getOrDefault("total_sales",  "0");
            String invCount    = current.getOrDefault("invoice_count","0");

            tvOpeningCash.setText(formatCurrency(parseDouble(openingCash)));
            tvOpenedAt.setText(openedAt);
            tvCurrentSales.setText(formatCurrency(parseDouble(totalSales)));
            tvInvoiceCount.setText(invCount);

            layoutShiftOpen.setVisibility(View.VISIBLE);
            layoutNoShift.setVisibility(View.GONE);

            btnCloseShift.setVisibility(View.VISIBLE);
            btnOpenShift.setVisibility(View.GONE);

            // Wire close button
            String shiftId = current.getOrDefault("id", "0");
            btnCloseShift.setOnClickListener(v ->
                    showCloseShiftDialog(Long.parseLong(shiftId), current));

        } else {
            // ── No open shift ──────────────────────────────────
            cardShiftStatus.setCardBackgroundColor(
                    getResources().getColor(R.color.shift_closed_bg, getTheme()));
            tvShiftStatusBadge.setText(getString(R.string.shift_status_closed));
            tvShiftStatusBadge.setTextColor(
                    getResources().getColor(R.color.shift_closed_text, getTheme()));
            tvShiftStatusLabel.setText(getString(R.string.shift_no_open));

            layoutShiftOpen.setVisibility(View.GONE);
            layoutNoShift.setVisibility(View.VISIBLE);

            btnCloseShift.setVisibility(View.GONE);
            btnOpenShift.setVisibility(View.VISIBLE);

            btnOpenShift.setOnClickListener(v -> showOpenShiftDialog());
        }
    }

    private void refreshShiftHistory() {
        shiftsList.clear();
        List<HashMap<String, String>> all = dbHelper.getAllShifts();
        if (all != null) shiftsList.addAll(all);
        adapter.notifyDataSetChanged();

        if (shiftsList.isEmpty()) {
            tvNoShifts.setVisibility(View.VISIBLE);
            rvShifts.setVisibility(View.GONE);
        } else {
            tvNoShifts.setVisibility(View.GONE);
            rvShifts.setVisibility(View.VISIBLE);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  Dialogs
    // ═══════════════════════════════════════════════════════════

    /** Dialog: Enter opening cash → open shift */
    private void showOpenShiftDialog() {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_opening_cash, null);
        TextInputEditText etCash = dialogView.findViewById(R.id.et_dialog_opening_cash);

        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.shift_open_title))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.shift_open_confirm), (dialog, which) -> {
                    hideKeyboard();
                    String cashStr = etCash.getText() != null
                            ? etCash.getText().toString().trim() : "0";
                    double cash = parseDouble(cashStr);
                    long shiftId = dbHelper.openShift(cash);
                    if (shiftId > 0) {
                        snack(getString(R.string.shift_opened_success));
                        refreshUI();
                    } else {
                        snack(getString(R.string.shift_open_error));
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    /** Dialog: Show shift summary, enter closing cash → close shift */
    private void showCloseShiftDialog(long shiftId, HashMap<String, String> current) {
        String openingCash = current.getOrDefault("opening_cash", "0");
        String totalSales  = current.getOrDefault("total_sales",  "0");
        String invCount    = current.getOrDefault("invoice_count","0");
        String openedAt    = current.getOrDefault("opened_at",    "-");

        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_closing_shift, null);

        TextView tvSummaryOpening  = dialogView.findViewById(R.id.tv_summary_opening_cash);
        TextView tvSummarySales    = dialogView.findViewById(R.id.tv_summary_total_sales);
        TextView tvSummaryInvoices = dialogView.findViewById(R.id.tv_summary_invoice_count);
        TextView tvSummaryOpened   = dialogView.findViewById(R.id.tv_summary_opened_at);
        TextInputEditText etClosingCash = dialogView.findViewById(R.id.et_dialog_closing_cash);

        tvSummaryOpening.setText(formatCurrency(parseDouble(openingCash)));
        tvSummarySales.setText(formatCurrency(parseDouble(totalSales)));
        tvSummaryInvoices.setText(invCount);
        tvSummaryOpened.setText(openedAt);

        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.shift_close_title))
                .setView(dialogView)
                .setPositiveButton(getString(R.string.shift_close_confirm), (dialog, which) -> {
                    hideKeyboard();
                    String cashStr = etClosingCash.getText() != null
                            ? etClosingCash.getText().toString().trim() : "0";
                    double closingCash = parseDouble(cashStr);
                    boolean success = dbHelper.closeShift(shiftId, closingCash);
                    if (success) {
                        snack(getString(R.string.shift_closed_success));
                        refreshUI();
                    } else {
                        snack(getString(R.string.shift_close_error));
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    // ═══════════════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════════════
    private String formatCurrency(double amount) {
        return String.format(Locale.US, "%.2f %s", amount, currency);
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0; }
    }

    private void snack(String msg) {
        View root = findViewById(R.id.coordinator_shift);
        if (root != null) Snackbar.make(root, msg, Snackbar.LENGTH_SHORT).show();
        else showToast(msg);
    }

    // ═══════════════════════════════════════════════════════════
    //  RecyclerView Adapter — Shift History
    // ═══════════════════════════════════════════════════════════
    private class ShiftsAdapter
            extends RecyclerView.Adapter<ShiftsAdapter.ShiftViewHolder> {

        private final List<HashMap<String, String>> data;

        ShiftsAdapter(List<HashMap<String, String>> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public ShiftViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_shift, parent, false);
            return new ShiftViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ShiftViewHolder h, int position) {
            HashMap<String, String> shift = data.get(position);

            String id           = shift.getOrDefault("id",            "-");
            String openingCash  = shift.getOrDefault("opening_cash",  "0");
            String closingCash  = shift.getOrDefault("closing_cash",  null);
            String totalSales   = shift.getOrDefault("total_sales",   "0");
            String invCount     = shift.getOrDefault("invoice_count", "0");
            String status       = shift.getOrDefault("status",        "closed");
            String openedAt     = shift.getOrDefault("opened_at",     "-");
            String closedAt     = shift.getOrDefault("closed_at",     null);

            // Shift date header = opened_at date part only
            String dateDisplay = openedAt.length() >= 10
                    ? openedAt.substring(0, 10) : openedAt;

            h.tvShiftDate.setText(getString(R.string.shift_number_format, id, dateDisplay));
            h.tvOpenedAt.setText(getString(R.string.shift_opened_at_label, openedAt));
            h.tvOpeningCash.setText(getString(R.string.shift_opening_cash_label,
                    formatCurrency(parseDouble(openingCash))));
            h.tvClosingCash.setText(getString(R.string.shift_closing_cash_label,
                    closingCash != null && !closingCash.isEmpty()
                            ? formatCurrency(parseDouble(closingCash))
                            : getString(R.string.shift_not_closed_yet)));
            h.tvTotalSales.setText(getString(R.string.shift_total_sales_label,
                    formatCurrency(parseDouble(totalSales))));
            h.tvInvoiceCount.setText(getString(R.string.shift_invoice_count_label, invCount));

            // Closed at (only when closed)
            if ("closed".equals(status) && closedAt != null && !closedAt.isEmpty()) {
                h.tvClosedAt.setVisibility(View.VISIBLE);
                h.tvClosedAt.setText(getString(R.string.shift_closed_at_label, closedAt));
            } else {
                h.tvClosedAt.setVisibility(View.GONE);
            }

            // Status chip
            if ("open".equals(status)) {
                h.tvStatusChip.setText(getString(R.string.shift_status_open));
                h.tvStatusChip.setBackgroundResource(R.drawable.bg_chip_open);
                h.tvStatusChip.setTextColor(
                        getResources().getColor(R.color.shift_open_text, getTheme()));
            } else {
                h.tvStatusChip.setText(getString(R.string.shift_status_closed));
                h.tvStatusChip.setBackgroundResource(R.drawable.bg_chip_closed);
                h.tvStatusChip.setTextColor(
                        getResources().getColor(R.color.shift_closed_text, getTheme()));
            }
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class ShiftViewHolder extends RecyclerView.ViewHolder {
            TextView tvShiftDate;
            TextView tvOpenedAt;
            TextView tvClosedAt;
            TextView tvOpeningCash;
            TextView tvClosingCash;
            TextView tvTotalSales;
            TextView tvInvoiceCount;
            TextView tvStatusChip;

            ShiftViewHolder(@NonNull View itemView) {
                super(itemView);
                tvShiftDate    = itemView.findViewById(R.id.tv_shift_date);
                tvOpenedAt     = itemView.findViewById(R.id.tv_shift_opened_at);
                tvClosedAt     = itemView.findViewById(R.id.tv_shift_closed_at);
                tvOpeningCash  = itemView.findViewById(R.id.tv_shift_opening_cash);
                tvClosingCash  = itemView.findViewById(R.id.tv_shift_closing_cash);
                tvTotalSales   = itemView.findViewById(R.id.tv_shift_total_sales);
                tvInvoiceCount = itemView.findViewById(R.id.tv_shift_invoice_count);
                tvStatusChip   = itemView.findViewById(R.id.tv_shift_status_chip);
            }
        }
    }
}
