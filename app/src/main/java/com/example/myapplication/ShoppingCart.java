package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Objects;
import java.util.HashMap;
import java.util.Map;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;

public class ShoppingCart extends AppCompatActivity {
    private FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_shopping_cart);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Firebase setting
        FirebaseApp.initializeApp(ShoppingCart.this);
        database = FirebaseDatabase.getInstance("https://ing-soft-firebase-default-rtdb.europe-west1.firebasedatabase.app/");

        // Setting basic buttons
        Button logout_btn = findViewById(R.id.logout_btn);
        logout_btn.setOnClickListener(v -> BasicButtons.handleLogoutButton(ShoppingCart.this));
        Button name_btn = findViewById(R.id.user);
        BasicButtons.checkUserAndSetNameButton(ShoppingCart.this, name_btn);

        //showCartEvents();
        showBoughtEvents();
    }

    private void showBoughtEvents() {
        // Setting recycler view
        RecyclerView recyclerView = findViewById(R.id.recycler_sale);
        TextView empty = findViewById(R.id.empty_sale);
        Log.d("showBoughtEvents", "showBoughtEvents called");

        // Fetch all events
        database.getReference().child("event").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot eventSnapshot) {
                // Map to store events by their UID
                Map<String, Event> eventMap = new HashMap<>();
                for (DataSnapshot eventData : eventSnapshot.getChildren()) {
                    Event event = eventData.getValue(Event.class);
                    if (event != null) {
                        event.setKey(eventData.getKey());
                        eventMap.put(eventData.getKey(), event);
                    }
                }
                Log.d("showBoughtEvents", "Number of events: " + eventMap.size());

                // Fetch sales and filter for the current user
                String currentUserUID = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
                database.getReference().child("sale").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot saleSnapshot) {
                        ArrayList<Event> arrayList = new ArrayList<>();

                        for (DataSnapshot saleData : saleSnapshot.getChildren()) {
                            Sale sale = saleData.getValue(Sale.class);
                            if (sale != null && currentUserUID.equals(sale.getUserUID())) {
                                Event event = eventMap.get(sale.getEventUID());
                                if (event != null)
                                    arrayList.add(event);
                            }
                        }

                        // Update UI based on whether any events are found
                        if (arrayList.isEmpty()) {
                            recyclerView.setVisibility(View.GONE);
                            empty.setVisibility(View.VISIBLE);
                        } else {
                            recyclerView.setVisibility(View.VISIBLE);
                            empty.setVisibility(View.GONE);
                        }

                        Log.d("showBoughtEvents", "Number of events: " + arrayList.size());
                        EventAdapter adapter = new EventAdapter(ShoppingCart.this, arrayList, 3);
                        recyclerView.setAdapter(adapter);

                        // Show details of event on dialog
                        adapter.setOnEventListener(event -> {
                            // Setting view and finding XML elements
                            View dialogView = LayoutInflater.from(ShoppingCart.this).inflate(R.layout.event_dialog, null);
                            TextView title = dialogView.findViewById(R.id.title);
                            TextView place = dialogView.findViewById(R.id.place);
                            TextView date = dialogView.findViewById(R.id.date);
                            TextView time = dialogView.findViewById(R.id.time);
                            TextView desc = dialogView.findViewById(R.id.desc);
                            TextView price = dialogView.findViewById(R.id.price);
                            TextView seats = dialogView.findViewById(R.id.seats);
                            TextView organizer = dialogView.findViewById(R.id.organizer);
                            ImageView qrImage = dialogView.findViewById(R.id.qrImage);
                            LocalDateTime dateEvent = LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getDate()), ZoneId.systemDefault());

                            DecimalFormatSymbols symbols = new DecimalFormatSymbols();
                            symbols.setDecimalSeparator(',');
                            String dateText = dateEvent.getDayOfMonth() + "/" + dateEvent.getMonthValue() + "/" + dateEvent.getYear();
                            String timeText = dateEvent.getHour() + ":" + String.format("%02d", dateEvent.getMinute());
                            QRGEncoder qrgEncoder = new QRGEncoder(event.getKey(), null, QRGContents.Type.TEXT, 200);

                            // !!!SOS!!! BUG FORSE DELLA LIBRERIA!? I COLORI DEL QR CODE SONO INVERTITI
                            // Set colors for the QR code
                            qrgEncoder.setColorBlack(Color.WHITE);
                            qrgEncoder.setColorWhite(Color.BLACK);
                            Bitmap bitmap = qrgEncoder.getBitmap();

                            // Set text and visibilities
                            title.setText(event.getTitle());
                            place.setText(event.getPlace());
                            desc.setText(event.getDescription());
                            date.setText(dateText);
                            time.setText(timeText);
                            seats.setVisibility(View.GONE);
                            organizer.setVisibility(View.GONE);
                            qrImage.setImageBitmap(bitmap);
                            if (event.getPrice() == 0)
                                price.setText(R.string.free);
                            else
                                price.setText(new DecimalFormat("#0.00", symbols).format(event.getPrice()));

                            AlertDialog alertDialog = new AlertDialog.Builder(ShoppingCart.this)
                                    .setTitle("Event Details")
                                    .setView(dialogView)
                                    .setPositiveButton("Close", (dialog, which) -> dialog.dismiss()) // Close button
                                    .setNeutralButton("Share QR", (dialogInterface, which) -> {
                                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                        shareIntent.putExtra(Intent.EXTRA_STREAM, BasicButtons.getUriFromBitmap(ShoppingCart.this, bitmap));
                                        shareIntent.putExtra(Intent.EXTRA_TEXT, "Scan this code to book the event " + event.getTitle());
                                        shareIntent.setType("image/*");
                                        startActivity(Intent.createChooser(shareIntent, "Share via"));
                                    }).create();
                            alertDialog.show();
                        });

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("showBoughtEvents", "Error fetching sales: " + error.getMessage());
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("showBoughtEvents", "Error fetching events: " + error.getMessage());
            }
        });
    }

    private void showCartEvents() {
        // Setting recycler view
        RecyclerView recyclerView = findViewById(R.id.recycler_cart);
        TextView empty = findViewById(R.id.empty_cart);

        // Fetch all events
        database.getReference().child("event").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot eventSnapshot) {
                // Map to store events by their UID
                Map<String, Event> eventMap = new HashMap<>();
                for (DataSnapshot eventData : eventSnapshot.getChildren()) {
                    Event event = eventData.getValue(Event.class);
                    if (event != null) {
                        event.setKey(eventData.getKey());
                        eventMap.put(eventData.getKey(), event);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("showBoughtEvents", "Error fetching events: " + error.getMessage());
            }
        });
    }
}