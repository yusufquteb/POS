package com.pos.system;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.pos.system.managers.LanguageManager;

/**
 * ActivityLanguageSelectionActivity - اختيار اللغة (ثاني شاشة عند أول تشغيل)
 *
 * العربية والإنجليزية مفعّلتان الآن. الفرنسية والأردو "قريبًا" —
 * الواجهة جاهزة لهما، بانتظار الترجمة الكاملة لاحقًا.
 */
public class ActivityLanguageSelectionActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language_selection);
        applyWindowInsets(findViewById(android.R.id.content));

        View cardAr = findViewById(R.id.card_lang_ar);
        View cardEn = findViewById(R.id.card_lang_en);
        View cardFr = findViewById(R.id.card_lang_fr);
        View cardUr = findViewById(R.id.card_lang_ur);

        if (cardAr != null) cardAr.setOnClickListener(v -> selectLanguage(LanguageManager.LANG_ARABIC));
        if (cardEn != null) cardEn.setOnClickListener(v -> selectLanguage(LanguageManager.LANG_ENGLISH));
        if (cardFr != null) cardFr.setOnClickListener(v -> showToast(getString(R.string.onboarding_language_coming_soon)));
        if (cardUr != null) cardUr.setOnClickListener(v -> showToast(getString(R.string.onboarding_language_coming_soon)));
    }

    private void selectLanguage(String languageCode) {
        try {
            LanguageManager.setLanguagePreference(this, languageCode);
        } catch (Exception ignored) {}

        startActivity(new Intent(this, OnboardingActivity.class));
        finish();
    }

    @Override
    public void onBackPressed() {
        // منع الخروج من شاشة اختيار اللغة
    }
}
