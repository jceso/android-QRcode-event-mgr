package com.example.myapplication;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Objects;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;

public class EventListAdmin extends AppCompatActivity {

    private FirebaseDatabase database;
    private int[] dateInfos;
    private Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_event_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Firebase setting
        FirebaseApp.initializeApp(EventListAdmin.this);
        database = FirebaseDatabase.getInstance("https://ing-soft-firebase-default-rtdb.europe-west1.firebasedatabase.app/");

        // CRUD setting
        dateInfos = new int[] {1, 1, 1, 1, 1};
        FloatingActionButton addButton = findViewById(R.id.addButton);

        addButton.setOnClickListener(v -> addOnDB());
        editDeleteEvent();
    }

    private void addOnDB() {
        // Setting view and finding XML elements
        View dialogView = LayoutInflater.from(EventListAdmin.this).inflate(R.layout.add_event_dialog, null);
        EditText title = dialogView.findViewById(R.id.titleET);
        EditText place = dialogView.findViewById(R.id.placeET);
        EditText desc = dialogView.findViewById(R.id.descET);
        Button btn_date = dialogView.findViewById(R.id.btn_date);
        Button btn_time = dialogView.findViewById(R.id.btn_time);
        EditText price = dialogView.findViewById(R.id.priceET);
        EditText seats = dialogView.findViewById(R.id.seatsET);

        // Set date and time picker
        dateDialog(btn_date, btn_time, LocalDateTime.now());

        AlertDialog alertDialog = new AlertDialog.Builder(EventListAdmin.this)
                .setTitle("Add event")
                .setView(dialogView)
                .setPositiveButton("Add", (dialogInterface, which) -> {
                    if (title.getText().toString().isEmpty())
                        title.setError("Title is required");
                    else if (place.getText().toString().isEmpty())
                        place.setError("Place is required");
                    else if (desc.getText().toString().isEmpty())
                        desc.setError("Description is required");
                    else {
                        ProgressBar progressBar = findViewById(R.id.progress_bar);
                        progressBar.setVisibility(View.VISIBLE);
                        Event event = new Event();

                        // Price setting
                        if (!price.getText().toString().isEmpty())
                            event.setPrice(Float.parseFloat(price.getText().toString()));
                        else
                            event.setPrice(0);

                        // Availability setting
                        if (!seats.getText().toString().isEmpty())
                            event.setSeats(Integer.parseInt(seats.getText().toString()));
                        else
                            event.setSeats(0);

                        // Rest of event setting
                        event.setTitle(title.getText().toString());
                        event.setPlace(place.getText().toString());
                        event.setDescription(desc.getText().toString());
                        event.setDate(dateInfos[0], dateInfos[1], dateInfos[2], dateInfos[3], dateInfos[4]);

                        database.getReference().child("event").push().setValue(event).addOnSuccessListener(unused -> {
                            progressBar.setVisibility(View.GONE);
                            dialogInterface.dismiss();
                            Toast.makeText(EventListAdmin.this, "Saved successfully", Toast.LENGTH_SHORT).show();
                        }).addOnFailureListener(e -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(EventListAdmin.this, "There was an error while saving data", Toast.LENGTH_SHORT).show();
                        });
                    }
                }).setNeutralButton("Cancel", (dialogInterface, which) -> dialogInterface.dismiss())
                .create();
        alertDialog.show();
    }

    private void editDeleteEvent() {
        // Setting recycler view
        RecyclerView recyclerView = findViewById(R.id.recycler);
        TextView empty = findViewById(R.id.empty_list);

        database.getReference().child("event").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<Event> arrayList = new ArrayList<>();

                // Show events in recycler view and set adapter
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Event event = dataSnapshot.getValue(Event.class);
                    Objects.requireNonNull(event).setKey(dataSnapshot.getKey());
                    arrayList.add(event);
                }
                if (arrayList.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    empty.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    empty.setVisibility(View.GONE);
                }

                DecimalFormatSymbols symbols = new DecimalFormatSymbols();
                symbols.setDecimalSeparator(',');
                ProgressBar progressBar = findViewById(R.id.progress_bar);
                EventAdapter adapter = new EventAdapter(EventListAdmin.this, arrayList);
                recyclerView.setAdapter(adapter);

                // Show details of event on dialog
                adapter.setOnItemClickListener(event -> {
                    // Setting view and finding XML elements
                    View view = LayoutInflater.from(EventListAdmin.this).inflate(R.layout.event_dialog, null);
                    TextView title = view.findViewById(R.id.title);
                    TextView place = view.findViewById(R.id.place);
                    TextView date = view.findViewById(R.id.date);
                    TextView time = view.findViewById(R.id.time);
                    TextView desc = view.findViewById(R.id.desc);
                    TextView price = view.findViewById(R.id.price);
                    TextView seats = view.findViewById(R.id.seats);
                    ImageView qrImage = view.findViewById(R.id.qrImage);
                    LocalDateTime dateEvent = LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getDate()), ZoneId.systemDefault());

                    String dateText = dateEvent.getDayOfMonth() + "/" + dateEvent.getMonthValue() + "/" + dateEvent.getYear();
                    String timeText = dateEvent.getHour() + ":" + String.format("%02d", dateEvent.getMinute());
                    QRGEncoder qrgEncoder = new QRGEncoder(event.getKey(), null, QRGContents.Type.TEXT, 200);
                    bitmap = qrgEncoder.getBitmap();

                    title.setText(event.getTitle());
                    place.setText(event.getPlace());
                    desc.setText(event.getDescription());
                    date.setText(dateText);
                    time.setText(timeText);
                    if (event.getPrice() == 0)
                        price.setText("Free");
                    else
                        price.setText(new DecimalFormat("#0.00", symbols).format(event.getPrice()));
                    if (event.getSeats() == 0)
                        seats.setText("No limits");
                    else
                        seats.setHint(String.valueOf(event.getSeats()));
                    qrImage.setImageBitmap(bitmap);

                    // Create and show the AlertDialog
                    AlertDialog alertDialog = new AlertDialog.Builder(EventListAdmin.this)
                            .setTitle("Event Details")
                            .setView(view)
                            .setPositiveButton("Close", (dialog, which) -> dialog.dismiss()) // Close button
                            .setNeutralButton("Share QR", (dialogInterface, which) -> {
                                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                shareIntent.putExtra(Intent.EXTRA_STREAM, getUriFromBitmap());
                                shareIntent.putExtra(Intent.EXTRA_TEXT, "Scan this code to book the event " + event.getTitle());
                                shareIntent.setType("image/*");
                                startActivity(Intent.createChooser(shareIntent, "Share via"));
                            }).setNegativeButton("Delete", (dialogInterface, which) -> {
                                progressBar.setVisibility(View.VISIBLE);

                                database.getReference().child("event").child(event.getKey()).removeValue().addOnSuccessListener(unused -> {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(EventListAdmin.this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                                }).addOnFailureListener(e -> {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(EventListAdmin.this, "There was an error while deleting data", Toast.LENGTH_SHORT).show();
                                });
                            })
                            .create();
                    alertDialog.show();
                });

                // Edit details of event on dialog
                adapter.setOnEditButtonClickListener(event -> {
                    // Setting view and finding XML elements
                    View view = LayoutInflater.from(EventListAdmin.this).inflate(R.layout.add_event_dialog, null);
                    TextView title = view.findViewById(R.id.titleET);
                    TextView place = view.findViewById(R.id.placeET);
                    TextView desc = view.findViewById(R.id.descET);
                    Button btn_date = view.findViewById(R.id.btn_date);
                    Button btn_time = view.findViewById(R.id.btn_time);
                    EditText price = view.findViewById(R.id.priceET);
                    EditText seats = view.findViewById(R.id.seatsET);
                    LocalDateTime dateEvent = LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getDate()), ZoneId.systemDefault());

                    String btnDateText = dateEvent.getDayOfMonth() + "/" + dateEvent.getMonthValue() + "/" + dateEvent.getYear();
                    String btnTimeText = dateEvent.getHour() + ":" + String.format("%02d", dateEvent.getMinute());
                    dateDialog(btn_date, btn_time, dateEvent);

                    title.setText(event.getTitle());
                    place.setText(event.getPlace());
                    desc.setText(event.getDescription());
                    btn_date.setText(btnDateText);
                    btn_time.setText(btnTimeText);
                    if (event.getPrice() == 0)
                        price.setHint("Free");
                    else
                        price.setHint(String.valueOf(event.getPrice()));
                    if (event.getSeats() == 0)
                        seats.setHint("No limits");
                    else
                        seats.setHint(String.valueOf(event.getSeats()));

                    AlertDialog alertDialog = new AlertDialog.Builder(EventListAdmin.this)
                            .setTitle("Edit")
                            .setView(view)
                            .setPositiveButton("Save", (dialogInterface, which) -> {
                                if (title.getText().toString().isEmpty())
                                    title.setError("Title is required");
                                if (place.getText().toString().isEmpty())
                                    place.setError("Place is required");
                                if (desc.getText().toString().isEmpty())
                                    desc.setError("Description is required");

                                Event eventUpd = new Event();

                                // Price setting
                                if (!price.getText().toString().isEmpty())
                                    eventUpd.setPrice(Float.parseFloat(price.getText().toString()));
                                else if (price.getHint() == "Free")
                                    eventUpd.setSeats(0);
                                else
                                    eventUpd.setPrice(Float.parseFloat(price.getHint().toString()));

                                // Availability setting
                                if (!seats.getText().toString().isEmpty())
                                    eventUpd.setSeats(Integer.parseInt(seats.getText().toString()));
                                else if (seats.getHint() == "No limits")
                                    eventUpd.setSeats(0);
                                else
                                    eventUpd.setSeats(Integer.parseInt(seats.getHint().toString()));

                                // Rest of event setting
                                eventUpd.setTitle(title.getText().toString());
                                eventUpd.setPlace(place.getText().toString());
                                eventUpd.setDescription(desc.getText().toString());
                                eventUpd.setDate(dateInfos[0], dateInfos[1], dateInfos[2], dateInfos[3], dateInfos[4]);

                                progressBar.setVisibility(View.VISIBLE);
                                database.getReference().child("event").child(event.getKey()).setValue(eventUpd).addOnSuccessListener(unused -> {
                                    progressBar.setVisibility(View.GONE);
                                    dialogInterface.dismiss();
                                    Toast.makeText(EventListAdmin.this, "Saved successfully", Toast.LENGTH_SHORT).show();
                                }).addOnFailureListener(e -> {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(EventListAdmin.this, "There was an error while saving data", Toast.LENGTH_SHORT).show();
                                });
                            }).setNeutralButton("Close", (dialogInterface, which) -> dialogInterface.dismiss())
                            .setNegativeButton("Delete", (dialogInterface, which) -> {
                                progressBar.setVisibility(View.VISIBLE);

                                database.getReference().child("event").child(event.getKey()).removeValue().addOnSuccessListener(unused -> {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(EventListAdmin.this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                                }).addOnFailureListener(e -> {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(EventListAdmin.this, "There was an error while deleting data", Toast.LENGTH_SHORT).show();
                                });
                            }).create();
                    alertDialog.show();
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void dateDialog(Button btn_date, Button btn_time, LocalDateTime editDate) {
        dateInfos = new int[] { editDate.getDayOfMonth(), editDate.getMonthValue(), editDate.getYear(), editDate.getHour(), editDate.getMinute() };

        // Date picker
        btn_date.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(EventListAdmin.this, (view, year, month, day) -> {
                dateInfos[0] = day;
                dateInfos[1] = month+1;   //Convert 0-based month to 1-based (DatePickerDialog -> LocalDateTime)
                dateInfos[2] = year;
                String dateText = day + "/" + (month+1) + "/" + year;

                // Ensure month is valid (1-12)
                if (dateInfos[1] < 1 || dateInfos[1] > 12) {
                    // If the month is invalid, set to a default valid month, e.g., January (1)
                    Log.d("DatePickerDialog", "Invalid month: " + dateInfos[1]);
                }

                Log.d("DatePickerDialog", "Selected date: " + dateInfos[0] + "/" + dateInfos[1] + "/" + dateInfos[2]);
                btn_date.setText(dateText);
            }, editDate.getYear(), editDate.getMonthValue()-1, editDate.getDayOfMonth());
            dialog.show();
        });

        // Time picker
        btn_time.setOnClickListener(v -> {
            TimePickerDialog dialog = new TimePickerDialog(EventListAdmin.this, (view, hour, minute) -> {
                dateInfos[3] = hour;
                dateInfos[4] = minute;
                String timeText = hour + ":" + String.format("%02d", minute);

                btn_time.setText(timeText);
            }, editDate.getHour(), editDate.getMinute(), true);
            dialog.show();
        });
    }

    private Uri getUriFromBitmap() {
        try {
            // Create a file to store the QR code image
            File file = new File(this.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "qr_code_" + System.currentTimeMillis() + ".png");

            // Write the bitmap to the file
            FileOutputStream outStream = new FileOutputStream(file);
            if (bitmap != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, outStream);
                outStream.close();
            }

            // Get the URI for the file using FileProvider
            return FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", file);
        } catch (IOException e) {
            Log.e("QR-Sharing", "getUriFromBitmap: " + e.getMessage());
        }

        return null;
    }
}