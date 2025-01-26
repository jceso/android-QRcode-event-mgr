package com.example.myapplication;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EditProfile extends AppCompatActivity {
    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        user = FirebaseAuth.getInstance().getCurrentUser();
        Button cancelBtn = findViewById(R.id.cancel_btn);
        cancelBtn.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), UserProfile.class));
            finish();
        });

        infoEdit();
        pwEdit();
    }

    private void infoEdit() {
        EditText name = findViewById(R.id.name);
        EditText email = findViewById(R.id.email);
        EditText phone = findViewById(R.id.phone);
        Button saveBtn = findViewById(R.id.save_btn);

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("Users").document(user.getUid()).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    name.setText(documentSnapshot.getString("UserName"));
                    email.setText(documentSnapshot.getString("UserEmail"));
                    phone.setText(documentSnapshot.getString("PhoneNumber"));

                    saveBtn.setOnClickListener(v -> {
                        boolean isValid = true;

                        if (name.getText().toString().trim().isEmpty()) {
                            name.setError("Name is required");
                            isValid = false;
                        } else
                            name.setError(null);

                        if (phone.getText().toString().trim().isEmpty() || !phone.getText().toString().trim().matches("^[+]?[0-9]{1,4}?[0-9]{7,10}$")) {
                            phone.setError("Phone number is required and has to be a real number");
                            isValid = false;
                        } else
                            phone.setError(null);

                        if (isValid) {
                            Map<String, Object> updatedData = new HashMap<>();
                            updatedData.put("UserName", name.getText().toString().trim());
                            updatedData.put("PhoneNumber", phone.getText().toString().trim());

                            // Update user infos
                            firestore.collection("Users").document(user.getUid())
                                .update(updatedData)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getApplicationContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(getApplicationContext(), UserProfile.class));
                                    finish();
                                }).addOnFailureListener(e -> Toast.makeText(getApplicationContext(), "Error updating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    });
                } else
                    Log.d("EditProfile", "No such document");
        });

    }

    private void pwEdit() {
        Button chPw = findViewById(R.id.password);

        chPw.setOnClickListener(v -> {
            View passwordView = LayoutInflater.from(EditProfile.this).inflate(R.layout.pw_update, null);
            EditText currPw = passwordView.findViewById(R.id.currPw);
            EditText newPw1 = passwordView.findViewById(R.id.newPw1);
            EditText newPw2 = passwordView.findViewById(R.id.newPw2);
            AlertDialog pwDialog = new AlertDialog.Builder(EditProfile.this)
                    .setTitle("Change password")
                    .setView(passwordView)
                    .setCancelable(false)
                    .setPositiveButton("Save", null) // We will handle the click manually
                    .setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .create();

            pwDialog.setOnShowListener(d -> pwDialog
                    .getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v1 -> {
                        boolean isValid = true;

                        if (newPw1.getText().toString().trim().matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$")) {
                            newPw1.setError("At least 8 characters, one capital letter, one number and one special character");
                            isValid = false;
                        } else
                            newPw1.setError(null);

                        if (!newPw2.getText().toString().trim().equals(newPw1.getText().toString().trim())) {
                            newPw2.setError("New passwords are not the same!");
                            isValid = false;
                        } else
                            newPw1.setError(null);

                        if (!isValid)
                            return;

                        // Set password
                        String password = currPw.getText().toString().trim();
                        String newPass = newPw1.getText().toString().trim();
                        AuthCredential credential = EmailAuthProvider.getCredential(Objects.requireNonNull(user.getEmail()), password);

                        user.reauthenticate(credential).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                user.updatePassword(newPass).addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful())
                                        Toast.makeText(getApplicationContext(), "Password updated successfully", Toast.LENGTH_SHORT).show();
                                    else
                                        Toast.makeText(getApplicationContext(), "ERROR: Password not updated!", Toast.LENGTH_SHORT).show();
                                });
                            } else
                                Toast.makeText(getApplicationContext(), "ERROR: Authentication failed!", Toast.LENGTH_SHORT).show();
                        });

                        pwDialog.dismiss();
                    }));
            pwDialog.show();
        });
    }
}