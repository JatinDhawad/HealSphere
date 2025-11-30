package com.example.healsphere;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DoctorsActivity extends AppCompatActivity {

    private EditText etDocName, etSpeciality, etPhone, etWhatsApp, etEmail, etPharmacyUrl;
    private Button btnSaveDoctor, btnAddAppointment;
    private LinearLayout layoutDoctorsList;
    private FirebaseFirestore firestore;
    private Long chosenAppointmentMillis = null;

    private ActivityResultLauncher<String> requestCallPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctors);

        firestore = FirebaseFirestore.getInstance();

        etDocName = findViewById(R.id.etDocName);
        etSpeciality = findViewById(R.id.etSpeciality);
        etPhone = findViewById(R.id.etPhone);
        etWhatsApp = findViewById(R.id.etWhatsApp);
        etEmail = findViewById(R.id.etEmail);
        etPharmacyUrl = findViewById(R.id.etPharmacyUrl);

        btnAddAppointment = findViewById(R.id.btnAddAppointment);
        btnSaveDoctor = findViewById(R.id.btnSaveDoctor);
        layoutDoctorsList = findViewById(R.id.layoutDoctorsList);

        requestCallPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (!granted) {
                        Toast.makeText(this, "Call permission denied", Toast.LENGTH_SHORT).show();
                    }
                });

        btnAddAppointment.setOnClickListener(v -> showDateTimePicker());
        btnSaveDoctor.setOnClickListener(v -> saveDoctor());

        loadDoctors();
    }

    private void showDateTimePicker() {
        final Calendar cal = Calendar.getInstance();
        DatePickerDialog dp = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    cal.set(Calendar.YEAR, year);
                    cal.set(Calendar.MONTH, month);
                    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    TimePickerDialog tp = new TimePickerDialog(this,
                            (TimePicker view1, int hourOfDay, int minute) -> {
                                cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                cal.set(Calendar.MINUTE, minute);
                                cal.set(Calendar.SECOND, 0);
                                chosenAppointmentMillis = cal.getTimeInMillis();

                                Toast.makeText(this, "Appointment set", Toast.LENGTH_SHORT).show();
                            },
                            cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true);
                    tp.show();

                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dp.show();
    }

    private void saveDoctor() {
        String name = etDocName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            etDocName.setError("Enter name");
            return;
        }
        String speciality = etSpeciality.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String whatsapp = etWhatsApp.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String pharmacyUrl = etPharmacyUrl.getText().toString().trim();

        String profileId = getSharedPreferences("HealSpherePrefs", MODE_PRIVATE)
                .getString("currentProfileId", null);
        if (profileId == null) {
            Toast.makeText(this, "No profile selected", Toast.LENGTH_SHORT).show();
            return;
        }

        String docId = "doc_" + UUID.randomUUID().toString();

        Doctor doctor = new Doctor(name,
                speciality,
                phone,
                whatsapp,
                email,
                pharmacyUrl.isEmpty() ? null : pharmacyUrl,
                chosenAppointmentMillis);

        firestore.collection("users")
                .document(profileId)
                .collection("doctors")
                .document(docId)
                .set(doctor)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(DoctorsActivity.this, "Doctor saved", Toast.LENGTH_SHORT).show();
                    clearInputs();
                    chosenAppointmentMillis = null;
                    loadDoctors(); // refresh list
                })
                .addOnFailureListener(e -> Toast.makeText(DoctorsActivity.this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void clearInputs() {
        etDocName.setText("");
        etSpeciality.setText("");
        etPhone.setText("");
        etWhatsApp.setText("");
        etEmail.setText("");
        etPharmacyUrl.setText("");
    }

    private void loadDoctors() {
        layoutDoctorsList.removeAllViews();

        String profileId = getSharedPreferences("HealSpherePrefs", MODE_PRIVATE)
                .getString("currentProfileId", null);
        if (profileId == null) return;

        firestore.collection("users")
                .document(profileId)
                .collection("doctors")
                .get()
                .addOnSuccessListener(query -> {
                    for (var doc : query.getDocuments()) {
                        Doctor d = doc.toObject(Doctor.class);
                        if (d == null) continue;
                        addDoctorCard(doc.getId(), d);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Load failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void addDoctorCard(String docId, Doctor d) {
        View card = getLayoutInflater().inflate(R.layout.item_doctor, layoutDoctorsList, false);

        TextView tvName = card.findViewById(R.id.tvDocName);
        TextView tvSpec = card.findViewById(R.id.tvDocSpec);
        TextView tvPhone = card.findViewById(R.id.tvDocPhone);
        Button btnCall = card.findViewById(R.id.btnCallDoc);
        Button btnWhatsApp = card.findViewById(R.id.btnWhatsAppDoc);
        Button btnRefill = card.findViewById(R.id.btnRefill);
        Button btnAddToCalendar = card.findViewById(R.id.btnAddToCalendar);

        tvName.setText(d.getName());
        tvSpec.setText(d.getSpeciality() != null ? d.getSpeciality() : "");
        tvPhone.setText(d.getPhone() != null ? d.getPhone() : "No phone");

        btnCall.setOnClickListener(v -> {
            if (d.getPhone() == null || d.getPhone().isEmpty()) {
                Toast.makeText(this, "No phone number", Toast.LENGTH_SHORT).show();
                return;
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                requestCallPermissionLauncher.launch(Manifest.permission.CALL_PHONE);
                Toast.makeText(this, "Please grant call permission and try again", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + d.getPhone()));
            startActivity(intent);
        });

        btnWhatsApp.setOnClickListener(v -> {
            String wa = d.getWhatsapp();
            if (wa == null || wa.isEmpty()) {
                Toast.makeText(this, "No WhatsApp number", Toast.LENGTH_SHORT).show();
                return;
            }
            String phoneWithNoPlus = wa.replace("+", "").replace(" ", "");
            try {
                String url = "https://api.whatsapp.com/send?phone=" + phoneWithNoPlus + "&text=" + Uri.encode("Hello " + d.getName());
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            } catch (Exception e) {
                Toast.makeText(this, "WhatsApp not available", Toast.LENGTH_SHORT).show();
            }
        });

        btnRefill.setOnClickListener(v -> {
            if (d.getPharmacyUrl() != null && !d.getPharmacyUrl().isEmpty()) {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(d.getPharmacyUrl()));
                startActivity(i);
            } else if (d.getEmail() != null && !d.getEmail().isEmpty()) {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + d.getEmail()));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Prescription refill request");
                startActivity(Intent.createChooser(emailIntent, "Send email"));
            } else {
                Toast.makeText(this, "No pharmacy URL or email set", Toast.LENGTH_SHORT).show();
            }
        });

        btnAddToCalendar.setOnClickListener(v -> {
            if (d.getNextAppointment() == null) {
                Toast.makeText(this, "No appointment set for this doctor", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_INSERT)
                    .setData(CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.Events.TITLE, "Appointment: " + d.getName())
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, d.getNextAppointment())
                    .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, d.getNextAppointment() + 30 * 60 * 1000L);
            startActivity(intent);
        });

        layoutDoctorsList.addView(card);
    }
}
