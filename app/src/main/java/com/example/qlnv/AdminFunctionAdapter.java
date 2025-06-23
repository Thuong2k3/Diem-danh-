package com.example.qlnv; // Hoặc package của bạn

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AdminFunctionAdapter extends RecyclerView.Adapter<AdminFunctionAdapter.FunctionViewHolder> {

    private List<AdminFunction> functionList;
    private OnFunctionClickListener listener;

    public interface OnFunctionClickListener {
        void onFunctionClick(AdminFunction adminFunction);
    }

    public AdminFunctionAdapter(List<AdminFunction> functionList, OnFunctionClickListener listener) {
        this.functionList = functionList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FunctionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_function, parent, false);
        return new FunctionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FunctionViewHolder holder, int position) {
        AdminFunction adminFunction = functionList.get(position);
        holder.tvFunctionName.setText(adminFunction.getName());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFunctionClick(adminFunction);
            }
        });
    }

    @Override
    public int getItemCount() {
        return functionList.size();
    }

    static class FunctionViewHolder extends RecyclerView.ViewHolder {
        TextView tvFunctionName;
        // ImageView ivFunctionIcon; // Nếu bạn có icon

        public FunctionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFunctionName = itemView.findViewById(R.id.tvFunctionName);
            // ivFunctionIcon = itemView.findViewById(R.id.ivFunctionIcon); // Nếu có icon
        }
    }
}