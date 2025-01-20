package com.example.myapplication;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public class SaleAdapter extends RecyclerView.Adapter<SaleAdapter.ViewHolder> {
    private final Context context;
    private List<Sale> arrayList;

    public SaleAdapter(Context context, List<Sale> arrayList) {
        this.context = context;
        this.arrayList = arrayList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.sale_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Sale sale = arrayList.get(position);
        LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(sale.getTime()), ZoneId.systemDefault());
        String seatText = "Seat: " + sale.getSeat();
        String dateText = date.getDayOfMonth() + "/" + date.getMonthValue() + " - " + date.getHour() + ":" + date.getMinute();

        sale.getName(name -> {
            if (name != null) {
                Log.d("Sale", "Fetched user name: " + name);
                holder.name.setText(name);
            } else {
                Log.d("Sale", "User name not found or error fetching.");
            }
        });

        holder.seat.setText(seatText);
        holder.date.setText(dateText);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, seat, date;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.list_name);
            seat = itemView.findViewById(R.id.list_seat);
            date = itemView.findViewById(R.id.list_time);
        }
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    // Update the sales list
    public void updateSalesList(List<Sale> updatedList) {
        this.arrayList = updatedList;
        notifyDataSetChanged(); // Notify adapter that data has changed
    }
}