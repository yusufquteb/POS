package com.pos.system.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.pos.system.data.local.entity.ShiftEntity;

import java.util.List;

@Dao
public interface ShiftDao {

    @Query("SELECT * FROM shifts ORDER BY opened_at DESC")
    List<ShiftEntity> getAll();

    @Query("SELECT * FROM shifts WHERE status = 'open' ORDER BY opened_at DESC LIMIT 1")
    ShiftEntity getOpenShift();

    @Query("SELECT * FROM shifts WHERE id = :id")
    ShiftEntity getById(long id);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(ShiftEntity shift);

    @Update
    int update(ShiftEntity shift);

    @Query("UPDATE shifts SET status = 'closed', closed_at = :closedAt, closing_cash = :closingCash WHERE id = :id")
    int closeShift(long id, String closedAt, double closingCash);
}
