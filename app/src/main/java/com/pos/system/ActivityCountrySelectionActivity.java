package com.pos.system;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

/**
 * ActivityCountrySelectionActivity - اختيار الدولة (أول شاشة عند أول تشغيل)
 *
 * تظهر مرة واحدة فقط قبل شاشة اختيار اللغة، ثم مقدمة التطبيق.
 */
public class ActivityCountrySelectionActivity extends BaseActivity {

    private RecyclerView recyclerCountries;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_country_selection);
        applyWindowInsets(findViewById(android.R.id.content));

        dbHelper = new DBHelper(this);

        recyclerCountries = findViewById(R.id.recycler_countries);
        recyclerCountries.setLayoutManager(new LinearLayoutManager(this));
        recyclerCountries.setAdapter(new CountryAdapter(CountryConfig.all()));
    }

    private void onCountrySelected(CountryConfig country) {
        try {
            dbHelper.saveStoreSetting("country_code", country.code);
            dbHelper.saveStoreSetting("currency", country.currency);
            dbHelper.saveStoreSetting("tax_rate", String.valueOf(country.vatRate));
        } catch (Exception ignored) {}

        startActivity(new Intent(this, ActivityLanguageSelectionActivity.class));
        finish();
    }

    @Override
    public void onBackPressed() {
        // منع الخروج من شاشة اختيار الدولة الأولى
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) dbHelper.close();
    }

    private class CountryAdapter extends RecyclerView.Adapter<CountryAdapter.VH> {
        private final List<CountryConfig> countries;

        CountryAdapter(List<CountryConfig> countries) {
            this.countries = countries;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_country_flag, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            CountryConfig country = countries.get(position);
            if (holder.ivFlag != null) holder.ivFlag.setImageResource(country.flagRes);
            if (holder.tvName != null) holder.tvName.setText(country.displayName());
            if (holder.tvCurrency != null) holder.tvCurrency.setText(country.currency);
            holder.itemView.setOnClickListener(v -> onCountrySelected(country));
        }

        @Override public int getItemCount() { return countries.size(); }

        class VH extends RecyclerView.ViewHolder {
            ImageView ivFlag;
            TextView tvName, tvCurrency;
            VH(View v) {
                super(v);
                ivFlag      = v.findViewById(R.id.iv_flag);
                tvName      = v.findViewById(R.id.tv_country_name);
                tvCurrency  = v.findViewById(R.id.tv_country_currency);
            }
        }
    }
}
