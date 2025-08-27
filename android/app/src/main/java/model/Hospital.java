package model;

public class Hospital {
    private String name;
    private String phone;
    private double latitude;
    private double longitude;
    // 추가적으로 상세정보도 추후 추가

    public Hospital(String name, String phone, Double latitude, Double longitude) {
        this.name = name;
        this.phone = phone;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // getter 생성
    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
