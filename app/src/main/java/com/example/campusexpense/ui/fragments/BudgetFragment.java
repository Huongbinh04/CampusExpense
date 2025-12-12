// Đảm bảo file này chứa lớp public class BudgetFragment
// (Nội dung file này đã được cung cấp ở câu trả lời trước, bạn chỉ cần đảm bảo đúng file).
package com.example.campusexpense.ui.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusexpense.R;
import com.example.campusexpense.data.database.AppDatabase;
import com.example.campusexpense.data.database.BudgetDao;
import com.example.campusexpense.data.database.CategoryDao;
import com.example.campusexpense.data.database.ExpenseDao;
import com.example.campusexpense.data.model.Budget;
import com.example.campusexpense.data.model.Category;
import com.example.campusexpense.ui.budget.BudgetRecyclerAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

public class BudgetFragment extends Fragment {
    private static final String TAG = "BudgetFragment";
    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;
    private BudgetRecyclerAdapter adapter;
    private List<BudgetRecyclerAdapter.BudgetItem> budgetItemList;
    private BudgetDao budgetDao;
    private CategoryDao categoryDao;
    private ExpenseDao expenseDao;
    private TextView emptyView;
    private int currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budget, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        fabAdd = view.findViewById(R.id.fabAdd);
        emptyView = view.findViewById(R.id.emptyView);

        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        currentUserId = sharedPreferences.getInt("userId", -1);

        AppDatabase database = AppDatabase.getInstance(requireContext());
        budgetDao = database.budgetDao();
        categoryDao = database.categoryDao();
        expenseDao = database.expenseDao();

