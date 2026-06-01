package com.pos.system;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * CrashActivity - تعرض الخطأ بوضوح على الشاشة
 * لا تحتاج layout XML — تبني الـ UI بالكود مباشرة
 */
public class CrashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String error = getIntent().getStringExtra("error");
        if (error == null) error = "خطأ غير معروف";

        // ═══ بناء الـ UI بالكود (لا يحتاج XML) ═══
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#1a1a2e"));
        root.setPadding(32, 60, 32, 32);

        // العنوان
        TextView title = new TextView(this);
        title.setText("⚠️ خطأ في التطبيق");
        title.setTextColor(Color.parseColor("#FF6B6B"));
        title.setTextSize(20);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setPadding(0, 0, 0, 8);
        root.addView(title);

        // التعليمات
        TextView instructions = new TextView(this);
        instructions.setText("انسخ النص أدناه وأرسله للمطور:");
        instructions.setTextColor(Color.parseColor("#AAAAAA"));
        instructions.setTextSize(14);
        instructions.setPadding(0, 0, 0, 16);
        root.addView(instructions);

        // منطقة الخطأ القابلة للتمرير
        ScrollView scrollView = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        scrollView.setLayoutParams(scrollParams);

        TextView errorText = new TextView(this);
        errorText.setText(error);
        errorText.setTextColor(Color.parseColor("#00FF88"));
        errorText.setTextSize(11);
        errorText.setBackgroundColor(Color.parseColor("#0d0d0d"));
        errorText.setPadding(16, 16, 16, 16);
        errorText.setTextIsSelectable(true);
        scrollView.addView(errorText);
        root.addView(scrollView);

        // ─── أزرار ───
        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setPadding(0, 16, 0, 0);

        // زر النسخ
        final String finalError = error;
        Button btnCopy = new Button(this);
        btnCopy.setText("📋 نسخ الخطأ");
        btnCopy.setBackgroundColor(Color.parseColor("#4CAF50"));
        btnCopy.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        btnParams.setMargins(0, 0, 8, 0);
        btnCopy.setLayoutParams(btnParams);
        btnCopy.setOnClickListener(v -> {
            ClipboardManager clipboard =
                    (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(
                    ClipData.newPlainText("crash_log", finalError));
            Toast.makeText(this, "✅ تم النسخ!", Toast.LENGTH_SHORT).show();
        });
        buttons.addView(btnCopy);

        // زر الإغلاق
        Button btnClose = new Button(this);
        btnClose.setText("إغلاق");
        btnClose.setBackgroundColor(Color.parseColor("#F44336"));
        btnClose.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        btnClose.setLayoutParams(closeParams);
        btnClose.setOnClickListener(v -> {
            android.os.Process.killProcess(android.os.Process.myPid());
        });
        buttons.addView(btnClose);

        root.addView(buttons);
        setContentView(root);
    }
}
