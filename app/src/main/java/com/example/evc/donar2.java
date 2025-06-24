package com.example.evc;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class donar2 extends AppCompatActivity {

    private String donorPhone = "";
    private String acceptorPhone = "";
    private String requestId = "";

    private TextView tvName, tvCarId, tvCarModel, tvPhone;
    private Button trackButton, cancelButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_donar2);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // UI bindings
        tvName = findViewById(R.id.textView16);
        tvCarId = findViewById(R.id.textView17);
        tvCarModel = findViewById(R.id.textView18);
        tvPhone = findViewById(R.id.textView19);
        trackButton = findViewById(R.id.button11);
        cancelButton = findViewById(R.id.button12);

        donorPhone = getIntent().getStringExtra("phone");
        requestId = getIntent().getStringExtra("requestId");

        if (requestId == null || donorPhone == null) {
            Toast.makeText(this, "Missing data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fetchRequestDetails();

        trackButton.setOnClickListener(v -> {
            Intent intent = new Intent(donar2.this, RouteActivity.class);
            intent.putExtra("donorPhone", donorPhone);
            intent.putExtra("acceptorPhone", acceptorPhone);
            intent.putExtra("role", "donor");
            startActivity(intent);
            finish();
        });

        cancelButton.setOnClickListener(v -> {
            FirebaseDatabase.getInstance().getReference("requests").child(requestId)
                    .child("status").setValue("rejected")
                    .addOnCompleteListener(task -> {
                        Toast.makeText(donar2.this, "Request cancelled", Toast.LENGTH_SHORT).show();
                        finish();
                    });
        });
    }

    private void fetchRequestDetails() {
        FirebaseDatabase.getInstance().getReference("requests").child(requestId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        acceptorPhone = snapshot.child("from").getValue(String.class);
                        if (acceptorPhone != null) {
                            approveRequestAndShowDetails();
                        } else {
                            Toast.makeText(this, "No acceptor found", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading request", Toast.LENGTH_SHORT).show());
    }

    private void approveRequestAndShowDetails() {
        FirebaseDatabase.getInstance().getReference("requests").child(requestId).child("status").setValue("accepted");

        FirebaseFirestore.getInstance()
                .collection("UserNumbers")
                .document(acceptorPhone)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String name = snapshot.getString("name");
                        String carId = snapshot.getString("carID");
                        String carModel = snapshot.getString("carModel");
                        String phone = snapshot.getString("phone");

                        tvName.setText("Name: " + name);
                        tvCarId.setText("CAR ID: " + carId);
                        tvCarModel.setText("CAR MODEL: " + carModel);
                        tvPhone.setText("Phone Number: " + phone);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load acceptor details", Toast.LENGTH_SHORT).show());
    }
}
