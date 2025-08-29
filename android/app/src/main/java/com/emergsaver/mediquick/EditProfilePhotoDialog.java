package com.emergsaver.mediquick;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import android.app.Dialog;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup; // 이미 있을 수도 있지만, 혹시 없다면 추가

public class EditProfilePhotoDialog extends DialogFragment {

    private ImageView ivProfilePhotoPreview;
    private EditText etNameDialog;
    private Button btnUploadPhotoDialog;
    private Button btnSave;

    // Firebase
    private FirebaseFirestore db;
    private String userUid;

    private ActivityResultLauncher<String> getContentLauncher;
    private Uri selectedImageUri;

    // ProfileFragment에서 userUid를 받기 위한 newInstance 메소드 추가
    public static EditProfilePhotoDialog newInstance(String userUid) {
        EditProfilePhotoDialog dialog = new EditProfilePhotoDialog();
        Bundle args = new Bundle();
        args.putString("userUid", userUid);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            userUid = getArguments().getString("userUid");
        }

        getContentLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        ivProfilePhotoPreview.setImageURI(uri);
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_profile_photo_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ivProfilePhotoPreview = view.findViewById(R.id.iv_profile_photo_preview);
        etNameDialog = view.findViewById(R.id.et_name_dialog);
        btnUploadPhotoDialog = view.findViewById(R.id.btn_upload_photo_dialog);
        btnSave = view.findViewById(R.id.btn_save);

        // 기존 프로필 정보 불러오기
        loadUserProfileData();

        btnUploadPhotoDialog.setOnClickListener(v -> {
            getContentLauncher.launch("image/*");
        });

        btnSave.setOnClickListener(v -> {
            saveUserProfileData();
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        // 팝업 창의 가로와 세로 크기를 설정하는 코드
        if (getDialog() != null) {
            Dialog dialog = getDialog();
            Window window = dialog.getWindow();
            if (window != null) {
                WindowManager.LayoutParams params = window.getAttributes();

                // 가로를 화면 전체 폭의 90%로 설정
                params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);

                // 높이를 WRAP_CONTENT로 설정 (내용에 맞게 자동 조절)
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;

                window.setAttributes(params);
            }
        }
    }

    // 기존 프로필 정보 불러오는 메소드 (다이얼로그 열릴 때 실행)
    private void loadUserProfileData() {
        if (userUid == null) {
            Toast.makeText(getContext(), "사용자 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(userUid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        Blob profileImageBlob = documentSnapshot.getBlob("profileImage");

                        if (name != null) {
                            etNameDialog.setText(name);
                        }
                        if (profileImageBlob != null && getContext() != null) {
                            try {
                                byte[] imageData = profileImageBlob.toBytes();
                                Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                                ivProfilePhotoPreview.setImageBitmap(bitmap);
                            } catch (Exception e) {
                                Toast.makeText(getContext(), "이미지 디코딩 실패", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "프로필 정보 불러오기 실패", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveUserProfileData() {
        String updatedName = etNameDialog.getText().toString().trim();
        if (updatedName.isEmpty()) {
            Toast.makeText(getContext(), "이름을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userUid == null) {
            Toast.makeText(getContext(), "사용자 ID를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", updatedName);

        // 프로필 사진이 선택된 경우
        if (selectedImageUri != null) {
            try {
                // 이미지를 바이트 배열로 변환
                InputStream inputStream = requireActivity().getContentResolver().openInputStream(selectedImageUri);
                ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
                int bufferSize = 1024;
                byte[] buffer = new byte[bufferSize];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    byteBuffer.write(buffer, 0, len);
                }
                byte[] imageData = byteBuffer.toByteArray();

                // Firestore Blob 타입으로 변환 후 저장
                Blob imageBlob = Blob.fromBytes(imageData);
                updates.put("profileImage", imageBlob);

                // Firestore에 데이터 업데이트
                updateFirestore(updates);

            } catch (Exception e) {
                Toast.makeText(getContext(), "이미지 변환 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            // 사진이 선택되지 않은 경우, 이름만 업데이트
            updateFirestore(updates);
        }
    }

    private void updateFirestore(Map<String, Object> updates) {
        db.collection("users").document(userUid)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "프로필이 성공적으로 업데이트되었습니다.", Toast.LENGTH_SHORT).show();

                    // Fragment Result API로 결과 전달
                    Bundle result = new Bundle();
                    result.putString("updatedName", (String) updates.get("name"));
                    if (updates.containsKey("profileImage")) {
                        // 바이트 배열을 직접 전달하지 않고, 업데이트가 성공했음을 알리는 플래그 전달
                        result.putBoolean("photoUpdated", true);
                    }
                    getParentFragmentManager().setFragmentResult("profilePhotoRequestKey", result);
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "프로필 업데이트 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}