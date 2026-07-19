package com.pos.system;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import com.pos.system.databinding.ActivityPriceQuotesBinding;

public class ActivityPriceQuotesActivity extends BaseActivity {

    private ActivityPriceQuotesBinding binding;
    private DBHelper dbHelper;
    private String currency = "ج.م";
    private List<HashMap<String, String>> quotes = new ArrayList<>();
    private QuotesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPriceQuotesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets(binding.getRoot());

        dbHelper = new DBHelper(this);
        try {
            HashMap<String, String> s = dbHelper.getStoreSettings();
            if (s != null) currency = s.getOrDefault("currency", "ج.م");
        } catch (Exception ignored) {}

        setupToolbar();
        setupRecycler();
        binding.fabNewQuote.setOnClickListener(v ->
            startActivity(new Intent(this, ActivityPriceQuoteActivity.class)));
        loadData();
    }

    @Override
    protected void onResume() { super.onResume(); loadData(); }

    private void setupToolbar() {
        MaterialToolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecycler() {
        binding.recyclerQuotes.setLayoutManager(new LinearLayoutManager(this));
        adapter = new QuotesAdapter();
        binding.recyclerQuotes.setAdapter(adapter);
    }

    private void loadData() {
        quotes.clear();
        quotes.addAll(dbHelper.getAllPriceQuotes());
        boolean empty = quotes.isEmpty();
        binding.recyclerQuotes.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        adapter.notifyDataSetChanged();
    }

    private class QuotesAdapter extends RecyclerView.Adapter<QuotesAdapter.VH> {
        class VH extends RecyclerView.ViewHolder {
            TextView tvNumber, tvCustomer, tvDate, tvTotal, tvStatus;
            VH(View v) { super(v);
                tvNumber   = v.findViewById(R.id.tv_quote_number);
                tvCustomer = v.findViewById(R.id.tv_customer);
                tvDate     = v.findViewById(R.id.tv_date);
                tvTotal    = v.findViewById(R.id.tv_total);
                tvStatus   = v.findViewById(R.id.tv_status);
            }
        }
        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_price_quote, parent, false));
        }
        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            HashMap<String, String> q = quotes.get(pos);
            double total = 0;
            try { total = Double.parseDouble(q.getOrDefault("total","0")); } catch (Exception ignored) {}
            if (h.tvNumber   != null) h.tvNumber.setText(q.getOrDefault("quote_number",""));
            if (h.tvCustomer != null) h.tvCustomer.setText(q.getOrDefault("customer_name", getString(R.string.general_customer)));
            if (h.tvDate     != null) h.tvDate.setText(q.getOrDefault("created_at",""));
            if (h.tvTotal    != null) h.tvTotal.setText(String.format(Locale.US,"%.2f %s", total, currency));
            if (h.tvStatus   != null) {
                String status = q.getOrDefault("status","active");
                h.tvStatus.setText("active".equals(status) ? "نشط" : "محوّل");
            }
            h.itemView.setOnClickListener(v -> showQuoteOptions(q));
        }
        @Override public int getItemCount() { return quotes.size(); }
    }

    private void showQuoteOptions(HashMap<String, String> q) {
        String quoteNumber = q.getOrDefault("quote_number", "");
        String status      = q.getOrDefault("status", "active");
        boolean converted  = "converted".equals(status);

        String[] options = converted
            ? new String[]{getString(R.string.quote_option_view)}
            : new String[]{getString(R.string.quote_option_convert), getString(R.string.quote_option_view)};

        new MaterialAlertDialogBuilder(this)
            .setTitle(quoteNumber)
            .setItems(options, (d, which) -> {
                if (!converted && which == 0) {
                    confirmConvertToInvoice(q);
                }
            })
            .setNegativeButton(getString(R.string.cancel), null)
            .show();
    }

    private void confirmConvertToInvoice(HashMap<String, String> q) {
        String quoteNumber = q.getOrDefault("quote_number", "");
        new MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.quote_convert_title))
            .setMessage(getString(R.string.quote_convert_msg, quoteNumber))
            .setPositiveButton(getString(R.string.ok), (d, w) -> {
                long quoteId = 0;
                try { quoteId = Long.parseLong(q.getOrDefault("id", "0")); }
                catch (Exception ignored) {}
                Intent intent = new Intent(this, ActivityCartActivity.class);
                intent.putExtra(ActivityCartActivity.EXTRA_QUOTE_ID, quoteId);
                startActivity(intent);
            })
            .setNegativeButton(getString(R.string.cancel), null)
            .show();
    }
}
