package com.example.smartroad.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartroad.R;
import com.example.smartroad.home.HomeActivity;
import com.example.smartroad.util.SnackbarUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    private TextInputEditText editName;
    private TextInputEditText editUsername;
    private TextInputEditText editEmail;
    private TextInputEditText editPassword;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        editName = findViewById(R.id.editName);
        editUsername = findViewById(R.id.editUsername);
        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        progressBar = findViewById(R.id.progressBar);
        MaterialButton buttonRegister = findViewById(R.id.buttonRegister);

        buttonRegister.setOnClickListener(v -> attemptRegister());
    }

    private void attemptRegister() {
        String name = textOf(editName);
        String username = textOf(editUsername);
        String email = textOf(editEmail);
        String password = textOf(editPassword);

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(username)
                || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            SnackbarUtil.showError(this, "Fill in all fields");
            return;
        }
        if (password.length() < 6) {
            SnackbarUtil.showError(this, "Password must be at least 6 characters");
            return;
        }

        setLoading(true);
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        createUserProfile(name, username, email);
                    } else {
                        setLoading(false);
                        SnackbarUtil.showError(this,
                                "Registration failed: " + task.getException().getMessage());
                    }
                });
    }

    private void createUserProfile(String name, String username, String email) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            setLoading(false);
            return;
        }

        Map<String, Object> profile = new HashMap<>();
        profile.put("name", name);
        profile.put("username", username);
        profile.put("email", email);
        profile.put("totalReports", 0);
        profile.put("createdAt", System.currentTimeMillis());

        firestore.collection("users").document(user.getUid())
                .set(profile)
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        startActivity(new Intent(this, HomeActivity.class));
                        finish();
                    } else {
                        SnackbarUtil.showError(this,
                                "Account created but profile save failed: "
                                        + task.getException().getMessage());
                    }
                });
    }

    private String textOf(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
