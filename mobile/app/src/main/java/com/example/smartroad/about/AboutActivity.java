package com.example.smartroad.about;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartroad.R;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
    }
}
