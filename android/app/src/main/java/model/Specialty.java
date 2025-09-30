package model;

import java.io.Serializable;

public class Specialty implements Serializable {
    private int id;
    private String dept_code;
    private String dept_name;
    private int doctor_count;

    public int getDoctor_count() {
        return doctor_count;
    }

    public String getDept_name() {
        return dept_name;
    }

    public void setDoctor_count(int doctor_count) {
        this.doctor_count = doctor_count;
    }

    public void setDept_name(String dept_name) {
        this.dept_name = dept_name;
    }
}
