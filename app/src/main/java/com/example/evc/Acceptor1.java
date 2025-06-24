// Acceptor1.java
package com.example.evc;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.database.*;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.*;

public class Acceptor1 extends AppCompatActivity {

    private MapView map;
    private String phoneNumber;
    private GeoPoint myLocation;
    private final Handler handler = new Handler();
    private final int interval = 3000;
    private final Set<String> activeMarkers = new HashSet<>();
    private String activeRequestId = null;
    private DatabaseReference requestStatusRef;
    private ValueEventListener requestStatusListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accpector1);

        phoneNumber = getIntent().getStringExtra("phone");
        if (phoneNumber == null) {
            SharedPreferences preferences = getSharedPreferences("userdata", MODE_PRIVATE);
            phoneNumber = preferences.getString("phone", "");
        }

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, androidx.preference.PreferenceManager.getDefaultSharedPreferences(ctx));

        map = findViewById(R.id.mapview);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        requestPermissions();

        CompassOverlay compassOverlay = new CompassOverlay(this, map);
        compassOverlay.enableCompass();
        map.getOverlays().add(compassOverlay);

        setupLocationOverlay();
    }

    private void setupLocationOverlay() {
        MyLocationNewOverlay locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
        locationOverlay.enableMyLocation();
        locationOverlay.enableFollowLocation();
        map.getOverlays().add(locationOverlay);

        locationOverlay.runOnFirstFix(() -> runOnUiThread(() -> {
            Location location = locationOverlay.getLastFix();
            if (location != null) {
                myLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                map.getController().setZoom(18.0);
                map.getController().setCenter(myLocation);

                updateLocationToFirebase();
                handler.post(refreshNearbyUsers);
            }
        }));
    }

    private final Runnable refreshNearbyUsers = new Runnable() {
        @Override
        public void run() {
            updateLocationToFirebase();
            queryNearbyUsers();
            handler.postDelayed(this, interval);
        }
    };

    private void updateLocationToFirebase() {
        if (myLocation == null) return;
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("users").child(phoneNumber);
        dbRef.child("latitude").setValue(myLocation.getLatitude());
        dbRef.child("longitude").setValue(myLocation.getLongitude());
        dbRef.child("Role").setValue("Acceptor");
    }

    private void queryNearbyUsers() {
        map.getOverlays().removeIf(item -> item instanceof Marker);
        activeMarkers.clear();

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String number = userSnapshot.getKey();
                    if (Objects.equals(number, phoneNumber)) continue;
                    String role = userSnapshot.child("Role").getValue(String.class);
                    if (!"Donor".equalsIgnoreCase(role)) continue;

                    Double lat = userSnapshot.child("latitude").getValue(Double.class);
                    Double lng = userSnapshot.child("longitude").getValue(Double.class);
                    if (lat == null || lng == null) continue;

                    GeoPoint otherLocation = new GeoPoint(lat, lng);
                    if (distanceBetween(myLocation, otherLocation) <= 5000) {
                        if (activeMarkers.contains(number)) continue;
                        activeMarkers.add(number);

                        String name = userSnapshot.child("Name").getValue(String.class);
                        String carModel = userSnapshot.child("CarModel").getValue(String.class);
                        String carId = userSnapshot.child("CarID").getValue(String.class);

                        Marker marker = new Marker(map);
                        marker.setPosition(otherLocation);
                        marker.setTitle(name);
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                        marker.setOnMarkerClickListener((marker1, mapView) -> {
                            showDonorDetailsBottomSheet(name, carModel, carId, number);
                            return true;
                        });

                        map.getOverlays().add(marker);
                    }
                }
                map.invalidate();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Acceptor1.this, "Firebase error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDonorDetailsBottomSheet(String name, String carModel, String carId, String number) {
        View view = getLayoutInflater().inflate(R.layout.showdonordetailsbottomsheet, null);

        ImageView donorImage = view.findViewById(R.id.donorImage);
        TextView nameText = view.findViewById(R.id.nameText);
        TextView carModelText = view.findViewById(R.id.carModelText);
        TextView carIdText = view.findViewById(R.id.carIdText);
        TextView phoneText = view.findViewById(R.id.phoneText);
        Button sendRequestButton = view.findViewById(R.id.sendRequestButton);

        nameText.setText("Name: " + name);
        carModelText.setText("Car Model: " + carModel);
        carIdText.setText("Car ID: " + carId);
        phoneText.setText("Phone: " + number);

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();

        sendRequestButton.setOnClickListener(v -> {
            sendRequestToDonor(number);
            bottomSheetDialog.dismiss();
        });
    }

    private void sendRequestToDonor(String donorPhone) {
        DatabaseReference requestsRef = FirebaseDatabase.getInstance().getReference("requests").push();
        activeRequestId = requestsRef.getKey();

        requestsRef.child("from").setValue(phoneNumber);
        requestsRef.child("to").setValue(donorPhone);
        requestsRef.child("status").setValue("pending");

        Toast.makeText(this, "Request sent to " + donorPhone, Toast.LENGTH_SHORT).show();
        listenForStatusChange(activeRequestId, donorPhone);
    }

    private void listenForStatusChange(String requestId, String donorPhone) {
        requestStatusRef = FirebaseDatabase.getInstance().getReference("requests").child(requestId);
        requestStatusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String status = snapshot.child("status").getValue(String.class);
                if ("accepted".equalsIgnoreCase(status)) {
                    Intent intent = new Intent(Acceptor1.this, RouteActivity.class);
                    intent.putExtra("donorPhone", donorPhone);
                    intent.putExtra("acceptorPhone", phoneNumber);
                    startActivity(intent);
                    finish();
                } else if ("rejected".equalsIgnoreCase(status)) {
                    Toast.makeText(Acceptor1.this, "Request was rejected by donor", Toast.LENGTH_SHORT).show();
                    requestStatusRef.removeValue();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        requestStatusRef.addValueEventListener(requestStatusListener);
    }

    public void deleteUserFromFirebase(View v) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(phoneNumber);
        userRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(Acceptor1.this, "User data deleted", Toast.LENGTH_SHORT).show();
                finishAffinity();
                System.exit(0);
            } else {
                Toast.makeText(Acceptor1.this, "Failed to delete user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void pfp(View v) {
        startActivity(new Intent(Acceptor1.this, Acceptorpfp.class));
    }

    private float distanceBetween(GeoPoint a, GeoPoint b) {
        Location loc1 = new Location("");
        loc1.setLatitude(a.getLatitude());
        loc1.setLongitude(a.getLongitude());
        Location loc2 = new Location("");
        loc2.setLatitude(b.getLatitude());
        loc2.setLongitude(b.getLongitude());
        return loc1.distanceTo(loc2);
    }

    private void requestPermissions() {
        String[] permissions = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.INTERNET
        };
        ArrayList<String> requestList = new ArrayList<>();
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                requestList.add(perm);
            }
        }
        if (!requestList.isEmpty()) {
            ActivityCompat.requestPermissions(this, requestList.toArray(new String[0]), 1);
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(refreshNearbyUsers);
        if (requestStatusRef != null && requestStatusListener != null)
            requestStatusRef.removeEventListener(requestStatusListener);
        super.onDestroy();
    }
}
