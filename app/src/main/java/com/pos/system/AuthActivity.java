package com.pos.system;

import com.pos.system.BaseActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

// Firebase imports

/**
 * AuthActivity - شاشة تسجيل الدخول المتقدمة
 * 
 * الميزات:
 * - تسجيل دخول Gmail
 * - تسجيل دخول Email/Password
 * - تصميم Material Design 3
 * - Animations سلسة
 * - معالجة أخطاء شاملة
 * 
 * @author POS System
 * @version 2.0
 * @since 2026-02-17
 */
public class AuthActivity extends BaseActivity {

    private static final String TAG = "AuthActivity";
    private static final int RC_SIGN_IN = 9001;
    private static final String PREFS_NAME = "auth_prefs";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_PHOTO = "user_photo";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    // Views
    private MaterialCardView cardGoogleSignIn;
    private MaterialCardView cardEmailSignIn;
    private MaterialButton btnGoogleSignIn;
    private MaterialButton btnEmailSignIn;
    private MaterialButton btnEmailLogin;
    private MaterialButton btnSkip;
    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private TextView tvSwitchMode;
    private View progressOverlay;
    private ImageView ivLogo;

    private boolean isSignUpMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // تطبيق الثيم
        
        setContentView(R.layout.activity_auth);
        applyWindowInsets(findViewById(android.R.id.content));

        // التحقق من تسجيل الدخول السابق
        if (isUserLoggedIn()) {
            navigateToMain();
            return;
        }

        // تهيئة Firebase
        initializeFirebase();

        // تهيئة Views
        initializeViews();

        // إعداد Listeners
        setupListeners();

