package com.example.myapplication;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class EventViewHolder extends RecyclerView.ViewHolder {

    public TextView title, subtitle;

    public EventViewHolder(@NonNull View itemView) {
        super(itemView);
        title = itemView.findViewById(R.id.list_title);
        subtitle = itemView.findViewById(R.id.list_subtitle);
    }
}
