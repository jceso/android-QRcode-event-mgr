package com.example.myapplication;

import android.graphics.Point;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

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

        EditText qrText = findViewById(R.id.qrText);
        Button grtButton = findViewById(R.id.grtButton);
        ImageView qrImage = findViewById(R.id.qrImage);


        grtButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(qrText.getText().toString())) {
                    Toast.makeText(MainActivity.this, "Inserisci un testo", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    WindowManager mgr = (WindowManager) getSystemService(WINDOW_SERVICE);
                    Display display = mgr.getDefaultDisplay();
                    Point size = new Point();
                    display.getSize(size);

                    int width = size.x;
                    int height = size.y;

                    int dimen = width < height ? width : height;
                    dimen = dimen * 3 / 4;


                }

            }
        });


    }

}