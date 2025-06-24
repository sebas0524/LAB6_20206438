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
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.bumptech.glide.Glide;
import java.io.File;

public class EgresosFragment extends Fragment implements EgresosAdapter.OnEgresoListener {

    private RecyclerView recyclerView;
    private EgresosAdapter adapter;
    private List<Egreso> egresosList;
    private Button btnAgregarEgreso;

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
        View view = inflater.inflate(R.layout.fragment_egresos, container, false);


        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        initViews(view);

        setupRecyclerView();

        setupListeners();

        loadEgresos();

        return view;
    }*/
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_egresos, container, false);

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
        loadEgresos();

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
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(getContext(), "Error al cargar egresos: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        egresosList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Egreso egreso = doc.toObject(Egreso.class);
                            egreso.setId(doc.getId());
                            egresosList.add(egreso);
                        }

                        egresosList.sort((e1, e2) -> e2.getFecha().compareTo(e1.getFecha()));

                        adapter.notifyDataSetChanged();
                    }
                });
    }

    /*private void showAddEgresoDialog() {
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
    }*/
    private void showAddEgresoDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_egreso, null);

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
                .setTitle("Agregar Egreso")
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
                            // Subir imagen primero, luego crear egreso
                            subirImagenYCrearEgreso(titulo, monto, descripcion, fecha, userId);
                        } else {
                            Toast.makeText(getContext(), "El comprobante es requerido", Toast.LENGTH_SHORT).show();
                        }
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
    private void subirImagenYCrearEgreso(String titulo, double monto, String descripcion, Date fecha, String userId) {
        String nombreArchivo = "egreso_" + System.currentTimeMillis();

        servicioAlmacenamiento.guardarArchivo(selectedImageUri, nombreArchivo, new ServicioAlmacenamiento.CloudinaryCallback() {
            @Override
            public void onSuccess(String url, String publicId) {
                // Crear egreso con información del comprobante
                Egreso egreso = new Egreso(titulo, monto, descripcion, fecha, userId, url, publicId, nombreArchivo);
                addEgreso(egreso);
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
    public void onDownloadComprobante(Egreso egreso) {
        if (egreso.tieneComprobante()) {
            servicioAlmacenamiento.descargarArchivo(egreso.getComprobantePublicId(),
                    egreso.getComprobanteNombre(), new ServicioAlmacenamiento.CloudinaryCallback() {
                        @Override
                        public void onSuccess(String url, String fileName) {
                            // Abrir la imagen en el navegador o galería
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(url));
                            startActivity(intent);
                            Toast.makeText(getContext(), "Comprobante abierto", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(getContext(), "Error al descargar: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}