package com.example.myapplication;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
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

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Objects;

public class EventListAdmin extends AppCompatActivity {

    FirebaseDatabase database;

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
        RecyclerView recyclerView = findViewById(R.id.recycler);
        TextView empty = findViewById(R.id.empty_list);

        // CRUD setting
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addOnDB();
            }
        });

        editDeleteEvent(recyclerView, empty);

    }

    private void addOnDB() {
        View dialogView = LayoutInflater.from(EventListAdmin.this).inflate(R.layout.add_event_dialog, null);
        TextView title = dialogView.findViewById(R.id.titleET);
        TextView subtitle = dialogView.findViewById(R.id.subtitleET);
        TextView desc = dialogView.findViewById(R.id.descET);

        AlertDialog alertDialog = new AlertDialog.Builder(EventListAdmin.this)
                .setTitle("Add event")
                .setView(dialogView)
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        if (title.getText().toString().isEmpty())
                            title.setError("Title is required");
                        if (subtitle.getText().toString().isEmpty())
                            subtitle.setError("Subtitle is required");
                        if (desc.getText().toString().isEmpty())
                            desc.setError("Description is required");

                        Log.d("AddEvent", "desc: " + desc.getText().toString() + "so desc is empty? " + desc.getText().toString().isEmpty());

                        ProgressDialog dialog = new ProgressDialog(EventListAdmin.this);
                        dialog.setMessage("Adding event...");
                        dialog.show();

                        Event event = new Event();
                        event.setTitle(title.getText().toString());
                        event.setSubtitle(subtitle.getText().toString());
                        event.setDescription(desc.getText().toString());

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
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        dialogInterface.dismiss();
                    }
                })
                .create();
        alertDialog.show();
    }

    private void editDeleteEvent(RecyclerView recyclerView, TextView empty) {
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

                Log.d("EditDeleteEvent", "Adapter set");
                adapter.setOnItemClickListener(new EventAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(Event event) {
                        View view = LayoutInflater.from(EventListAdmin.this).inflate(R.layout.add_event_dialog, null);

                        TextView title = view.findViewById(R.id.titleET);
                        TextView subtitle = view.findViewById(R.id.subtitleET);
                        TextView desc = view.findViewById(R.id.descET);

                        title.setText(event.getTitle());
                        subtitle.setText(event.getSubtitle());
                        desc.setText(event.getDescription());

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
                                        eventUpd.setSubtitle(subtitle.getText().toString());
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
}