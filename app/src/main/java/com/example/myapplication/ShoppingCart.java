package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

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
import java.util.Locale;
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

        Log.d("ShoppingCart", "onCreate called");

        // Firebase setting
        FirebaseApp.initializeApp(ShoppingCart.this);
        database = FirebaseDatabase.getInstance("https://ing-soft-firebase-default-rtdb.europe-west1.firebasedatabase.app/");

        // Setting basic buttons
        Button logout_btn = findViewById(R.id.logout_btn);
        logout_btn.setOnClickListener(v -> BasicButtons.handleLogoutButton(ShoppingCart.this));
        Button name_btn = findViewById(R.id.user);
        BasicButtons.checkUserAndSetNameButton(ShoppingCart.this, name_btn);
        ImageButton backButton = findViewById(R.id.back);
        BasicButtons.handleBackButton(ShoppingCart.this, backButton);

        //showCartEvents();
        showBoughtEvents();
    }

    private void showBoughtEvents() {
        // Setting recycler view
        RecyclerView recyclerView = findViewById(R.id.recycler_sale);
        TextView empty = findViewById(R.id.empty_sale);
        Log.d("showBoughtEvents", "showBoughtEvents called");

        // Filter sales for the current user
        String currentUserUID = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        database.getReference().child("sale").orderByChild("userUID").equalTo(currentUserUID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot saleSnapshot) {
                if (!saleSnapshot.exists()) {
                    Log.d("ShoppingCart", "No sales found for user: " + currentUserUID);
                    empty.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    Log.d("SalesList", "Sales found for user: " + currentUserUID);
                    empty.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);

                    ArrayList<Event> arrayList = new ArrayList<>();
                    for (DataSnapshot saleData : saleSnapshot.getChildren()) {
                        String eventUID = saleData.child("eventUID").getValue(String.class);

                        if (eventUID != null) {
                            database.getReference().child("event").child(eventUID).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot eventSnapshot) {
                                    if (eventSnapshot.exists()) {
                                        Event event = eventSnapshot.getValue(Event.class);
                                        if (event != null) {
                                            event.setKey(eventSnapshot.getKey());
                                            Log.d("showBoughtEvents", "Title: " + event.getTitle() + " | Event: " + event.getKey());
                                            arrayList.add(event);
                                        }
                                    }
                                    updateRecyclerView(arrayList, recyclerView);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e("showBoughtEvents", "Error fetching event: " + error.getMessage());
                                }
                            });
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("showBoughtEvents", "Error fetching sales: " + error.getMessage());
            }
        });
    }

    private void updateRecyclerView(ArrayList<Event> eventList, RecyclerView recyclerView) {
        // Check if the event list is empty
        if (eventList.isEmpty()) {
            Log.d("ShoppingCart", "No events to display");
            // You can show a message or do something else if the list is empty
        }

        // Create the adapter and set it to the RecyclerView
        EventAdapter adapter = new EventAdapter(ShoppingCart.this, eventList, 3);
        recyclerView.setAdapter(adapter);
        //adapter.notifyDataSetChanged();   // Update the UI if necessary

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
            Button btn_showSales = dialogView.findViewById(R.id.salesBtn);
            ImageView qrImage = dialogView.findViewById(R.id.qrImage);
            LocalDateTime dateEvent = LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getDate()), ZoneId.systemDefault());

            DecimalFormatSymbols symbols = new DecimalFormatSymbols();
            symbols.setDecimalSeparator(',');
            String dateText = dateEvent.getDayOfMonth() + "/" + dateEvent.getMonthValue() + "/" + dateEvent.getYear();
            String timeText = dateEvent.getHour() + ":" + String.format(Locale.getDefault(), "%02d", dateEvent.getMinute());
            QRGEncoder qrgEncoder = new QRGEncoder(event.getKey(), null, QRGContents.Type.TEXT, 200);
            Log.d("showBoughtEvents", "Event: " + event);

            // BUG FORSE DELLA LIBRERIA!? I COLORI DEL QR CODE SONO INVERTITI
            qrgEncoder.setColorBlack(Color.WHITE);
            qrgEncoder.setColorWhite(Color.BLACK);
            Bitmap bitmap = qrgEncoder.getBitmap();
            Log.d("showBoughtEvents", "QR code generated: " + bitmap);

            // Set text and visibility
            title.setText(event.getTitle());
            place.setText(event.getPlace());
            desc.setText(event.getDescription());
            date.setText(dateText);
            time.setText(timeText);
            seats.setVisibility(View.GONE);
            btn_showSales.setVisibility(View.GONE);
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