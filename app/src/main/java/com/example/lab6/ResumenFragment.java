package com.example.lab6;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class ResumenFragment extends Fragment {

    private TextView tvMesSeleccionado, tvTotalIngresos, tvTotalEgresos, tvBalance;
    private Button btnSeleccionarMes;
    private PieChart pieChart;
    private BarChart barChart;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private Calendar selectedCalendar;
    private SimpleDateFormat monthYearFormat;
    private NumberFormat currencyFormat;

    private double totalIngresos = 0;
    private double totalEgresos = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_resumen, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        monthYearFormat = new SimpleDateFormat("MMMM yyyy", new Locale("es", "ES"));
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "PE"));

        selectedCalendar = Calendar.getInstance();

        initViews(view);

        setupListeners();

        setupCharts();

        loadDataForSelectedMonth();

        return view;
    }

    private void initViews(View view) {
        tvMesSeleccionado = view.findViewById(R.id.tvMesSeleccionado);
        tvTotalIngresos = view.findViewById(R.id.tvTotalIngresos);
        tvTotalEgresos = view.findViewById(R.id.tvTotalEgresos);
        tvBalance = view.findViewById(R.id.tvBalance);
        btnSeleccionarMes = view.findViewById(R.id.btnSeleccionarMes);
        pieChart = view.findViewById(R.id.pieChart);
        barChart = view.findViewById(R.id.barChart);
        tvMesSeleccionado.setText(monthYearFormat.format(selectedCalendar.getTime()));
    }

    private void setupListeners() {
        btnSeleccionarMes.setOnClickListener(v -> showMonthYearPicker());
    }

    private void setupCharts() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setHoleRadius(58f);
        pieChart.setDrawCenterText(true);
        pieChart.setCenterText("Ingresos vs Egresos");
        pieChart.setRotationAngle(0);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);

        barChart.getDescription().setEnabled(false);
        barChart.setMaxVisibleValueCount(60);
        barChart.setPinchZoom(false);
        barChart.setDrawBarShadow(false);
        barChart.setDrawGridBackground(false);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(3);

        barChart.getAxisLeft().setDrawGridLines(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.getLegend().setEnabled(true);
    }

    private void showMonthYearPicker() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedCalendar.set(Calendar.YEAR, year);
                    selectedCalendar.set(Calendar.MONTH, month);
                    selectedCalendar.set(Calendar.DAY_OF_MONTH, 1);

                    tvMesSeleccionado.setText(monthYearFormat.format(selectedCalendar.getTime()));
                    loadDataForSelectedMonth();
                },
                selectedCalendar.get(Calendar.YEAR),
                selectedCalendar.get(Calendar.MONTH),
                selectedCalendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void loadDataForSelectedMonth() {
        String userId = auth.getCurrentUser().getUid();

        Calendar startOfMonth = (Calendar) selectedCalendar.clone();
        startOfMonth.set(Calendar.DAY_OF_MONTH, 1);
        startOfMonth.set(Calendar.HOUR_OF_DAY, 0);
        startOfMonth.set(Calendar.MINUTE, 0);
        startOfMonth.set(Calendar.SECOND, 0);
        startOfMonth.set(Calendar.MILLISECOND, 0);

        Calendar endOfMonth = (Calendar) selectedCalendar.clone();
        endOfMonth.set(Calendar.DAY_OF_MONTH, endOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH));
        endOfMonth.set(Calendar.HOUR_OF_DAY, 23);
        endOfMonth.set(Calendar.MINUTE, 59);
        endOfMonth.set(Calendar.SECOND, 59);
        endOfMonth.set(Calendar.MILLISECOND, 999);

        Date startDate = startOfMonth.getTime();
        Date endDate = endOfMonth.getTime();

        loadIngresosForMonth(userId, startDate, endDate);
    }

    private void loadIngresosForMonth(String userId, Date startDate, Date endDate) {
        db.collection("ingresos")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    totalIngresos = 0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Ingreso ingreso = doc.toObject(Ingreso.class);
                        if (ingreso.getFecha() != null &&
                                ingreso.getFecha().compareTo(startDate) >= 0 &&
                                ingreso.getFecha().compareTo(endDate) <= 0) {
                            totalIngresos += ingreso.getMonto();
                        }
                    }

                    loadEgresosForMonth(userId, startDate, endDate);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al cargar ingresos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadEgresosForMonth(String userId, Date startDate, Date endDate) {
        db.collection("egresos")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    totalEgresos = 0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Egreso egreso = doc.toObject(Egreso.class);

                        if (egreso.getFecha() != null &&
                                egreso.getFecha().compareTo(startDate) >= 0 &&
                                egreso.getFecha().compareTo(endDate) <= 0) {
                            totalEgresos += egreso.getMonto();
                        }
                    }

                    updateUI();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al cargar egresos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUI() {
        tvTotalIngresos.setText("Ingresos: " + currencyFormat.format(totalIngresos));
        tvTotalEgresos.setText("Egresos: " + currencyFormat.format(totalEgresos));

        double balance = totalIngresos - totalEgresos;
        tvBalance.setText("Balance: " + currencyFormat.format(balance));
        tvBalance.setTextColor(balance >= 0 ? Color.GREEN : Color.RED);

        updatePieChart();
        updateBarChart();
    }

    private void updatePieChart() {
        ArrayList<PieEntry> entries = new ArrayList<>();

        if (totalIngresos > 0 || totalEgresos > 0) {
            double total = totalIngresos + totalEgresos;

            if (totalIngresos > 0) {
                float ingresosPorcentaje = (float) ((totalIngresos / total) * 100);
                entries.add(new PieEntry(ingresosPorcentaje, "Ingresos"));
            }

            if (totalEgresos > 0) {
                float egresosPorcentaje = (float) ((totalEgresos / total) * 100);
                entries.add(new PieEntry(egresosPorcentaje, "Egresos"));
            }
        } else {
            entries.add(new PieEntry(100f, "Sin datos"));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.rgb(76, 175, 80));
        colors.add(Color.rgb(244, 67, 54));
        colors.add(Color.GRAY);
        dataSet.setColors(colors);

        PieData data = new PieData(dataSet);
        data.setValueTextSize(12f);
        data.setValueTextColor(Color.WHITE);

        pieChart.setData(data);
        pieChart.invalidate();
    }

    private void updateBarChart() {
        ArrayList<BarEntry> entries = new ArrayList<>();


        entries.add(new BarEntry(0f, (float) totalIngresos));
        entries.add(new BarEntry(1f, (float) totalEgresos));
        entries.add(new BarEntry(2f, (float) (totalIngresos - totalEgresos)));

        BarDataSet dataSet = new BarDataSet(entries, "Montos");

        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.rgb(76, 175, 80));
        colors.add(Color.rgb(244, 67, 54));
        colors.add(totalIngresos >= totalEgresos ? Color.rgb(33, 150, 243) : Color.rgb(255, 152, 0));
        dataSet.setColors(colors);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.5f);

        String[] labels = {"Ingresos", "Egresos", "Balance"};
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));

        barChart.setData(data);
        barChart.invalidate();
    }
}
