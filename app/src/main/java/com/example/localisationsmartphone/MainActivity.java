package com.example.localisationsmartphone;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final long LOCATION_UPDATE_INTERVAL_MS = 60_000;
    private static final float LOCATION_UPDATE_DISTANCE_M = 150f;

    private double latitude;
    private double longitude;
    private double altitude;
    private float accuracy;

    private RequestQueue requestQueue;
    private TextView tvInfo;
    private LocationManager locationManager;

    private final String insertUrl = "http://192.168.11.130/localisation/createPosition.php";

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            altitude = location.getAltitude();
            accuracy = location.getAccuracy();

            String msg = getString(
                    R.string.location_format,
                    latitude,
                    longitude,
                    altitude,
                    accuracy
            );

            tvInfo.setText(msg);
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            addPosition(latitude, longitude);
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            if (LocationManager.GPS_PROVIDER.equals(provider)) {
                startLocationUpdates();
            }
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            if (LocationManager.GPS_PROVIDER.equals(provider)) {
                tvInfo.setText(R.string.gps_disabled);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvInfo = findViewById(R.id.tvInfo);
        requestQueue = Volley.newRequestQueue(getApplicationContext());
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (hasAnyLocationPermission()) {
            startLocationUpdates();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && hasAnyLocationPermission()) {
            startLocationUpdates();
        } else if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            tvInfo.setText(R.string.location_permission_required);
            Toast.makeText(this, R.string.location_permission_required, Toast.LENGTH_LONG).show();
        }
    }

    private boolean hasFineLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasCoarseLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasAnyLocationPermission() {
        return hasFineLocationPermission() || hasCoarseLocationPermission();
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        if (locationManager == null) {
            tvInfo.setText(R.string.location_service_unavailable);
            return;
        }

        if (!hasAnyLocationPermission()) {
            return;
        }

        List<String> providers = getAvailableLocationProviders();
        if (providers.isEmpty()) {
            tvInfo.setText(R.string.location_disabled);
            Toast.makeText(this, R.string.location_disabled, Toast.LENGTH_LONG).show();
            return;
        }

        locationManager.removeUpdates(locationListener);

        for (String provider : providers) {
            locationManager.requestLocationUpdates(
                    provider,
                    LOCATION_UPDATE_INTERVAL_MS,
                    LOCATION_UPDATE_DISTANCE_M,
                    locationListener
            );
        }

        tvInfo.setText(R.string.waiting_for_location);
    }

    private List<String> getAvailableLocationProviders() {
        List<String> providers = new ArrayList<>();

        if (hasFineLocationPermission() && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            providers.add(LocationManager.GPS_PROVIDER);
        }

        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            providers.add(LocationManager.NETWORK_PROVIDER);
        }

        return providers;
    }

    private void addPosition(final double lat, final double lon) {
        StringRequest request = new StringRequest(
                Request.Method.POST,
                insertUrl,
                response -> Toast.makeText(getApplicationContext(), response, Toast.LENGTH_SHORT).show(),
                error -> Toast.makeText(getApplicationContext(), R.string.send_error, Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                HashMap<String, String> params = new HashMap<>();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

                params.put("latitude", String.valueOf(lat));
                params.put("longitude", String.valueOf(lon));
                params.put("date_position", sdf.format(new Date()));
                params.put("imei", "test123");

                return params;
            }
        };

        requestQueue.add(request);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
    }
}
