package com.example.java_sql;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    ArrayList<ProductModel> list;
    OnItemClickListener listener;

    public interface OnItemClickListener {
        void onClick(ProductModel product);
    }

    public ProductAdapter(ArrayList<ProductModel> list, OnItemClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView txt;

        public ViewHolder(View itemView) {
            super(itemView);
            txt = itemView.findViewById(android.R.id.text1);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ProductModel product = list.get(position);
        holder.txt.setText(product.id + " - " + product.name + " - $" + product.price);
        holder.itemView.setOnClickListener(v -> listener.onClick(product));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}