        // تشغيل Animations
        startAnimations();
    }

    /**
     * تهيئة بسيطة (بدون Firebase)
     */
    private void initializeFirebase() {
        // تم إلغاء Firebase - تسجيل دخول محلي فقط
        Log.d(TAG, "Auth initialized - Local only");
    }

    /**
     * تهيئة Views
     */
    private void initializeViews() {
        cardGoogleSignIn = findViewById(R.id.card_google_signin);
        cardEmailSignIn = findViewById(R.id.card_email_signin);
        btnGoogleSignIn = findViewById(R.id.btn_google_signin);
        btnEmailSignIn = findViewById(R.id.btn_email_signin);
        btnEmailLogin = findViewById(R.id.btn_email_login);
        btnSkip = findViewById(R.id.btn_skip);
        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        tvSwitchMode = findViewById(R.id.tv_switch_mode);
        progressOverlay = findViewById(R.id.progress_overlay);
        ivLogo = findViewById(R.id.iv_logo);

        // إخفاء البطاقة الثانية في البداية
        if (cardEmailSignIn != null) {
            cardEmailSignIn.setVisibility(View.GONE);
        }
    }

    /**
     * إعداد Listeners
     */
    private void setupListeners() {
        // Google Sign-In
        if (btnGoogleSignIn != null) {
            btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());
        }

        // Email Sign-In Button (Show Form)
        if (btnEmailSignIn != null) {
            btnEmailSignIn.setOnClickListener(v -> showEmailSignInForm());
        }

        // Email Login/Register
        if (btnEmailLogin != null) {
            btnEmailLogin.setOnClickListener(v -> handleEmailAuth());
        }

        // Switch Mode (Login ↔ Register)
        if (tvSwitchMode != null) {
            tvSwitchMode.setOnClickListener(v -> toggleSignUpMode());
        }

        // Skip
        if (btnSkip != null) {
            btnSkip.setOnClickListener(v -> skipAuth());
        }
    }

    /**
     * تسجيل الدخول (تخطي Firebase)
     */
    private void signInWithGoogle() {
        // تم إلغاء Google Sign-in
        showToast("تسجيل الدخول عبر Google غير متاح حالياً");
        skipAuth();
    }

    /**
     * عرض نموذج تسجيل الدخول بالإيميل
     */
    private void showEmailSignInForm() {
        if (cardGoogleSignIn != null && cardEmailSignIn != null) {
            // إخفاء بطاقة Google
            cardGoogleSignIn.animate()
                    .alpha(0f)
                    .scaleX(0.8f)
                    .scaleY(0.8f)
                    .setDuration(300)
                    .withEndAction(() -> {
                        cardGoogleSignIn.setVisibility(View.GONE);
                        
                        // إظهار بطاقة Email
                        cardEmailSignIn.setVisibility(View.VISIBLE);
                        cardEmailSignIn.setAlpha(0f);
                        cardEmailSignIn.setScaleX(0.8f);
                        cardEmailSignIn.setScaleY(0.8f);
                        cardEmailSignIn.animate()
                                .alpha(1f)
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(300)
                                .start();
                    })
                    .start();
        }
    }

    /**
     * معالجة تسجيل الدخول/التسجيل بالإيميل
     */
    private void handleEmailAuth() {
        String email = etEmail != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword != null ? etPassword.getText().toString().trim() : "";

        // التحقق من البيانات
        if (!validateEmailInput(email, password)) {
            return;
        }

        showProgress(true);

        if (isSignUpMode) {
            // إنشاء حساب جديد
            createAccountWithEmail(email, password);
        } else {
            // تسجيل الدخول
            signInWithEmail(email, password);
        }
    }

    /**
     * التحقق من صحة البيانات
     */
    private boolean validateEmailInput(String email, String password) {
        boolean valid = true;

        // التحقق من الإيميل
        if (email.isEmpty()) {
            if (tilEmail != null) {
                tilEmail.setError("الرجاء إدخال البريد الإلكتروني");
            }
            valid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            if (tilEmail != null) {
                tilEmail.setError("البريد الإلكتروني غير صحيح");
            }
            valid = false;
        } else {
            if (tilEmail != null) {
                tilEmail.setError(null);
            }
        }

        // التحقق من كلمة المرور
        if (password.isEmpty()) {
            if (tilPassword != null) {
                tilPassword.setError("الرجاء إدخال كلمة المرور");
            }
            valid = false;
        } else if (password.length() < 6) {
            if (tilPassword != null) {
                tilPassword.setError("كلمة المرور يجب أن تكون 6 أحرف على الأقل");
            }
            valid = false;
        } else {
            if (tilPassword != null) {
                tilPassword.setError(null);
            }
        }

        return valid;
    }

    /**
     * تسجيل الدخول بالإيميل (محلي)
     */
    private void signInWithEmail(String email, String password) {
        showProgress(false);
        // تسجيل دخول بسيط - قبول أي بيانات
        saveUserData(email, "مستخدم", null);
        navigateToMain();
    }

    /**
     * إنشاء حساب بالإيميل (محلي)
     */
    private void createAccountWithEmail(String email, String password) {
        showProgress(false);
        // قبول أي بيانات
        saveUserData(email, "مستخدم جديد", null);
        navigateToMain();
    }

    /**
     * التبديل بين وضع التسجيل ووضع الدخول
     */
    private void toggleSignUpMode() {
        isSignUpMode = !isSignUpMode;

        if (btnEmailLogin != null) {
            btnEmailLogin.setText(isSignUpMode ? "إنشاء حساب" : "تسجيل الدخول");
        }

        if (tvSwitchMode != null) {
            tvSwitchMode.setText(isSignUpMode ? 
                "لديك حساب؟ تسجيل الدخول" : 
                "ليس لديك حساب؟ إنشاء حساب");
        }
    }

    /**
     * تخطي تسجيل الدخول
     */
    private void skipAuth() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("تخطي تسجيل الدخول")
                .setMessage("هل تريد المتابعة بدون تسجيل الدخول؟\n\n" +
                        "ملاحظة: لن تتمكن من مزامنة بياناتك.")
                .setPositiveButton("نعم، متابعة", (d, w) -> {
                    saveGuestMode();
                    navigateToMain();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    /**
     * معالجة نتيجة (تم إلغاء Google Sign-In)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // تم إلغاء Google Sign-in
    }

    /**
     * معالجة نجاح المصادقة (محلي)
     */
    private void onAuthSuccess(String email, String name) {
        saveUserData(email, name, null);
        showSuccess("مرحباً " + (name != null ? name : "بك") + "!");
        navigateToMain();
    }

    /**
     * حفظ بيانات المستخدم
     */
    private void saveUserData(String email, String name, String photoUrl) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_USER_EMAIL, email)
                .putString(KEY_USER_NAME, name)
                .putString(KEY_USER_PHOTO, photoUrl)
                .putLong("login_time", System.currentTimeMillis())
                .apply();

        Log.d(TAG, "User data saved: " + email);
    }

    /**
     * حفظ وضع الضيف
     */
    private void saveGuestMode() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putBoolean("is_guest", true)
                .putLong("login_time", System.currentTimeMillis())
                .apply();
    }

    /**
     * التحقق من تسجيل الدخول السابق
     */
    private boolean isUserLoggedIn() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * الانتقال إلى الشاشة الرئيسية
     */
    private void navigateToMain() {
        // التحقق من Onboarding
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean onboardingCompleted = prefs.getBoolean("onboarding_completed", false);

        Intent intent;
        if (onboardingCompleted) {
            intent = new Intent(this, MainActivity.class);
        } else {
            intent = new Intent(this, OnboardingActivity.class);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();

        // Animation
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    /**
     * إظهار/إخفاء شاشة التحميل
     */
    private void showProgress(boolean show) {
        if (progressOverlay != null) {
            progressOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * عرض رسالة خطأ
     */
    private void showError(String message) {
        showToast("⚠️ " + message);
    }

    /**
     * عرض رسالة نجاح
     */
    private void showSuccess(String message) {
        showToast("✓ " + message);
    }

    /**
     * تشغيل Animations
     */
    private void startAnimations() {
        // Logo animation
        if (ivLogo != null) {
            ivLogo.setAlpha(0f);
            ivLogo.setScaleX(0.5f);
            ivLogo.setScaleY(0.5f);
            ivLogo.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(600)
                    .start();
        }

        // Cards animation
        if (cardGoogleSignIn != null) {
            cardGoogleSignIn.setAlpha(0f);
            cardGoogleSignIn.setTranslationY(100f);
            cardGoogleSignIn.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(500)
                    .setStartDelay(200)
                    .start();
        }
    }

    /**
     * دالة مساعدة: الحصول على بيانات المستخدم الحالي
     */
    public static UserData getCurrentUser(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        if (!prefs.getBoolean(KEY_IS_LOGGED_IN, false)) {
            return null;
        }

        return new UserData(
                prefs.getString(KEY_USER_EMAIL, null),
                prefs.getString(KEY_USER_NAME, null),
                prefs.getString(KEY_USER_PHOTO, null),
                prefs.getBoolean("is_guest", false)
        );
    }

    /**
     * دالة مساعدة: تسجيل الخروج
     */
    public static void logout(android.content.Context context) {
        // Logout from Firebase
        // local logout only

        // Clear SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().clear().apply();

        // Navigate to AuthActivity
        Intent intent = new Intent(context, AuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | 
                       Intent.FLAG_ACTIVITY_NEW_TASK | 
                       Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    /**
     * كلاس بيانات المستخدم
     */
    public static class UserData {
        public final String email;
        public final String name;
        public final String photoUrl;
        public final boolean isGuest;

        public UserData(String email, String name, String photoUrl, boolean isGuest) {
            this.email = email;
            this.name = name;
            this.photoUrl = photoUrl;
            this.isGuest = isGuest;
        }
    }

    @Override
    public void onBackPressed() {
        // إذا كانت بطاقة Email مفتوحة، ارجع لبطاقة Google
        if (cardEmailSignIn != null && cardEmailSignIn.getVisibility() == View.VISIBLE) {
            cardEmailSignIn.animate()
                    .alpha(0f)
                    .scaleX(0.8f)
                    .scaleY(0.8f)
                    .setDuration(300)
                    .withEndAction(() -> {
                        cardEmailSignIn.setVisibility(View.GONE);
                        
                        if (cardGoogleSignIn != null) {
                            cardGoogleSignIn.setVisibility(View.VISIBLE);
                            cardGoogleSignIn.setAlpha(0f);
                            cardGoogleSignIn.setScaleX(0.8f);
                            cardGoogleSignIn.setScaleY(0.8f);
                            cardGoogleSignIn.animate()
                                    .alpha(1f)
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(300)
                                    .start();
                        }
                    })
                    .start();
        } else {
            // تأكيد الخروج
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle("تأكيد الخروج")
                    .setMessage("هل تريد الخروج من التطبيق؟")
                    .setPositiveButton("نعم", (d, w) -> finish())
                    .setNegativeButton("لا", null)
                    .show();
        }
    }
}
