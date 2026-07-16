package com.example.smartroad.profile;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartroad.R;
import com.example.smartroad.about.AboutActivity;
import com.example.smartroad.auth.LoginActivity;
import com.example.smartroad.map.HazardDetailsActivity;
import com.example.smartroad.model.HazardReport;
import com.example.smartroad.util.SnackbarUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private ProgressBar progressMyReports;
    private TextView textMyReportsEmpty;
    private TextView textName;
    private TextView textAvatarInitial;
    private MyReportsAdapter myReportsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        auth = FirebaseAuth.getInstance();

        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
        findViewById(R.id.buttonAbout).setOnClickListener(v ->
                startActivity(new Intent(this, AboutActivity.class)));

        textName = findViewById(R.id.textName);
        TextView textUsername = findViewById(R.id.textUsername);
        TextView textTotalReports = findViewById(R.id.textTotalReports);
        textAvatarInitial = findViewById(R.id.textAvatarInitial);
        MaterialButton buttonLogout = findViewById(R.id.buttonLogout);

        findViewById(R.id.buttonEditName).setOnClickListener(v -> showEditNameDialog());

        RecyclerView recyclerMyReports = findViewById(R.id.recyclerMyReports);
        progressMyReports = findViewById(R.id.progressMyReports);
        textMyReportsEmpty = findViewById(R.id.textMyReportsEmpty);

        myReportsAdapter = new MyReportsAdapter(reportId -> {
            Intent intent = new Intent(this, HazardDetailsActivity.class);
            intent.putExtra(HazardDetailsActivity.EXTRA_REPORT_ID, reportId);
            startActivity(intent);
        });
        recyclerMyReports.setLayoutManager(new LinearLayoutManager(this));
        recyclerMyReports.setAdapter(myReportsAdapter);

        buttonLogout.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("name");
                    textName.setText(name);
                    textUsername.setText("@" + doc.getString("username"));
                    Long totalReports = doc.getLong("totalReports");
                    textTotalReports.setText("Total reports submitted: "
                            + (totalReports != null ? totalReports : 0));
                    setAvatar(textAvatarInitial, name);
                });

        loadMyReports(currentUser.getUid());
    }

    private void loadMyReports(String uid) {
        progressMyReports.setVisibility(View.VISIBLE);
        textMyReportsEmpty.setVisibility(View.GONE);

        FirebaseFirestore.getInstance().collection("reports")
                .whereEqualTo("userId", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    progressMyReports.setVisibility(View.GONE);

                    List<HazardReport> reports = new ArrayList<>();
                    snapshot.forEach(doc -> {
                        HazardReport report = doc.toObject(HazardReport.class);
                        report.id = doc.getId();
                        reports.add(report);
                    });

                    myReportsAdapter.submitList(reports);
                    textMyReportsEmpty.setVisibility(reports.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressMyReports.setVisibility(View.GONE);
                    SnackbarUtil.showError(this, "Could not load your reports: " + e.getMessage());
                });
    }

    private void showEditNameDialog() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_name, null);
        TextInputLayout inputLayoutName = dialogView.findViewById(R.id.inputLayoutName);
        TextInputEditText editNewName = dialogView.findViewById(R.id.editNewName);
        editNewName.setText(textName.getText());
        if (editNewName.getText() != null) {
            editNewName.setSelection(editNewName.getText().length());
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Edit Name")
                .setView(dialogView)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String newName = editNewName.getText() == null
                            ? "" : editNewName.getText().toString().trim();

                    if (TextUtils.isEmpty(newName)) {
                        inputLayoutName.setError("Name cannot be empty");
                        return;
                    }

                    FirebaseFirestore.getInstance().collection("users")
                            .document(currentUser.getUid())
                            .update("name", newName)
                            .addOnSuccessListener(unused -> {
                                textName.setText(newName);
                                setAvatar(textAvatarInitial, newName);
                                SnackbarUtil.showSuccess(this, "Name updated");
                                dialog.dismiss();
                            })
                            .addOnFailureListener(e -> SnackbarUtil.showError(this,
                                    "Could not update name: " + e.getMessage()));
                }));

        dialog.show();
    }

    private void setAvatar(TextView avatarView, String name) {
        String initial = (name != null && !name.isEmpty())
                ? name.substring(0, 1).toUpperCase()
                : "?";
        avatarView.setText(initial);

        boolean useSecondary = initial.charAt(0) % 2 == 0;
        int color = ContextCompat.getColor(this,
                useSecondary ? R.color.smartroad_secondary : R.color.smartroad_primary);

        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(color);
        avatarView.setBackground(background);
    }
}
