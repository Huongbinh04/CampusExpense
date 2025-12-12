package com.example.campusexpense.ui.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.example.campusexpense.R;
import com.example.campusexpense.data.database.AppDatabase;
import com.example.campusexpense.data.database.BudgetDao;
import com.example.campusexpense.data.database.CategoryDao;
import com.example.campusexpense.data.database.ExpenseDao;
import com.example.campusexpense.data.model.Budget;
import com.example.campusexpense.data.model.Category;
import com.example.campusexpense.data.model.Expense;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {
    // UI Components
    private TextView welcomeText, totalExpenseText, currentMonthText;

    // Top Category Card Views
    private CardView topCategoryCard;
    private TextView topCategoryName, topCategorySpentText, topCategoryBudgetText, topCategoryProgressText;
    private LinearLayout topCategoryBudgetLayout;
    private ProgressBar topCategoryProgressBar;

    private LinearLayout distributionContainer;
    private TextView transactionCountText, avgPerDayText, budgetCountText;
    private TextView totalBudgetText, spentText, remainingText;

    private ExpenseDao expenseDao;
    private BudgetDao budgetDao;
    private CategoryDao categoryDao;

    private int userId = -1;
    private String username = "";

    public HomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initViews(view);

        AppDatabase db = AppDatabase.getInstance(requireContext());
        expenseDao = db.expenseDao();
        budgetDao = db.budgetDao();
        categoryDao = db.categoryDao();

        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        userId = sharedPreferences.getInt("userId", -1);
        username = sharedPreferences.getString("username", "User");

        welcomeText.setText(String.format("Welcome, %s", username));

        return view;
    }

    private void initViews(View view) {
        welcomeText = view.findViewById(R.id.welcomeText);
        totalExpenseText = view.findViewById(R.id.totalExpenseText);
        currentMonthText = view.findViewById(R.id.currentMonthText);

        topCategoryCard = view.findViewById(R.id.topCategoryCard);
        topCategoryName = view.findViewById(R.id.topCategoryName);
        topCategorySpentText = view.findViewById(R.id.topCategorySpentText);
        topCategoryBudgetLayout = view.findViewById(R.id.topCategoryBudgetLayout);
        topCategoryBudgetText = view.findViewById(R.id.topCategoryBudgetText);
        topCategoryProgressBar = view.findViewById(R.id.topCategoryProgressBar);
        topCategoryProgressText = view.findViewById(R.id.topCategoryProgressText);

        distributionContainer = view.findViewById(R.id.distributionContainer);

        transactionCountText = view.findViewById(R.id.transactionCountText);
        avgPerDayText = view.findViewById(R.id.avgPerDayText);
        budgetCountText = view.findViewById(R.id.budgetCountText);

        totalBudgetText = view.findViewById(R.id.totalBudgetText);
        spentText = view.findViewById(R.id.spentText);
        remainingText = view.findViewById(R.id.remainingText);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (userId != -1) refreshData();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && isResumed() && userId != -1) refreshData();
    }

    public void refreshData() {
        new Thread(() -> {
            try {
                Calendar calendar = Calendar.getInstance();
                String currentMonthStr = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.getTime());
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0);
                long startDate = calendar.getTimeInMillis();
                calendar.add(Calendar.MONTH, 1);
                long endDate = calendar.getTimeInMillis();

                Double totalExpenseObj = expenseDao.getTotalExpensesByDateRange(userId, startDate, endDate);
                double totalExpense = (totalExpenseObj != null) ? totalExpenseObj : 0.0;

                List<Budget> allBudgets = budgetDao.getAllBudgetsByUser(userId);
                Map<Integer, Double> budgetMap = new HashMap<>();
                double totalBudget = 0.0;
                int tempBudgetCount = (allBudgets != null) ? allBudgets.size() : 0;

                if (allBudgets != null) {
                    for (Budget b : allBudgets) {
                        totalBudget += b.getBudgetAmount();
                        budgetMap.put(b.getCategoryId(), b.getBudgetAmount());
                    }
                }

                double remaining = totalBudget - totalExpense;
                int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
                double avgPerDayValue = (currentDay > 0) ? totalExpense / currentDay : 0.0;
                int transactionCount = expenseDao.getExpenseCountByDateRange(userId, startDate, endDate);

                List<Expense> expenses = expenseDao.getExpensesByDateRange(userId, startDate, endDate);
                Map<Integer, Double> categoryExpenseMap = new HashMap<>();
                for (Expense e : expenses) {
                    categoryExpenseMap.put(e.getCategoryId(), categoryExpenseMap.getOrDefault(e.getCategoryId(), 0.0) + e.getAmount());
                }

                int topCategoryId = -1;
                double maxExpense = -1.0;
                for (Map.Entry<Integer, Double> entry : categoryExpenseMap.entrySet()) {
                    if (entry.getValue() > maxExpense) {
                        maxExpense = entry.getValue();
                        topCategoryId = entry.getKey();
                    }
                }

                String categoryName = "None";
                double categoryBudgetAmount = 0.0;
                double categorySpent = (maxExpense >= 0) ? maxExpense : 0.0;

                if (topCategoryId != -1) {
                    Category category = categoryDao.getById(topCategoryId);
                    if (category != null) categoryName = category.getName();
                    categoryBudgetAmount = budgetMap.getOrDefault(topCategoryId, 0.0);
                }

                List<Map.Entry<Integer, Double>> sortedList = new ArrayList<>(categoryExpenseMap.entrySet());
                sortedList.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));

                class DistItem {
                    String name;
                    double amount;
                    double budget;
                    int percent;
                }
                List<DistItem> distItems = new ArrayList<>();
                for (Map.Entry<Integer, Double> entry : sortedList) {
                    Category cat = categoryDao.getById(entry.getKey());
                    if (cat != null) {
                        DistItem item = new DistItem();
                        item.name = cat.getName();
                        item.amount = entry.getValue();
                        item.budget = budgetMap.getOrDefault(entry.getKey(), 0.0);

                        if (item.budget > 0) {
                            item.percent = (int) ((item.amount / item.budget) * 100);
                        } else {
                            item.percent = 0;
                        }
                        distItems.add(item);
                    }
                }

                FragmentActivity activity = getActivity();
                if (activity == null) return;

                double finalTotalBudget = totalBudget;
                double finalRemaining = remaining;
                String finalCategoryName = categoryName;
                double finalCategoryBudgetAmount = categoryBudgetAmount;
                double finalCategorySpent = categorySpent;
                int finalBudgetCount = tempBudgetCount;
                int finalTransactionCount = transactionCount;
                double finalAvgPerDayValue = avgPerDayValue;

                activity.runOnUiThread(() -> {
                    NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
                    currentMonthText.setText(currentMonthStr);
                    totalExpenseText.setText(currencyFormat.format(totalExpense));

                    if ("None".equals(finalCategoryName)) {
                        topCategoryCard.setVisibility(View.GONE);
                    } else {
                        topCategoryCard.setVisibility(View.VISIBLE);
                        topCategoryName.setText(finalCategoryName);
                        topCategorySpentText.setText(currencyFormat.format(finalCategorySpent));

                        if (finalCategoryBudgetAmount > 0) {
                            topCategoryBudgetLayout.setVisibility(View.VISIBLE);
                            topCategoryProgressBar.setVisibility(View.VISIBLE);
                            topCategoryProgressText.setVisibility(View.VISIBLE);

                            topCategoryBudgetText.setText(currencyFormat.format(finalCategoryBudgetAmount));

                            int percent = (int) ((finalCategorySpent / finalCategoryBudgetAmount) * 100);
                            topCategoryProgressBar.setProgress(Math.min(percent, 100));
                            topCategoryProgressText.setText(percent + "%");

                            updateProgressColor(topCategoryProgressBar, topCategoryProgressText, topCategorySpentText, percent);
                        } else {
                            topCategoryBudgetLayout.setVisibility(View.GONE);
                            topCategoryProgressBar.setVisibility(View.GONE);
                            topCategoryProgressText.setVisibility(View.GONE);
                            topCategorySpentText.setTextColor(Color.parseColor("#D32F2F"));
                        }
                    }

                    distributionContainer.removeAllViews();
                    if (distItems.isEmpty()) {
                        distributionContainer.setVisibility(View.GONE);
                    } else {
                        distributionContainer.setVisibility(View.VISIBLE);
                        LayoutInflater inflater = LayoutInflater.from(getContext());
                        if (inflater != null) {
                            for (DistItem item : distItems) {
                                View itemView = inflater.inflate(R.layout.item_distribution, distributionContainer, false);

                                TextView nameView = itemView.findViewById(R.id.itemCategoryName);
                                TextView spentView = itemView.findViewById(R.id.itemSpentText);
                                LinearLayout budgetLayout = itemView.findViewById(R.id.itemBudgetLayout);
                                TextView budgetView = itemView.findViewById(R.id.itemBudgetText);
                                ProgressBar progressBar = itemView.findViewById(R.id.itemProgressBar);
                                TextView progressText = itemView.findViewById(R.id.itemProgressText);

                                nameView.setText(item.name);
                                spentView.setText(currencyFormat.format(item.amount));

                                if (item.budget > 0) {
                                    budgetLayout.setVisibility(View.VISIBLE);
                                    progressBar.setVisibility(View.VISIBLE);
                                    progressText.setVisibility(View.VISIBLE);

                                    budgetView.setText(currencyFormat.format(item.budget));
                                    progressBar.setProgress(Math.min(item.percent, 100));
                                    progressText.setText(item.percent + "%");

                                    updateProgressColor(progressBar, progressText, spentView, item.percent);
                                } else {
                                    budgetLayout.setVisibility(View.GONE);
                                    progressBar.setVisibility(View.GONE);
                                    progressText.setVisibility(View.GONE);
                                    spentView.setTextColor(Color.parseColor("#D32F2F"));
                                }
                                distributionContainer.addView(itemView);
                            }
                        }
                    }
                    transactionCountText.setText(String.valueOf(finalTransactionCount));
                    avgPerDayText.setText(String.format("%s / day", currencyFormat.format(finalAvgPerDayValue)));
                    budgetCountText.setText(String.valueOf(finalBudgetCount));
                    totalBudgetText.setText(currencyFormat.format(finalTotalBudget));
                    spentText.setText(currencyFormat.format(totalExpense));
                    remainingText.setText(currencyFormat.format(finalRemaining));
                });

            } catch (Exception e) {
                Log.e("HomeFragment", "Error refreshing data", e);
            }
        }).start();
    }

    private void updateProgressColor(ProgressBar progressBar, TextView progressText, TextView amountText, int percent) {
        if (percent > 100) {
            int alertColor = Color.parseColor("#D32F2F");
            progressBar.setProgressTintList(ColorStateList.valueOf(alertColor));
            progressText.setTextColor(alertColor);
            amountText.setTextColor(alertColor);
        } else {
            int normalColor = Color.parseColor("#4CAF50");
            int normalTextColor = Color.parseColor("#757575");
            int normalAmountColor = Color.parseColor("#D32F2F");

            progressBar.setProgressTintList(ColorStateList.valueOf(normalColor));
            progressText.setTextColor(normalTextColor);
            amountText.setTextColor(normalAmountColor);
        }
    }
}