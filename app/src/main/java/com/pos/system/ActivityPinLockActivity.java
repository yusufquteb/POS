package com.pos.system;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

/**
 * PIN Cashier Lock — 4-digit PIN screen.
 *
 * Modes:
 *   MODE_VERIFY — verify existing PIN before accessing a protected screen.
 *   MODE_SET    — set a new PIN (used from settings).
 *
 * Usage:
 *   Intent i = new Intent(ctx, ActivityPinLockActivity.class);
 *   i.putExtra(EXTRA_MODE, MODE_VERIFY);
 *   i.putExtra(EXTRA_TARGET_CLASS, TargetActivity.class.getName()); // optional
 *   startActivity(i);
 */
public class ActivityPinLockActivity extends BaseActivity {

    public static final String EXTRA_MODE         = "pin_mode";
    public static final String EXTRA_TARGET_CLASS = "target_class";
    public static final int    MODE_VERIFY        = 0;
    public static final int    MODE_SET           = 1;

    private static final String PREFS_NAME = "pos_pin_prefs";
    private static final String KEY_PIN    = "cashier_pin";
    private static final int    PIN_LENGTH = 4;

    private StringBuilder enteredPin = new StringBuilder();
    private int mode;
    private String firstPin; // used during SET to confirm

    private TextView tvTitle;
    private TextView tvSubtitle;
    private TextView tvError;
    private View[] dots;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_lock);

        mode = getIntent().getIntExtra(EXTRA_MODE, MODE_VERIFY);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvTitle    = findViewById(R.id.tv_pin_title);
        tvSubtitle = findViewById(R.id.tv_pin_subtitle);
        tvError    = findViewById(R.id.tv_error);

        dots = new View[]{
            findViewById(R.id.dot1),
            findViewById(R.id.dot2),
            findViewById(R.id.dot3),
            findViewById(R.id.dot4)
        };

        setupMode();
        setupNumpad();
    }

    private void setupMode() {
        if (mode == MODE_SET) {
            tvTitle.setText("تعيين رمز PIN جديد");
            tvSubtitle.setText("أدخل رمزاً مكوناً من 4 أرقام");
        } else {
            String savedPin = getSavedPin();
            if (savedPin == null || savedPin.isEmpty()) {
                // No PIN set yet — skip verification
                launchTarget();
                return;
            }
            tvTitle.setText("أدخل رمز PIN");
            tvSubtitle.setText("أدخل الرمز المكون من 4 أرقام للمتابعة");
        }
    }

    private void setupNumpad() {
        int[] btnIds = {
            R.id.btn_1, R.id.btn_2, R.id.btn_3,
            R.id.btn_4, R.id.btn_5, R.id.btn_6,
            R.id.btn_7, R.id.btn_8, R.id.btn_9,
            R.id.btn_0
        };
        char[] digits = {'1','2','3','4','5','6','7','8','9','0'};

        for (int i = 0; i < btnIds.length; i++) {
            final char d = digits[i];
            MaterialButton btn = findViewById(btnIds[i]);
            if (btn != null) btn.setOnClickListener(v -> onDigit(d));
        }

        MaterialButton btnBack  = findViewById(R.id.btn_back);
        MaterialButton btnClear = findViewById(R.id.btn_clear);
        if (btnBack  != null) btnBack.setOnClickListener(v -> onBackspace());
        if (btnClear != null) btnClear.setOnClickListener(v -> onClear());
    }

    private void onDigit(char d) {
        if (enteredPin.length() >= PIN_LENGTH) return;
        enteredPin.append(d);
        updateDots();
        if (enteredPin.length() == PIN_LENGTH) onPinComplete();
    }

    private void onBackspace() {
        if (enteredPin.length() > 0) {
            enteredPin.deleteCharAt(enteredPin.length() - 1);
            updateDots();
            hideError();
        }
    }

    private void onClear() {
        enteredPin.setLength(0);
        updateDots();
        hideError();
    }

    private void updateDots() {
        for (int i = 0; i < dots.length; i++) {
            dots[i].setBackgroundResource(
                i < enteredPin.length() ? R.drawable.pin_dot_filled : R.drawable.pin_dot_empty
            );
        }
    }

    private void onPinComplete() {
        String pin = enteredPin.toString();

        if (mode == MODE_SET) {
            if (firstPin == null) {
                // First entry — ask to confirm
                firstPin = pin;
                enteredPin.setLength(0);
                updateDots();
                tvSubtitle.setText("أعد إدخال الرمز للتأكيد");
                hideError();
            } else {
                // Second entry — confirm
                if (pin.equals(firstPin)) {
                    savePin(pin);
                    showToast("تم حفظ رمز PIN بنجاح ✓");
                    finish();
                } else {
                    firstPin = null;
                    enteredPin.setLength(0);
                    updateDots();
                    tvSubtitle.setText("أدخل رمزاً مكوناً من 4 أرقام");
                    showError("الرمزان غير متطابقين — حاول مرة أخرى");
                }
            }
        } else {
            // VERIFY mode
            String saved = getSavedPin();
            if (pin.equals(saved)) {
                launchTarget();
            } else {
                enteredPin.setLength(0);
                updateDots();
                showError("رمز PIN غير صحيح");
            }
        }
    }

    private void launchTarget() {
        String targetClass = getIntent().getStringExtra(EXTRA_TARGET_CLASS);
        if (targetClass != null) {
            try {
                Class<?> cls = Class.forName(targetClass);
                startActivity(new Intent(this, cls));
            } catch (ClassNotFoundException ignored) {}
        }
        finish();
    }

    private void showError(String msg) {
        if (tvError != null) {
            tvError.setText(msg);
            tvError.setVisibility(View.VISIBLE);
        }
    }

    private void hideError() {
        if (tvError != null) tvError.setVisibility(View.GONE);
    }

    private String getSavedPin() {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_PIN, null);
    }

    private void savePin(String pin) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putString(KEY_PIN, pin).apply();
    }

    /** Convenience: check if a PIN has been set on this device. */
    public static boolean isPinEnabled(android.content.Context ctx) {
        String p = ctx.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE).getString(KEY_PIN, null);
        return p != null && !p.isEmpty();
    }

    /** Clear the stored PIN (e.g., from Settings). */
    public static void clearPin(android.content.Context ctx) {
        ctx.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE).edit().remove(KEY_PIN).apply();
    }

    private void showToast(String msg) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show();
    }
}
