package com.example.healsphere;

public class Medicine {
    private String name;
    private String dosage;
    private String time;

    public Medicine() {}

    public Medicine(String name, String dosage, String time) {
        this.name = name;
        this.dosage = dosage;
        this.time = time;
    }

    public String getName() { return name; }
    public String getDosage() { return dosage; }
    public String getTime() { return time; }
}
