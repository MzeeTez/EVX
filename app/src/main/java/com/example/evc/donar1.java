package com.example.evc;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class donar1 extends AppCompatActivity {

    private MapView map;
    private String phoneNumber;
    private GeoPoint myLocation;
    private MyLocationNewOverlay locationOverlay;
    private final Handler handler = new Handler();
    private final int interval = 3000;
    private BroadcastReceiver statusReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donar1);
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        prefs.edit().putString("phone", phoneNumber).apply();


        phoneNumber = getIntent().getStringExtra("phone");
        createNotificationChannel();

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, androidx.preference.PreferenceManager.getDefaultSharedPreferences(ctx));

        map = findViewById(R.id.mapview);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.getController().setZoom(18.0);
        map.setMultiTouchControls(true);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);

        requestPermissionsIfNecessary(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.INTERNET,
                Manifest.permission.POST_NOTIFICATIONS
        });

        CompassOverlay compassOverlay = new CompassOverlay(this, map);
        compassOverlay.enableCompass();
        map.getOverlays().add(compassOverlay);

        setupMyLocationOverlay();
        listenForRequests();

        Button reloadButton = findViewById(R.id.reloadButton);
        reloadButton.setOnClickListener(v -> reloadNearbyUsers());

        statusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String status = intent.getStringExtra("status");
                String reqId = intent.getStringExtra("requestId");
                if ("accepted".equals(status)) {
                    Toast.makeText(context, "You accepted the request!", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(donar1.this, donar2.class);
                    i.putExtra("requestId", reqId);
                    i.putExtra("phone", phoneNumber);
                    startActivity(i);
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(statusReceiver, new IntentFilter("REQUEST_STATUS_UPDATED"));
    }

    private void setupMyLocationOverlay() {
        locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
        locationOverlay.enableMyLocation();
        locationOverlay.enableFollowLocation();
        map.getOverlays().add(locationOverlay);

        locationOverlay.runOnFirstFix(() -> runOnUiThread(() -> {
            Location location = locationOverlay.getLastFix();
            if (location != null) {
                myLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                map.getController().setCenter(myLocation);
                updateFirebaseLocation();
                handler.post(refreshNearbyUsers);
            }
        }));
    }

    private final Runnable refreshNearbyUsers = new Runnable() {
        @Override
        public void run() {
            updateFirebaseLocation();
            reloadNearbyUsers();
            handler.postDelayed(this, interval);
        }
    };

    private void updateFirebaseLocation() {
        if (myLocation != null) {
            DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("users").child(phoneNumber);
            dbRef.child("latitude").setValue(myLocation.getLatitude());
            dbRef.child("longitude").setValue(myLocation.getLongitude());
            dbRef.child("Role").setValue("Donor");
        }
    }

    private void reloadNearbyUsers() {
        if (myLocation == null) {
            Toast.makeText(this, "Waiting for location fix...", Toast.LENGTH_SHORT).show();
            return;
        }

        map.getOverlays().removeIf(o -> o instanceof Marker);
        queryNearbyUsers();
    }

    private void queryNearbyUsers() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot snap : snapshot.getChildren()) {
                    String number = snap.getKey();
                    if (number.equals(phoneNumber)) continue;

                    String role = snap.child("Role").getValue(String.class);
                    if (!"Acceptor".equals(role)) continue;

                    Double lat = snap.child("latitude").getValue(Double.class);
                    Double lng = snap.child("longitude").getValue(Double.class);
                    if (lat == null || lng == null) continue;

                    GeoPoint acceptorLoc = new GeoPoint(lat, lng);
                    if (distanceBetween(myLocation, acceptorLoc) <= 5000) {
                        Marker m = new Marker(map);
                        m.setPosition(acceptorLoc);
                        m.setTitle("Acceptor: " + number);
                        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                        m.setOnMarkerClickListener((marker, mapView) -> {
                            Toast.makeText(donar1.this, "Clicked on: " + number, Toast.LENGTH_SHORT).show();
                            return true;
                        });
                        map.getOverlays().add(m);
                    }
                }
                map.invalidate();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(donar1.this, "Firebase error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private float distanceBetween(GeoPoint a, GeoPoint b) {
        Location l1 = new Location("");
        l1.setLatitude(a.getLatitude());
        l1.setLongitude(a.getLongitude());
        Location l2 = new Location("");
        l2.setLatitude(b.getLatitude());
        l2.setLongitude(b.getLongitude());
        return l1.distanceTo(l2);
    }

    private void listenForRequests() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("requests");
        ref.orderByChild("to").equalTo(phoneNumber).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot requestSnap : snapshot.getChildren()) {
                    String from = requestSnap.child("from").getValue(String.class);
                    String reqId = requestSnap.getKey();
                    String status = requestSnap.child("status").getValue(String.class);

                    if ("accepted".equals(status) || "rejected".equals(status)) continue;

                    Intent acceptIntent = new Intent(donar1.this, NotificationActionReceiver.class);
                    acceptIntent.setAction("ACCEPT_ACTION");
                    acceptIntent.putExtra("requestId", reqId);
                    PendingIntent acceptPending = PendingIntent.getBroadcast(
                            donar1.this, reqId.hashCode(), acceptIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                    Intent rejectIntent = new Intent(donar1.this, NotificationActionReceiver.class);
                    rejectIntent.setAction("REJECT_ACTION");
                    rejectIntent.putExtra("requestId", reqId);
                    PendingIntent rejectPending = PendingIntent.getBroadcast(
                            donar1.this, reqId.hashCode() + 1, rejectIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                    NotificationCompat.Builder builder = new NotificationCompat.Builder(donar1.this, "REQ_CHANNEL")
                            .setSmallIcon(R.drawable.ic_ev_notification)
                            .setContentTitle("New Charging Request")
                            .setContentText("Acceptor " + from + " is requesting a charge.")
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .addAction(R.drawable.ic_check, "Accept", acceptPending)
                            .addAction(R.drawable.ic_close, "Reject", rejectPending)
                            .setAutoCancel(true);

                    NotificationManagerCompat manager = NotificationManagerCompat.from(donar1.this);
                    if (ActivityCompat.checkSelfPermission(donar1.this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                        return;

                    manager.notify(reqId.hashCode(), builder.build());

                    // Auto-cancel logic after 1 minute
                    new Handler().postDelayed(() -> {
                        DatabaseReference thisRequest = FirebaseDatabase.getInstance().getReference("requests").child(reqId);
                        thisRequest.get().addOnSuccessListener(snap -> {
                            if (snap.exists()) {
                                String currentStatus = snap.child("status").getValue(String.class);
                                if (currentStatus == null || currentStatus.equals("pending")) {
                                    thisRequest.removeValue(); // Remove request
                                    manager.cancel(reqId.hashCode()); // Dismiss notification
                                }
                            }
                        });
                    }, 60 * 1000); // 60 seconds
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(donar1.this, "Request listen error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel("REQ_CHANNEL", "RequestChannel", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Channel for EV request notifications");
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(ch);
        }
    }

    private void requestPermissionsIfNecessary(String[] permissions) {
        ArrayList<String> req = new ArrayList<>();
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED)
                req.add(perm);
        }
        if (!req.isEmpty()) {
            ActivityCompat.requestPermissions(this, req.toArray(new String[0]), 1);
        }
    }
    public void deleteUserFromFirebase(View v) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(phoneNumber);
        userRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(donar1.this, "User data deleted", Toast.LENGTH_SHORT).show();
                finishAffinity();
                System.exit(0);
            } else {
                Toast.makeText(donar1.this, "Failed to delete user data", Toast.LENGTH_SHORT).show();
            }
        });
    }
    public void pfp(View v) {
        startActivity(new Intent(donar1.this, donarpfp.class));
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(refreshNearbyUsers);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(statusReceiver);
        super.onDestroy();
    }
}
