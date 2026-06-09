package com.pos.system;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

/**
 * ItemSwipeHelper - مساعد لإضافة خلفيات ملونة عند سحب عناصر RecyclerView
 * 
 * الميزات:
 * - خلفية حمراء + أيقونة حذف عند السحب لليسار
 * - خلفية خضراء + أيقونة تعديل عند السحب لليمين
 * - تطبيق الألوان بما يتناسب مع تصميم التطبيق
 * 
 * تاريخ الإنشاء: 2026-02-11
 */
public abstract class ItemSwipeHelper extends ItemTouchHelper.SimpleCallback {

    /**
     * Interface للتعامل مع أحداث السحب
     */
    public interface SwipeCallback {
        void onSwiped(int position);
        default void onSwipedRight(int position) { onSwiped(position); }
    }

    private Context context;
    private Paint clearPaint;
    private ColorDrawable backgroundDelete;
    private ColorDrawable backgroundEdit;
    private Drawable deleteIcon;
    private Drawable editIcon;
    private int iconMargin;

    /**
     * Constructor
     * @param context السياق
     */
    public ItemSwipeHelper(Context context) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.context = context;
        
        // الألوان
        backgroundDelete = new ColorDrawable(ContextCompat.getColor(context, R.color.color_error));
        backgroundEdit   = new ColorDrawable(ContextCompat.getColor(context, R.color.purple_500));

        // الأيقونات
        deleteIcon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_delete);
        editIcon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_edit);

        if (deleteIcon != null) {
            deleteIcon.setColorFilter(ContextCompat.getColor(context, R.color.white), PorterDuff.Mode.SRC_ATOP);
        }
        if (editIcon != null) {
            editIcon.setColorFilter(ContextCompat.getColor(context, R.color.white), PorterDuff.Mode.SRC_ATOP);
        }
        
        iconMargin = (int) (context.getResources().getDisplayMetrics().density * 16);
        
        clearPaint = new Paint();
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
                         @NonNull RecyclerView.ViewHolder viewHolder,
                         @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onChildDraw(@NonNull Canvas c,
                           @NonNull RecyclerView recyclerView,
                           @NonNull RecyclerView.ViewHolder viewHolder,
                           float dX, float dY,
                           int actionState,
                           boolean isCurrentlyActive) {
        
        View itemView = viewHolder.itemView;
        int itemHeight = itemView.getHeight();
        
        boolean isCancelled = dX == 0 && !isCurrentlyActive;
        
        if (isCancelled) {
            clearCanvas(c, itemView.getRight() + dX, itemView.getTop(), 
                       itemView.getRight(), itemView.getBottom());
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, 
                            actionState, isCurrentlyActive);
            return;
        }

        if (dX < 0) { // السحب لليسار = حذف
            // رسم الخلفية الحمراء
            backgroundDelete.setBounds(
                itemView.getRight() + (int) dX,
                itemView.getTop(),
                itemView.getRight(),
                itemView.getBottom()
            );
            backgroundDelete.draw(c);

            // رسم أيقونة الحذف
            if (deleteIcon != null) {
                int iconSize = (int) (context.getResources().getDisplayMetrics().density * 24);
                int iconTop = itemView.getTop() + (itemHeight - iconSize) / 2;
                int iconBottom = iconTop + iconSize;
                int iconLeft = itemView.getRight() - iconMargin - iconSize;
                int iconRight = itemView.getRight() - iconMargin;

                deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                deleteIcon.draw(c);
            }
            
        } else if (dX > 0) { // السحب لليمين = تعديل
            // رسم الخلفية البنفسجية
            backgroundEdit.setBounds(
                itemView.getLeft(),
                itemView.getTop(),
                itemView.getLeft() + (int) dX,
                itemView.getBottom()
            );
            backgroundEdit.draw(c);

            // رسم أيقونة التعديل
            if (editIcon != null) {
                int iconSize = (int) (context.getResources().getDisplayMetrics().density * 24);
                int iconTop = itemView.getTop() + (itemHeight - iconSize) / 2;
                int iconBottom = iconTop + iconSize;
                int iconLeft = itemView.getLeft() + iconMargin;
                int iconRight = iconLeft + iconSize;

                editIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                editIcon.draw(c);
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, 
                        actionState, isCurrentlyActive);
    }

    private void clearCanvas(Canvas c, float left, float top, 
                            float right, float bottom) {
        c.drawRect(left, top, right, bottom, clearPaint);
    }

    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        return 0.7f;
    }
}
