package com.example.gridviewapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class AdapterCustomerList extends RecyclerView.Adapter<AdapterCustomerList.CustomerViewHolder> {

    private ArrayList<POJO_id_string> customerList;
    private Context context;
    private gridListener mListener;

    AdapterCustomerList(ArrayList<POJO_id_string> customerList, Context context, gridListener mListener) {
        this.customerList = customerList;
        this.context = context;
        this.mListener = mListener;
    }


    static class CustomerViewHolder extends RecyclerView.ViewHolder {
        TextView cust_name;
        RelativeLayout tile;

        CustomerViewHolder(View itemView) {
            super(itemView);
            cust_name = itemView.findViewById(R.id.tv_cust_single_name);
            tile = itemView.findViewById(R.id.rl_cust_single);
        }
    }

    @NonNull
    @Override
    public AdapterCustomerList.CustomerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.customer_single, parent, false);
        return new CustomerViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AdapterCustomerList.CustomerViewHolder holder, int position) {
        holder.cust_name.setText(customerList.get(position).getDisplay_name());
        holder.tile.setOnClickListener(v -> mListener.listClick(position));
    }

    @Override
    public int getItemCount() {
        return customerList.size();
    }

    public interface gridListener {
        void listClick(int position);
    }
}
