package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;
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
import java.util.Objects;

public class EventBooking extends AppCompatActivity {

    private FirebaseDatabase database;

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

        // Retrieve the QR code content passed from QrScanner activity
        String scannedQRCode = getIntent().getStringExtra("scannedQRCode");

        database.getReference().child("event").orderByChild("key").equalTo(scannedQRCode).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Event with matching UID found, get the event details
                    for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
                        Event event = eventSnapshot.getValue(Event.class);
                        if (event != null) {
                            // Finding and setting XML elements
                            TextView title = findViewById(R.id.title);
                            TextView place = findViewById(R.id.place);
                            TextView description = findViewById(R.id.desc);
                            TextView date = findViewById(R.id.date);
                            TextView time = findViewById(R.id.time);
                            Button btn_book = findViewById(R.id.book);
                            LocalDateTime dateEvent = LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getDate()), ZoneId.systemDefault());
                            DecimalFormatSymbols symbols = new DecimalFormatSymbols();
                            symbols.setDecimalSeparator(',');

                            String dateText = dateEvent.getDayOfMonth() + "/" + dateEvent.getMonthValue() + "/" + dateEvent.getYear();
                            String timeText = dateEvent.getHour() + ":" + String.format("%02d", dateEvent.getMinute());

                            title.setText(event.getTitle());
                            place.setText(event.getPlace());
                            description.setText(event.getDescription());
                            date.setText(dateText);
                            time.setText(timeText);
                            if (event.getNum_subs() < event.getSeats()) {
                                if (event.getPrice() == 0)
                                    btn_book.setText(R.string.free);
                                else
                                    btn_book.setText(new DecimalFormat("#0.00", symbols).format(event.getPrice()));
                            } else {
                                btn_book.setText("Sold out!");
                                btn_book.setEnabled(false);
                            }

                            // Set the click listener for the "Book" button
                            bookEvent(btn_book, scannedQRCode, event);
                        }
                    }
                } else
                    Toast.makeText(EventBooking.this, "No event found with the given UID", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EventBooking.this, "Error loading event", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bookEvent(Button btn_book, String scannedQRCode, Event event) {
        btn_book.setOnClickListener(v -> {
            // Get the current user's UID
            String user_uid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

            // Create a new subscription (sale)
            Sale sale = new Sale();
            sale.setEventUID(scannedQRCode);
            sale.setUserUID(user_uid);
            sale.setTime(LocalDateTime.now().toString());
            database.getReference().child("sale").push().setValue(sale);

            // Update the event number of subscriptions
            event.addNum_subs();
            database.getReference().child("event").child(scannedQRCode).setValue(event)
                    .addOnSuccessListener(aVoid -> Toast.makeText(EventBooking.this, "Successfully updated number of subscriptions!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(EventBooking.this, "Error updating event", Toast.LENGTH_SHORT).show());
        });
    }
}