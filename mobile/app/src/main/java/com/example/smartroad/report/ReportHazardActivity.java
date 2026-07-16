package com.example.smartroad.report;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.smartroad.R;
import com.example.smartroad.util.SnackbarUtil;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ReportHazardActivity extends AppCompatActivity {

    private FusedLocationProviderClient locationClient;
    private FirebaseAuth auth;

    private Spinner spinnerHazardType;
    private TextInputEditText editDescription;
    private ImageView imagePreview;
    private TextView textLocationStatus;
    private TextView textHazardTypeError;
    private TextView textDescriptionError;
    private ProgressBar progressLocation;
    private ProgressBar progressBar;
    private TextView textSubmitStatus;
    private MaterialButton buttonSubmit;

    private Bitmap selectedPhoto;
    private Double latitude;
    private Double longitude;

    private final ActivityResultLauncher<Void> takePhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) {
                    setPhoto(bitmap);
                }
            });

    private final ActivityResultLauncher<String> pickPhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    Bitmap bitmap = loadBitmapFromUri(uri);
                    if (bitmap != null) {
                        setPhoto(bitmap);
                    }
                }
            });

    private final ActivityResultLauncher<String> requestCameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    takePhotoLauncher.launch(null);
                } else {
                    SnackbarUtil.showError(this, "Camera permission is needed to take a photo");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_hazard);

        auth = FirebaseAuth.getInstance();
        locationClient = LocationServices.getFusedLocationProviderClient(this);

        spinnerHazardType = findViewById(R.id.spinnerHazardType);
        editDescription = findViewById(R.id.editDescription);
        imagePreview = findViewById(R.id.imagePreview);
        textLocationStatus = findViewById(R.id.textLocationStatus);
        textHazardTypeError = findViewById(R.id.textHazardTypeError);
        textDescriptionError = findViewById(R.id.textDescriptionError);
        progressLocation = findViewById(R.id.progressLocation);
        progressBar = findViewById(R.id.progressBar);
        textSubmitStatus = findViewById(R.id.textSubmitStatus);
        buttonSubmit = findViewById(R.id.buttonSubmit);
        MaterialButton buttonAddPhoto = findViewById(R.id.buttonAddPhoto);

        buttonAddPhoto.setOnClickListener(v -> showPhotoSourceDialog());
        buttonSubmit.setOnClickListener(v -> attemptSubmit());

        fetchCurrentLocation();
    }

    private void showPhotoSourceDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Add Photo")
                .setItems(new CharSequence[]{"Take Photo", "Choose from Gallery"}, (dialog, which) -> {
                    if (which == 0) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                == PackageManager.PERMISSION_GRANTED) {
                            takePhotoLauncher.launch(null);
                        } else {
                            requestCameraPermission.launch(Manifest.permission.CAMERA);
                        }
                    } else {
                        pickPhotoLauncher.launch("image/*");
                    }
                })
                .show();
    }

    private void setPhoto(Bitmap bitmap) {
        selectedPhoto = bitmap;
        imagePreview.setVisibility(View.VISIBLE);
        imagePreview.setImageBitmap(bitmap);
    }

    private Bitmap loadBitmapFromUri(Uri uri) {
        try {
            return MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
        } catch (IOException e) {
            SnackbarUtil.showError(this, "Could not load photo");
            return null;
        }
    }

    private void fetchCurrentLocation() {
        progressLocation.setVisibility(View.VISIBLE);
        textLocationStatus.setText("Fetching current location...");

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            progressLocation.setVisibility(View.GONE);
            textLocationStatus.setText("Location permission not granted");
            return;
        }
        locationClient.getLastLocation().addOnSuccessListener(this::onLocationResolved);
    }

    private void onLocationResolved(Location location) {
        progressLocation.setVisibility(View.GONE);
        if (location == null) {
            textLocationStatus.setText("Location unavailable — enable GPS and try again");
            return;
        }
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        textLocationStatus.setText(String.format("Location: %.5f, %.5f", latitude, longitude));
    }

    private void attemptSubmit() {
        String description = editDescription.getText() == null
                ? "" : editDescription.getText().toString().trim();

        boolean hazardTypeMissing = spinnerHazardType.getSelectedItemPosition() == 0;
        boolean descriptionMissing = TextUtils.isEmpty(description);

        textHazardTypeError.setVisibility(hazardTypeMissing ? View.VISIBLE : View.GONE);
        textDescriptionError.setVisibility(descriptionMissing ? View.VISIBLE : View.GONE);

        if (hazardTypeMissing || descriptionMissing) {
            return;
        }
        if (selectedPhoto == null) {
            SnackbarUtil.showError(this, "Please add a photo");
            return;
        }
        if (latitude == null || longitude == null) {
            SnackbarUtil.showError(this, "Waiting for GPS location, please try again");
            fetchCurrentLocation();
            return;
        }
        if (auth.getCurrentUser() == null) {
            return;
        }

        setLoading(true);
        uploadPhotoAndSave(description);
    }

    private void uploadPhotoAndSave(String description) {
        String uid = auth.getCurrentUser().getUid();
        String hazardType = spinnerHazardType.getSelectedItem().toString();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        selectedPhoto.compress(Bitmap.CompressFormat.JPEG, 85, baos);
        byte[] photoBytes = baos.toByteArray();

        long timestamp = System.currentTimeMillis();
        StorageReference photoRef = FirebaseStorage.getInstance()
                .getReference("hazard_photos/" + uid + "_" + timestamp + ".jpg");

        UploadTask uploadTask = photoRef.putBytes(photoBytes);
        uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            return photoRef.getDownloadUrl();
        }).addOnSuccessListener(downloadUri ->
                saveReport(uid, hazardType, description, downloadUri.toString(), timestamp)
        ).addOnFailureListener(e -> {
            setLoading(false);
            SnackbarUtil.showError(this, "Photo upload failed: " + e.getMessage());
        });
    }

    private void saveReport(String uid, String hazardType, String description,
                             String photoUrl, long timestamp) {
        Map<String, Object> report = new HashMap<>();
        report.put("userId", uid);
        report.put("hazardType", hazardType);
        report.put("description", description);
        report.put("photoUrl", photoUrl);
        report.put("latitude", latitude);
        report.put("longitude", longitude);
        report.put("status", "New");
        report.put("timestamp", timestamp);
        report.put("userAgent", "Android/" + android.os.Build.VERSION.RELEASE
                + " (" + android.os.Build.MODEL + ")");

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("reports").add(report)
                .addOnSuccessListener(ref -> {
                    firestore.collection("users").document(uid)
                            .update("totalReports", FieldValue.increment(1));
                    setLoading(false);
                    SnackbarUtil.showSuccess(this, "Report submitted successfully!", this::finish);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    SnackbarUtil.showError(this, "Could not save report: " + e.getMessage());
                });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        textSubmitStatus.setVisibility(loading ? View.VISIBLE : View.GONE);
        buttonSubmit.setEnabled(!loading);
    }
}
