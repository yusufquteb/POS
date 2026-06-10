package com.pos.system.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.pos.system.data.local.entity.WalletTransactionEntity;

import java.util.List;

@Dao
public interface WalletTransactionDao {

    @Query("SELECT * FROM wallet_transactions ORDER BY created_at DESC")
    List<WalletTransactionEntity> getAll();

    @Query("SELECT * FROM wallet_transactions WHERE date BETWEEN :from AND :to ORDER BY date DESC")
    List<WalletTransactionEntity> getByDateRange(String from, String to);

    @Query("SELECT * FROM wallet_transactions WHERE id = :id")
    WalletTransactionEntity getById(long id);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(WalletTransactionEntity tx);

    @Update
    int update(WalletTransactionEntity tx);

    @Query("DELETE FROM wallet_transactions WHERE id = :id")
    int delete(long id);

    @Query("SELECT COALESCE(SUM(amount),0) FROM wallet_transactions WHERE type='IN'")
    double getTotalIn();

    @Query("SELECT COALESCE(SUM(amount),0) FROM wallet_transactions WHERE type='OUT'")
    double getTotalOut();

    /** الرصيد الحالي = مجموع الإيداعات - مجموع السحوبات */
    @Query("SELECT COALESCE(SUM(CASE WHEN type='IN' THEN amount ELSE -amount END),0) FROM wallet_transactions")
    double getCurrentBalance();

    @Query("SELECT COALESCE(SUM(CASE WHEN type='IN' THEN amount ELSE -amount END),0) FROM wallet_transactions WHERE date BETWEEN :from AND :to")
    double getBalanceForPeriod(String from, String to);

    @Query("SELECT COALESCE(SUM(amount),0) FROM wallet_transactions WHERE type='IN' AND date BETWEEN :from AND :to")
    double getTotalInForPeriod(String from, String to);

    @Query("SELECT COALESCE(SUM(amount),0) FROM wallet_transactions WHERE type='OUT' AND date BETWEEN :from AND :to")
    double getTotalOutForPeriod(String from, String to);
}
