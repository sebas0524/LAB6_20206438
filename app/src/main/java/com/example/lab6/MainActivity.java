package com.example.lab6;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private Button btnIngresos, btnEgresos, btnResumen, btnCerrarSesion;
    private TextView tvWelcome;
    private FirebaseAuth mAuth;
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        fragmentManager = getSupportFragmentManager();

        // Verificar si el usuario está logueado
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // No hay usuario logueado, volver al login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Inicializar vistas
        initViews();

        // Configurar listeners
        setupListeners();

        // Mostrar mensaje de bienvenida
        tvWelcome.setText("Bienvenido: " + currentUser.getEmail());

        // Cargar fragment inicial (Resumen)
        loadFragment(new ResumenFragment());
    }

    private void initViews() {
        btnIngresos = findViewById(R.id.btnIngresos);
        btnEgresos = findViewById(R.id.btnEgresos);
        btnResumen = findViewById(R.id.btnResumen);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
        tvWelcome = findViewById(R.id.tvWelcome);
    }

    private void setupListeners() {
        btnIngresos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadFragment(new IngresosFragment());
                setActiveButton(btnIngresos);
            }
        });

        btnEgresos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadFragment(new EgresosFragment());
                setActiveButton(btnEgresos);
            }
        });

        btnResumen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadFragment(new ResumenFragment());
                setActiveButton(btnResumen);
            }
        });

        btnCerrarSesion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cerrarSesion();
            }
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.commit();
    }

    private void setActiveButton(Button activeButton) {
        // Resetear todos los botones
        btnIngresos.setSelected(false);
        btnEgresos.setSelected(false);
        btnResumen.setSelected(false);

        // Activar el botón seleccionado
        activeButton.setSelected(true);
    }

    private void cerrarSesion() {
        mAuth.signOut();
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();

        // Volver al LoginActivity
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}