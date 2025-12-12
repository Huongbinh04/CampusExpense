package com.example.campusexpense.ui.budget;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusexpense.R;
import com.example.campusexpense.data.model.Budget;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder> {

    private List<Budget> budgetList = new ArrayList<>();

    @NonNull
    @Override
    public BudgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.budget_item, parent, false);

        return new BudgetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BudgetViewHolder holder, int position) {
        Budget budget = budgetList.get(position);
        holder.bind(budget);
    }

    @Override
    public int getItemCount() {
        return budgetList.size();
    }

    public void setBudgets(List<Budget> budgets) {
        this.budgetList = budgets;
        notifyDataSetChanged();
    }

    static class BudgetViewHolder extends RecyclerView.ViewHolder {
        private final TextView categoryNameText;
        private final TextView frequencyText;
        private final TextView budgetText;
        private final ProgressBar budgetProgressBar;
        private final TextView usedPercentageText;
        private final ImageView editButton;
        private final ImageView deleteButton;

        public BudgetViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryNameText = itemView.findViewById(R.id.categoryNameText);
            frequencyText = itemView.findViewById(R.id.frequencyText);
            budgetText = itemView.findViewById(R.id.budgetText);
            budgetProgressBar = itemView.findViewById(R.id.budgetProgressBar);
            usedPercentageText = itemView.findViewById(R.id.usedPercentageText);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }

        public void bind(Budget budget) {
            categoryNameText.setText(budget.getCategoryName());
            frequencyText.setText(budget.getFrequency());
            budgetText.setText(String.format(Locale.getDefault(), "Budget: %,.1f", budget.getBudgetAmount()));
            budgetProgressBar.setProgress(budget.getUsedPercentage());
            usedPercentageText.setText(String.format(Locale.getDefault(), "%d%% used", budget.getUsedPercentage()));

            // editButton.setOnClickListener(v -> { ... });
            // deleteButton.setOnClickListener(v -> { ... });
        }
    }
}
