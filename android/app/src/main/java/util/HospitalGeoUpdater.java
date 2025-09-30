package util;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import model.Hospital;

// DB의 주소를 위도/경도로 변환해서 저장
public class HospitalGeoUpdater {
    private FirebaseFirestore db;
    private Context context;

    public HospitalGeoUpdater(Context context) {
        this.context = context;
        db = FirebaseFirestore.getInstance();
    }

    public void updateHospitalCoordinates() {
        db.collection("hospitals")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (var document : queryDocumentSnapshots.getDocuments()) {
                        String jsonStr = document.getString("hospital_json");
                        if (jsonStr != null) {
                            Gson gson = new Gson();
                            Hospital hospital = gson.fromJson(jsonStr, Hospital.class);

                            String address = hospital.getAddress();
                            if (address != null && !address.isEmpty()) {
                                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                                try {
                                    List<Address> addressList = geocoder.getFromLocationName(address, 1);
                                    if (!addressList.isEmpty()) {
                                        Address loc = addressList.get(0);
                                        hospital.setLatitude(loc.getLatitude());
                                        hospital.setLongitude(loc.getLongitude());

                                        Log.d("GeoCoder", "변환 성공: " + address +
                                                " -> 위도: " + loc.getLatitude() +
                                                ", 경도: " + loc.getLongitude());

                                        db.collection("hospitals")
                                                .document(document.getId())
                                                .update("latitude", loc.getLatitude(),
                                                        "longitude", loc.getLongitude())
                                                .addOnSuccessListener(aVoid ->
                                                        Log.d("FIREBASE", hospital.getHospital_name() + " 좌표 저장 완료"))
                                                .addOnFailureListener(e ->
                                                        Log.e("FIREBASE", "좌표 저장 실패", e));
                                    }
                                    Thread.sleep(200);
                                } catch (IOException | InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                });
    }
}
