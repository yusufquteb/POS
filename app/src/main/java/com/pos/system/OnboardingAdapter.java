package com.pos.system;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * OnboardingAdapter - Adapter لشرائح Onboarding
 * يُمرّر بيانات كل شريحة إلى Fragment
 */
public class OnboardingAdapter extends FragmentStateAdapter {

    public static final int NUM_PAGES = 3;

    // بيانات الشرائح
    private static final String[] TITLES = {
        "إدارة المنتجات",
        "فواتير احترافية",
        "تقارير وإحصائيات"
    };

    private static final String[] DESCRIPTIONS = {
        "أضف منتجاتك بسهولة مع صور وباركود وأسعار. تتبع المخزون تلقائياً واحصل على تنبيهات عند نفاد الكميات.",
        "أنشئ فواتير احترافية في ثوانٍ وأرسلها للعملاء. دعم كامل للطباعة الحرارية عبر البلوتوث.",
        "تقارير مبيعات يومية وشهرية وسنوية. رسوم بيانية تفاعلية لفهم أداء متجرك بلمحة."
    };

    public OnboardingAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        OnboardingPageFragment fragment = new OnboardingPageFragment();
        Bundle args = new Bundle();
        args.putString(OnboardingPageFragment.ARG_TITLE, TITLES[position]);
        args.putString(OnboardingPageFragment.ARG_DESCRIPTION, DESCRIPTIONS[position]);
        args.putInt(OnboardingPageFragment.ARG_PAGE, position);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return NUM_PAGES;
    }
}
