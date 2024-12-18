package com.example.myapplication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Random;

public class BasicButtons {

    // Logout functionality
    public static void handleLogoutButton(Context context) {
        FirebaseAuth.getInstance().signOut();
        context.startActivity(new Intent(context, Login.class));
        if (context instanceof Activity) {
            ((Activity) context).finish();
        }
    }

    // Check user authentication and set user button text
    public static void checkUserAndSetNameButton(Context context, Button userBtn) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            // If the user is not authenticated, navigate to Login
            Intent intent = new Intent(context, Login.class);
            context.startActivity(intent);
            if (context instanceof Activity) {
                ((Activity) context).finish();
            }
        } else {
            // If the user is authenticated, get user info from Firestore
            FirebaseFirestore fstore = FirebaseFirestore.getInstance();
            fstore.collection("Users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Get the name from the Firestore document and set to button
                            String name = documentSnapshot.getString("UserName");
                            if (name != null && !name.isEmpty()) {
                                userBtn.setText(name.substring(0, 1));  // First letter of the name
                            }
                        } else {
                            Log.d("User Info", "No such document");
                        }
                    });
        }

        // Generate random RGB values ensuring brightness and moderate saturation, not vivid colors
        Random rand = new Random();
        int r = (int) (Math.min(0.5f + rand.nextFloat() * 0.5f, 0.9f) * 255);
        int g = (int) (Math.min(0.5f + rand.nextFloat() * 0.5f, 0.9f) * 255);
        int b = (int) (Math.min(0.5f + rand.nextFloat() * 0.5f, 0.9f) * 255);
        // Set random color for username button
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor((0xFF << 24) | (r << 16) | (g << 8) | b);
        drawable.setCornerRadius(100);
        userBtn.setBackground(drawable);
    }
}
