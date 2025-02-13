package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {
    private static final int LAYOUT_ADMIN = 1, LAYOUT_USER = 2, LAYOUT_SALE = 3;

    private final Context context;
    private final int layoutType;
    private final ArrayList<Event> arrayList;
    private final String userUid;
    private OnEventListener onEventClickListener;
    private OnEditListener onEditClickListener;

    public EventAdapter(Context context, ArrayList<Event> arrayList, int layoutType) {
        this.context = context;
        this.arrayList = arrayList;
        this.layoutType = layoutType;
        this.userUid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        sortEventsByDate();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Define an array that maps layout types to layout resources
        int[] layoutResArray = {
                R.layout.event_list_admin_item, // LAYOUT_ADMIN
                R.layout.event_list_user_item,  // LAYOUT_USER
                R.layout.user_sale_list_item    // LAYOUT_SALE
        };

        View view = LayoutInflater.from(context).inflate(layoutResArray[layoutType - 1], parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = arrayList.get(position);
        LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getDate()), ZoneId.systemDefault());
        String dateText = date.getDayOfMonth() + "/" + date.getMonthValue();

        holder.title.setText(event.getTitle());
        holder.date.setText(dateText);

        if (layoutType == LAYOUT_ADMIN) {
            // Edit button (ADMIN LAYOUT)
            if (event.getOrganizer() != null && event.getOrganizer().equals(userUid))
                holder.editButton.setVisibility(View.VISIBLE);
            else
                holder.editButton.setVisibility(View.GONE);

            holder.editButton.setOnClickListener(v -> {
                if (onEditClickListener != null) {
                    onEditClickListener.onEditButtonClick(event);
                }
            });
        } else if (layoutType == LAYOUT_USER) {
            // Price text (USER LAYOUT)
            DecimalFormatSymbols symbols = new DecimalFormatSymbols();
            symbols.setDecimalSeparator(',');

            if (event.getPrice() == 0)
                holder.price.setText(R.string.free);
            else
                holder.price.setText(new DecimalFormat("#0.00", symbols).format(event.getPrice()));
        } else if (layoutType == LAYOUT_SALE) {
            String timeText = date.getHour() + ":" + String.format(Locale.getDefault(), "%02d", date.getMinute());
            holder.time.setText(timeText);
        }

        // Click on items (BOTH LAYOUT)
        holder.itemView.setOnClickListener(v -> {
            if (layoutType == LAYOUT_ADMIN || layoutType == LAYOUT_SALE)
                onEventClickListener.onItemClick(event);    // Show event details
            else if (layoutType == LAYOUT_USER) {
                // USER LAYOUT behaviour
                Intent intent = new Intent(context, EventBooking.class);
                intent.putExtra("event_uid", event.getKey());   // Use UID of the event
                context.startActivity(intent);
            }
        });
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, date, price, time;
        Button editButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.list_title);
            date = itemView.findViewById(R.id.list_date);

            editButton = itemView.findViewById(R.id.btn_edit);  // ADMIN LAYOUT
            price = itemView.findViewById(R.id.list_price);     // USER LAYOUT
            time = itemView.findViewById(R.id.list_time);       // SALE LAYOUT
        }
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    private void sortEventsByDate() {
        LocalDateTime now = LocalDateTime.now();
        Log.d("Sort events", "ArrayList at first, size: " + arrayList.size() + "\nEvents: " + this.arrayList);

        // Filter out the events that are before the current time
        List<Event> sortedEvents = arrayList.stream()
            .filter(event -> {
                // Only filter out past events if layoutType is not LAYOUT_ADMIN
                if (layoutType != LAYOUT_ADMIN) {
                    return event.getDate() > now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                }
                return true; // Don't filter out past events if it's LAYOUT_ADMIN
            })
            .sorted((event1, event2) -> {
                Long date1 = event1.getDate();
                Long date2 = event2.getDate();
                return date1.compareTo(date2); // Ascending order
            })
            .collect(Collectors.toList());

        Log.d("Sort events", "FutureEvents after filtering, size: " + sortedEvents.size() + "\nEvents: " + sortedEvents);
        // Set the filtered and sorted list back into the adapter
        arrayList.clear();
        arrayList.addAll(sortedEvents);
        Log.d("Sort events", "ArrayList after clear and adding, size: " + arrayList.size() + "\nEvents: " + this.arrayList);
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