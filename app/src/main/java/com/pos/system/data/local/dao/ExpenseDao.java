package com.pos.system.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.pos.system.data.local.entity.ExpenseEntity;

import java.util.List;

@Dao
public interface ExpenseDao {

    @Query("SELECT * FROM expenses ORDER BY created_at DESC")
    List<ExpenseEntity> getAll();

    @Query("SELECT * FROM expenses WHERE date BETWEEN :from AND :to ORDER BY date DESC")
    List<ExpenseEntity> getByDateRange(String from, String to);

    @Query("SELECT * FROM expenses WHERE category = :category ORDER BY date DESC")
    List<ExpenseEntity> getByCategory(String category);

    @Query("SELECT * FROM expenses WHERE id = :id")
    ExpenseEntity getById(long id);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(ExpenseEntity expense);

    @Update
    int update(ExpenseEntity expense);

    @Query("DELETE FROM expenses WHERE id = :id")
    int delete(long id);

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE date BETWEEN :from AND :to")
    double getTotalForPeriod(String from, String to);
}
