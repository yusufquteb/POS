package com.pos.system;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * OnboardingPageFragment - Fragment لكل شريحة Onboarding
 * يستقبل بيانات العنوان والوصف من Bundle
 */
public class OnboardingPageFragment extends Fragment {

    public static final String ARG_TITLE       = "title";
    public static final String ARG_DESCRIPTION = "description";
    public static final String ARG_PAGE        = "page";

    // أيقونات مناسبة لكل شريحة
    private static final int[] PAGE_ICONS = {
        R.drawable.ic_add,          // شريحة المنتجات
        R.drawable.ic_launcher,     // شريحة الفواتير
        R.drawable.stat_bg,         // شريحة التقارير
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                              @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_page, container, false);

        Bundle args = getArguments();
        if (args != null) {
            String title = args.getString(ARG_TITLE, "");
            String desc  = args.getString(ARG_DESCRIPTION, "");
            int    page  = args.getInt(ARG_PAGE, 0);

            TextView tvTitle = view.findViewById(R.id.tv_onboarding_title);
            TextView tvDesc  = view.findViewById(R.id.tv_onboarding_desc);
            ImageView ivIcon = view.findViewById(R.id.iv_onboarding_icon);

            if (tvTitle != null) tvTitle.setText(title);
            if (tvDesc  != null) tvDesc.setText(desc);
            if (ivIcon  != null && page >= 0 && page < PAGE_ICONS.length) {
                try { ivIcon.setImageResource(PAGE_ICONS[page]); }
                catch (Exception e) { ivIcon.setImageResource(R.drawable.ic_launcher); }
            }
        }

        return view;
    }
}
