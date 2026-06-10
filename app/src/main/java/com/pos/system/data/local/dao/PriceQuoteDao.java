package com.pos.system.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.pos.system.data.local.entity.PriceQuoteEntity;
import com.pos.system.data.local.entity.PriceQuoteItemEntity;

import java.util.List;

@Dao
public interface PriceQuoteDao {

    @Query("SELECT * FROM price_quotes ORDER BY created_at DESC")
    List<PriceQuoteEntity> getAll();

    @Query("SELECT * FROM price_quotes WHERE customer_id=:customerId ORDER BY created_at DESC")
    List<PriceQuoteEntity> getByCustomer(long customerId);

    @Query("SELECT * FROM price_quotes WHERE id=:id")
    PriceQuoteEntity getById(long id);

    @Query("SELECT * FROM price_quote_items WHERE quote_id=:quoteId")
    List<PriceQuoteItemEntity> getItems(long quoteId);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertQuote(PriceQuoteEntity quote);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insertItems(List<PriceQuoteItemEntity> items);

    @Update
    int updateQuote(PriceQuoteEntity quote);

    @Query("UPDATE price_quotes SET status=:status WHERE id=:id")
    int updateStatus(long id, String status);

    @Query("DELETE FROM price_quotes WHERE id=:id")
    int deleteQuote(long id);

    @Query("DELETE FROM price_quote_items WHERE quote_id=:quoteId")
    int deleteItems(long quoteId);
}
