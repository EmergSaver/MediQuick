package com.emergsaver.mediquick.search;

public class Category {
    private int iconResId; // 아이콘 이미지 리소스 ID
    private String name;   // 버튼 이름(예: "신경계")

    public Category(int iconResId, String name) {
        this.iconResId = iconResId;
        this.name = name;
    }

    public int getIconResId() {
        return iconResId;
    }

    public String getName() {
        return name;
    }
}
