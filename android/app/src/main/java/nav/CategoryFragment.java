package nav;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.emergsaver.mediquick.R;
import com.emergsaver.mediquick.adapter.CategoryAdapter;

import java.util.Arrays;
import java.util.List;

import model.Category;

public class CategoryFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // fragment_category.xml 이 레이아웃을 inflate
        View root = inflater.inflate(R.layout.fragment_category, container, false);

        RecyclerView recycler = root.findViewById(R.id.recycler_categories);
        recycler.setLayoutManager(new GridLayoutManager(getContext(), 3));

        List<Category> categories = Arrays.asList(
                new Category(R.drawable.icon_brain, "신경계"),
                new Category(R.drawable.ic_launcher_foreground, "눈"),
                new Category(R.drawable.icon_nose, "코/귀"),
                new Category(R.drawable.ic_launcher_foreground, "입/목/얼굴"),
                new Category(R.drawable.ic_launcher_foreground, "호흡기계"),
                new Category(R.drawable.ic_launcher_foreground, "심혈관계"),
                new Category(R.drawable.ic_launcher_foreground, "소화기계"),
                new Category(R.drawable.ic_launcher_foreground, "비뇨기계"),
                new Category(R.drawable.ic_launcher_foreground, "피부"),
                new Category(R.drawable.ic_launcher_foreground, "임신/여성생식계"),
                new Category(R.drawable.ic_launcher_foreground, "근골격계"),
                new Category(R.drawable.ic_launcher_foreground, "정신건강"),
                new Category(R.drawable.ic_launcher_foreground, "물질오용/중독"),
                new Category(R.drawable.ic_launcher_foreground, "일반")
        );

        CategoryAdapter adapter = new CategoryAdapter(categories, item ->
                Toast.makeText(getContext(), item.getName() + " 선택됨", Toast.LENGTH_SHORT).show()
        );

        recycler.setAdapter(adapter);

        return root;
    }
}
