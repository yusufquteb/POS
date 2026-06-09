package com.pos.system;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.pos.system.managers.BillingManager;
import com.pos.system.managers.LanguageManager;
import com.pos.system.managers.ReviewManager;
import com.pos.system.managers.ThemeManager;
import com.pos.system.databinding.ActivitySettingsBinding;

/**
 * ActivitySettingsActivity - صفحة الإعدادات
 *
 * يرث من BaseActivity الذي يطبق الثيم واللغة تلقائياً.
 *
 * الميزات:
 * - تغيير المظهر (داكن/فاتح/تلقائي)
 * - اختيار مجموعة الألوان (5 خيارات)
 * - تغيير اللغة (عربي/إنجليزي)
 * - إعدادات المتجر والطابعة والنسخ الاحتياطي
 * - حول التطبيق وتسجيل الخروج
 *
 * @author POS System
 * @version 2.0
 * @since 2026-02-17
 */
public class ActivitySettingsActivity extends BaseActivity {

    private ActivitySettingsBinding binding;


    // Views
    private MaterialCardView cardTheme;
    private TextView         tvCurrentTheme;

    private MaterialCardView cardLanguage;
    private TextView         tvCurrentLanguage;

    private MaterialCardView cardStore;
    private MaterialCardView cardPrinter;
    private MaterialCardView cardBackup;
    private MaterialCardView cardPinLock;

    private MaterialCardView cardAccount;
    private TextView         tvUserName;
    private TextView         tvUserEmail;

    private MaterialCardView cardAbout;
    private MaterialCardView cardPremium;
    private MaterialCardView cardRateApp;
    private MaterialButton   btnLogout;
    private TextView         tvAppVersion;

    private BillingManager billingManager;
    private ReviewManager  reviewManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // BaseActivity يطبق الثيم واللغة تلقائياً
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets(binding.getRoot());

