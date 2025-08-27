package model;

public class Hospital {
    private String id;
    private String hospital_name;
    private String phone;
    private String address;
    private double latitude;
    private double longitude;
    // 추가적으로 상세정보도 추후 추가

    // 기본 생성자
    public Hospital() {
    }

    public Hospital(String id, String hospital_name, String phone, String address, Double latitude, Double longitude) {
        this.id = id;
        this.hospital_name = hospital_name;
        this.phone = phone;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // getter 생성
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHospital_name() {
        return hospital_name;
    }

    public void setHospital_name(String hospital_name) {
        this.hospital_name = hospital_name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
