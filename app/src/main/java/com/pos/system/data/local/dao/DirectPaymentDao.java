package com.pos.system.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.pos.system.data.local.entity.DirectPaymentEntity;

import java.util.List;

@Dao
public interface DirectPaymentDao {

    @Query("SELECT * FROM direct_payments WHERE entity_type=:type AND entity_id=:entityId ORDER BY date DESC")
    List<DirectPaymentEntity> getForEntity(String type, long entityId);

    @Query("SELECT * FROM direct_payments ORDER BY created_at DESC")
    List<DirectPaymentEntity> getAll();

    @Query("SELECT * FROM direct_payments WHERE id = :id")
    DirectPaymentEntity getById(long id);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(DirectPaymentEntity payment);

    @Update
    int update(DirectPaymentEntity payment);

    @Query("DELETE FROM direct_payments WHERE id = :id")
    int delete(long id);

    /**
     * صافي المبالغ المباشرة لعميل/مورد معين
     * IN = مبلغ له (يُقلل الدين) — OUT = مبلغ عليه (يزيد الدين)
     */
    @Query("SELECT COALESCE(SUM(CASE WHEN type='OUT' THEN amount ELSE -amount END),0) FROM direct_payments WHERE entity_type=:entityType AND entity_id=:entityId")
    double getNetDebtForEntity(String entityType, long entityId);

    @Query("SELECT COALESCE(SUM(CASE WHEN type='IN' THEN amount ELSE 0 END),0) FROM direct_payments WHERE entity_type=:entityType AND entity_id=:entityId")
    double getTotalPaidForEntity(String entityType, long entityId);

    @Query("DELETE FROM direct_payments WHERE entity_type=:entityType")
    int deleteAllForEntityType(String entityType);
}
