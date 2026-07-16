package com.example.smartroad.home;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.example.smartroad.R;
import com.example.smartroad.about.AboutActivity;
import com.example.smartroad.auth.LoginActivity;
import com.example.smartroad.map.HazardMapActivity;
import com.example.smartroad.profile.ProfileActivity;
import com.example.smartroad.report.ReportHazardActivity;
import com.example.smartroad.util.SnackbarUtil;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback {

    private FirebaseAuth auth;
    private FusedLocationProviderClient locationClient;
    private GoogleMap map;
    private ProgressBar progressMapLoading;
    private Toolbar toolbar;

    private final ActivityResultLauncher<String> requestLocationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    showCurrentLocation();
                } else {
                    SnackbarUtil.showError(this, "Location permission is needed to show your position");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        auth = FirebaseAuth.getInstance();
        locationClient = LocationServices.getFusedLocationProviderClient(this);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton buttonMyLocation = findViewById(R.id.buttonMyLocation);
        progressMapLoading = findViewById(R.id.progressMapLoading);
        ExtendedFloatingActionButton buttonReportHazard = findViewById(R.id.buttonReportHazard);

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            goToLogin();
            return;
        }

        buttonMyLocation.setOnClickListener(v -> requestLocationAndShow());
        buttonReportHazard.setOnClickListener(v ->
                startActivity(new Intent(this, ReportHazardActivity.class)));

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshGreeting();
    }

    private void refreshGreeting() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return;
        }
        FirebaseFirestore.getInstance()
                .collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("name");
                    toolbar.setTitle(getString(R.string.home_greeting,
                            name != null ? name : currentUser.getEmail()));
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_hazard_map) {
            startActivity(new Intent(this, HazardMapActivity.class));
            return true;
        } else if (itemId == R.id.action_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        } else if (itemId == R.id.action_about) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        } else if (itemId == R.id.action_logout) {
            auth.signOut();
            goToLogin();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        progressMapLoading.setVisibility(View.GONE);
        requestLocationAndShow();
    }

    private void requestLocationAndShow() {
        if (hasLocationPermission()) {
            showCurrentLocation();
        } else {
            requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void showCurrentLocation() {
        if (map == null || !hasLocationPermission()) {
            return;
        }
        map.setMyLocationEnabled(true);
        locationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location == null) {
                return;
            }
            LatLng here = new LatLng(location.getLatitude(), location.getLongitude());
            map.clear();
            map.addMarker(new MarkerOptions().position(here).title("You are here"));
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(here, 16f));
        });
    }

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
