package com.example.campusexpense.data.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.campusexpense.data.model.Budget;
import java.util.List;

@Dao
public interface BudgetDao {
    @Insert
    long insert(Budget budget);

    @Update
    void update(Budget budget);

    @Delete
    void delete(Budget budget);

    @Query("SELECT * FROM budgets_table WHERE user_id = :userId ORDER BY created_at DESC")
    List<Budget> getAllBudgetsByUser(int userId);

    @Query("SELECT * FROM budgets_table WHERE user_id = :userId AND category_id = :categoryId")
    Budget getBudgetByCategoryAndUser(int userId, int categoryId);

    @Query("SELECT * FROM budgets_table WHERE id = :id")
    Budget getBudgetById(int id);

    @Query("SELECT COUNT(*) FROM budgets_table WHERE user_id = :userId")
    Integer getBudgetCount(int userId);

    @Query("SELECT * FROM budgets_table WHERE user_id = :userId ORDER BY created_at DESC LIMIT 1")
    Budget getLatestBudget(int userId);

    @Query("SELECT SUM(budget_amount) FROM budgets_table WHERE user_id = :userId")
    Double getTotalBudgetAmount(int userId);
}
