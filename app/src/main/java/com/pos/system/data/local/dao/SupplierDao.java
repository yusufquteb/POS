package com.pos.system.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.pos.system.data.local.entity.SupplierEntity;

import java.util.List;

@Dao
public interface SupplierDao {

    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    List<SupplierEntity> getAll();

    @Query("SELECT * FROM suppliers WHERE name LIKE :q OR phone LIKE :q OR company LIKE :q ORDER BY name ASC")
    List<SupplierEntity> search(String q);

    @Query("SELECT * FROM suppliers WHERE id=:id")
    SupplierEntity getById(long id);

    @Query("SELECT COUNT(*) FROM suppliers WHERE phone=:phone")
    int phoneExists(String phone);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(SupplierEntity supplier);

    @Update
    int update(SupplierEntity supplier);

    @Query("DELETE FROM suppliers WHERE id=:id")
    int delete(long id);

    @Query("UPDATE suppliers SET debt = debt + :delta WHERE id=:id")
    int updateDebt(long id, double delta);

    @Query("UPDATE suppliers SET debt=:debt WHERE id=:id")
    int setDebt(long id, double debt);

    @Query("SELECT COUNT(*) FROM suppliers")
    int getTotalCount();

    @Query("SELECT COALESCE(SUM(debt),0) FROM suppliers WHERE debt > 0")
    double getTotalDebt();

    @Query("DELETE FROM suppliers")
    int deleteAll();
}
