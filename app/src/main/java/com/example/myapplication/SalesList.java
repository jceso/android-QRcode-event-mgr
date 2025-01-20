package com.example.myapplication;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;

public class SalesList extends AppCompatActivity {
    private FirebaseDatabase database;
    private String eventUID;
    private ArrayList<Sale> salesList = new ArrayList<>();
    private final ArrayList<Sale> filteredSalesList = new ArrayList<>();
    private SaleAdapter saleAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sales_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Firebase setting
        FirebaseApp.initializeApp(SalesList.this);
        database = FirebaseDatabase.getInstance("https://ing-soft-firebase-default-rtdb.europe-west1.firebasedatabase.app/");
        eventUID = getIntent().getStringExtra("event_uid");
        Log.d("Sales", "Event UID: " + eventUID);

        // Setting basic buttons
        Button logout_btn = findViewById(R.id.logout_btn);
        logout_btn.setOnClickListener(v -> BasicButtons.handleLogoutButton(SalesList.this));
        Button name_btn = findViewById(R.id.user);
        BasicButtons.checkUserAndSetNameButton(SalesList.this, name_btn);

        // Search functionality
        EditText searchInput = findViewById(R.id.search_input);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                String searchQuery = charSequence.toString().toLowerCase();
                filterSales(searchQuery);  // Call filter method
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        showSales();
    }

    private void showSales() {
        // Setting recycler view
        RecyclerView recyclerView = findViewById(R.id.recycler);
        TextView empty = findViewById(R.id.empty_list);
        saleAdapter = new SaleAdapter(SalesList.this, filteredSalesList);
        recyclerView.setAdapter(saleAdapter);

        // Fetch sales where eventId equals eventUID
        database.getReference().child("sale").orderByChild("eventUID").equalTo(eventUID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.d("SalesList", "No sales found for event: " + eventUID);
                    empty.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    Log.d("SalesList", "Sales found for event: " + eventUID);
                    empty.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);

                    salesList = new ArrayList<>();
                    for (DataSnapshot saleSnapshot : snapshot.getChildren()) {
                        // Retrieve datas from Firebase
                        String eventUID = saleSnapshot.child("eventUID").getValue(String.class);
                        String userUID = saleSnapshot.child("userUID").getValue(String.class);
                        long timeLong = saleSnapshot.child("time").getValue(Long.class); // Retrieve time as Long
                        int seat = saleSnapshot.child("seat").getValue(Integer.class);
                        LocalDateTime saleTime = Instant.ofEpochMilli(timeLong).atZone(ZoneId.systemDefault()).toLocalDateTime();

                        // Create a Sale object and set the values
                        Sale sale = new Sale();
                        sale.setEventUID(eventUID);
                        sale.setUserUID(userUID);
                        sale.setSeat(seat);
                        sale.setTime(saleTime);
                        Log.d("SalesList", "Sale: " + sale + "\n user " + sale.getUserUID() + "| time " + sale.getTime() + " | seat " + sale.getSeat());

                        salesList.add(sale);
                        Log.d("SalesList", "Sale: " + sale);
                    }
                    filterSales("");  // Initially show all sales
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle possible errors
                Log.e("SalesList", "Error fetching sales", error.toException());
            }
        });
    }

    // Filter the sales based on the search query
    private void filterSales(String query) {
        // Clear the filtered sales list
        filteredSalesList.clear();
        if (query.isEmpty()) {
            // If search query is empty, show all sales
            filteredSalesList.addAll(salesList);
            saleAdapter.updateSalesList(filteredSalesList); // Update the adapter
            return;
        }

        // Use a counter to ensure we update the UI only after all name fetches are done
        int[] remainingItems = {salesList.size()};

        // Filter the sales based on the search query
        for (Sale sale : salesList) {
            sale.getName(name -> {
                // Decrement the counter when name is fetched
                remainingItems[0]--;

                // Check if the name is found and matches the query (case-insensitive)
                if (name != null && name.toLowerCase().contains(query.toLowerCase())) {
                    filteredSalesList.add(sale);
                }

                // If all names have been fetched, update the adapter with the filtered list
                if (remainingItems[0] == 0) {
                    // Sort the filtered list based on time
                    filteredSalesList.sort((sale1, sale2) -> {
                        Long date1 = sale1.getTime();
                        Long date2 = sale2.getTime();
                        return date1.compareTo(date2); // Ascending order
                    });
                    saleAdapter.updateSalesList(filteredSalesList); // Update the adapter
                }
            });
        }
    }
}