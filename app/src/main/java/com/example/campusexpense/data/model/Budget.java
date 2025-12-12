package com.example.campusexpense.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "budgets_table")
public class Budget {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "user_id")
    private int userId;

    @ColumnInfo(name = "category_id")
    private int categoryId;

    @ColumnInfo(name = "created_at", defaultValue = "CURRENT_TIMESTAMP")
    private String createdAt;

    @ColumnInfo(name = "category_name")
    private String categoryName;

    @ColumnInfo(name = "budget_amount")
    private double budgetAmount;

    @ColumnInfo(name = "used_amount")
    private double usedAmount;

    @ColumnInfo(name = "frequency")
    private String frequency;

    public Budget(int userId, int categoryId, String categoryName, double budgetAmount, double usedAmount, String frequency) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.budgetAmount = budgetAmount;
        this.usedAmount = usedAmount;
        this.frequency = frequency;
    }

    // --- GETTERS ---
    public int getId() { return id; }
    public int getUserId() { return userId; }
    public int getCategoryId() { return categoryId; }
    public String getCreatedAt() { return createdAt; }
    public String getCategoryName() { return categoryName; }
    public double getBudgetAmount() { return budgetAmount; }
    public double getUsedAmount() { return usedAmount; }
    public String getFrequency() { return frequency; }
    public int getUsedPercentage() {
        if (budgetAmount == 0) return 0;
        return (int) ((usedAmount / budgetAmount) * 100);
    }

    // --- SETTERS ---
    public void setId(int id) { this.id = id; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public void setBudgetAmount(double budgetAmount) { this.budgetAmount = budgetAmount; }
    public void setUsedAmount(double usedAmount) { this.usedAmount = usedAmount; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
}