        budgetItemList = new ArrayList<>();
        adapter = new BudgetRecyclerAdapter(budgetItemList, this::showEditDialog, this::showDeleteDialog);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> showAddDialog());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentUserId != -1) {
            refreshList();
        }
    }

    private void refreshList() {
        new Thread(() -> {
            try {
                List<Budget> allBudgets = budgetDao.getAllBudgetsByUser(currentUserId);
                List<BudgetRecyclerAdapter.BudgetItem> newBudgetItemList = new ArrayList<>();

                for (Budget budget : allBudgets) {
                    Category category = categoryDao.getById(budget.getCategoryId());
                    String categoryName = (category != null) ? category.getName() : "Unknown";

                    Calendar calendar = Calendar.getInstance();
                    long startDate, endDate;

                    if ("Monthly".equalsIgnoreCase(budget.getFrequency())) {
                        calendar.set(Calendar.DAY_OF_MONTH, 1);
                        startDate = calendar.getTimeInMillis();
                        calendar.add(Calendar.MONTH, 1);
                        calendar.add(Calendar.DAY_OF_MONTH, -1);
                        endDate = calendar.getTimeInMillis();
                    } else { // Weekly
                        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
                        startDate = calendar.getTimeInMillis();
                        calendar.add(Calendar.WEEK_OF_YEAR, 1);
                        calendar.add(Calendar.DAY_OF_WEEK, -1);
                        endDate = calendar.getTimeInMillis();
                    }

                    Double spent = expenseDao.getTotalExpensesByCategoryAndDateRange(currentUserId, budget.getCategoryId(), startDate, endDate);
                    double spentAmount = (spent != null) ? spent : 0.0;
                    budget.setUsedAmount(spentAmount); // Cập nhật số tiền đã dùng vào đối tượng budget

                    newBudgetItemList.add(new BudgetRecyclerAdapter.BudgetItem(budget, categoryName, spentAmount));
                }

                FragmentActivity activity = getActivity();
                if (activity == null) return;

                activity.runOnUiThread(() -> {
                    adapter.updateData(newBudgetItemList);
                    if (newBudgetItemList.isEmpty()) {
                        emptyView.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        emptyView.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error refreshing budget list", e);
            }
        }).start();
    }

    private void showDeleteDialog(Budget budget) {
        FragmentActivity activity = getActivity();
        if (activity == null) return;

        new AlertDialog.Builder(activity)
                .setTitle("Delete Budget")
                .setMessage("Are you sure you want to delete this budget?")
                .setPositiveButton("Delete", (dialog, which) -> new Thread(() -> {
                    try {
                        budgetDao.delete(budget);
                        activity.runOnUiThread(() -> {
                            Toast.makeText(activity, "Budget deleted successfully", Toast.LENGTH_SHORT).show();
                            refreshList();
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error deleting budget", e);
                    }
                }).start())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddDialog() {
        new Thread(() -> {
            try {
                List<Category> allCategories = categoryDao.getAll();
                FragmentActivity activity = getActivity();
                if (activity == null) return;

                activity.runOnUiThread(() -> {
                    if (allCategories.isEmpty()) {
                        Toast.makeText(activity, "Please add categories first", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_budget, null);

                    Spinner categorySpinner = dialogView.findViewById(R.id.categorySpinner);
                    TextInputEditText amountInput = dialogView.findViewById(R.id.amountInput);
                    Spinner periodSpinner = dialogView.findViewById(R.id.periodSpinner);
                    Button saveButton = dialogView.findViewById(R.id.saveButton);
                    Button cancelButton = dialogView.findViewById(R.id.cancelButton);

                    List<String> categoryNameList = new ArrayList<>();
                    for (Category cat : allCategories) {
                        categoryNameList.add(cat.getName());
                    }
                    ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, categoryNameList);
                    categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    categorySpinner.setAdapter(categoryAdapter);

                    String[] periods = {"Monthly", "Weekly"};
                    ArrayAdapter<String> periodAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, periods);
                    periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    periodSpinner.setAdapter(periodAdapter);

                    builder.setView(dialogView);
                    AlertDialog dialog = builder.create();

                    saveButton.setOnClickListener(v -> {
                        String amountStr = Objects.requireNonNull(amountInput.getText()).toString().trim();
                        if (TextUtils.isEmpty(amountStr)) {
                            Toast.makeText(activity, "Please enter amount", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        double amount;
                        try {
                            amount = Double.parseDouble(amountStr);
                        } catch (NumberFormatException e) {
                            Toast.makeText(activity, "Invalid amount format", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Category selectedCategory = allCategories.get(categorySpinner.getSelectedItemPosition());
                        String period = periods[periodSpinner.getSelectedItemPosition()];

                        new Thread(() -> {
                            try {
                                Budget existingBudget = budgetDao.getBudgetByCategoryAndUser(currentUserId, selectedCategory.getId());
                                if (existingBudget != null) {
                                    activity.runOnUiThread(() -> Toast.makeText(activity, "Budget for this category already exists", Toast.LENGTH_SHORT).show());
                                    return;
                                }

                                Budget budget = new Budget(currentUserId, selectedCategory.getId(), selectedCategory.getName(), amount, 0.0, period);
                                budgetDao.insert(budget);

                                activity.runOnUiThread(() -> {
                                    dialog.dismiss();
                                    Toast.makeText(activity, "Budget added successfully", Toast.LENGTH_SHORT).show();
                                    refreshList();
                                });
                            } catch (Exception e) {
                                Log.e(TAG, "Error adding budget", e);
                            }
                        }).start();
                    });
                    cancelButton.setOnClickListener(v -> dialog.dismiss());
                    dialog.show();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error preparing add dialog", e);
            }
        }).start();
    }

    private void showEditDialog(Budget budget) {
        new Thread(() -> {
            try {
                FragmentActivity activity = getActivity();
                if (activity == null) return;

                activity.runOnUiThread(() -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_budget, null);

                    Spinner categorySpinner = dialogView.findViewById(R.id.categorySpinner);
                    TextInputEditText amountInput = dialogView.findViewById(R.id.amountInput);
                    Spinner periodSpinner = dialogView.findViewById(R.id.periodSpinner);
                    Button saveButton = dialogView.findViewById(R.id.saveButton);
                    Button cancelButton = dialogView.findViewById(R.id.cancelButton);

                    ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, new String[]{budget.getCategoryName()});
                    categorySpinner.setAdapter(categoryAdapter);
                    categorySpinner.setEnabled(false);

                    amountInput.setText(String.valueOf(budget.getBudgetAmount()));

                    String[] periods = {"Monthly", "Weekly"};
                    ArrayAdapter<String> periodAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, periods);
                    periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    periodSpinner.setAdapter(periodAdapter);

                    for (int i = 0; i < periods.length; i++) {
                        if (periods[i].equals(budget.getFrequency())) {
                            periodSpinner.setSelection(i);
                            break;
                        }
                    }

                    builder.setView(dialogView);
                    AlertDialog dialog = builder.create();

                    saveButton.setOnClickListener(v -> {
                        String amountStr = Objects.requireNonNull(amountInput.getText()).toString().trim();
                        if (TextUtils.isEmpty(amountStr)) {
                            Toast.makeText(activity, "Please enter amount", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        double amount;
                        try {
                            amount = Double.parseDouble(amountStr);
                        } catch (NumberFormatException e) {
                            Toast.makeText(activity, "Invalid amount format", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String period = periods[periodSpinner.getSelectedItemPosition()];
                        budget.setBudgetAmount(amount);
                        budget.setFrequency(period);

                        new Thread(() -> {
                            try {
                                budgetDao.update(budget);
                                activity.runOnUiThread(() -> {
                                    dialog.dismiss();
                                    Toast.makeText(activity, "Budget updated successfully", Toast.LENGTH_SHORT).show();
                                    refreshList();
                                });
                            } catch (Exception e) {
                                Log.e(TAG, "Error updating budget", e);
                            }
                        }).start();
                    });
                    cancelButton.setOnClickListener(v -> dialog.dismiss());
                    dialog.show();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error preparing edit dialog", e);
            }
        }).start();
    }
}
