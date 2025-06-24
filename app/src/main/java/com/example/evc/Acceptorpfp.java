package com.example.evc;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class Acceptorpfp extends AppCompatActivity {

    private TextView carIdView, carModelView, nameView, phoneView;
    private FirebaseFirestore firestoreDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_acceptorpfp);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firestore
        firestoreDb = FirebaseFirestore.getInstance();

        // Link UI elements
        carIdView = findViewById(R.id.tvCarID);
        carModelView = findViewById(R.id.tvCarModel);
        nameView = findViewById(R.id.tvName);
        phoneView = findViewById(R.id.tvPhone);

        // Get saved phone number from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserProfile", MODE_PRIVATE);
        String savedPhone = prefs.getString("phone", null);

        if (savedPhone != null && !savedPhone.isEmpty()) {
            fetchUserDetails(savedPhone);
        } else {
            Toast.makeText(this, "No saved number found", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchUserDetails(String phone) {
        DocumentReference docRef = firestoreDb.collection("UserNumbers").document(phone);

        docRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        carIdView.setText(documentSnapshot.getString("carID") != null ? documentSnapshot.getString("carID") : "N/A");
                        carModelView.setText(documentSnapshot.getString("carModel") != null ? documentSnapshot.getString("carModel") : "N/A");
                        nameView.setText(documentSnapshot.getString("name") != null ? documentSnapshot.getString("name") : "N/A");

                        String number = documentSnapshot.getString("phone");
                        phoneView.setText(number != null ? number : phone);
                    } else {
                        Toast.makeText(this, "No user data found for this number", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to fetch data: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
