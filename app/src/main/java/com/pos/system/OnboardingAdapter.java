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
    private final String[] TITLES;
    private final String[] DESCRIPTIONS;

    public OnboardingAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        TITLES = new String[]{
            fragmentActivity.getString(R.string.onboarding_slide_products_title),
            fragmentActivity.getString(R.string.onboarding_slide_invoices_title),
            fragmentActivity.getString(R.string.onboarding_slide_reports_title)
        };
        DESCRIPTIONS = new String[]{
            fragmentActivity.getString(R.string.onboarding_slide_products_desc),
            fragmentActivity.getString(R.string.onboarding_slide_invoices_desc),
            fragmentActivity.getString(R.string.onboarding_slide_reports_desc)
        };
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
