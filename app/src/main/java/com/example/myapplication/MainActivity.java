package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    EditText titleInput;
    EditText subtitleInput;
    EditText descriptionInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        titleInput = findViewById(R.id.title);
        subtitleInput = findViewById(R.id.subtitle);
        descriptionInput = findViewById(R.id.description);
        Button addButton = findViewById(R.id.add_btn);

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get infos
                String titleText = titleInput.getText().toString();
                String subtitleText = subtitleInput.getText().toString();
                String descriptionText = descriptionInput.getText().toString();

                // Check if empty
                if (titleText.isEmpty() || subtitleText.isEmpty() || descriptionText.isEmpty()) {
                    return;
                }

                addToDB(titleText, subtitleText, descriptionText);
            }
        });

    }

    private void addToDB(String title, String subtitle, String description){
        // Create a hashmap
        HashMap<String, Object> event = new HashMap<>();
        event.put("title", title);
        event.put("subtitle", subtitle);
        event.put("description", description);

        // Initiate database connection
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://ing-soft-firebase-default-rtdb.europe-west1.firebasedatabase.app/");
        DatabaseReference myRef = database.getReference("event");

        String key = myRef.push().getKey();
        event.put("key", key);

        if (key != null) {
            myRef.child(key).setValue(event).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    Toast.makeText(MainActivity.this, "Event added", Toast.LENGTH_SHORT).show();
                    titleInput.getText().clear();
                    subtitleInput.setText("");
                    descriptionInput.setText("");
                }
            });
        }
    }

}