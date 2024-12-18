package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {
    Context context;
    ArrayList<Event> arrayList;
    private String userUid;
    OnEventListener onEventClickListener;
    OnEditListener onEditClickListener;

    public EventAdapter(Context context, ArrayList<Event> arrayList) {
        this.context = context;
        this.arrayList = arrayList;
        this.userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        sortEventsByDate();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.event_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = arrayList.get(position);
        LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getDate()), ZoneId.systemDefault());

        holder.title.setText(event.getTitle());
        holder.date.setText(date.getDayOfMonth() + "/" + date.getMonthValue());

        if (event.getOrganizer() != null && event.getOrganizer().equals(userUid))
            holder.editButton.setVisibility(View.VISIBLE);
        else
            holder.editButton.setVisibility(View.GONE);

        holder.editButton.setOnClickListener(v -> {
            if (onEditClickListener != null) {
                onEditClickListener.onEditButtonClick(event);
            }
        });

        holder.itemView.setOnClickListener(v -> onEventClickListener.onItemClick(event));
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, date;
        Button editButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.list_title);
            date = itemView.findViewById(R.id.list_date);
            editButton = itemView.findViewById(R.id.btn_edit);
        }
    }

    private void sortEventsByDate() {
        LocalDateTime now = LocalDateTime.now();

        // Filter out the events that are before the current time
        List<Event> futureEvents = arrayList.stream()
                .filter(event -> event.getDate() > now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()).sorted((event1, event2) -> {
                    Long date1 = event1.getDate();
                    Long date2 = event2.getDate();
                    return date1.compareTo(date2); // Ascending order
                }).collect(Collectors.toList());

        // Set the filtered and sorted list back into the adapter
        arrayList.clear();
        arrayList.addAll(futureEvents);
    }

    public void setOnEventListener(OnEventListener onEventListener) {
        this.onEventClickListener = onEventListener;
    }

    public interface OnEventListener {
        void onItemClick(Event event);
    }

    public void setOnEditListener(OnEditListener onEditListener) {
        this.onEditClickListener = onEditListener;
    }

    public interface OnEditListener {
        void onEditButtonClick(Event event);
    }
}
