package com.emergsaver.mediquick.search;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.emergsaver.mediquick.R;
import com.emergsaver.mediquick.adapter.CategoryAdapter;

import java.util.Arrays;
import java.util.List;

import model.Category;

public class SearchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        RecyclerView recycler = findViewById(R.id.recycler_categories);
        recycler.setLayoutManager(new GridLayoutManager(this, 3)); // 한 줄에 3개씩

        // 카테고리 목록 데이터
        List<model.Category> categories = Arrays.asList(
                new Category(R.drawable.ic_launcher_foreground, "신경계"),
                new Category(R.drawable.ic_launcher_foreground, "눈"),
                new Category(R.drawable.ic_launcher_foreground, "코/귀"),
                new Category(R.drawable.ic_launcher_foreground, "입/목/얼굴"),
                new Category(R.drawable.ic_launcher_foreground, "호흡기계"),
                new Category(R.drawable.ic_launcher_foreground, "심혈관계"),
                new Category(R.drawable.ic_launcher_foreground, "소화기계"),
                new Category(R.drawable.ic_launcher_foreground, "비뇨기계"),
                new Category(R.drawable.ic_launcher_foreground, "피부"),
                new Category(R.drawable.ic_launcher_foreground, "임신/여성생식계"),
                new Category(R.drawable.ic_launcher_foreground, "근골격계"),
                new Category(R.drawable.ic_launcher_foreground, "정신건강"),
                new Category(R.drawable.ic_launcher_foreground, "물질오용"),
                new Category(R.drawable.ic_launcher_foreground, "몸통외상"),
                new Category(R.drawable.ic_launcher_foreground, "일반")
        );

        // 어댑터 연결
        CategoryAdapter adapter = new CategoryAdapter(categories, item -> {
            Toast.makeText(this, item.getName() + " 선택됨", Toast.LENGTH_SHORT).show();
            // TODO: 여기서 item 클릭 시 증상 선택 화면으로 이동하면 됨
        });

        recycler.setAdapter(adapter);
    }
}
