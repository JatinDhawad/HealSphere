package com.example.healsphere;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MedicineAdapter extends RecyclerView.Adapter<MedicineAdapter.ViewHolder> {

    private final Context context;
    private final List<Medicine> medicineList;
    private final FirebaseFirestore firestore;

    public MedicineAdapter(Context context, List<Medicine> list) {
        this.context = context;
        this.medicineList = list;
        this.firestore = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_medicine, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Medicine m = medicineList.get(position);

        holder.tvName.setText(m.getName());
        holder.tvDosage.setText("Dosage: " + m.getDosage());
        holder.tvTime.setText("Time: " + m.getTime());

        holder.btnTaken.setOnClickListener(v -> logMedicineStatus(m, "taken"));
        holder.btnMissed.setOnClickListener(v -> logMedicineStatus(m, "missed"));
    }

    private void logMedicineStatus(Medicine med, String status) {
        SharedPreferences prefs = context.getSharedPreferences("HealSpherePrefs", Context.MODE_PRIVATE);
        String profileId = prefs.getString("currentProfileId", null);

        if (profileId == null) {
            Toast.makeText(context, "No active profile found!", Toast.LENGTH_SHORT).show();
            return;
        }

        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        Map<String, Object> logData = new HashMap<>();
        logData.put("medicineName", med.getName());
        logData.put("status", status);
        logData.put("time", med.getTime());
        logData.put("timestamp", System.currentTimeMillis());

        firestore.collection("users")
                .document(profileId)
                .collection("logs")
                .document(date)
                .collection("entries")
                .document(med.getName())
                .set(logData)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(context,
                                med.getName() + " marked as " + status,
                                Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(context,
                                "Failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() {
        return medicineList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDosage, tvTime;
        Button btnTaken, btnMissed;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvDosage = itemView.findViewById(R.id.tvDosage);
            tvTime = itemView.findViewById(R.id.tvTime);
            btnTaken = itemView.findViewById(R.id.btnTaken);
            btnMissed = itemView.findViewById(R.id.btnMissed);
        }
    }
}