        initializeViews();
        setupToolbar();
        updateUI();
        setupListeners();
    }

    private void initializeViews() {
        cardTheme      = binding.cardTheme;
        tvCurrentTheme = binding.tvCurrentTheme;
        View tvCurrentColor = binding.tvCurrentColor;
        if (tvCurrentColor != null) tvCurrentColor.setVisibility(View.GONE);

        cardLanguage      = binding.cardLanguage;
        tvCurrentLanguage = binding.tvCurrentLanguage;

        cardStore   = binding.cardStore;
        cardPrinter = binding.cardPrinter;
        cardBackup  = binding.cardBackup;
        cardPinLock = binding.cardPinLock;

        cardAccount  = binding.cardAccount;
        tvUserName   = binding.tvUserName;
        tvUserEmail  = binding.tvUserEmail;

        cardAbout   = binding.cardAbout;
        cardPremium = binding.cardPremium;
        cardRateApp = binding.cardRateApp;
        btnLogout   = binding.btnLogout;
        tvAppVersion = binding.tvAppVersion;

        // إدارة المشتريات والتقييم
        DBHelper db = new DBHelper(this);
        billingManager = new BillingManager(this, db);
        reviewManager  = new ReviewManager(this);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = binding.toolbar;
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void updateUI() {
        updateThemeInfo();
        updateLanguageInfo();
        updateUserInfo();
    }

    private void updateThemeInfo() {
        if (tvCurrentTheme != null) tvCurrentTheme.setText(ThemeManager.getThemeModeName());
    }

    private void updateLanguageInfo() {
        if (tvCurrentLanguage != null)
            tvCurrentLanguage.setText(LanguageManager.getCurrentLanguageName());
    }

    private void updateUserInfo() {
        // Show app version
        if (tvAppVersion != null) {
            try {
                String version = getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName;
                tvAppVersion.setText(getString(R.string.settings_version) + " " + version);
            } catch (Exception ignored) {
                tvAppVersion.setText(getString(R.string.settings_version));
            }
        }
        AuthActivity.UserData userData = AuthActivity.getCurrentUser(this);
        if (userData != null) {
            if (tvUserName != null)
                tvUserName.setText(userData.name != null ? userData.name : getString(R.string.user));
            if (tvUserEmail != null) {
                tvUserEmail.setText(userData.email != null ? userData.email : "");
                tvUserEmail.setVisibility(userData.email != null ? View.VISIBLE : View.GONE);
            }
            if (cardAccount != null)
                cardAccount.setVisibility(userData.isGuest ? View.GONE : View.VISIBLE);
        }
    }

    private void setupListeners() {
        if (cardTheme    != null) cardTheme.setOnClickListener(v -> showThemeDialog());
        if (cardLanguage != null) cardLanguage.setOnClickListener(v -> showLanguageDialog());
        if (cardStore    != null) cardStore.setOnClickListener(v -> openActivity(ActivityStoreSettingsActivity.class));
        if (cardPrinter  != null) cardPrinter.setOnClickListener(v -> openActivity(ActivityPrinterSettingsActivity.class));
        if (cardBackup   != null) cardBackup.setOnClickListener(v -> openActivity(ActivityBackupActivity.class));
        if (cardPinLock  != null) cardPinLock.setOnClickListener(v -> showPinOptions());
        if (cardAbout    != null) cardAbout.setOnClickListener(v -> showAboutDialog());
        if (cardPremium  != null) cardPremium.setOnClickListener(v -> showPremiumDialog());
        if (cardRateApp  != null) cardRateApp.setOnClickListener(v -> rateApp());
        if (btnLogout    != null) btnLogout.setOnClickListener(v -> confirmLogout());
    }

    // ════════════════════════════════════════════════════════════
    // Dialogs
    // ════════════════════════════════════════════════════════════

    private void showThemeDialog() {
        showThemeModeDialog();
    }

    /** اختيار الوضع: فاتح / داكن / تلقائي */
    private void showThemeModeDialog() {
        String[] modes       = ThemeManager.getAvailableThemeModes();
        int      currentMode = ThemeManager.getThemeMode();

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.theme_mode)
                .setSingleChoiceItems(modes, currentMode, (d, which) -> {
                    ThemeManager.setThemeMode(which);
                    d.dismiss();
                    // إعادة إنشاء Activity لتطبيق التغيير على الفور
                    recreate();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /** اختيار اللغة */
    private void showLanguageDialog() {
        String[] languages   = LanguageManager.getAvailableLanguages();
        String   currentLang = LanguageManager.getCurrentLanguage();
        int      currentIdx  = currentLang.equals(LanguageManager.LANG_ARABIC) ? 0 : 1;

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.settings_language)
                .setSingleChoiceItems(languages, currentIdx, (d, which) -> {
                    String newLang = which == 0
                            ? LanguageManager.LANG_ARABIC
                            : LanguageManager.LANG_ENGLISH;
                    if (!newLang.equals(currentLang)) {
                        d.dismiss();
                        confirmLanguageChange(newLang);
                    } else {
                        d.dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /** تأكيد تغيير اللغة */
    private void confirmLanguageChange(String newLang) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.settings_language)
                .setMessage(R.string.language_change_message)
                .setPositiveButton(R.string.ok, (d, w) ->
                        LanguageManager.setLanguage(this, newLang))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /** عرض خيارات الاشتراك Premium */
    private void showPremiumDialog() {
        DBHelper db = new DBHelper(this);
        if (db.isPremiumUser()) {
            new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.premium_title)
                .setMessage(R.string.already_subscribed)
                .setPositiveButton(R.string.ok, null)
                .show();
            db.close();
            return;
        }
        db.close();

        String[] options = {
            getString(R.string.subscription_monthly),
            getString(R.string.subscription_yearly),
            getString(R.string.subscription_lifetime),
            getString(R.string.restore_purchases)
        };

        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.premium_title)
            .setMessage(R.string.premium_desc)
            .setItems(options, (d, which) -> {
                billingManager.setBillingListener(new BillingManager.BillingListener() {
                    @Override public void onPurchaseSuccess(com.android.billingclient.api.Purchase p) {
                        showToast(getString(R.string.subscription_success));
                    }
                    @Override public void onPurchaseFailure(String err) {
                        showToast(getString(R.string.subscription_failed) + ": " + err);
                    }
                    @Override public void onBillingReady() {}
                });
                switch (which) {
                    case 0: billingManager.purchasePremium(this, BillingManager.PREMIUM_MONTHLY);  break;
                    case 1: billingManager.purchasePremium(this, BillingManager.PREMIUM_YEARLY);   break;
                    case 2: billingManager.purchaseLifetime(this); break;
                    case 3: billingManager.restorePurchases(); showToast(getString(R.string.please_wait)); break;
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    /** طلب تقييم التطبيق */
    private void rateApp() {
        reviewManager.resetForTesting(); // نسمح للمستخدم بالتقييم يدوياً دائماً
        reviewManager.onAppLaunched();
        for (int i = 0; i < 10; i++) reviewManager.onInvoiceCreated(); // simulate criteria
        reviewManager.requestReview(this);
    }

    /** حول التطبيق */
    private void showAboutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.settings_about)
                .setMessage(getString(R.string.about_message))
                .setIcon(R.drawable.ic_info)
                .setPositiveButton(R.string.ok, null)
                .setNeutralButton(R.string.privacy_policy, (d, w) -> openPrivacyPolicy())
                .show();
    }

    /** سياسة الخصوصية */
    private void openPrivacyPolicy() {
        Intent intent = new Intent(Intent.ACTION_VIEW,
                android.net.Uri.parse("https://yourwebsite.com/privacy"));
        try {
            startActivity(intent);
        } catch (Exception e) {
            showToast(getString(R.string.cannot_open_browser));
        }
    }

    /** تأكيد تسجيل الخروج */
    private void confirmLogout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.logout)
                .setMessage(R.string.logout_confirm)
                .setPositiveButton(R.string.yes, (d, w) -> {
                    AuthActivity.logout(this);
                    finish();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /** خيارات قفل الكاشير بـ PIN */
    private void showPinOptions() {
        boolean pinEnabled = ActivityPinLockActivity.isPinEnabled(this);
        String[] options = pinEnabled
            ? new String[]{"تغيير رمز PIN", "إلغاء رمز PIN"}
            : new String[]{"تعيين رمز PIN جديد"};

        new MaterialAlertDialogBuilder(this)
            .setTitle("قفل الكاشير (PIN)")
            .setItems(options, (d, which) -> {
                if (!pinEnabled || which == 0) {
                    Intent i = new Intent(this, ActivityPinLockActivity.class);
                    i.putExtra(ActivityPinLockActivity.EXTRA_MODE, ActivityPinLockActivity.MODE_SET);
                    startActivity(i);
                } else {
                    confirmClearPin();
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void confirmClearPin() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("إلغاء رمز PIN")
            .setMessage("هل تريد إلغاء قفل الكاشير؟ سيتمكن أي شخص من الوصول بدون رمز.")
            .setPositiveButton("إلغاء الرمز", (d, w) -> {
                ActivityPinLockActivity.clearPin(this);
                showToast("تم إلغاء رمز PIN");
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (billingManager != null) billingManager.destroy();
    }
}
