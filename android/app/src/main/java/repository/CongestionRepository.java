package repository;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class CongestionRepository {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface OnAnalysisLoaded {
        void onLoaded(Object peopleCount);
        void onError(Exception e);
    }

    // 최신 순으로 혼잡도를 가져오기
    public void fetchLatestAnalysis(OnAnalysisLoaded callback) {
        db.collection("analysis")
                .orderBy("datetime", Query.Direction.DESCENDING)    // 최신 시간 순 정렬
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Integer> peopleCounts = new ArrayList<>();
                    for (var document : queryDocumentSnapshots.getDocuments()) {
                        Object countObj = document.get("people_count");

                        Log.d("ANALYSIS_LOG", "Raw people_count object: " + countObj);

                        if (countObj != null) {
                            callback.onLoaded(countObj);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("ANALYSIS_LOG", "Error fetching analysis", e));
    }
}