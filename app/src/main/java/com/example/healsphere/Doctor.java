package com.example.healsphere;

public class Doctor {
    private String name;
    private String speciality;
    private String phone;
    private String whatsapp;
    private String email;
    private String pharmacyUrl;
    private Long nextAppointment;

    public Doctor() {}

    public Doctor(String name, String speciality, String phone, String whatsapp,
                  String email, String pharmacyUrl, Long nextAppointment) {
        this.name = name;
        this.speciality = speciality;
        this.phone = phone;
        this.whatsapp = whatsapp;
        this.email = email;
        this.pharmacyUrl = pharmacyUrl;
        this.nextAppointment = nextAppointment;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpeciality() { return speciality; }
    public void setSpeciality(String speciality) { this.speciality = speciality; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getWhatsapp() { return whatsapp; }
    public void setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPharmacyUrl() { return pharmacyUrl; }
    public void setPharmacyUrl(String pharmacyUrl) { this.pharmacyUrl = pharmacyUrl; }

    public Long getNextAppointment() { return nextAppointment; }
    public void setNextAppointment(Long nextAppointment) { this.nextAppointment = nextAppointment; }
}
