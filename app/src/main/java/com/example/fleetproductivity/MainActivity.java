package com.example.fleetproductivity;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Gravity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        TextView textView = new TextView(this);
        textView.setText("Fleet Productivity App is Working!");
        textView.setTextSize(24);
        textView.setGravity(Gravity.CENTER);
        
        setContentView(textView);
    }
}
