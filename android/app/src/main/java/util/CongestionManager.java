package util;

import android.os.Handler;
import android.util.Log;

import repository.CongestionRepository;

public class CongestionManager {
    private CongestionRepository congestionRepository;
    private Handler handler = new Handler();
    private final int REFRESH_INTERVAL_MS = 3 * 60 * 1000;     // 3분
    private OnCongestionUpdateListener listener;

    public interface OnCongestionUpdateListener {
        void onUpdate(Object peopleCount);
        void onError(Exception e);
    }

    public CongestionManager() {
        congestionRepository = new CongestionRepository();
    }

    // 업데이트 시작
    public void startCongestionUpdates(OnCongestionUpdateListener listener) {
        this.listener = listener;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                fetchCongestion();
                handler.postDelayed(this, REFRESH_INTERVAL_MS);
            }
        }, 0); // 첫 실행 바로
    }

    public void stopUpdates() {
        // Handler 해제
        handler.removeCallbacksAndMessages(null);
    }

    // 혼잡도 가져오기
    private void fetchCongestion() {
        congestionRepository.fetchLatestAnalysis(new CongestionRepository.OnAnalysisLoaded() {
            @Override
            public void onLoaded(Object peopleCount) {
                if(listener != null) {
                    listener.onUpdate(peopleCount);
                }
            }

            @Override
            public void onError(Exception e) {
                if(listener != null) {
                    listener.onError(e);
                }
                Log.e("DETAIL_HOSPITAL", "Error fetching congestion", e);
            }
        });
    }

}
