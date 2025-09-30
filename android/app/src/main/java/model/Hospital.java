package model;

import java.util.List;
import java.io.Serializable;

public class Hospital implements Serializable{
    private String id;
    private String hospital_name;
    private String phone;
    private String address;
    private double latitude;
    private double longitude;
    private int doctor_count;
    private int currentPeople;
    private List<Specialty> specialties;

    private double distanceToUser;
    // 기본 생성자
    public Hospital() {
    }


    public Hospital(String id, String hospital_name, String phone, String address, Double latitude, Double longitude, int doctor_count) {
        this.id = id;
        this.hospital_name = hospital_name;
        this.phone = phone;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.doctor_count = doctor_count;
    }

    // getter 생성
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getDistanceToUser() {
        return distanceToUser;
    }
    public void setDistanceToUser(double distanceToUser) {
        this.distanceToUser = distanceToUser;
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

    public List<Specialty> getSpecialties() {
        return specialties;
    }

    public void setSpecialties(List<Specialty> specialties) {
        this.specialties = specialties;
    }

    public int getDoctor_count() {
        return doctor_count;
    }

    public void setDoctor_count(int doctor_count) {
        this.doctor_count = doctor_count;
    }

    public int getCurrentPeople() {
        return currentPeople;
    }

    public void setCurrentPeople(int currentPeople) {
        this.currentPeople = currentPeople;
    }
}
