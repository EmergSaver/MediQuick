package repository;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Integer> peopleCounts = new ArrayList<>();
                    if(!queryDocumentSnapshots.isEmpty()) {
                        var document = queryDocumentSnapshots.getDocuments().get(0);
                        Object countObj = document.get("people_count");
                        Object time = document.get("datetime");

                        if (time instanceof com.google.firebase.Timestamp) {
                            com.google.firebase.Timestamp ts = (com.google.firebase.Timestamp) time;
                            Date date = ts.toDate();
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA);
                            String formattedTime = sdf.format(date);

                            Log.d("ANALYSIS_LOG", "Raw people_count object: " + countObj);
                            Log.d("ANALYSIS_LOG", "Formatted Time: " + formattedTime);
                        } else {
                            Log.d("ANALYSIS_LOG", "datetime is not a Timestamp: " + time);
                        }

                        if(countObj != null) {
                            callback.onLoaded(countObj);
                        }
                    }else {
                        Log.d("ANALYSIS_LOG", "No analysis data found");
                    }
                })
                .addOnFailureListener(e -> Log.e("ANALYSIS_LOG", "Error fetching analysis", e));
    }
}