package com.example.smartroad.map;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.smartroad.R;
import com.example.smartroad.model.HazardReport;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HazardMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String[] ALL_HAZARD_TYPES = {
            "Pothole", "Flood", "Accident", "Fallen Tree", "Traffic Light"
    };

    private static final Map<String, Integer> TYPE_ICONS = new HashMap<>();
    static {
        TYPE_ICONS.put("Pothole", R.drawable.marker_pothole);
        TYPE_ICONS.put("Flood", R.drawable.marker_flood);
        TYPE_ICONS.put("Accident", R.drawable.marker_accident);
        TYPE_ICONS.put("Fallen Tree", R.drawable.marker_fallen_tree);
        TYPE_ICONS.put("Traffic Light", R.drawable.marker_traffic_light);
    }

    private GoogleMap map;
    private ProgressBar progressMapLoading;
    private TextView textEmptyState;
    private final Map<String, Marker> markersByReportId = new HashMap<>();
    private final Map<String, HazardReport> reportsByReportId = new HashMap<>();
    private final Set<String> hiddenHazardTypes = new HashSet<>();
    private boolean cameraFitted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hazard_map);

        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());
        progressMapLoading = findViewById(R.id.progressMapLoading);
        textEmptyState = findViewById(R.id.textEmptyState);

        ImageButton buttonFilter = findViewById(R.id.buttonFilter);
        buttonFilter.setOnClickListener(v -> showFilterDialog());

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        progressMapLoading.setVisibility(View.GONE);
        map.setOnInfoWindowClickListener(marker -> {
            String reportId = (String) marker.getTag();
            if (reportId != null) {
                Intent intent = new Intent(this, HazardDetailsActivity.class);
                intent.putExtra(HazardDetailsActivity.EXTRA_REPORT_ID, reportId);
                startActivity(intent);
            }
        });
        listenForHazards();
    }

    private void listenForHazards() {
        FirebaseFirestore.getInstance().collection("reports")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) {
                        return;
                    }
                    snapshots.getDocumentChanges().forEach(change -> {
                        String id = change.getDocument().getId();
                        switch (change.getType()) {
                            case ADDED:
                            case MODIFIED:
                                upsertMarker(id, change.getDocument().toObject(HazardReport.class));
                                break;
                            case REMOVED:
                                reportsByReportId.remove(id);
                                Marker marker = markersByReportId.remove(id);
                                if (marker != null) {
                                    marker.remove();
                                }
                                break;
                        }
                    });
                    fitCameraToMarkersOnce();
                    textEmptyState.setVisibility(
                            markersByReportId.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void upsertMarker(String reportId, HazardReport report) {
        if (report.latitude == null || report.longitude == null) {
            return;
        }
        reportsByReportId.put(reportId, report);

        LatLng position = new LatLng(report.latitude, report.longitude);
        BitmapDescriptor icon = createMarkerIcon(report.hazardType, report.status);
        boolean visible = !hiddenHazardTypes.contains(report.hazardType);

        Marker existing = markersByReportId.get(reportId);
        if (existing != null) {
            existing.setPosition(position);
            existing.setIcon(icon);
            existing.setTitle(report.hazardType);
            existing.setSnippet(report.status + " · tap for details");
            existing.setVisible(visible);
        } else {
            Marker marker = map.addMarker(new MarkerOptions()
                    .position(position)
                    .title(report.hazardType)
                    .snippet(report.status + " · tap for details")
                    .icon(icon)
                    .visible(visible));
            if (marker != null) {
                marker.setTag(reportId);
                markersByReportId.put(reportId, marker);
            }
        }
    }

    private void showFilterDialog() {
        boolean[] checked = new boolean[ALL_HAZARD_TYPES.length];
        for (int i = 0; i < ALL_HAZARD_TYPES.length; i++) {
            checked[i] = !hiddenHazardTypes.contains(ALL_HAZARD_TYPES[i]);
        }

        new AlertDialog.Builder(this)
                .setTitle("Show Hazard Types")
                .setMultiChoiceItems(ALL_HAZARD_TYPES, checked, (dialog, which, isChecked) -> {
                    if (isChecked) {
                        hiddenHazardTypes.remove(ALL_HAZARD_TYPES[which]);
                    } else {
                        hiddenHazardTypes.add(ALL_HAZARD_TYPES[which]);
                    }
                })
                .setPositiveButton("Apply", (dialog, which) -> applyHazardTypeFilter())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void applyHazardTypeFilter() {
        for (Map.Entry<String, Marker> entry : markersByReportId.entrySet()) {
            HazardReport report = reportsByReportId.get(entry.getKey());
            if (report != null) {
                entry.getValue().setVisible(!hiddenHazardTypes.contains(report.hazardType));
            }
        }
    }

    private void fitCameraToMarkersOnce() {
        if (cameraFitted || markersByReportId.isEmpty() || map == null) {
            return;
        }
        LatLngBounds.Builder bounds = new LatLngBounds.Builder();
        for (Marker marker : markersByReportId.values()) {
            bounds.include(marker.getPosition());
        }
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100));
        cameraFitted = true;
    }

    private BitmapDescriptor createMarkerIcon(String hazardType, String status) {
        int backgroundColor = statusColor(status);

        int size = 90;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(backgroundColor);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4f, circlePaint);

        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4f);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4f, borderPaint);

        Integer iconRes = TYPE_ICONS.get(hazardType);
        if (iconRes != null) {
            Bitmap icon = BitmapFactory.decodeResource(getResources(), iconRes);
            int iconSize = (int) (size * 0.55f);
            Bitmap scaledIcon = Bitmap.createScaledBitmap(icon, iconSize, iconSize, true);
            float offset = (size - iconSize) / 2f;
            Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            canvas.drawBitmap(scaledIcon, offset, offset, iconPaint);
        }

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private int statusColor(String status) {
        if (status == null) {
            return ContextCompat.getColor(this, R.color.status_new);
        }
        switch (status) {
            case "Under Investigation":
                return ContextCompat.getColor(this, R.color.status_investigating);
            case "Resolved":
                return ContextCompat.getColor(this, R.color.status_resolved);
            default:
                return ContextCompat.getColor(this, R.color.status_new);
        }
    }
}
