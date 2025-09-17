package com.emergsaver.mediquick.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.emergsaver.mediquick.R;
import java.util.List;
import model.Hospital;

public class HospitalAdapter extends RecyclerView.Adapter<HospitalAdapter.ViewHolder> {

    private List<Hospital> hospitals;

    public HospitalAdapter(List<Hospital> hospitals) {
        this.hospitals = hospitals;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hospital, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Hospital hospital = hospitals.get(position);
        holder.name.setText(hospital.getHospital_name());
        holder.address.setText(hospital.getAddress());
    }

    @Override
    public int getItemCount() { return hospitals.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, address;
        ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.text_name);
            address = itemView.findViewById(R.id.text_address);
        }
    }
}
