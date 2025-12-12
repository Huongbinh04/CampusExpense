package com.example.campusexpense.data.database;


import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.campusexpense.data.model.Category;

import java.util.List;

@Dao
public interface CategoryDao {
    @Insert
    long insert(Category category);
    @Update
    void update(Category category);
    @Delete
    void delete(Category category);
    @Query("SELECT * FROM categories ORDER BY name ASC")
    List<Category> getAll();
    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    Category getById(int id);


}
