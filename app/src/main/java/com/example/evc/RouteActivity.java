package com.example.evc;

import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class RouteActivity extends AppCompatActivity {

    private MapView mapView;
    private String donorPhone, acceptorPhone;
    private GeoPoint donorLocation, acceptorLocation;

    private Marker donorMarker, acceptorMarker;
    private Polyline routePolyline;

    private ProgressBar loading;
    private TextView routeInfo;
    private Button navigateButton;

    private final String ORS_API_KEY = "5b3ce3597851110001cf6248bc50f566d1a5491c84c7e5af00c41d15"; // Replace with actual API key

    private SensorManager sensorManager;
    private SensorEventListener sensorListener;
    private ImageView compassImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(getApplicationContext(),
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        setContentView(R.layout.activity_route);

        compassImage = findViewById(R.id.compass_image);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        mapView = findViewById(R.id.routeMapView);mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(16.0);

        loading = findViewById(R.id.loading);
        routeInfo = findViewById(R.id.route_info);
        navigateButton = findViewById(R.id.btn_navigate);
        navigateButton.setOnClickListener(v -> openGoogleMapsNavigation());

        donorPhone = getIntent().getStringExtra("donorPhone");
        acceptorPhone = getIntent().getStringExtra("acceptorPhone");

        if (donorPhone == null || acceptorPhone == null) {
            Toast.makeText(this, "Missing phone numbers", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupRealtimeTracking();
    }

    private void setupRealtimeTracking() {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        usersRef.child(donorPhone).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Double lat = snapshot.child("latitude").getValue(Double.class);
                Double lng = snapshot.child("longitude").getValue(Double.class);
                if (lat != null && lng != null) {
                    donorLocation = new GeoPoint(lat, lng);
                    if (donorMarker == null) {
                        donorMarker = createMarker(donorLocation, "Donor", Color.RED);
                    } else {
                        donorMarker.setPosition(donorLocation);
                        mapView.invalidate();
                    }

                    if (acceptorLocation != null) {
                        zoomToFit();
                        fetchRouteFromORS();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showError("Failed to track donor.");
            }
        });

        usersRef.child(acceptorPhone).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Double lat = snapshot.child("latitude").getValue(Double.class);
                Double lng = snapshot.child("longitude").getValue(Double.class);
                if (lat != null && lng != null) {
                    acceptorLocation = new GeoPoint(lat, lng);
                    if (acceptorMarker == null) {
                        acceptorMarker = createMarker(acceptorLocation, "Acceptor", Color.GREEN);
                    } else {
                        acceptorMarker.setPosition(acceptorLocation);
                        mapView.invalidate();
                    }

                    if (donorLocation != null) {
                        zoomToFit();
                        fetchRouteFromORS();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showError("Failed to track acceptor.");
            }
        });
    }

    private Marker createMarker(GeoPoint point, String title, int color) {
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setTitle(title);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        if (title.equals("Donor")) {
            marker.setIcon(getResources().getDrawable(R.drawable.locared));
        } else if (title.equals("Acceptor")) {
            marker.setIcon(getResources().getDrawable(R.drawable.ic_pin_green));
        }

        mapView.getOverlays().add(marker);
        mapView.invalidate();
        return marker;
    }

    private void zoomToFit() {
        ArrayList<GeoPoint> points = new ArrayList<>();
        points.add(donorLocation);
        points.add(acceptorLocation);
        BoundingBox box = BoundingBox.fromGeoPointsSafe(points);
        mapView.zoomToBoundingBox(box, true);
    }

    private void fetchRouteFromORS() {
        if (donorLocation == null || acceptorLocation == null) return;

        loading.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                String urlStr = "https://api.openrouteservice.org/v2/directions/driving-car?"
                        + "api_key=" + ORS_API_KEY
                        + "&start=" + donorLocation.getLongitude() + "," + donorLocation.getLatitude()
                        + "&end=" + acceptorLocation.getLongitude() + "," + acceptorLocation.getLatitude();

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject json = new JSONObject(sb.toString());
                JSONArray coords = json.getJSONArray("features")
                        .getJSONObject(0)
                        .getJSONObject("geometry")
                        .getJSONArray("coordinates");

                double distance = json.getJSONArray("features")
                        .getJSONObject(0)
                        .getJSONObject("properties")
                        .getJSONObject("summary")
                        .getDouble("distance") / 1000;

                double duration = json.getJSONArray("features")
                        .getJSONObject(0)
                        .getJSONObject("properties")
                        .getJSONObject("summary")
                        .getDouble("duration") / 60;

                ArrayList<GeoPoint> routePoints = new ArrayList<>();
                for (int i = 0; i < coords.length(); i++) {
                    JSONArray coord = coords.getJSONArray(i);
                    routePoints.add(new GeoPoint(coord.getDouble(1), coord.getDouble(0)));
                }

                runOnUiThread(() -> {
                    if (routePolyline != null) {
                        mapView.getOverlays().remove(routePolyline);
                    }
                    routePolyline = new Polyline();
                    routePolyline.setPoints(routePoints);
                    routePolyline.setColor(Color.BLUE);
                    routePolyline.setWidth(8f);
                    mapView.getOverlays().add(routePolyline);
                    mapView.invalidate();

                    String info = String.format("Distance: %.2f km | ETA: %d mins", distance, Math.round(duration));
                    routeInfo.setText(info);
                    loading.setVisibility(View.GONE);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> showError("Failed to fetch route."));
            }
        }).start();
    }

    private void openGoogleMapsNavigation() {
        if (donorLocation == null || acceptorLocation == null) {
            Toast.makeText(this, "Locations not available", Toast.LENGTH_SHORT).show();
            return;
        }

        String uri = "http://maps.google.com/maps?saddr=" + donorLocation.getLatitude() + "," + donorLocation.getLongitude()
                + "&daddr=" + acceptorLocation.getLatitude() + "," + acceptorLocation.getLongitude();
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "Google Maps not installed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float degree = Math.round(event.values[0]);
                compassImage.setRotation(-degree);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };
        sensorManager.registerListener(sensorListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(sensorListener);
    }

    private void showError(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        loading.setVisibility(View.GONE);
    }
}