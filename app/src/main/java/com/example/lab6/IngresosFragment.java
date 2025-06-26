package com.example.lab6;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
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
import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;

public class IngresosFragment extends Fragment implements IngresosAdapter.OnIngresoListener {

    private RecyclerView recyclerView;
    private IngresosAdapter adapter;
    private List<Ingreso> ingresosList;
    private Button btnAgregarIngreso;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private SimpleDateFormat dateFormat;
    private ListenerRegistration listenerRegistration;
    private ServicioAlmacenamiento servicioAlmacenamiento;
    private Uri selectedImageUri;
    private ImageView ivPreviewComprobante;
    private FrameLayout framePreview;
    private TextView tvNombreArchivo;
    private Button btnSeleccionarComprobante, btnRemoverComprobante;

    // ActivityResultLauncher para seleccionar imagen
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<String> permissionLauncher;

    /*@Nullable
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
    }*/
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ingresos, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        // Inicializar servicio de almacenamiento
        servicioAlmacenamiento = new ServicioAlmacenamiento(getContext());

        // Inicializar launchers
        initLaunchers();

        initViews(view);
        setupRecyclerView();
        setupListeners();
        loadIngresos();

        return view;
    }
    private void initLaunchers() {
        // Launcher para seleccionar imagen
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            mostrarPreviewImagen(selectedImageUri);
                        }
                    }
                }
        );

        // Launcher para permisos
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        abrirSelectorImagen();
                    } else {
                        Toast.makeText(getContext(), "Permiso denegado para acceder a las imágenes", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void mostrarPreviewImagen(Uri imageUri) {
        framePreview.setVisibility(View.VISIBLE);
        tvNombreArchivo.setVisibility(View.VISIBLE);

        // Cargar imagen con Glide
        Glide.with(this)
                .load(imageUri)
                .centerCrop()
                .into(ivPreviewComprobante);

        // Mostrar nombre del archivo
        String fileName = "Imagen_" + System.currentTimeMillis() + ".jpg";
        tvNombreArchivo.setText("Archivo: " + fileName);
    }

    private void abrirSelectorImagen() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void solicitarPermisosImagen() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            abrirSelectorImagen();
        }
    }

    /*@Override
    public void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }*/
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
        if (servicioAlmacenamiento != null) {
            servicioAlmacenamiento.limpiarRecursos();
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

    /*private void showAddIngresoDialog() {
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
    }*/
    private void showAddIngresoDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_ingreso, null);

        EditText etTitulo = dialogView.findViewById(R.id.etTitulo);
        EditText etMonto = dialogView.findViewById(R.id.etMonto);
        EditText etDescripcion = dialogView.findViewById(R.id.etDescripcion);
        EditText etFecha = dialogView.findViewById(R.id.etFecha);

        // Referencias a elementos del comprobante
        btnSeleccionarComprobante = dialogView.findViewById(R.id.btnSeleccionarComprobante);
        framePreview = dialogView.findViewById(R.id.framePreview);
        ivPreviewComprobante = dialogView.findViewById(R.id.ivPreviewComprobante);
        btnRemoverComprobante = dialogView.findViewById(R.id.btnRemoverComprobante);
        tvNombreArchivo = dialogView.findViewById(R.id.tvNombreArchivo);

        // Reset de imagen seleccionada
        selectedImageUri = null;
        framePreview.setVisibility(View.GONE);
        tvNombreArchivo.setVisibility(View.GONE);

        Calendar calendar = Calendar.getInstance();
        etFecha.setText(dateFormat.format(calendar.getTime()));

        // Configurar listeners
        etFecha.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        etFecha.setText(dateFormat.format(calendar.getTime()));
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        btnSeleccionarComprobante.setOnClickListener(v -> solicitarPermisosImagen());

        btnRemoverComprobante.setOnClickListener(v -> {
            selectedImageUri = null;
            framePreview.setVisibility(View.GONE);
            tvNombreArchivo.setVisibility(View.GONE);
        });

        new AlertDialog.Builder(getContext())
                .setTitle("Agregar Ingreso")
                .setView(dialogView)
                .setPositiveButton("Agregar", (dialog, which) -> {
                    String titulo = etTitulo.getText().toString().trim();
                    String montoStr = etMonto.getText().toString().trim();
                    String descripcion = etDescripcion.getText().toString().trim();

                    if (validateInputWithImage(titulo, montoStr)) {
                        double monto = Double.parseDouble(montoStr);
                        Date fecha = calendar.getTime();
                        String userId = auth.getCurrentUser().getUid();

                        if (selectedImageUri != null) {
                            // Subir imagen primero, luego crear ingreso
                            subirImagenYCrearIngreso(titulo, monto, descripcion, fecha, userId);
                        } else {
                            Toast.makeText(getContext(), "El comprobante es requerido", Toast.LENGTH_SHORT).show();
                        }
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
    private void subirImagenYCrearIngreso(String titulo, double monto, String descripcion, Date fecha, String userId) {
        String nombreArchivo = "ingreso_" + System.currentTimeMillis();

        servicioAlmacenamiento.guardarArchivo(selectedImageUri, nombreArchivo, new ServicioAlmacenamiento.CloudinaryCallback() {
            @Override
            public void onSuccess(String url, String publicId) {
                // Crear ingreso con información del comprobante
                Ingreso ingreso = new Ingreso(titulo, monto, descripcion, fecha, userId, url, publicId, nombreArchivo);
                addIngreso(ingreso);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Error al subir comprobante: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean validateInputWithImage(String titulo, String montoStr) {
        if (TextUtils.isEmpty(titulo)) {
            Toast.makeText(getContext(), "El título es requerido", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(montoStr)) {
            Toast.makeText(getContext(), "El monto es requerido", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (selectedImageUri == null) {
            Toast.makeText(getContext(), "El comprobante es requerido", Toast.LENGTH_SHORT).show();
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
    @Override
    public void onDownloadComprobante(Ingreso ingreso) {
        if (ingreso.tieneComprobante()) {
            servicioAlmacenamiento.descargarArchivo(ingreso.getComprobantePublicId(),
                    ingreso.getComprobanteNombre(), new ServicioAlmacenamiento.CloudinaryCallback() {
                        @Override
                        public void onSuccess(String url, String fileName) {
                            descargarImagenAlDispositivo(url, fileName);
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(getContext(), "Error al descargar: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void descargarImagenAlDispositivo(String imageUrl, String fileName) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(imageUrl));
            request.setTitle("Descargando comprobante");
            request.setDescription("Descargando " + fileName);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName + ".jpg");

            DownloadManager downloadManager = (DownloadManager) getContext().getSystemService(Context.DOWNLOAD_SERVICE);
            downloadManager.enqueue(request);

            Toast.makeText(getContext(), "Descarga iniciada. Revisa tu carpeta Downloads", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error al iniciar descarga: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}