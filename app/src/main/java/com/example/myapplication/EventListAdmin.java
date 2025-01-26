package com.example.myapplication;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;

public class EventListAdmin extends AppCompatActivity {

    private FirebaseDatabase database;
    private FirebaseFirestore fstore;
    private FirebaseUser user;
    private int[] dateInfos;
    private Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_event_list_admin);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Firebase setting
        FirebaseApp.initializeApp(EventListAdmin.this);
        database = FirebaseDatabase.getInstance("https://ing-soft-firebase-default-rtdb.europe-west1.firebasedatabase.app/");
        user = FirebaseAuth.getInstance().getCurrentUser();
        fstore = FirebaseFirestore.getInstance();

        // Setting basic buttons
        Button logout_btn = findViewById(R.id.logout_btn);
        logout_btn.setOnClickListener(v -> BasicButtons.handleLogoutButton(EventListAdmin.this));
        Button name_btn = findViewById(R.id.user);
        BasicButtons.checkUserAndSetNameButton(EventListAdmin.this, name_btn);
        ImageButton backButton = findViewById(R.id.back);
        BasicButtons.handleBackButton(EventListAdmin.this, backButton);

        // CRUD setting
        dateInfos = new int[] {1, 1, 1, 1, 1};
        FloatingActionButton addButton = findViewById(R.id.addButton);
        FloatingActionButton svdButton = findViewById(R.id.svdButton);

        svdButton.setOnClickListener(v -> {
            Intent intent = new Intent(EventListAdmin.this, EventListUser.class);
            intent.putExtra("targetActivity", EventListAdmin.class); // Send back callback
            startActivity(intent);
        });

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

        AlertDialog addDialog = new AlertDialog.Builder(EventListAdmin.this)
            .setTitle("Add event")
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("Create Event", null) // We will handle the click manually
            .setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss())
            .create();

        // Set a listener for the positive button after the dialog is created
        addDialog.setOnShowListener(d -> addDialog
            .getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
                boolean isValid = true;

                // Check if title, place, and description are empty
                if (title.getText().toString().trim().isEmpty() || title.getText().toString().length() > 20) {
                    title.setError("Title is required and must have less than 20 characters");
                    isValid = false;
                } else
                    title.setError(null);

                if (place.getText().toString().trim().isEmpty() || place.getText().toString().length() > 20) {
                    place.setError("Place is required and must have less than 20 characters");
                    isValid = false;
                } else
                    place.setError(null);

                if (btn_date.getText().toString().equals("Date")) {
                    btn_date.setError("Date is required");
                    isValid = false;
                } else
                    btn_date.setError(null);

                if (btn_time.getText().toString().equals("Time")) {
                    btn_time.setError("Time is required");
                    isValid = false;
                } else
                    btn_time.setError(null);

                if (desc.getText().toString().trim().isEmpty()) {
                    desc.setError("Description is required");
                    isValid = false;
                } else
                    desc.setError(null);

                if (!price.getText().toString().trim().isEmpty() && !price.getText().toString().trim().matches("^\\d+(\\.\\d{1,2})?$")) {
                    price.setError("Price must be a valid number (ex 12, 12.3, 12.34");
                    isValid = false;
                } else
                    price.setError(null);

                if (!seats.getText().toString().trim().isEmpty() && !(Integer.parseInt(seats.getText().toString().trim()) <= 0)) {
                    seats.setError("Availability must be greater than 0");
                    isValid = false;
                } else {
                    seats.setError(null);
                }

                // If any input is invalid, don't dismiss the dialog
                if (!isValid)
                    return;

                // Proceed with saving the event if all validations are passed
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
                event.setOrganizer(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid());

                // Save event to Firebase
                database.getReference().child("event").push().setValue(event).addOnSuccessListener(unused -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(EventListAdmin.this, "Saved successfully", Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(EventListAdmin.this, "There was an error while saving data", Toast.LENGTH_SHORT).show();
                });

                // Close the dialog after event creation
                addDialog.dismiss();
        }));
        addDialog.show();
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
                EventAdapter adapter = new EventAdapter(EventListAdmin.this, arrayList, 1);
                recyclerView.setAdapter(adapter);

                // SHOW details of event on dialog
                adapter.setOnEventListener(event -> {
                    // Setting view and finding XML elements
                    View dialogView = LayoutInflater.from(EventListAdmin.this).inflate(R.layout.event_dialog, null);
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

                    String dateText = dateEvent.getDayOfMonth() + "/" + dateEvent.getMonthValue() + "/" + dateEvent.getYear();
                    String timeText = dateEvent.getHour() + ":" + String.format(Locale.getDefault(), "%02d", dateEvent.getMinute());
                    QRGEncoder qrgEncoder = new QRGEncoder(event.getKey(), null, QRGContents.Type.TEXT, 200);

                    // BUG FORSE DELLA LIBRERIA!? I COLORI DEL QR CODE SONO INVERTITI
                    qrgEncoder.setColorBlack(Color.WHITE);
                    qrgEncoder.setColorWhite(Color.BLACK);
                    bitmap = qrgEncoder.getBitmap();

                    // Set text and visibility
                    title.setText(event.getTitle());
                    place.setText(event.getPlace());
                    desc.setText(event.getDescription());
                    date.setText(dateText);
                    time.setText(timeText);
                    fstore.collection("Users").document(event.getOrganizer()).get().addOnSuccessListener(documentSnapshot -> {
                        // Get organizer of the event
                        if (documentSnapshot.exists() && !event.getOrganizer().equals(user.getUid()))
                            organizer.setText(String.format("Organized by %s", documentSnapshot.getString("UserName")));
                        else
                            organizer.setVisibility(View.GONE);
                    });

                    if (event.getPrice() == 0)
                        price.setText(R.string.free);
                    else
                        price.setText(new DecimalFormat("#0.00", symbols).format(event.getPrice()));
                    if (event.getSeats() == 0)
                        seats.setText(R.string.availability);
                    else
                        seats.setHint(String.valueOf(event.getSeats()));
                    qrImage.setImageBitmap(bitmap);

                    btn_showSales.setOnClickListener(v -> {
                        Intent intent = new Intent(EventListAdmin.this, SalesList.class);
                        intent.putExtra("event_uid", event.getKey());
                        startActivity(intent);
                    });

                    AlertDialog alertDialog = new AlertDialog.Builder(EventListAdmin.this)
                            .setTitle("Event Details")
                            .setView(dialogView)
                            .setPositiveButton("Close", (dialog, which) -> dialog.dismiss()) // Close button
                            .setNeutralButton("Share QR", (dialogInterface, which) -> {
                                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                shareIntent.putExtra(Intent.EXTRA_STREAM, BasicButtons.getUriFromBitmap(EventListAdmin.this, bitmap));
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

                // EDIT details of event on dialog
                adapter.setOnEditListener(event -> {
                    // Setting view and finding XML elements
                    View dialogView = LayoutInflater.from(EventListAdmin.this).inflate(R.layout.add_event_dialog, null);
                    TextView title = dialogView.findViewById(R.id.titleET);
                    TextView place = dialogView.findViewById(R.id.placeET);
                    TextView desc = dialogView.findViewById(R.id.descET);
                    Button btn_date = dialogView.findViewById(R.id.btn_date);
                    Button btn_time = dialogView.findViewById(R.id.btn_time);
                    EditText price = dialogView.findViewById(R.id.priceET);
                    EditText seats = dialogView.findViewById(R.id.seatsET);
                    LocalDateTime dateEvent = LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getDate()), ZoneId.systemDefault());

                    String btnDateText = dateEvent.getDayOfMonth() + "/" + dateEvent.getMonthValue() + "/" + dateEvent.getYear();
                    String btnTimeText = dateEvent.getHour() + ":" + String.format(Locale.getDefault(), "%02d", dateEvent.getMinute());
                    dateDialog(btn_date, btn_time, dateEvent);

                    // Set text and visibility
                    title.setText(event.getTitle());
                    place.setText(event.getPlace());
                    desc.setText(event.getDescription());
                    btn_date.setText(btnDateText);
                    btn_time.setText(btnTimeText);
                    if (event.getPrice() == 0)
                        price.setHint(R.string.free);
                    else
                        price.setHint(String.valueOf(event.getPrice()));
                    if (event.getSeats() == 0)
                        seats.setHint(R.string.availability);
                    else
                        seats.setHint(String.valueOf(event.getSeats()));


                    AlertDialog editDialog = new AlertDialog.Builder(EventListAdmin.this)
                            .setTitle("Edit event")
                            .setView(dialogView) // This is your custom dialog view
                            .setCancelable(false)
                            .setPositiveButton("Save", null) // We will handle the click manually
                            .setNeutralButton("Close", (dialogInterface, which) -> dialogInterface.dismiss())
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

                    // Set a listener for the positive button after the dialog is created
                    editDialog.setOnShowListener(d -> editDialog
                            .getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
                        boolean isValid = true;

                        // Check if title, place, and description are empty
                        if (title.getText().toString().trim().isEmpty()) {
                            title.setError("Title is required");
                            isValid = false;
                        } else
                            title.setError(null);

                        if (place.getText().toString().trim().isEmpty()) {
                            place.setError("Place is required");
                            isValid = false;
                        } else place.setError(null);

                        if (desc.getText().toString().trim().isEmpty()) {
                            desc.setError("Description is required");
                            isValid = false;
                        } else
                            desc.setError(null);

                        // Don't proceed to save if validation fails
                        if (!isValid)
                            return;

                        progressBar.setVisibility(View.VISIBLE);
                        Event eventUpd = new Event();

                        // Price and availability setting
                        if (!price.getText().toString().isEmpty())
                            eventUpd.setPrice(Float.parseFloat(price.getText().toString()));
                        else if (price.getHint() == "Free")
                            eventUpd.setSeats(0);
                        else
                            eventUpd.setPrice(Float.parseFloat(price.getHint().toString()));
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

                        database.getReference().child("event").child(event.getKey()).setValue(eventUpd).addOnSuccessListener(unused -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(EventListAdmin.this, "Saved successfully", Toast.LENGTH_SHORT).show();
                        }).addOnFailureListener(e -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(EventListAdmin.this, "There was an error while saving data", Toast.LENGTH_SHORT).show();
                        });

                        editDialog.dismiss();
                    }));

                    editDialog.show();
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
                String timeText = hour + ":" + String.format(Locale.getDefault(), "%02d", minute);

                btn_time.setText(timeText);
            }, editDate.getHour(), editDate.getMinute(), true);
            dialog.show();
        });
    }
}