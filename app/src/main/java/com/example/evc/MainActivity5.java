package com.example.evc;

import android.Manifest;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class MainActivity5 extends AppCompatActivity {

    private String phoneNumber;
    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference databaseReference;
    private FirebaseFirestore firestoreDb;

    private static final int LOCATION_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main5);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        databaseReference = FirebaseDatabase.getInstance().getReference("users");
        firestoreDb = FirebaseFirestore.getInstance();

        // Get phone number from intent or shared preferences
        phoneNumber = getIntent().getStringExtra("phone");
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
            phoneNumber = prefs.getString("phone", null);
        }

        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(this, "Phone number not found", Toast.LENGTH_SHORT).show();
        }

        // Request location permission if not granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
        }

        // Apply window insets (for full screen handling)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // Called when Accept button is clicked
    public void accept(View v) {
        updateUser("Acceptor", Acceptor1.class);
    }

    // Called when Donate button is clicked
    public void dona(View v) {
        updateUser("Donor", donar1.class);
    }

    // Main method to fetch user profile and upload to Firebase
    private void updateUser(String role, Class<?> nextActivity) {
        if (phoneNumber == null) return;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    uploadUserData(role, nextActivity, location);
                } else {
                    // Fallback to requestLocationUpdates
                    LocationRequest locationRequest = LocationRequest.create()
                            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                            .setInterval(1000)
                            .setNumUpdates(1);

                    fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
                        @Override
                        public void onLocationResult(LocationResult locationResult) {
                            if (locationResult != null && !locationResult.getLocations().isEmpty()) {
                                Location newLocation = locationResult.getLastLocation();
                                uploadUserData(role, nextActivity, newLocation);
                            } else {
                                Toast.makeText(MainActivity5.this, "Unable to fetch location", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }, getMainLooper());
                }
            });
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
        }
    }

    // Helper method to upload data to Firebase
    private void uploadUserData(String role, Class<?> nextActivity, Location location) {
        DocumentReference docRef = firestoreDb.collection("UserNumbers").document(phoneNumber);

        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String name = documentSnapshot.getString("name");
                String carModel = documentSnapshot.getString("carModel");
                String carID = documentSnapshot.getString("carID");

                HashMap<String, Object> userData = new HashMap<>();
                userData.put("Role", role);
                userData.put("status", "online");
                userData.put("latitude", location.getLatitude());
                userData.put("longitude", location.getLongitude());
                userData.put("Name", name != null ? name : "");
                userData.put("Number", phoneNumber);
                userData.put("CarModel", carModel != null ? carModel : "");
                userData.put("CarID", carID != null ? carID : "");

                databaseReference.child(phoneNumber)
                        .setValue(userData)
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(this, "Role updated as " + role, Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(MainActivity5.this, nextActivity);
                            intent.putExtra("phone", phoneNumber);
                            Bundle b = ActivityOptions.makeSceneTransitionAnimation(this).toBundle();
                            startActivity(intent, b);
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(this, "User profile not found in Firestore", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to fetch user data: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }
}
