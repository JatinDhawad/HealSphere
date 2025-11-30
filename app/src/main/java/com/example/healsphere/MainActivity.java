package com.example.healsphere;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Button;
import com.bumptech.glide.Glide;
import android.view.View;
import android.content.SharedPreferences;
import android.widget.TextView;
import android.Manifest;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.healsphere.databinding.ActivityMainBinding;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import android.content.pm.PackageManager;


public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    FirebaseFirestore firestore;
    FirebaseStorage storage;
    Uri imageUri;
    int PICK_IMAGE = 1001;
    int PICK_PROFILE_IMAGE = 1002;
    Uri profileImageUri;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("HealSpherePrefs", MODE_PRIVATE);
        String currentProfileId = prefs.getString("currentProfileId", null);

        if (currentProfileId == null) {
            startActivity(new Intent(this, ProfileSelectionActivity.class));
            finish();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        HealthTipsManager.scheduleDailyTip(this);
        sendBroadcast(new Intent(this, HealthTipReceiver.class));



        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        TextView tvCurrentProfile = findViewById(R.id.tvCurrentProfile);
        if (currentProfileId != null) {
            firestore.collection("users").document(currentProfileId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String name = doc.getString("name");
                            tvCurrentProfile.setText("Current Profile: " + name);
                        } else {
                            tvCurrentProfile.setText("No active profile found");
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }


        checkExactAlarmPermission();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }


        binding.btnAttach.setOnClickListener(v -> pickImage());
        binding.btnSave.setOnClickListener(v -> saveMedicine());
        binding.btnView.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ViewMedicinesActivity.class)));

        ImageView imgProfile = findViewById(R.id.imgProfile);
        EditText etUserName = findViewById(R.id.etUserName);
        EditText etUserAge = findViewById(R.id.etUserAge);
        EditText etUserContact = findViewById(R.id.etUserContact);
        ImageView icEditProfile = findViewById(R.id.icEditProfile);


        loadUserProfile(etUserName, etUserAge, etUserContact, imgProfile);
        EditText etEmergencyContact = findViewById(R.id.etEmergencyContact);
        setFieldsEnabled(false, etUserName, etUserAge, etUserContact, etEmergencyContact);





        Button btnEditProfile = findViewById(R.id.btnEditProfile);

        btnEditProfile.setOnClickListener(v -> {

            if (etUserName.isEnabled()) {
                saveUserProfile(etUserName, etUserAge, etUserContact, imgProfile);
                setFieldsEnabled(false, etUserName, etUserAge, etUserContact, etEmergencyContact);
                btnEditProfile.setText("Edit Profile");

                icEditProfile.setVisibility(View.GONE);
                imgProfile.setOnClickListener(null);

            } else {
                setFieldsEnabled(true, etUserName, etUserAge, etUserContact, etEmergencyContact);
                btnEditProfile.setText("Save Changes");
                Toast.makeText(this, "You can now edit your profile", Toast.LENGTH_SHORT).show();

                icEditProfile.setVisibility(View.VISIBLE);
                imgProfile.setOnClickListener(x -> pickProfileImage());
            }
        });


        Button btnSwitchProfile = findViewById(R.id.btnSwitchProfile);
        btnSwitchProfile.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileSelectionActivity.class);
            startActivity(intent);
            finish();
        });

        binding.btnDailyReport.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, DailyReportActivity.class)));

        Button btnDoctors = findViewById(R.id.btnDoctors);
        btnDoctors.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, DoctorsActivity.class)));

        Button btnCallEmergency = findViewById(R.id.btnCallEmergency);
        Button btnSendSOS = findViewById(R.id.btnSendSOS);


        btnCallEmergency.setOnClickListener(v -> {
            String phone = etEmergencyContact.getText().toString();
            if (phone.isEmpty()) {
                Toast.makeText(this, "Please enter emergency contact first", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + phone));

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, 200);
            } else {
                startActivity(intent);
            }
        });

        btnSendSOS.setOnClickListener(v -> {
            String phone = etEmergencyContact.getText().toString();
            if (phone.isEmpty()) {
                Toast.makeText(this, "Please enter emergency contact first", Toast.LENGTH_SHORT).show();
                return;
            }

            String message = "🚨 This is an emergency! I need help. Please contact me immediately.";
            Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
            smsIntent.setData(Uri.parse("smsto:" + phone));
            smsIntent.putExtra("sms_body", message);
            startActivity(smsIntent);
        });

        TextView tvWaterCount = findViewById(R.id.tvWaterCount);
        Button btnAddWater = findViewById(R.id.btnAddWater);
        Button btnResetWater = findViewById(R.id.btnResetWater);

        String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String lastDate = prefs.getString("lastWaterDate", "");

        if (!today.equals(lastDate)) {
            prefs.edit()
                    .putString("lastWaterDate", today)
                    .putInt("waterCount", 0)
                    .apply();
        }

        int waterCount = prefs.getInt("waterCount", 0);
        tvWaterCount.setText("Glasses taken: " + waterCount + " / 8");

        btnAddWater.setOnClickListener(v -> {
            int current = prefs.getInt("waterCount", 0);
            if (current < 8) {
                current++;
                prefs.edit().putInt("waterCount", current).apply();
                tvWaterCount.setText("Glasses taken: " + current + " / 8");
                Toast.makeText(this, "Great! Stay hydrated 💧", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "You've reached your goal! 🏆", Toast.LENGTH_SHORT).show();
            }
        });

        btnResetWater.setOnClickListener(v -> {
            prefs.edit().putInt("waterCount", 0).apply();
            tvWaterCount.setText("Glasses taken: 0 / 8");
            Toast.makeText(this, "Counter reset!", Toast.LENGTH_SHORT).show();
        });

        EditText etBP = findViewById(R.id.etBP);
        EditText etSugar = findViewById(R.id.etSugar);
        EditText etWeight = findViewById(R.id.etWeight);
        Button btnSaveVitals = findViewById(R.id.btnSaveVitals);

        btnSaveVitals.setOnClickListener(v -> {
            String bp = etBP.getText().toString().trim();
            String sugarStr = etSugar.getText().toString().trim();
            String weightStr = etWeight.getText().toString().trim();

            if (bp.isEmpty() && sugarStr.isEmpty() && weightStr.isEmpty()) {
                Toast.makeText(this, "Please enter at least one vital!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentProfileId == null) {
                Toast.makeText(this, "No profile selected!", Toast.LENGTH_SHORT).show();
                return;
            }

            int systolic = 0, diastolic = 0;
            if (!bp.isEmpty() && bp.contains("/")) {
                try {
                    String[] parts = bp.split("/");
                    systolic = Integer.parseInt(parts[0].trim());
                    diastolic = Integer.parseInt(parts[1].trim());
                } catch (Exception e) {
                    Toast.makeText(this, "Invalid BP format! Use 120/80", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            Double sugar = sugarStr.isEmpty() ? null : Double.parseDouble(sugarStr);
            Double weight = weightStr.isEmpty() ? null : Double.parseDouble(weightStr);

            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

            Map<String, Object> vitals = new HashMap<>();
            if (systolic > 0 && diastolic > 0) {
                vitals.put("systolic", systolic);
                vitals.put("diastolic", diastolic);
            }
            if (sugar != null) vitals.put("glucose", sugar);
            if (weight != null) vitals.put("weight", weight);

            firestore.collection("users")
                    .document(currentProfileId)
                    .collection("vitals")
                    .document(date)
                    .set(vitals, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Vitals saved successfully!", Toast.LENGTH_SHORT).show();
                        etBP.setText("");
                        etSugar.setText("");
                        etWeight.setText("");
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

    }


    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            if (requestCode == PICK_IMAGE) {
                imageUri = data.getData();
                binding.tvImageStatus.setText("Image Selected");
            } else if (requestCode == PICK_PROFILE_IMAGE) {
                profileImageUri = data.getData();
                ImageView imgProfile = findViewById(R.id.imgProfile);
                imgProfile.setImageURI(profileImageUri);
            }
        }
    }


    private void saveMedicine() {
        String name   = binding.etName.getText().toString();
        String dosage = binding.etDosage.getText().toString();
        String time   = binding.etTime.getText().toString();
        String notes  = binding.etNotes.getText().toString();

        if (name.isEmpty() || time.isEmpty()) {
            Toast.makeText(this, "Please enter medicine name and time", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUri != null) {
            StorageReference ref = storage.getReference("prescriptions/" + System.currentTimeMillis());
            ref.putFile(imageUri)
                    .addOnSuccessListener(task -> ref.getDownloadUrl()
                            .addOnSuccessListener(uri -> saveToFirestore(name, dosage, time, notes, uri.toString())));
        } else {
            saveToFirestore(name, dosage, time, notes, "");
        }
    }

    private void saveToFirestore(String name, String dosage, String time, String notes, String url) {
        SharedPreferences prefs = getSharedPreferences("HealSpherePrefs", MODE_PRIVATE);
        String currentProfileId = prefs.getString("currentProfileId", null);

        if (currentProfileId == null) {
            Toast.makeText(this, "No profile selected!", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> medicine = new HashMap<>();
        medicine.put("name", name);
        medicine.put("dosage", dosage);
        medicine.put("time", time);
        medicine.put("notes", notes);
        medicine.put("prescriptionUrl", url);

        firestore.collection("users")
                .document(currentProfileId)
                .collection("medicines")
                .add(medicine)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Medicine saved under your profile!", Toast.LENGTH_SHORT).show();
                    scheduleReminder(name, dosage, time);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    private void scheduleReminder(String name, String dosage, String time) {
        try {
            String[] parts = time.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            cal.set(Calendar.SECOND, 0);

            if (cal.before(Calendar.getInstance())) {
                cal.add(Calendar.DAY_OF_MONTH, 1);
            }

            Intent i = new Intent(this, ReminderReceiver.class);
            i.putExtra("name", name);
            i.putExtra("dosage", dosage);

            PendingIntent pi = PendingIntent.getBroadcast(
                    this,
                    1001,
                    i,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

            am.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);

            Toast.makeText(this, "Reminder set for " +
                    String.format("%02d:%02d", hour, minute), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void checkExactAlarmPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (!am.canScheduleExactAlarms()) {
                Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
                Toast.makeText(this, "Please allow HealSphere to set exact alarms.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void pickProfileImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_PROFILE_IMAGE);
    }

    private void saveUserProfile(EditText etUserName, EditText etUserAge, EditText etUserContact, ImageView imgProfile) {
        String name = etUserName.getText().toString();
        String age = etUserAge.getText().toString();
        String contact = etUserContact.getText().toString();

        String emergencyContact = ((EditText) findViewById(R.id.etEmergencyContact)).getText().toString();
        if (emergencyContact.isEmpty()) {
            Toast.makeText(this, "Please enter emergency contact", Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String, Object> user = new HashMap<>();

        user.put("emergencyContact", emergencyContact);


        if (name.isEmpty() || age.isEmpty() || contact.isEmpty()) {
            Toast.makeText(this, "Please fill all profile details", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Saving profile...", Toast.LENGTH_SHORT).show();

        SharedPreferences prefs = getSharedPreferences("HealSpherePrefs", MODE_PRIVATE);
        String currentProfileId = prefs.getString("currentProfileId", null);

        if (currentProfileId == null) {
            currentProfileId = "user_" + System.currentTimeMillis();
            prefs.edit().putString("currentProfileId", currentProfileId).apply();
        }

        final String finalProfileId = currentProfileId;

        if (profileImageUri != null) {
            StorageReference ref = storage.getReference("profile_photos/" + finalProfileId + ".jpg");
            ref.putFile(profileImageUri)
                    .addOnSuccessListener(task ->
                            ref.getDownloadUrl().addOnSuccessListener(uri -> {
                                saveProfileData(finalProfileId, name, age, contact, uri.toString());
                            }))
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            saveProfileData(finalProfileId, name, age, contact, null);
        }
    }


    private void saveProfileData(String userId, String name, String age, String contact, String photoUrl) {
        EditText etEmergencyContact = findViewById(R.id.etEmergencyContact);
        String emergencyContact = etEmergencyContact.getText().toString();

        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("age", age);
        user.put("contact", contact);
        user.put("emergencyContact", emergencyContact);
        if (photoUrl != null) user.put("photoUrl", photoUrl);

        firestore.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile saved successfully!", Toast.LENGTH_SHORT).show();
                    TextView tvCurrentProfile = findViewById(R.id.tvCurrentProfile);
                    tvCurrentProfile.setText("Current Profile: " + name);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }




    private void loadUserProfile(EditText etUserName, EditText etUserAge, EditText etUserContact, ImageView imgProfile) {
        SharedPreferences prefs = getSharedPreferences("HealSpherePrefs", MODE_PRIVATE);
        String currentProfileId = prefs.getString("currentProfileId", null);

        if (currentProfileId == null) {
            Toast.makeText(this, "No active profile selected!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, ProfileSelectionActivity.class));
            finish();
            return;
        }

        firestore.collection("users")
                .document(currentProfileId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String age = documentSnapshot.getString("age");
                        String contact = documentSnapshot.getString("contact");
                        String photoUrl = documentSnapshot.getString("photoUrl");
                        String emergencyContact = documentSnapshot.getString("emergencyContact");

                        etUserName.setText(name);
                        etUserAge.setText(age);
                        etUserContact.setText(contact);

                        EditText etEmergencyContact = findViewById(R.id.etEmergencyContact);
                        if (emergencyContact != null) {
                            etEmergencyContact.setText(emergencyContact);
                        }

                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(photoUrl)
                                    .placeholder(R.drawable.ic_person)
                                    .into(imgProfile);
                        }

                        TextView tvCurrentProfile = findViewById(R.id.tvCurrentProfile);
                        tvCurrentProfile.setText("Current Profile: " + name);

                        Toast.makeText(this, "Profile loaded: " + name, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void setFieldsEnabled(boolean enabled, EditText... fields) {
        for (EditText e : fields) e.setEnabled(enabled);
    }



}
