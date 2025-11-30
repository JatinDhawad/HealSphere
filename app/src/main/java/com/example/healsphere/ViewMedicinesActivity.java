package com.example.healsphere;

import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.os.Environment;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import android.widget.Button;
import android.widget.TextView;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ViewMedicinesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MedicineAdapter adapter;
    private List<Medicine> medicineList = new ArrayList<>();
    private FirebaseFirestore firestore;
    private TextView tvProfileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_medicines);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
        }

        recyclerView = findViewById(R.id.recyclerViewMedicines);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MedicineAdapter(this, medicineList);

        recyclerView.setAdapter(adapter);

        firestore = FirebaseFirestore.getInstance();

        tvProfileName = findViewById(R.id.tvCurrentProfile);

        Button btnExport = findViewById(R.id.btnExportCSV);
        btnExport.setOnClickListener(v -> exportToCSV());

        loadMedicines();
    }

    private void loadMedicines() {
        SharedPreferences prefs = getSharedPreferences("HealSpherePrefs", MODE_PRIVATE);
        String currentProfileId = prefs.getString("currentProfileId", null);

        if (currentProfileId == null) {
            Toast.makeText(this, "No profile selected!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        firestore.collection("users").document(currentProfileId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        if (tvProfileName != null)
                            tvProfileName.setText("Viewing medicines for: " + name);
                    }
                });

        firestore.collection("users")
                .document(currentProfileId)
                .collection("medicines")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    medicineList.clear();
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "No medicines for this profile yet.", Toast.LENGTH_SHORT).show();
                    } else {
                        for (DocumentSnapshot doc : querySnapshot) {
                            Medicine med = doc.toObject(Medicine.class);
                            if (med != null) medicineList.add(med);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private void exportToCSV() {
        if (medicineList.isEmpty()) {
            Toast.makeText(this, "No data to export!", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = "HealSphere_Medicines.csv";
        File exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        File file = new File(exportDir, fileName);

        try {
            FileWriter writer = new FileWriter(file);
            writer.append("Name,Dosage,Time\n");
            for (Medicine med : medicineList) {
                writer.append(med.getName()).append(",");
                writer.append(med.getDosage()).append(",");
                writer.append(med.getTime()).append("\n");
            }
            writer.flush();
            writer.close();

            Toast.makeText(this, "Exported to: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();

            openCSVFile(file);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void openCSVFile(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".provider",
                    file);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "text/csv");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(intent, "Open CSV File"));
        } catch (Exception e) {
            Toast.makeText(this, "No app found to open CSV file", Toast.LENGTH_LONG).show();
        }
    }
}
