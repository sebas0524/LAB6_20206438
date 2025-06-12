package com.example.lab6;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class IngresosFragment extends Fragment implements IngresosAdapter.OnIngresoListener {

    private RecyclerView recyclerView;
    private IngresosAdapter adapter;
    private List<Ingreso> ingresosList;
    private Button btnAgregarIngreso;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private SimpleDateFormat dateFormat;
    private ListenerRegistration listenerRegistration;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ingresos, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());


        initViews(view);

        setupRecyclerView();

        setupListeners();

        loadIngresos();

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewIngresos);
        btnAgregarIngreso = view.findViewById(R.id.btnAgregarIngreso);
        ingresosList = new ArrayList<>();
    }

    private void setupRecyclerView() {
        adapter = new IngresosAdapter(ingresosList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        btnAgregarIngreso.setOnClickListener(v -> showAddIngresoDialog());
    }

    private void loadIngresos() {
        String userId = auth.getCurrentUser().getUid();

        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }

        listenerRegistration = db.collection("ingresos")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(getContext(), "Error al cargar ingresos: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        ingresosList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Ingreso ingreso = doc.toObject(Ingreso.class);
                            ingreso.setId(doc.getId());
                            ingresosList.add(ingreso);
                        }
                        ingresosList.sort((i1, i2) -> i2.getFecha().compareTo(i1.getFecha()));

                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void showAddIngresoDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_ingreso, null);

        EditText etTitulo = dialogView.findViewById(R.id.etTitulo);
        EditText etMonto = dialogView.findViewById(R.id.etMonto);
        EditText etDescripcion = dialogView.findViewById(R.id.etDescripcion);
        EditText etFecha = dialogView.findViewById(R.id.etFecha);

        Calendar calendar = Calendar.getInstance();
        etFecha.setText(dateFormat.format(calendar.getTime()));

        etFecha.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        etFecha.setText(dateFormat.format(calendar.getTime()));
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        new AlertDialog.Builder(getContext())
                .setTitle("Agregar Ingreso")
                .setView(dialogView)
                .setPositiveButton("Agregar", (dialog, which) -> {
                    String titulo = etTitulo.getText().toString().trim();
                    String montoStr = etMonto.getText().toString().trim();
                    String descripcion = etDescripcion.getText().toString().trim();

                    if (validateInput(titulo, montoStr)) {
                        double monto = Double.parseDouble(montoStr);
                        Date fecha = calendar.getTime();
                        String userId = auth.getCurrentUser().getUid();

                        Ingreso ingreso = new Ingreso(titulo, monto, descripcion, fecha, userId);
                        addIngreso(ingreso);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void addIngreso(Ingreso ingreso) {
        db.collection("ingresos")
                .add(ingreso)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Ingreso agregado exitosamente", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al agregar ingreso", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onEditIngreso(Ingreso ingreso) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_ingreso, null);

        EditText etMonto = dialogView.findViewById(R.id.etMonto);
        EditText etDescripcion = dialogView.findViewById(R.id.etDescripcion);

        etMonto.setText(String.valueOf(ingreso.getMonto()));
        etDescripcion.setText(ingreso.getDescripcion());

        new AlertDialog.Builder(getContext())
                .setTitle("Editar Ingreso")
                .setView(dialogView)
                .setPositiveButton("Actualizar", (dialog, which) -> {
                    String montoStr = etMonto.getText().toString().trim();
                    String descripcion = etDescripcion.getText().toString().trim();

                    if (!TextUtils.isEmpty(montoStr)) {
                        double monto = Double.parseDouble(montoStr);
                        updateIngreso(ingreso.getId(), monto, descripcion);
                    } else {
                        Toast.makeText(getContext(), "El monto es requerido", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void updateIngreso(String ingresoId, double monto, String descripcion) {
        db.collection("ingresos").document(ingresoId)
                .update("monto", monto, "descripcion", descripcion)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Ingreso actualizado exitosamente", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al actualizar ingreso", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDeleteIngreso(Ingreso ingreso) {
        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar Ingreso")
                .setMessage("¿Estás seguro de que quieres eliminar este ingreso?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    db.collection("ingresos").document(ingreso.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Ingreso eliminado exitosamente", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Error al eliminar ingreso", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private boolean validateInput(String titulo, String montoStr) {
        if (TextUtils.isEmpty(titulo)) {
            Toast.makeText(getContext(), "El título es requerido", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(montoStr)) {
            Toast.makeText(getContext(), "El monto es requerido", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            double monto = Double.parseDouble(montoStr);
            if (monto <= 0) {
                Toast.makeText(getContext(), "El monto debe ser mayor a 0", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Monto inválido", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }
}