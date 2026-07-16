package com.example.smartroad.map;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.smartroad.R;
import com.example.smartroad.model.HazardReport;
import com.example.smartroad.util.SnackbarUtil;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HazardDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_REPORT_ID = "extra_report_id";

    private TextView textHazardType;
    private TextView textStatus;
    private TextView textDescription;
    private TextView textReportedBy;
    private TextView textDateTime;
    private TextView textCoordinates;
    private TextView textUserAgent;
    private ImageView imagePhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hazard_details);

        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());

        imagePhoto = findViewById(R.id.imagePhoto);
        textHazardType = findViewById(R.id.textHazardType);
        textStatus = findViewById(R.id.textStatus);
        textDescription = findViewById(R.id.textDescription);
        textReportedBy = findViewById(R.id.textReportedBy);
        textDateTime = findViewById(R.id.textDateTime);
        textCoordinates = findViewById(R.id.textCoordinates);
        textUserAgent = findViewById(R.id.textUserAgent);

        String reportId = getIntent().getStringExtra(EXTRA_REPORT_ID);
        if (reportId == null) {
            finish();
            return;
        }
        loadReport(reportId);
    }

    private void loadReport(String reportId) {
        FirebaseFirestore.getInstance().collection("reports").document(reportId)
                .get()
                .addOnSuccessListener(doc -> {
                    HazardReport report = doc.toObject(HazardReport.class);
                    if (report == null) {
                        SnackbarUtil.showError(this, "Report not found", this::finish);
                        return;
                    }
                    bind(report);
                })
                .addOnFailureListener(e -> SnackbarUtil.showError(this,
                        "Could not load report: " + e.getMessage(), this::finish));
    }

    private void bind(HazardReport report) {
        textHazardType.setText(report.hazardType);
        textDescription.setText(report.description);

        String status = report.status != null ? report.status : "New";
        textStatus.setText(status);
        textStatus.setBackgroundColor(statusColor(status));

        if (report.timestamp != null) {
            SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
            textDateTime.setText("Reported: " + format.format(new Date(report.timestamp)));
        }

        if (report.latitude != null && report.longitude != null) {
            textCoordinates.setText(String.format(Locale.getDefault(),
                    "Location: %.5f, %.5f", report.latitude, report.longitude));
        }

        textUserAgent.setText(report.userAgent != null ? report.userAgent : "");

        if (report.photoUrl != null) {
            Glide.with(this).load(report.photoUrl).into(imagePhoto);
        }

        loadReporterName(report.userId);
    }

    private void loadReporterName(String userId) {
        if (userId == null) {
            textReportedBy.setText("Reported By: Unknown");
            return;
        }
        FirebaseFirestore.getInstance().collection("users").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("name");
                    textReportedBy.setText("Reported By: " + (name != null ? name : "Unknown"));
                })
                .addOnFailureListener(e -> textReportedBy.setText("Reported By: Unknown"));
    }

    private int statusColor(String status) {
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
