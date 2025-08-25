package com.emergsaver.mediquick;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import android.content.Context; // Context를 사용하기 위해 추가
import android.content.SharedPreferences; // SharedPreferences를 사용하기 위해 추가

public class EditProfilePhotoDialog extends DialogFragment {

    private ImageView ivProfilePhotoPreview;
    private EditText etNameDialog;
    private Button btnUploadPhotoDialog;
    private Button btnSave;

    // 갤러리에서 이미지를 선택하기 위한 ActivityResultLauncher
    private ActivityResultLauncher<String> getContentLauncher;
    private Uri selectedImageUri;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 갤러리에서 콘텐츠를 가져오는 ActivityResultLauncher 초기화
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

        btnUploadPhotoDialog.setOnClickListener(v -> {
            // "image/*"는 모든 종류의 이미지 파일을 의미
            getContentLauncher.launch("image/*");
        });

        btnSave.setOnClickListener(v -> {
            String updatedName = etNameDialog.getText().toString();

            // SharedPreferences에 데이터 저장
            SharedPreferences sharedPref = requireActivity().getSharedPreferences("profile_data", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("user_name", updatedName);
            if (selectedImageUri != null) {
                //  URI에 대한 영구적인 접근 권한을 얻습니다.
                requireActivity().getContentResolver().takePersistableUriPermission(
                        selectedImageUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                );


                editor.putString("user_photo_uri", selectedImageUri.toString());
            }
            editor.apply(); // 비동기적으로 저장

            // 결과를 Bundle에 담아 Fragment Result API로 전달
            Bundle result = new Bundle();
            result.putString("updatedName", updatedName);
            if (selectedImageUri != null) {
                result.putString("updatedPhotoUri", selectedImageUri.toString());
            }

            getParentFragmentManager().setFragmentResult("profilePhotoRequestKey", result);
            dismiss();
        });
    }
}