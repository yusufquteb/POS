package com.pos.system.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.pos.system.data.local.entity.PurchaseOrderEntity;

import java.util.List;

@Dao
public interface PurchaseOrderDao {

    @Query("SELECT * FROM purchase_orders ORDER BY created_at DESC")
    List<PurchaseOrderEntity> getAll();

    @Query("SELECT * FROM purchase_orders WHERE status = :status ORDER BY created_at DESC")
    List<PurchaseOrderEntity> getByStatus(String status);

    @Query("SELECT * FROM purchase_orders WHERE id = :id")
    PurchaseOrderEntity getById(long id);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(PurchaseOrderEntity order);

    @Update
    int update(PurchaseOrderEntity order);

    @Query("UPDATE purchase_orders SET status = :status WHERE id = :id")
    int updateStatus(long id, String status);

    @Query("DELETE FROM purchase_orders WHERE id = :id")
    int delete(long id);
}
