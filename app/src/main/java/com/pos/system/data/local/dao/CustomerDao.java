package com.pos.system.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.pos.system.data.local.entity.CustomerEntity;

import java.util.List;

@Dao
public interface CustomerDao {

    @Query("SELECT * FROM customers ORDER BY name ASC")
    List<CustomerEntity> getAll();

    @Query("SELECT * FROM customers WHERE name LIKE :q OR phone LIKE :q ORDER BY name ASC")
    List<CustomerEntity> search(String q);

    @Query("SELECT * FROM customers WHERE id=:id")
    CustomerEntity getById(long id);

    @Query("SELECT COUNT(*) FROM customers WHERE phone=:phone")
    int phoneExists(String phone);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(CustomerEntity customer);

    @Update
    int update(CustomerEntity customer);

    @Query("DELETE FROM customers WHERE id=:id")
    int delete(long id);

    @Query("UPDATE customers SET debt = debt + :delta WHERE id=:id")
    int updateDebt(long id, double delta);

    @Query("UPDATE customers SET debt=:debt WHERE id=:id")
    int setDebt(long id, double debt);

    @Query("UPDATE customers SET last_purchase_at=:date, total_spent = total_spent + :amount WHERE id=:id")
    int recordPurchase(long id, String date, double amount);

    @Query("SELECT COUNT(*) FROM customers")
    int getTotalCount();

    @Query("SELECT COALESCE(SUM(debt),0) FROM customers WHERE debt > 0")
    double getTotalDebt();

    @Query("DELETE FROM customers")
    int deleteAll();
}
