package com.pos.system.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.pos.system.data.local.entity.LocationEntity;

import java.util.List;

@Dao
public interface LocationDao {

    @Query("SELECT * FROM locations ORDER BY is_main DESC, name ASC")
    List<LocationEntity> getAll();

    @Query("SELECT * FROM locations WHERE id = :id")
    LocationEntity getById(long id);

    @Query("SELECT * FROM locations WHERE is_main = 1 LIMIT 1")
    LocationEntity getMainLocation();

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(LocationEntity location);

    @Update
    int update(LocationEntity location);

    @Query("DELETE FROM locations WHERE id = :id")
    int delete(long id);
}
