package com.example.campusexpense.ui.budget;

import android.content.Context;
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

import java.util.List;

public class BudgetRecyclerAdapter extends RecyclerView.Adapter<BudgetRecyclerAdapter.BudgetViewHolder> {

    private List<BudgetItem> budgetItems;
    private final OnBudgetActionClickListener editClickListener;
    private final OnBudgetActionClickListener deleteClickListener;

    public static class BudgetItem {
        public Budget budget;
        public String categoryName;
        public double spentAmount;

        public BudgetItem(Budget budget, String categoryName, double spentAmount) {
            this.budget = budget;
            this.categoryName = categoryName;
            this.spentAmount = spentAmount;
        }
    }

    public interface OnBudgetActionClickListener {
        void onBudgetActionClick(Budget budget);
    }

    public BudgetRecyclerAdapter(List<BudgetItem> budgetItems, OnBudgetActionClickListener editClickListener, OnBudgetActionClickListener deleteClickListener) {
        this.budgetItems = budgetItems;
        this.editClickListener = editClickListener;
        this.deleteClickListener = deleteClickListener;
    }

    @NonNull
    @Override
    public BudgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_budget_card, parent, false);
        return new BudgetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BudgetViewHolder holder, int position) {
        BudgetItem budgetItem = budgetItems.get(position);
        Budget budget = budgetItem.budget;
        Context context = holder.itemView.getContext();

        holder.categoryNameText.setText(budgetItem.categoryName);
        holder.periodText.setText(budget.getFrequency());

        // Sửa lại dòng bị lỗi và gộp logic tính toán
        holder.budgetAmountText.setText(context.getString(R.string.budget_amount_format, budget.getBudgetAmount()));

        double percentage = 0;
        if (budget.getBudgetAmount() > 0) {
            percentage = (budgetItem.spentAmount / budget.getBudgetAmount()) * 100;
        }

        holder.budgetProgressBar.setProgress((int) percentage);
        holder.usedAmountText.setText(context.getString(R.string.used_percentage_format, percentage));

        holder.editButton.setOnClickListener(v -> editClickListener.onBudgetActionClick(budget));
        holder.deleteButton.setOnClickListener(v -> deleteClickListener.onBudgetActionClick(budget));
    }

    @Override
    public int getItemCount() {
        return budgetItems.size();
    }

    public void updateData(List<BudgetItem> newBudgetItems) {
        this.budgetItems.clear();
        this.budgetItems.addAll(newBudgetItems);
        notifyDataSetChanged();
    }

    static class BudgetViewHolder extends RecyclerView.ViewHolder {
        TextView categoryNameText, periodText, budgetAmountText, usedAmountText;
        ImageView editButton, deleteButton;
        ProgressBar budgetProgressBar;

        public BudgetViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryNameText = itemView.findViewById(R.id.categoryNameText);
            periodText = itemView.findViewById(R.id.periodText);
            budgetAmountText = itemView.findViewById(R.id.budgetAmountText);
            usedAmountText = itemView.findViewById(R.id.usedAmountText);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            budgetProgressBar = itemView.findViewById(R.id.budgetProgressBar);
        }
    }
}
