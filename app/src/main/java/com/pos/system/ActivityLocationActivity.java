package com.pos.system;

import com.pos.system.BaseActivity;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.HashMap;

public class ActivityLocationActivity extends BaseActivity {

    private static final String TAG = "LocationActivity";
    private DBHelper dbHelper;
    private RecyclerView recyclerView;
    private LocationAdapter adapter;
    private ArrayList<HashMap<String, Object>> fullList = new ArrayList<>();
    private View emptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);
        applyWindowInsets(findViewById(R.id._main));

        dbHelper = new DBHelper(this);
        recyclerView = findViewById(R.id.recycler_view);
        emptyState = findViewById(R.id.empty_state);
        SearchView searchView = findViewById(R.id.search_view);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        refreshData();

        Snackbar.make(recyclerView, "اسحب لليسار للحذف، ولليمين للتعديل", Snackbar.LENGTH_LONG).show();

        findViewById(R.id.fab_add).setOnClickListener(v -> showDataSheet(null));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }
            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);
                return true;
            }
        });

        setupSwipeActions();
    }

    private void showDataSheet(HashMap<String, Object> editData) {
        BottomSheetDialog dialog = new BottomSheetDialog(this, com.google.android.material.R.style.Theme_Design_Light_BottomSheetDialog);
        View view = LayoutInflater.from(this).inflate(R.layout.layout_add_data_bottomdialog_fragment, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        TextInputEditText etName = view.findViewById(R.id.et_name);
        TextInputEditText etPhone = view.findViewById(R.id.et_phone);
        TextInputEditText etAddress = view.findViewById(R.id.et_address);

        // إخفاء حقول غير مطلوبة للموقع
        if (etPhone != null) etPhone.setVisibility(View.GONE);
        if (etAddress != null) etAddress.setVisibility(View.GONE);

        if (editData != null) {
            String name = editData.get("name") != null ? editData.get("name").toString() : "";
            etName.setText(name);
        }

        dialog.setOnShowListener(dialogInterface -> {
            etName.postDelayed(() -> {
                etName.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(etName, InputMethodManager.SHOW_IMPLICIT);
                }
            }, 200); 
        });

        view.findViewById(R.id.btn_save).setOnClickListener(v -> {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            
            if (name.isEmpty()) {
                showToast("يرجى إدخال اسم الموقع");
                return;
            }

            SQLiteDatabase db = null;
            try {
                db = dbHelper.getWritableDatabase();
                ContentValues cv = new ContentValues();
                cv.put("name", name);

                if (editData == null) {
                    long result = db.insert("locations", null, cv);
                    if (result != -1) {
                        showToast("تمت إضافة الموقع");
                        refreshData();
                        dialog.dismiss();
                    } else {
                        showSnackbar("فشل في إضافة الموقع", true);
                    }
                } else {
                    int result = db.update("locations", cv, "id=?", new String[]{editData.get("id").toString()});
                    if (result > 0) {
                        showToast("تم تحديث البيانات");
                        refreshData();
                        dialog.dismiss();
                    } else {
                        showSnackbar("فشل في تحديث البيانات", true);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating location", e);
                showToast("حدث خطأ: " + e.getMessage());
            }
        });
        
        dialog.show();
    }

    private void setupSwipeActions() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder t) { 
                return false; 
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;
                
                if (direction == ItemTouchHelper.LEFT) {
                    showDeleteConfirmDialog(position);
                } else {
                    showDataSheet(fullList.get(position));
                    refreshData();
                }
            }
        }).attachToRecyclerView(recyclerView);
    }

    private void showDeleteConfirmDialog(int position) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("حذف الموقع")
                .setMessage("هل أنت متأكد من حذف هذا الموقع؟")
                .setNegativeButton("إلغاء", (d, w) -> refreshData())
                .setPositiveButton("حذف", (d, w) -> {
                    try {
                        int id = (int) fullList.get(position).get("id");
                        int result = dbHelper.getWritableDatabase().delete("locations", "id=?", new String[]{String.valueOf(id)});
                        if (result > 0) {
                            showToast("تم حذف الموقع");
                            refreshData();
                        } else {
                            showSnackbar("فشل في حذف الموقع", true);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error deleting location", e);
                        showSnackbar("حدث خطأ أثناء الحذف", true);
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void refreshData() {
        fullList.clear();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.rawQuery("SELECT * FROM locations ORDER BY id DESC", null);
            
            int idIndex = cursor.getColumnIndexOrThrow("id");
            int nameIndex = cursor.getColumnIndexOrThrow("name");
            
            while (cursor.moveToNext()) {
                HashMap<String, Object> map = new HashMap<>();
                map.put("id", cursor.getInt(idIndex));
                map.put("name", cursor.getString(nameIndex));
                fullList.add(map);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading locations", e);
            showSnackbar("خطأ في تحميل البيانات", true);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        emptyState.setVisibility(fullList.isEmpty() ? View.VISIBLE : View.GONE);
        
        if (adapter == null) {
            adapter = new LocationAdapter(fullList);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.updateList(fullList);
        }
    }

    private void filter(String text) {
        ArrayList<HashMap<String, Object>> filteredList = new ArrayList<>();
        for (HashMap<String, Object> item : fullList) {
            String name = item.get("name") != null ? item.get("name").toString().toLowerCase() : "";
            
            if (name.contains(text.toLowerCase())) {
                filteredList.add(item);
            }
        }
        adapter.updateList(filteredList);
        emptyState.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.ViewHolder> {
        private ArrayList<HashMap<String, Object>> list;
        
        LocationAdapter(ArrayList<HashMap<String, Object>> list) { 
            this.list = new ArrayList<>(list); 
        }
        
        void updateList(ArrayList<HashMap<String, Object>> newList) { 
            this.list = new ArrayList<>(newList); 
            notifyDataSetChanged(); 
        }

        @NonNull 
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_data, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int i) {
            HashMap<String, Object> item = list.get(i);
            
            String name = item.get("name") != null ? item.get("name").toString() : "غير محدد";
            
            holder.tvName.setText(name);
            holder.tvDetails.setText("موقع رقم " + (i + 1));

            holder.btnCall.setVisibility(View.GONE);
            holder.btnWhatsapp.setVisibility(View.GONE);
        }

        @Override 
        public int getItemCount() { 
            return list.size(); 
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvDetails; 
            View btnCall, btnWhatsapp;
            
            ViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.tv_name);
                tvDetails = v.findViewById(R.id.tv_details);
                btnCall = v.findViewById(R.id.btn_call);
                btnWhatsapp = v.findViewById(R.id.btn_whatsapp);
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}
