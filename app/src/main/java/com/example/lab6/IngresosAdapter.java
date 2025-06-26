package com.example.lab6;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;


public class IngresosAdapter extends RecyclerView.Adapter<IngresosAdapter.IngresoViewHolder> {

    private List<Ingreso> ingresosList;
    private OnIngresoListener listener;
    private SimpleDateFormat dateFormat;
    private NumberFormat currencyFormat;


    public interface OnIngresoListener {
        void onEditIngreso(Ingreso ingreso);
        void onDeleteIngreso(Ingreso ingreso);
        void onDownloadComprobante(Ingreso ingreso);
    }

    public IngresosAdapter(List<Ingreso> ingresosList, OnIngresoListener listener) {
        this.ingresosList = ingresosList;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "PE"));
    }

    @NonNull
    @Override
    public IngresoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ingreso, parent, false);
        return new IngresoViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull IngresoViewHolder holder, int position) {
        Ingreso ingreso = ingresosList.get(position);

        holder.tvTitulo.setText(ingreso.getTitulo());
        holder.tvMonto.setText(currencyFormat.format(ingreso.getMonto()));
        holder.tvFecha.setText(dateFormat.format(ingreso.getFecha()));

        if (ingreso.tieneComprobante()) {
            holder.layoutComprobante.setVisibility(View.VISIBLE);
            holder.btnDescargarComprobante.setOnClickListener(v -> listener.onDownloadComprobante(ingreso));
        } else {
            holder.layoutComprobante.setVisibility(View.GONE);
        }

        holder.btnEditar.setOnClickListener(v -> listener.onEditIngreso(ingreso));
        holder.btnEliminar.setOnClickListener(v -> listener.onDeleteIngreso(ingreso));
    }

    @Override
    public int getItemCount() {
        return ingresosList.size();
    }

    public void updateList(List<Ingreso> newList) {
        this.ingresosList.clear();
        this.ingresosList.addAll(newList);
        notifyDataSetChanged();
    }
    static class IngresoViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvMonto, tvFecha;
        Button btnEditar, btnEliminar, btnDescargarComprobante;
        View layoutComprobante;

        public IngresoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvMonto = itemView.findViewById(R.id.tvMonto);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            btnEditar = itemView.findViewById(R.id.btnEditar);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
            btnDescargarComprobante = itemView.findViewById(R.id.btnDescargarComprobante);
            layoutComprobante = itemView.findViewById(R.id.layoutComprobante);
        }
    }
}