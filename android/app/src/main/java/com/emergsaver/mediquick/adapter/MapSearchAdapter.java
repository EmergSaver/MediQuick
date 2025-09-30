package com.emergsaver.mediquick.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.emergsaver.mediquick.R;

import java.util.ArrayList;
import java.util.List;

import model.Hospital;


public class MapSearchAdapter extends RecyclerView.Adapter<MapSearchAdapter.ViewHolder> {
    private List<Hospital> hospitals;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Hospital hospital);
    }

    public MapSearchAdapter(List<Hospital> hospitals, OnItemClickListener listener) {
        this.hospitals = hospitals != null ? hospitals : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hospital, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Hospital hospital = hospitals.get(position);
        holder.name.setText(hospital.getHospital_name());
        holder.address.setText(hospital.getAddress());

        holder.itemView.setOnClickListener(v -> {
            if(listener != null)
                listener.onItemClick(hospital);
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setItems(List<Hospital> newList) {
        hospitals.clear();
        if(newList != null) {
            hospitals.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return hospitals == null ? 0 : hospitals.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView address;

        public ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.hospital_name_text);
            address = itemView.findViewById(R.id.hospital_address_text);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateList(List<Hospital> newList) {
        if(hospitals == null)
            hospitals = new ArrayList<>();
        if(newList != null)
            hospitals = new ArrayList<>(newList);
        notifyDataSetChanged();
    }
}