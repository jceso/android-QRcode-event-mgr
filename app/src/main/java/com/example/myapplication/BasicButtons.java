package com.example.myapplication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.core.content.FileProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class BasicButtons {

    public static void handleBackButton(final Activity activity, ImageButton backButton) {
        backButton.setOnClickListener(v -> {
            activity.finish();
        });
    }

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
            context.startActivity(new Intent(context, Login.class));
            if (context instanceof Activity)
                ((Activity) context).finish();
        } else {
            // If the user is authenticated, get user info
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            firestore.collection("Users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Get the name from the FireStore document and set to button
                            String name = documentSnapshot.getString("UserName");
                            if (name != null && !name.isEmpty()) {
                                userBtn.setText(name.substring(0, 1));  // First letter of the name
                            }
                        } else {
                            Log.d("User Info", "No such document");
                        }
                    });
        }

        userBtn.setOnClickListener(v -> {
            context.startActivity(new Intent(context, UserProfile.class));
            if (context instanceof Activity)
                ((Activity) context).finish();
        });
    }

    // Method to create URI from a Bitmap
    public static Uri getUriFromBitmap(Context context, Bitmap bitmap) {
        try {
            // Create a file to store the QR code image
            File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "qr_code_" + System.currentTimeMillis() + ".png");

            // Write the bitmap to the file
            FileOutputStream outStream = new FileOutputStream(file);
            if (bitmap != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, outStream);
                outStream.close();
            }

            // Get the URI for the file using FileProvider
            return FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file);
        } catch (IOException e) {
            Log.e("QR-Sharing", "getUriFromBitmap: " + e.getMessage());
        }

        return null;
    }
}