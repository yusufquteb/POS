package com.pos.system;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

/**
 * SplashActivity - شاشة البداية
 *
 * المنطق:
 *   المرة الأولى  → Onboarding  → MainActivity
 *   المرات التالية → MainActivity مباشرة
 *
 * @version 1.1 (Final)
 */
public class SplashActivity extends BaseActivity {

    private static final long SPLASH_DELAY = 2000L; // 2 ثانية

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(
            this::navigateNext, SPLASH_DELAY);
    }

    private void navigateNext() {
        Intent intent;
        if (OnboardingActivity.isFirstTime(this)) {
            intent = new Intent(this, OnboardingActivity.class);
        } else {
            intent = new Intent(this, MainActivity.class);
        }
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // منع الخروج أثناء Splash
    }
}
