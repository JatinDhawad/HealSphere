package com.example.healsphere;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.formatter.ValueFormatter;

import com.github.mikephil.charting.charts.*;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.*;

public class DailyReportActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private PieChart pieChartMedicine;
    private BarChart barChartWater, chartBloodPressure;
    private LineChart chartGlucose, chartWeight;
    private TextView tvSummary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_report);

        firestore = FirebaseFirestore.getInstance();

        pieChartMedicine = findViewById(R.id.pieChartMedicine);
        barChartWater = findViewById(R.id.barChartWater);
        chartBloodPressure = findViewById(R.id.chartBloodPressure);
        chartGlucose = findViewById(R.id.chartGlucose);
        chartWeight = findViewById(R.id.chartWeight);
        tvSummary = findViewById(R.id.tvSummary);

        loadMedicineReport();
        loadWaterReport();
        loadVitalsFromFirestore();
    }

    private void loadMedicineReport() {
        SharedPreferences prefs = getSharedPreferences("HealSpherePrefs", MODE_PRIVATE);
        String profileId = prefs.getString("currentProfileId", null);

        if (profileId == null) {
            Toast.makeText(this, "No active profile found!", Toast.LENGTH_SHORT).show();
            return;
        }

        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        firestore.collection("users")
                .document(profileId)
                .collection("logs")
                .document(date)
                .collection("entries")
                .get()
                .addOnSuccessListener(query -> {
                    int taken = 0, missed = 0;
                    for (QueryDocumentSnapshot doc : query) {
                        String status = doc.getString("status");
                        if ("taken".equals(status)) taken++;
                        else if ("missed".equals(status)) missed++;
                    }

                    int total = taken + missed;
                    if (total == 0) {
                        tvSummary.setText("No medicines logged today.");
                        pieChartMedicine.clear();
                        return;
                    }

                    tvSummary.setText("Total: " + total + " | Taken: " + taken + " | Missed: " + missed);

                    ArrayList<PieEntry> entries = new ArrayList<>();
                    entries.add(new PieEntry(taken, "Taken"));
                    entries.add(new PieEntry(missed, "Missed"));

                    PieDataSet dataSet = new PieDataSet(entries, "Medicine Intake");
                    dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
                    dataSet.setValueTextSize(14f);

                    pieChartMedicine.setData(new PieData(dataSet));
                    pieChartMedicine.getDescription().setText("Medicine Intake Status");
                    pieChartMedicine.animateY(1000);
                    pieChartMedicine.invalidate();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load medicine report: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadWaterReport() {
        SharedPreferences prefs = getSharedPreferences("HealSpherePrefs", MODE_PRIVATE);
        int waterCount = prefs.getInt("waterCount", 0);

        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(1, waterCount));
        entries.add(new BarEntry(2, 8));

        BarDataSet dataSet = new BarDataSet(entries, "Water Intake (Glasses)");
        dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        dataSet.setValueTextSize(14f);

        barChartWater.setData(new BarData(dataSet));
        barChartWater.getDescription().setText("Daily Water Progress");
        barChartWater.animateY(1000);
        barChartWater.invalidate();
    }

    private void loadVitalsFromFirestore() {
        SharedPreferences prefs = getSharedPreferences("HealSpherePrefs", MODE_PRIVATE);
        String profileId = prefs.getString("currentProfileId", null);

        if (profileId == null) return;

        firestore.collection("users")
                .document(profileId)
                .collection("vitals")
                .get()
                .addOnSuccessListener(query -> {
                    ArrayList<BarEntry> bpEntries = new ArrayList<>();
                    ArrayList<Entry> glucoseEntries = new ArrayList<>();
                    ArrayList<Entry> weightEntries = new ArrayList<>();
                    ArrayList<String> labels = new ArrayList<>();

                    int index = 0;
                    for (QueryDocumentSnapshot doc : query) {
                        String date = doc.getId();
                        labels.add(date.substring(Math.max(0, date.length() - 5)));

                        double systolic = parseDoubleSafe(doc.get("systolic"));
                        double diastolic = parseDoubleSafe(doc.get("diastolic"));
                        double glucose = parseDoubleSafe(doc.get("glucose"));
                        double weight = parseDoubleSafe(doc.get("weight"));

                        if (systolic > 0 && diastolic > 0)
                            bpEntries.add(new BarEntry(index, new float[]{(float) systolic, (float) diastolic}));

                        if (glucose > 0)
                            glucoseEntries.add(new Entry(index, (float) glucose));

                        if (weight > 0)
                            weightEntries.add(new Entry(index, (float) weight));

                        index++;
                    }

                    BarDataSet bpDataSet = new BarDataSet(bpEntries, "Blood Pressure");
                    bpDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
                    bpDataSet.setStackLabels(new String[]{"Systolic", "Diastolic"});
                    chartBloodPressure.setData(new BarData(bpDataSet));
                    setupXAxis(chartBloodPressure, labels);
                    chartBloodPressure.getDescription().setText("BP over time");
                    chartBloodPressure.animateY(1000);
                    chartBloodPressure.invalidate();

                    LineDataSet glucoseSet = new LineDataSet(glucoseEntries, "Glucose (mg/dL)");
                    glucoseSet.setColor(ColorTemplate.rgb("#FFA726"));
                    glucoseSet.setCircleColor(ColorTemplate.rgb("#FB8C00"));
                    glucoseSet.setValueTextSize(12f);
                    chartGlucose.setData(new LineData(glucoseSet));
                    setupXAxis(chartGlucose, labels);
                    chartGlucose.getDescription().setText("Glucose Levels");
                    chartGlucose.animateX(1000);
                    chartGlucose.invalidate();

                    LineDataSet weightSet = new LineDataSet(weightEntries, "Weight (kg)");
                    weightSet.setColor(ColorTemplate.rgb("#66BB6A"));
                    weightSet.setCircleColor(ColorTemplate.rgb("#388E3C"));
                    weightSet.setValueTextSize(12f);
                    chartWeight.setData(new LineData(weightSet));
                    setupXAxis(chartWeight, labels);
                    chartWeight.getDescription().setText("Weight Trend");
                    chartWeight.animateX(1000);
                    chartWeight.invalidate();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load vitals: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private double parseDoubleSafe(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }


    private void setupXAxis(BarLineChartBase<?> chart, ArrayList<String> labels) {
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value >= 0 && value < labels.size()) {
                    return labels.get((int) value);
                } else {
                    return "";
                }
            }
        });

        xAxis.setGranularity(1f);
        xAxis.setTextSize(10f);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = chart.getAxisLeft();
        chart.getAxisRight().setEnabled(false);
        leftAxis.setTextSize(10f);
    }

}
