package model;

public class Specialty {
    private int id;
    private String dept_code;
    private String dept_name;
    private int doctor_count;

    public int getDoctor_count() {
        return doctor_count;
    }

    public void setDoctor_count(int doctor_count) {
        this.doctor_count = doctor_count;
    }
}
