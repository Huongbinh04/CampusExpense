package com.example.campusexpense.ui.expense;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusexpense.R;
import com.example.campusexpense.data.model.Budget;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class CategoryExpenseAdapter extends RecyclerView.Adapter<CategoryExpenseAdapter.ViewHolder> {

    private List<CategoryExpenseItem> itemList;
    private OnCategoryClickListener listener;
    private Context context;

    public interface OnCategoryClickListener {
        void onCategoryClick(int categoryId, String categoryName);
    }

    public CategoryExpenseAdapter(List<CategoryExpenseItem> itemList, OnCategoryClickListener listener) {
        this.itemList = itemList;
        this.listener = listener;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryExpenseItem item = itemList.get(position);
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());

        holder.tvCategoryName.setText(item.categoryName);
        holder.tvExpenseAmount.setText(currencyFormat.format(item.totalAmount));

        String transLabel = (context != null) ? "trans" : "trans";

        holder.tvExpenseCount.setText(String.format(Locale.getDefault(), "%d %s", item.transactionCount, transLabel));

        if (item.budget != null && item.budget.getBudgetAmount() > 0) {

            holder.layoutBudget.setVisibility(View.VISIBLE);
            holder.pbBudget.setVisibility(View.VISIBLE);
            holder.tvProgress.setVisibility(View.VISIBLE);

            double budgetAmount = item.budget.getBudgetAmount();
            double spentAmount = item.totalAmount;

            holder.tvBudgetAmount.setText(currencyFormat.format(budgetAmount));

            int percentage = (int) ((spentAmount / budgetAmount) * 100);

            holder.pbBudget.setMax(100);
            holder.pbBudget.setProgress(Math.min(percentage, 100));

            // Hiển thị số %
            holder.tvProgress.setText(String.format(Locale.getDefault(), "%d%%", percentage));

            if (percentage > 100) {
                int alertColor = Color.parseColor("#D32F2F");
                holder.pbBudget.setProgressTintList(ColorStateList.valueOf(alertColor));
                holder.tvProgress.setTextColor(alertColor);
                holder.tvExpenseAmount.setTextColor(alertColor);
            } else {
                int okColor = Color.parseColor("#4CAF50");
                int greyColor = Color.parseColor("#757575");
                int defaultRed = Color.parseColor("#D32F2F");

                holder.pbBudget.setProgressTintList(ColorStateList.valueOf(okColor));
                holder.tvProgress.setTextColor(greyColor);
                holder.tvExpenseAmount.setTextColor(defaultRed);
            }

        } else {
            holder.layoutBudget.setVisibility(View.GONE);
            holder.pbBudget.setVisibility(View.GONE);
            holder.tvProgress.setVisibility(View.GONE);

            holder.tvExpenseAmount.setTextColor(Color.parseColor("#D32F2F"));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCategoryClick(item.categoryId, item.categoryName);
            }
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName;
        TextView tvExpenseCount;
        TextView tvExpenseAmount;

        LinearLayout layoutBudget;
        TextView tvBudgetAmount;

        ProgressBar pbBudget;
        TextView tvProgress;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvExpenseCount = itemView.findViewById(R.id.tvExpenseCount);
            tvExpenseAmount = itemView.findViewById(R.id.tvExpenseAmount);

            layoutBudget = itemView.findViewById(R.id.layoutBudget);
            tvBudgetAmount = itemView.findViewById(R.id.tvBudgetAmount);

            pbBudget = itemView.findViewById(R.id.pbBudget);
            tvProgress = itemView.findViewById(R.id.tvProgress);
        }
    }

    // --- Model Class ---
    public static class CategoryExpenseItem {
        int categoryId;
        String categoryName;
        double totalAmount;
        int transactionCount;
        Budget budget;

        public CategoryExpenseItem(int categoryId, String categoryName, double totalAmount, int transactionCount, Budget budget) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.totalAmount = totalAmount;
            this.transactionCount = transactionCount;
            this.budget = budget;
        }
    }
}