package com.example.healsphere;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class ProfileSelectionActivity extends AppCompatActivity {

    FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_selection);

        firestore = FirebaseFirestore.getInstance();
        LinearLayout layoutProfiles = findViewById(R.id.layoutProfiles);
        Button btnCreateProfile = findViewById(R.id.btnCreateProfile);

        firestore.collection("users").get().addOnSuccessListener(query -> {
            layoutProfiles.removeAllViews();

            if (query.isEmpty()) {
                Toast.makeText(this, "No saved profiles found. Please create one.", Toast.LENGTH_SHORT).show();
            } else {
                for (var doc : query.getDocuments()) {
                    Map<String, Object> user = doc.getData();
                    if (user == null) continue;

                    String name = (String) user.get("name");
                    String age = (String) user.get("age");
                    String contact = (String) user.get("contact");

                    TextView tv = new TextView(this);
                    tv.setText("👤 " + name + " (Age: " + age + ", Contact: " + contact + ")");
                    tv.setPadding(25, 25, 25, 25);
                    tv.setTextSize(16);
                    tv.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
                    tv.setOnClickListener(v -> {
                        getSharedPreferences("HealSpherePrefs", MODE_PRIVATE)
                                .edit()
                                .putString("currentProfileId", doc.getId())
                                .apply();

                        Toast.makeText(this, name + " selected as current profile", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    });

                    layoutProfiles.addView(tv);
                }
            }
        });

        btnCreateProfile.setOnClickListener(v -> {
            showCreateProfileDialog();
        });
    }

    private void showCreateProfileDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Create New Profile");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        EditText etName = new EditText(this);
        etName.setHint("Enter Name");
        layout.addView(etName);

        EditText etAge = new EditText(this);
        etAge.setHint("Enter Age");
        layout.addView(etAge);

        EditText etContact = new EditText(this);
        etContact.setHint("Enter Contact");
        layout.addView(etContact);

        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String age = etAge.getText().toString().trim();
            String contact = etContact.getText().toString().trim();

            if (name.isEmpty() || age.isEmpty() || contact.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> newProfile = new HashMap<>();
            newProfile.put("name", name);
            newProfile.put("age", age);
            newProfile.put("contact", contact);
            newProfile.put("photoUrl", "");

            firestore.collection("users")
                    .add(newProfile)
                    .addOnSuccessListener(docRef -> {
                        SharedPreferences prefs = getSharedPreferences("HealSpherePrefs", MODE_PRIVATE);
                        prefs.edit().putString("currentProfileId", docRef.getId()).apply();

                        Toast.makeText(this, "Profile created successfully!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(ProfileSelectionActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
