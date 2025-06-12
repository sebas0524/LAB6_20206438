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

public class EgresosAdapter extends RecyclerView.Adapter<EgresosAdapter.EgresoViewHolder> {

    private List<Egreso> egresosList;
    private OnEgresoListener listener;
    private SimpleDateFormat dateFormat;
    private NumberFormat currencyFormat;

    public interface OnEgresoListener {
        void onEditEgreso(Egreso egreso);
        void onDeleteEgreso(Egreso egreso);
    }

    public EgresosAdapter(List<Egreso> egresosList, OnEgresoListener listener) {
        this.egresosList = egresosList;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "PE"));
    }

    @NonNull
    @Override
    public EgresoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_egreso, parent, false);
        return new EgresoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EgresoViewHolder holder, int position) {
        Egreso egreso = egresosList.get(position);

        holder.tvTitulo.setText(egreso.getTitulo());
        holder.tvMonto.setText(currencyFormat.format(egreso.getMonto()));
        holder.tvFecha.setText(dateFormat.format(egreso.getFecha()));

        holder.btnEditar.setOnClickListener(v -> listener.onEditEgreso(egreso));
        holder.btnEliminar.setOnClickListener(v -> listener.onDeleteEgreso(egreso));
    }

    @Override
    public int getItemCount() {
        return egresosList.size();
    }

    public void updateList(List<Egreso> newList) {
        this.egresosList.clear();
        this.egresosList.addAll(newList);
        notifyDataSetChanged();
    }

    static class EgresoViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvMonto, tvFecha;
        Button btnEditar, btnEliminar;

        public EgresoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvMonto = itemView.findViewById(R.id.tvMonto);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            btnEditar = itemView.findViewById(R.id.btnEditar);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
        }
    }
}