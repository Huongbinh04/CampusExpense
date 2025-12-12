package com.example.campusexpense.ui.category;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.example.campusexpense.data.model.Category;


import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.campusexpense.R;

import java.util.List;

public class CategoryRecyclerAdapter extends RecyclerView.Adapter<CategoryRecyclerAdapter.ViewHolder> {
    private List<Category> categories;
    private OnEditClickListener onEditClick;
    private OnDeleteClickListener onDeleteClick;
    public interface OnEditClickListener {
        void onEditClick(Category category);
    }
    public interface OnDeleteClickListener {
        void onDeleteClick(Category category);
    }
    public CategoryRecyclerAdapter(List<Category> categories, OnEditClickListener onEditClick, OnDeleteClickListener onDeleteClick) {
        this.categories = categories;
        this.onEditClick = onEditClick;
        this.onDeleteClick = onDeleteClick;
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_card, parent, false);
        return new ViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.nameTextView.setText(category.getName());
        holder.editButton.setOnClickListener(v -> onEditClick.onEditClick(category));
        holder.deleteButton.setOnClickListener(v -> onDeleteClick.onDeleteClick(category));

    }
    @Override
    public int getItemCount() {return categories.size();}

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        ImageButton editButton;
        ImageButton deleteButton;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameText);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }

    }

}