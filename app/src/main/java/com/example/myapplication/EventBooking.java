package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;

public class EventBooking extends AppCompatActivity {
    private FirebaseDatabase database;
    private Event event;
    private String eventUID;
    private Button btn_book;
    private ImageButton btn_save;
    private Button btn_share;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_event_booking);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Firebase setting
        FirebaseApp.initializeApp(EventBooking.this);
        database = FirebaseDatabase.getInstance("https://ing-soft-firebase-default-rtdb.europe-west1.firebasedatabase.app/");
        eventUID = getIntent().getStringExtra("event_uid");

        // Setting basic buttons
        Button logout_btn = findViewById(R.id.logout_btn);
        logout_btn.setOnClickListener(v -> BasicButtons.handleLogoutButton(EventBooking.this));
        Button name_btn = findViewById(R.id.user);
        BasicButtons.checkUserAndSetNameButton(EventBooking.this, name_btn);
        ImageButton backButton = findViewById(R.id.back);
        BasicButtons.handleBackButton(EventBooking.this, backButton);


        // Fetch event data from Firebase
        database.getReference().child("event").child(eventUID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    event = snapshot.getValue(Event.class);
                    if (event != null) {
                        // Finding XML elements
                        TextView title = findViewById(R.id.title);
                        TextView place = findViewById(R.id.place);
                        TextView description = findViewById(R.id.desc);
                        TextView date = findViewById(R.id.date);
                        TextView time = findViewById(R.id.time);
                        btn_book = findViewById(R.id.book);
                        btn_save = findViewById(R.id.save);
                        btn_share = findViewById(R.id.share);

                        LocalDateTime dateEvent = LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getDate()), ZoneId.systemDefault());
                        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
                        symbols.setDecimalSeparator(',');

                        String dateText = dateEvent.getDayOfMonth() + "/" + dateEvent.getMonthValue() + "/" + dateEvent.getYear();
                        String timeText = dateEvent.getHour() + ":" + String.format(Locale.getDefault(), "%02d", dateEvent.getMinute());
                        List<String> salesList = new ArrayList<>();

                        database.getReference().child("sale").orderByChild("eventUID").equalTo(eventUID).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (!snapshot.exists()) {
                                    Log.d("SalesList", "No sales found for event: " + eventUID);
                                } else {
                                    Log.d("SalesList", "Sales found for event: " + eventUID);

                                    for (DataSnapshot saleSnapshot : snapshot.getChildren()) {
                                        // Retrieve the userUID
                                        String userUID = saleSnapshot.child("userUID").getValue(String.class);
                                        salesList.add(userUID);
                                    }

                                    String currentUserUID = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
                                    Log.d("EventBooking", "User UID: " + currentUserUID);
                                    Log.d("EventBooking", "Sales List: " + salesList);

                                    // Set event data
                                    title.setText(event.getTitle());
                                    place.setText(event.getPlace());
                                    description.setText(event.getDescription());
                                    date.setText(dateText);
                                    time.setText(timeText);
                                    Log.d("salesList", "é contenuto" + salesList.contains(currentUserUID));
                                    if (event.getNum_subs() < event.getSeats() || event.getSeats() == 0 || !event.getOrganizer().equals(currentUserUID) && !salesList.contains(currentUserUID)) {
                                        if (event.getPrice() == 0)
                                            btn_book.setText(R.string.free);
                                        else {
                                            String priceText = new DecimalFormat("#0.00", symbols).format(event.getPrice()) + "€";
                                            btn_book.setText(priceText);
                                        }
                                    } else {
                                        btn_book.setEnabled(false);
                                        if (salesList.contains(currentUserUID))
                                            btn_book.setText(R.string.bought);
                                        else
                                            btn_book.setText(R.string.sold_out);
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                // Handle possible errors
                                Log.e("SalesList", "Error fetching sales", error.toException());
                            }
                        });

                        // Set the click listeners
                        bookEvent(eventUID, event);
                        shareSaveBtn(event.getTitle());
                    }
                } else {
                    Toast.makeText(EventBooking.this, "No event found with the given QR code", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("EventBooking", "Error fetching event: " + error.getMessage());
            }
        });
    }

    private void bookEvent(String eventUID, Event event) {
        btn_book.setOnClickListener(v -> {
            // Get the current user's UID
            String user_uid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
            Log.d("EventBooking", "User UID: " + user_uid + " | Event UID: " + eventUID);

            // Create a new subscription (sale)
            Sale sale = new Sale();
            sale.setEventUID(eventUID);
            sale.setUserUID(user_uid);
            sale.setTime(LocalDateTime.now());
            sale.setSeat(event.getNum_subs()+1);
            database.getReference().child("sale").push().setValue(sale);
            Log.d("EventBooking", "Sale added to Firebase");

            // Update the event number of subscriptions
            event.addNum_subs();
            database.getReference().child("event").child(eventUID).setValue(event)
                    .addOnSuccessListener(aVoid -> Toast.makeText(EventBooking.this, "Successfully updated number of subscriptions!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(EventBooking.this, "Error updating event", Toast.LENGTH_SHORT).show());

            btn_book.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.subscribed, 0);
            btn_book.setText(R.string.bought);
            btn_book.setEnabled(false);
        });
    }

    private void shareSaveBtn(String titleEvent) {
        // Initialize saved event list and set initial icon
        Set<String> savedEventKeys = EventFileManager.loadEvents(this);
        Log.d("EventBooking", "Saved Event Keys: " + savedEventKeys);
        updateBookmarkIcon(savedEventKeys.contains(eventUID));

        // Set SAVE button
        btn_save.setOnClickListener(v -> {
            if (savedEventKeys.contains(eventUID)) {
                // REMOVE EVENT FROM SAVED EVENT
                savedEventKeys.remove(eventUID);
                Log.d("EventBooking", "Removed event with key: " + eventUID + " | OR | "  + event.getKey());
                Log.d("EventBooking", "Saved Event Keys: " + savedEventKeys);
                Toast.makeText(this, "Event removed from saved events", Toast.LENGTH_SHORT).show();
            } else {
                // ADD EVENT TO SAVED EVENT
                savedEventKeys.add(eventUID);
                Log.d("EventBooking", "Added event with key: " + eventUID + " | OR | " + event.getKey());
                Log.d("EventBooking", "Saved Event Keys: " + savedEventKeys);
                Toast.makeText(this, "Event added to saved events", Toast.LENGTH_SHORT).show();
            }

            // Update SharedPreferences and bookmark icon
            EventFileManager.saveEvents(this, savedEventKeys);
            updateBookmarkIcon(savedEventKeys.contains(eventUID));
        });

        //Set SHARE button
        btn_share.setOnClickListener(v -> {
            QRGEncoder qrgEncoder = new QRGEncoder(eventUID, null, QRGContents.Type.TEXT, 200);

            // !!!SOS!!! BUG FORSE DELLA LIBRERIA!? I COLORI DEL QR CODE SONO INVERTITI
            // Set colors for the QR code
            qrgEncoder.setColorBlack(Color.WHITE);
            qrgEncoder.setColorWhite(Color.BLACK);
            Bitmap bitmap = qrgEncoder.getBitmap();

            // Share QR code
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, BasicButtons.getUriFromBitmap(EventBooking.this, bitmap));
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Scan this code to book the event " + titleEvent);
            shareIntent.setType("image/*");
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        });
    }

    // Helper method to update the bookmark icon
    private void updateBookmarkIcon(boolean isSaved) {
        if (isSaved)
            btn_save.setImageResource(R.drawable.bookmark_full);
        else
            btn_save.setImageResource(R.drawable.bookmark_empty);
    }
}