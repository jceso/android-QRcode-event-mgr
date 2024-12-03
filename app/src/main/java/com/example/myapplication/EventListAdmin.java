package com.example.myapplication;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Objects;

public class EventListAdmin extends AppCompatActivity {

    private FirebaseDatabase database;
    private int y, mth, d, h, min;


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

        // Buttons and views
        FloatingActionButton addButton = findViewById(R.id.addButton);

        // CRUD setting
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addOnDB();
            }
        });

        editDeleteEvent();

    }

    private void addOnDB() {
        View dialogView = LayoutInflater.from(EventListAdmin.this).inflate(R.layout.add_event_dialog, null);
        EditText title = dialogView.findViewById(R.id.titleET);
        EditText subtitle = dialogView.findViewById(R.id.subtitleET);
        EditText desc = dialogView.findViewById(R.id.descET);
        Button btn_date = dialogView.findViewById(R.id.btn_date);
        Button btn_time = dialogView.findViewById(R.id.btn_time);

        dateDialog(btn_date, btn_time);

        AlertDialog alertDialog = new AlertDialog.Builder(EventListAdmin.this)
                .setTitle("Add event")
                .setView(dialogView)
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        if (title.getText().toString().isEmpty())
                            title.setError("Title is required");
                        else if (subtitle.getText().toString().isEmpty())
                            subtitle.setError("Subtitle is required");
                        else if (desc.getText().toString().isEmpty())
                            desc.setError("Description is required");
                        else {
                            ProgressDialog dialog = new ProgressDialog(EventListAdmin.this);
                            dialog.setMessage("Adding event...");
                            dialog.show();

                            Event event = new Event();
                            event.setTitle(title.getText().toString());
                            event.setPlace(subtitle.getText().toString());
                            event.setDescription(desc.getText().toString());

                            Log.d("date", "Before setting");
                            Log.d("date", "Day: " +d + " | Month: " + mth + " | Year: " + y + "  -  Hour: " + h + " | Minute: " + min);
                            event.setDate(d, mth, y, h, min);

                            /*
                            LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getDate()), ZoneId.systemDefault());
                            Log.d("date", "After the set:\nYear "+date.getYear()+" | Month: "+(date.getMonthValue()+1)+" | Day: "+date.getDayOfMonth()+" | Hour: "+date.getHour()+" | Minute: "+date.getMinute());
                            */

                            database.getReference().child("event").push().setValue(event).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    dialog.dismiss();
                                    dialogInterface.dismiss();
                                    Toast.makeText(EventListAdmin.this, "Saved successfully", Toast.LENGTH_SHORT).show();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    dialog.dismiss();
                                    Toast.makeText(EventListAdmin.this, "There was an error while saving data", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        dialogInterface.dismiss();
                    }
                })
                .create();
        alertDialog.show();
    }

    private void editDeleteEvent() {
        RecyclerView recyclerView = findViewById(R.id.recycler);
        TextView empty = findViewById(R.id.empty_list);

        database.getReference().child("event").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<Event> arrayList = new ArrayList<>();

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

                EventAdapter adapter = new EventAdapter(EventListAdmin.this, arrayList);
                recyclerView.setAdapter(adapter);

                adapter.setOnItemClickListener(new EventAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(Event event) {
                        View view = LayoutInflater.from(EventListAdmin.this).inflate(R.layout.add_event_dialog, null);

                        TextView title = view.findViewById(R.id.titleET);
                        TextView subtitle = view.findViewById(R.id.subtitleET);
                        TextView desc = view.findViewById(R.id.descET);
                        Button btn_date = view.findViewById(R.id.btn_date);
                        Button btn_time = view.findViewById(R.id.btn_time);
                        //LocalDateTime date = event.getDate();

                        title.setText(event.getTitle());
                        subtitle.setText(event.getPlace());
                        desc.setText(event.getDescription());
                        //btn_date.setText(date.getDayOfMonth() + "/" + (date.getMonthValue() + 1) + "/" + date.getYear());
                        //btn_time.setText(date.getHour() + ":" + date.getMinute());

                        ProgressDialog progressDialog = new ProgressDialog(EventListAdmin.this);
                        AlertDialog alertDialog = new AlertDialog.Builder(EventListAdmin.this)
                                .setTitle("Edit")
                                .setView(view)
                                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int which) {
                                        if (title.getText().toString().isEmpty())
                                            title.setError("Title is required");
                                        if (subtitle.getText().toString().isEmpty())
                                            subtitle.setError("Subtitle is required");
                                        if (desc.getText().toString().isEmpty())
                                            desc.setError("Description is required");

                                        progressDialog.setMessage("Saving...");
                                        progressDialog.show();

                                        Event eventUpd = new Event();
                                        eventUpd.setTitle(title.getText().toString());
                                        eventUpd.setPlace(subtitle.getText().toString());
                                        eventUpd.setDescription(desc.getText().toString());

                                        database.getReference().child("event").child(event.getKey()).setValue(eventUpd).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                progressDialog.dismiss();
                                                dialogInterface.dismiss();
                                                Toast.makeText(EventListAdmin.this, "Saved successfully", Toast.LENGTH_SHORT).show();
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                progressDialog.dismiss();
                                                Toast.makeText(EventListAdmin.this, "There was an error while saving data", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                }).setNeutralButton("Close", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int which) {
                                        dialogInterface.dismiss();
                                    }
                                }).setNegativeButton("Delete", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int which) {
                                        progressDialog.setMessage("Deleting...");
                                        progressDialog.show();

                                        database.getReference().child("event").child(event.getKey()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                progressDialog.dismiss();
                                                Toast.makeText(EventListAdmin.this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                progressDialog.dismiss();
                                                Toast.makeText(EventListAdmin.this, "There was an error while deleting data", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                }).create();
                        alertDialog.show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void dateDialog(Button btn_date, Button btn_time) {
        btn_date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog dialog = new DatePickerDialog(EventListAdmin.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int day) {
                        d = day;
                        mth = month;
                        y = year;
                        btn_date.setText(d + "/" + (mth + 1) + "/" + y);
                    }
                }, 2024, 11, 29);
                dialog.show();
            }
        });

        btn_time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerDialog dialog = new TimePickerDialog(EventListAdmin.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hour, int minute) {
                        h = hour;
                        min = minute;
                        btn_time.setText(h + ":" + min);
                    }
                }, 15, 35, true);
                dialog.show();
            }
        });

    }
}