package com.example.baitapjt2;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
public class NotificationDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_detail);

        // Lấy dữ liệu từ Intent
        Intent intent = getIntent();
        String title = intent.getStringExtra("title");
        String body = intent.getStringExtra("body");

        // Gán dữ liệu lên giao diện
        TextView txtTitle = findViewById(R.id.txtTitle);
        TextView txtBody = findViewById(R.id.txtBody);
        txtTitle.setText(title);
        txtBody.setText(body);
    }
}
