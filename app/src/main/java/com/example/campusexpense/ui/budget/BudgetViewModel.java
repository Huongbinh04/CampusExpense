package com.example.campusexpense.ui.budget;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.campusexpense.data.model.Budget;

import java.util.ArrayList;
import java.util.List;

public class BudgetViewModel extends ViewModel {
    private final MutableLiveData<List<Budget>> budgets = new MutableLiveData<>();
    private final List<Budget> budgetList = new ArrayList<>();

    public BudgetViewModel() {

        budgetList.add(new Budget(1, 101, "food", 700, 21, "Monthly"));
        budgetList.add(new Budget(1, 102, "home", 900, 198, "Monthly"));
        budgets.setValue(budgetList);
    }

    public LiveData<List<Budget>> getBudgets() {
        return budgets;
    }

    public void addBudget(Budget newBudget) {
        boolean found = false;
        for (int i = 0; i < budgetList.size(); i++) {
            if (budgetList.get(i).getCategoryId() == newBudget.getCategoryId()) {
                budgetList.set(i, newBudget);
                found = true;
                break;
            }
        }
        if (!found) {
            budgetList.add(newBudget);
        }
        budgets.setValue(budgetList);
    }
}
