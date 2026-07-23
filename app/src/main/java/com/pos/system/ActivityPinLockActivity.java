package com.pos.system;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.pos.system.databinding.ActivityPinLockBinding;

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

    private ActivityPinLockBinding binding;


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
        binding = ActivityPinLockBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyWindowInsets(binding.getRoot());

        mode = getIntent().getIntExtra(EXTRA_MODE, MODE_VERIFY);

        MaterialToolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvTitle    = binding.tvPinTitle;
        tvSubtitle = binding.tvPinSubtitle;
        tvError    = binding.tvError;

        dots = new View[]{
            binding.dot1,
            binding.dot2,
            binding.dot3,
            binding.dot4
        };

        setupMode();
        setupNumpad();
    }

    private void setupMode() {
        if (mode == MODE_SET) {
            tvTitle.setText(R.string.pin_set_title);
            tvSubtitle.setText(R.string.str_70a793);
        } else {
            String savedPin = getSavedPin();
            if (savedPin == null || savedPin.isEmpty()) {
                // No PIN set yet — skip verification
                launchTarget();
                return;
            }
            tvTitle.setText(R.string.str_5f6a45);
            tvSubtitle.setText(R.string.str_70a793);
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

        MaterialButton btnBack  = binding.btnBack;
        MaterialButton btnClear = binding.btnClear;
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
                tvSubtitle.setText(getString(R.string.pin_reenter_to_confirm));
                hideError();
            } else {
                // Second entry — confirm
                if (pin.equals(firstPin)) {
                    savePin(pin);
                    showToast(getString(R.string.pin_saved_success));
                    finish();
                } else {
                    firstPin = null;
                    enteredPin.setLength(0);
                    updateDots();
                    tvSubtitle.setText(getString(R.string.pin_enter_4_digit));
                    showError(getString(R.string.pin_mismatch_retry));
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
                showError(getString(R.string.str_010e53));
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

}
