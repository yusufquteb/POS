package com.pos.system;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * OnboardingActivity - شاشات التعريف بالتطبيق
 *
 * تظهر للمرة الأولى فقط عند تشغيل التطبيق.
 * 3 شرائح: المنتجات | الفواتير | التقارير
 *
 * @version 1.1 (Final)
 */
public class OnboardingActivity extends BaseActivity {

    private static final String PREFS_NAME    = "onboarding_prefs";
    private static final String KEY_FIRST_TIME = "is_first_time";

    private ViewPager2      viewPager;
    private TabLayout       tabLayout;
    private MaterialButton  btnNext;
    private Button          btnSkip;
    private MaterialButton  btnGetStarted;

    private int currentPage = 0;
    private static final int TOTAL_PAGES = OnboardingAdapter.NUM_PAGES;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        initViews();
        setupViewPager();
        setupListeners();
    }

    // ─── Init ─────────────────────────────────────────────────────
    private void initViews() {
        viewPager     = findViewById(R.id.viewPager);
        tabLayout     = findViewById(R.id.tabLayout);
        btnNext       = findViewById(R.id.btn_next);
        btnSkip       = findViewById(R.id.btn_skip);
        btnGetStarted = findViewById(R.id.btn_get_started);

        if (btnGetStarted != null) btnGetStarted.setVisibility(View.GONE);
    }

    private void setupViewPager() {
        if (viewPager == null) return;

        viewPager.setAdapter(new OnboardingAdapter(this));

        if (tabLayout != null) {
            new TabLayoutMediator(tabLayout, viewPager, (tab, pos) -> {}).attach();
        }

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentPage = position;
                updateButtons();
            }
        });
    }

    private void setupListeners() {
        if (btnNext       != null) btnNext.setOnClickListener(v -> {
            if (currentPage < TOTAL_PAGES - 1) viewPager.setCurrentItem(currentPage + 1);
        });
        if (btnSkip       != null) btnSkip.setOnClickListener(v -> finishOnboarding());
        if (btnGetStarted != null) btnGetStarted.setOnClickListener(v -> finishOnboarding());
    }

    // ─── Buttons ──────────────────────────────────────────────────
    private void updateButtons() {
        boolean isLast = (currentPage == TOTAL_PAGES - 1);
        if (btnNext != null)       btnNext.setVisibility(isLast ? View.GONE : View.VISIBLE);
        if (btnSkip != null)       btnSkip.setVisibility(isLast ? View.GONE : View.VISIBLE);
        if (btnGetStarted != null) btnGetStarted.setVisibility(isLast ? View.VISIBLE : View.GONE);
    }

    // ─── Finish ───────────────────────────────────────────────────
    private void finishOnboarding() {
        // حفظ أن المستخدم شاهد Onboarding
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_FIRST_TIME, false)
            .apply();

        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    // ─── Static Helpers ───────────────────────────────────────────

    /** التحقق من المرة الأولى */
    public static boolean isFirstTime(android.content.Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                  .getBoolean(KEY_FIRST_TIME, true);
    }

    /** إعادة تعيين (للاختبار) */
    public static void resetOnboarding(android.content.Context ctx) {
        ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
           .edit()
           .putBoolean(KEY_FIRST_TIME, true)
           .apply();
    }

    @Override
    public void onBackPressed() {
        if (currentPage > 0) {
            viewPager.setCurrentItem(currentPage - 1);
        } else {
            finishOnboarding();
        }
    }
}
