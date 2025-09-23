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

        // 병원 이름
        holder.name.setText(hospital.getHospital_name());

        // 거리 표시 (km)
        if (hospital.getDistanceToUser() >= 0) {
            holder.distance.setText(String.format("%.1f km", hospital.getDistanceToUser()));
        } else {
            holder.distance.setText("");
        }

        // 주소 및 전화번호
        holder.address.setText(hospital.getAddress());
        holder.phone.setText(hospital.getPhone());

        // 🔹 병원 아이템 클릭 시 상세 화면 이동
        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, DetailHospitalActivity.class);
            // Hospital 클래스는 Serializable 구현 필수
            intent.putExtra("hospital", hospital);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return hospitals.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, distance, address, phone;

        ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.text_name);
            distance = itemView.findViewById(R.id.text_distance);
            address = itemView.findViewById(R.id.text_address);
            phone = itemView.findViewById(R.id.text_phone);
        }
    }
}
