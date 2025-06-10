package com.example.lab6;

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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

public class EgresosFragment extends Fragment implements EgresosAdapter.OnEgresoListener {

    private RecyclerView recyclerView;
    private EgresosAdapter adapter;
    private List<Egreso> egresosList;
    private Button btnAgregarEgreso;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private SimpleDateFormat dateFormat;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_egresos, container, false);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        // Inicializar vistas
        initViews(view);

        // Configurar RecyclerView
        setupRecyclerView();

        // Configurar listeners
        setupListeners();

        // Cargar datos
        loadEgresos();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewEgresos);
        btnAgregarEgreso = view.findViewById(R.id.btnAgregarEgreso);
        egresosList = new ArrayList<>();
    }

    private void setupRecyclerView() {
        adapter = new EgresosAdapter(egresosList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        btnAgregarEgreso.setOnClickListener(v -> showAddEgresoDialog());
    }

    private void loadEgresos() {
        String userId = auth.getCurrentUser().getUid();

        db.collection("egresos")
                .whereEqualTo("userId", userId)
                .orderBy("fecha", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(getContext(), "Error al cargar egresos", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    egresosList.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        Egreso egreso = doc.toObject(Egreso.class);
                        egreso.setId(doc.getId());
                        egresosList.add(egreso);
                    }
                    adapter.updateList(egresosList);
                });
    }

    private void showAddEgresoDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_egreso, null);

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
                .setTitle("Agregar Egreso")
                .setView(dialogView)
                .setPositiveButton("Agregar", (dialog, which) -> {
                    String titulo = etTitulo.getText().toString().trim();
                    String montoStr = etMonto.getText().toString().trim();
                    String descripcion = etDescripcion.getText().toString().trim();

                    if (validateInput(titulo, montoStr)) {
                        double monto = Double.parseDouble(montoStr);
                        Date fecha = calendar.getTime();
                        String userId = auth.getCurrentUser().getUid();

                        Egreso egreso = new Egreso(titulo, monto, descripcion, fecha, userId);
                        addEgreso(egreso);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void addEgreso(Egreso egreso) {
        db.collection("egresos")
                .add(egreso)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Egreso agregado exitosamente", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al agregar egreso", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onEditEgreso(Egreso egreso) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_egreso, null);

        EditText etMonto = dialogView.findViewById(R.id.etMonto);
        EditText etDescripcion = dialogView.findViewById(R.id.etDescripcion);

        etMonto.setText(String.valueOf(egreso.getMonto()));
        etDescripcion.setText(egreso.getDescripcion());

        new AlertDialog.Builder(getContext())
                .setTitle("Editar Egreso")
                .setView(dialogView)
                .setPositiveButton("Actualizar", (dialog, which) -> {
                    String montoStr = etMonto.getText().toString().trim();
                    String descripcion = etDescripcion.getText().toString().trim();

                    if (!TextUtils.isEmpty(montoStr)) {
                        double monto = Double.parseDouble(montoStr);
                        updateEgreso(egreso.getId(), monto, descripcion);
                    } else {
                        Toast.makeText(getContext(), "El monto es requerido", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void updateEgreso(String egresoId, double monto, String descripcion) {
        db.collection("egresos").document(egresoId)
                .update("monto", monto, "descripcion", descripcion)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Egreso actualizado exitosamente", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error al actualizar egreso", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDeleteEgreso(Egreso egreso) {
        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar Egreso")
                .setMessage("¿Estás seguro de que quieres eliminar este egreso?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    db.collection("egresos").document(egreso.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Egreso eliminado exitosamente", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Error al eliminar egreso", Toast.LENGTH_SHORT).show();
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