package com.example.campusexpense.ui.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusexpense.R;
import com.example.campusexpense.data.database.AppDatabase;
import com.example.campusexpense.data.database.BudgetDao;
import com.example.campusexpense.data.database.CategoryDao;
import com.example.campusexpense.data.database.ExpenseDao;
import com.example.campusexpense.data.model.Budget;
import com.example.campusexpense.data.model.Category;
import com.example.campusexpense.data.model.Expense;
import com.example.campusexpense.ui.expense.CategoryExpenseAdapter;
import com.example.campusexpense.ui.expense.ExpenseRecyclerAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpenseFragments extends Fragment {

    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;
    private TabLayout tabLayout;
    private Spinner monthSpinner;
    private Spinner categoryFilterSpinner;
    private TextView totalExpenseText;
    private TextView expenseCountText;
    private TextView emptyView;

    // --- Database & Logic ---
    private ExpenseDao expenseDao;
    private CategoryDao categoryDao;
    private BudgetDao budgetDao;
    private SharedPreferences sharedPreferences;
    private int currentUserId;

    // --- Adapters ---
    private CategoryExpenseAdapter categoryAdapter;
    private ExpenseRecyclerAdapter expenseAdapter;

    // --- Lists Data ---
    private List<Category> categoryList = new ArrayList<>();
    private List<CategoryExpenseAdapter.CategoryExpenseItem> categoryExpenseList = new ArrayList<>();
    private List<Expense> expenseList = new ArrayList<>();

    private List<Category> spinnerCategoryList = new ArrayList<>();

    private int currentMonth;
    private int currentYear;
    private int selectedCategoryId = -1;
    private int currentTab = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expense, container, false);

        initViews(view);
        initDatabase();

        Calendar calendar = Calendar.getInstance();
        currentMonth = calendar.get(Calendar.MONTH);
        currentYear = calendar.get(Calendar.YEAR);

        setupTabs();
        setupSpinners();
        setupRecyclerView();

        fabAdd.setOnClickListener(v -> showAddDialog());

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        fabAdd = view.findViewById(R.id.fabAdd);
        tabLayout = view.findViewById(R.id.tabLayout);
        monthSpinner = view.findViewById(R.id.monthSpinner);
        categoryFilterSpinner = view.findViewById(R.id.categoryFilterSpinner);
        totalExpenseText = view.findViewById(R.id.totalExpenseText);
        expenseCountText = view.findViewById(R.id.expenseCountText);
        emptyView = view.findViewById(R.id.emptyView);
    }

    private void initDatabase() {
        sharedPreferences = requireContext().getSharedPreferences("UserSession", 0);
        currentUserId = sharedPreferences.getInt("userId", -1);

        AppDatabase database = AppDatabase.getInstance(requireContext());
        expenseDao = database.expenseDao();
        categoryDao = database.categoryDao();
        budgetDao = database.budgetDao();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshData();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && isResumed()) {
            refreshData();
        }
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_by_category));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_by_date));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                refreshData();
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupSpinners() {

        Calendar calendar = Calendar.getInstance();
        List<String> months = new ArrayList<>();
        int currentMonthIndex = calendar.get(Calendar.MONTH);
        int currentYearValue = calendar.get(Calendar.YEAR);

        for (int i = -6; i <= 6; i++) {
            calendar.set(currentYearValue, currentMonthIndex + i, 1);
            months.add(new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.getTime()));
        }

        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, months);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        monthSpinner.setAdapter(monthAdapter);
        monthSpinner.setSelection(6); // Default: Current month

        monthSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                Calendar cal = Calendar.getInstance();
                cal.set(currentYearValue, currentMonthIndex + (position - 6), 1);
                currentMonth = cal.get(Calendar.MONTH);
                currentYear = cal.get(Calendar.YEAR);
                refreshData();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        new Thread(() -> {
            List<Category> cats = categoryDao.getAll();
            requireActivity().runOnUiThread(() -> {
                spinnerCategoryList.clear();
                spinnerCategoryList.addAll(cats);

                List<String> categoryNames = new ArrayList<>();
                categoryNames.add(getString(R.string.all_categories));
                for (Category cat : spinnerCategoryList) {
                    categoryNames.add(cat.getName());
                }

                ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_item, categoryNames);
                categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                categoryFilterSpinner.setAdapter(categoryAdapter);
            });
        }).start();

        categoryFilterSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedCategoryId = -1;
                } else if (!spinnerCategoryList.isEmpty() && position - 1 < spinnerCategoryList.size()) {
                    selectedCategoryId = spinnerCategoryList.get(position - 1).getId();
                }
                refreshData();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        categoryAdapter = new CategoryExpenseAdapter(categoryExpenseList, (categoryId, categoryName) -> {
            showCategoryExpensesDialog(categoryId, categoryName);
        });
        categoryAdapter.setContext(requireContext());

        expenseAdapter = new ExpenseRecyclerAdapter(expenseList, categoryList,
                this::showEditDialog,
                this::showDeleteDialog
        );
        expenseAdapter.setContext(requireContext());
    }


    private void refreshData() {
        new Thread(() -> {

            Calendar calendar = Calendar.getInstance();
            calendar.set(currentYear, currentMonth, 1, 0, 0, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long startDate = calendar.getTimeInMillis();

            calendar.add(Calendar.MONTH, 1);
            calendar.add(Calendar.MILLISECOND, -1);
            long endDate = calendar.getTimeInMillis();

            List<Category> tempCategories = categoryDao.getAll();
            List<CategoryExpenseAdapter.CategoryExpenseItem> tempCategoryItems = new ArrayList<>();
            List<Expense> tempExpenses = new ArrayList<>();

            double totalAmount = 0;
            int totalCount = 0;

            if (currentTab == 0) {

                List<Category> filteredCats = new ArrayList<>();
                if (selectedCategoryId == -1) {
                    filteredCats.addAll(tempCategories);
                } else {
                    for (Category cat : tempCategories) {
                        if (cat.getId() == selectedCategoryId) {
                            filteredCats.add(cat);
                            break;
                        }
                    }
                }

                for (Category category : filteredCats) {
                    Double catTotal = expenseDao.getTotalExpensesByCategoryAndDateRange(currentUserId, category.getId(), startDate, endDate);
                    double safeCatTotal = (catTotal != null) ? catTotal : 0.0;

                    int count = expenseDao.getExpensesByCategoryAndDateRange(currentUserId, category.getId(), startDate, endDate).size();

                    if (safeCatTotal > 0 || selectedCategoryId != -1) {

                        Budget budget = budgetDao.getBudgetByCategoryAndUser(currentUserId, category.getId());
                        tempCategoryItems.add(new CategoryExpenseAdapter.CategoryExpenseItem(
                                category.getId(),
                                category.getName(),
                                safeCatTotal,
                                count,
                                budget
                        ));
                        totalAmount += safeCatTotal;
                        totalCount += count;
                    }
                }
            } else {

                if (selectedCategoryId == -1) {
                    tempExpenses.addAll(expenseDao.getExpensesByDateRange(currentUserId, startDate, endDate));
                    Double dbTotal = expenseDao.getTotalExpensesByDateRange(currentUserId, startDate, endDate);
                    totalAmount = (dbTotal != null) ? dbTotal : 0.0;
                } else {
                    tempExpenses.addAll(expenseDao.getExpensesByCategoryAndDateRange(currentUserId, selectedCategoryId, startDate, endDate));
                    Double dbTotal = expenseDao.getTotalExpensesByCategoryAndDateRange(currentUserId, selectedCategoryId, startDate, endDate);
                    totalAmount = (dbTotal != null) ? dbTotal : 0.0;
                }
                totalCount = tempExpenses.size();
            }

            double finalTotalAmount = totalAmount;
            int finalTotalCount = totalCount;

            requireActivity().runOnUiThread(() -> {
                categoryList.clear();
                categoryList.addAll(tempCategories);

                if (currentTab == 0) {
                    categoryExpenseList.clear();
                    categoryExpenseList.addAll(tempCategoryItems);
                    recyclerView.setAdapter(categoryAdapter);
                    categoryAdapter.notifyDataSetChanged();
                } else {
                    expenseList.clear();
                    expenseList.addAll(tempExpenses);
                    expenseAdapter.setCategoryList(categoryList);
                    recyclerView.setAdapter(expenseAdapter);
                    expenseAdapter.notifyDataSetChanged();
                }

                updateStatisticsUI(finalTotalAmount, finalTotalCount);
                updateEmptyView();
            });

        }).start();
    }

    private void updateStatisticsUI(double totalAmount, int count) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
        totalExpenseText.setText(currencyFormat.format(totalAmount));
        expenseCountText.setText(String.format(Locale.getDefault(), "%d transactions", count));
    }

    private void updateEmptyView() {
        boolean isEmpty = (currentTab == 0 && categoryExpenseList.isEmpty()) ||
                (currentTab == 1 && expenseList.isEmpty());

        if (isEmpty) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    // --- ADD DIALOG ---
    private void showAddDialog() {
        new Thread(() -> {
            List<Category> cats = categoryDao.getAll();
            requireActivity().runOnUiThread(() -> {
                if (cats.isEmpty()) {
                    Toast.makeText(requireContext(), "Please add categories first", Toast.LENGTH_SHORT).show();
                    return;
                }
                showAddDialogInternal(cats);
            });
        }).start();
    }

    private void showAddDialogInternal(List<Category> cats) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_expense, null);

        Spinner categorySpinner = dialogView.findViewById(R.id.categorySpinner);
        TextInputEditText amountInput = dialogView.findViewById(R.id.amountInput);
        TextInputEditText descriptionInput = dialogView.findViewById(R.id.descriptionInput);
        Button dateButton = dialogView.findViewById(R.id.dateButton);
        Button saveButton = dialogView.findViewById(R.id.saveButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        List<String> categoryNames = new ArrayList<>();
        for (Category cat : cats) {
            categoryNames.add(cat.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, categoryNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        dateButton.setText(dateFormat.format(calendar.getTime()));
        long[] selectedDate = {calendar.getTimeInMillis()};

        dateButton.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        selectedDate[0] = calendar.getTimeInMillis();
                        dateButton.setText(dateFormat.format(calendar.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        saveButton.setOnClickListener(v -> {
            int categoryPosition = categorySpinner.getSelectedItemPosition();
            String amountStr = amountInput.getText().toString().trim();
            String description = descriptionInput.getText().toString().trim();

            if (TextUtils.isEmpty(amountStr)) {
                Toast.makeText(requireContext(), "Please enter amount", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    Toast.makeText(requireContext(), "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
                return;
            }

            Category selectedCategory = cats.get(categoryPosition);
            Expense expense = new Expense(currentUserId, selectedCategory.getId(), amount, description, selectedDate[0]);

            new Thread(() -> {
                expenseDao.insert(expense);
                refreshData();
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), R.string.expense_added, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
            }).start();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // --- EDIT DIALOG ---
    private void showEditDialog(Expense expense) {
        new Thread(() -> {
            List<Category> cats = categoryDao.getAll();
            requireActivity().runOnUiThread(() -> showEditDialogInternal(expense, cats));
        }).start();
    }

    private void showEditDialogInternal(Expense expense, List<Category> cats) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_expense, null);

        Spinner categorySpinner = dialogView.findViewById(R.id.categorySpinner);
        TextInputEditText amountInput = dialogView.findViewById(R.id.amountInput);
        TextInputEditText descriptionInput = dialogView.findViewById(R.id.descriptionInput);
        Button dateButton = dialogView.findViewById(R.id.dateButton);
        Button saveButton = dialogView.findViewById(R.id.saveButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        List<String> categoryNames = new ArrayList<>();
        for (Category cat : cats) {
            categoryNames.add(cat.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, categoryNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        int categoryIndex = -1;
        for (int i = 0; i < cats.size(); i++) {
            if (cats.get(i).getId() == expense.getCategoryId()) {
                categoryIndex = i;
                break;
            }
        }
        if (categoryIndex >= 0) {
            categorySpinner.setSelection(categoryIndex);
        }
        categorySpinner.setEnabled(true);

        amountInput.setText(String.valueOf(expense.getAmount()));
        descriptionInput.setText(expense.getDescription());

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(expense.getDate());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        dateButton.setText(dateFormat.format(calendar.getTime()));
        long[] selectedDate = {expense.getDate()};

        dateButton.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        selectedDate[0] = calendar.getTimeInMillis();
                        dateButton.setText(dateFormat.format(calendar.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        saveButton.setOnClickListener(v -> {
            String amountStr = amountInput.getText().toString().trim();
            String description = descriptionInput.getText().toString().trim();

            if (TextUtils.isEmpty(amountStr)) {
                Toast.makeText(requireContext(), "Please enter amount", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    Toast.makeText(requireContext(), "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
                return;
            }

            int newCategoryPos = categorySpinner.getSelectedItemPosition();
            expense.setCategoryId(cats.get(newCategoryPos).getId());
            expense.setAmount(amount);
            expense.setDescription(description);
            expense.setDate(selectedDate[0]);

            new Thread(() -> {
                expenseDao.update(expense);
                refreshData();
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), R.string.expense_updated, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
            }).start();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // --- DELETE DIALOG ---
    private void showDeleteDialog(Expense expense) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_expense)
                .setMessage(R.string.confirm_delete_expense)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    new Thread(() -> {
                        expenseDao.delete(expense);
                        refreshData();
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), R.string.expense_deleted, Toast.LENGTH_SHORT).show();
                        });
                    }).start();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showCategoryExpensesDialog(int categoryId, String categoryName) {
        new Thread(() -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(currentYear, currentMonth, 1, 0, 0, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long startDate = calendar.getTimeInMillis();

            calendar.add(Calendar.MONTH, 1);
            calendar.add(Calendar.MILLISECOND, -1);
            long endDate = calendar.getTimeInMillis();

            List<Expense> expenses = expenseDao.getExpensesByCategoryAndDateRange(currentUserId, categoryId, startDate, endDate);

            requireActivity().runOnUiThread(() -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle(getString(R.string.expense_title, categoryName));

                if (expenses.isEmpty()) {
                    builder.setMessage(R.string.no_transactions);
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.show();
                    return;
                }

                StringBuilder message = new StringBuilder();
                NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

                for (Expense expense : expenses) {
                    message.append(dateFormat.format(new Date(expense.getDate())));
                    message.append(" - ");
                    message.append(currencyFormat.format(expense.getAmount()));
                    if (expense.getDescription() != null && !expense.getDescription().trim().isEmpty()) {
                        message.append("\n");
                        message.append(expense.getDescription());
                    }
                    message.append("\n\n");
                }

                builder.setMessage(message.toString());
                builder.setPositiveButton(android.R.string.ok, null);
                builder.show();
            });
        }).start();
    }
}