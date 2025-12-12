package com.example.campusexpense.data.database;


import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.campusexpense.data.model.User;

@Dao
public interface UserDao {
    @Insert
    long insert(User user);
    @Query("SELECT * FROM user WHERE username = :username LIMIT 1")
    User getUserByUsername(String username);

    @Query("SELECT * FROM user WHERE username = :username AND password = :password LIMIT 1")
    User login(String username, String password);

    @Query("SELECT COUNT(*) FROM user WHERE username = :username")
    int checkUsernameExists(String username);
}
