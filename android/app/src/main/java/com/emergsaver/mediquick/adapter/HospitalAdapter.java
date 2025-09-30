package com.emergsaver.mediquick.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.emergsaver.mediquick.DetailHospitalActivity;
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

        if (hospital.getDistanceToUser() >= 0) {
            holder.distance.setText(String.format("%.1f km", hospital.getDistanceToUser()));
        } else {
            holder.distance.setText("");
        }

        holder.address.setText(hospital.getAddress());
        holder.phone.setText(hospital.getPhone());

        // 🔹 초기 혼잡도 0명 원활
        updateCongestion(holder, 0);

        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, DetailHospitalActivity.class);
            intent.putExtra("hospital", hospital);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return hospitals.size();
    }

    // 🔹 혼잡도 업데이트 메서드
    public void updateCongestion(ViewHolder holder, int peopleCount) {
        String congestionStatus;
        int color;

        if (peopleCount <= 20) {
            congestionStatus = "원활";
            color = holder.itemView.getResources().getColor(R.color.lime_green);
        } else if (peopleCount <= 40) {
            congestionStatus = "보통";
            color = holder.itemView.getResources().getColor(R.color.orange);
        } else {
            congestionStatus = "혼잡";
            color = holder.itemView.getResources().getColor(R.color.red);
        }

        holder.congestionDot.setBackgroundColor(color);
        holder.congestionText.setText(congestionStatus + " (" + peopleCount + " 명)");
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, distance, address, phone, congestionText;
        View congestionDot;

        ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.text_name);
            distance = itemView.findViewById(R.id.text_distance);
            address = itemView.findViewById(R.id.text_address);
            phone = itemView.findViewById(R.id.text_phone);
            congestionDot = itemView.findViewById(R.id.view_congestion_dot);  // View 타입
            congestionText = itemView.findViewById(R.id.text_congestion);    // TextView 타입
        }
    }
}
