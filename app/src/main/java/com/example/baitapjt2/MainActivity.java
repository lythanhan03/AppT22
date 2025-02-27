package com.example.baitapjt2;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import android.content.Intent;
import android.app.PendingIntent;

import com.example.baitapjt2.model.Post;
import com.example.baitapjt2.network.ApiService;
import com.example.baitapjt2.network.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final String CHANNEL_ID = "post_notifications";
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1;

    private static final String PREFS_NAME = "NotificationPrefs";
    private static final String PREF_LAST_TITLE = "last_title";
    private static final String PREF_LAST_BODY = "last_body";

    private String tempTitle;
    private String tempContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Tạo Notification Channel
        createNotificationChannel();

        // Gọi API lấy danh sách bài viết
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.getPosts().enqueue(new Callback<List<Post>>() {
            @Override
            public void onResponse(Call<List<Post>> call, Response<List<Post>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String newTitle = response.body().get(0).getTitle();
                    String newBody = response.body().get(0).getBody();

                    if (isNewNotification(newTitle, newBody)) {
                        saveLastNotification(newTitle, newBody);
                        showNotification(newTitle, newBody);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Post>> call, Throwable t) {
                Log.e("API_ERROR", "Lỗi kết nối API: " + t.getMessage());
            }
        });
    }

    // Tạo Notification Channel (chỉ cần chạy 1 lần)
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Post Notification";
            String description = "Thông báo khi có bài viết mới";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // Kiểm tra xem thông báo có mới không
    private boolean isNewNotification(String title, String body) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String lastTitle = prefs.getString(PREF_LAST_TITLE, "");
        String lastBody = prefs.getString(PREF_LAST_BODY, "");

        return !title.equals(lastTitle) || !body.equals(lastBody);
    }

    // Lưu thông báo mới nhất vào SharedPreferences
    private void saveLastNotification(String title, String body) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_LAST_TITLE, title);
        editor.putString(PREF_LAST_BODY, body);
        editor.apply();
    }

    // Hiển thị thông báo (Kiểm tra quyền trước khi gửi)
    private void showNotification(String title, String content) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {  // Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                tempTitle = title;
                tempContent = content;
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
                return;
            }
        }
        sendNotification(title, content);
    }

    // Phương thức thực sự hiển thị thông báo
    private void sendNotification(String title, String content) {
        try {
            // Intent mở NotificationDetailActivity khi click vào thông báo
            Intent intent = new Intent(this, NotificationDetailActivity.class);
            intent.putExtra("title", title);
            intent.putExtra("body", content);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            // PendingIntent để mở Activity khi click vào thông báo
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // Xây dựng thông báo
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(title)
                    .setContentText("Nhấn để xem chi tiết") // Chỉ hiển thị tóm tắt
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent) // Gắn Intent mở Activity
                    .setAutoCancel(true); // Tự động đóng thông báo sau khi nhấp

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.notify(1, builder.build());
        } catch (SecurityException e) {
            Log.e("NOTIFICATION_ERROR", "Quyền POST_NOTIFICATIONS bị từ chối hoặc chưa cấp", e);
        }
    }

    // Xử lý kết quả khi người dùng cấp quyền
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("PERMISSION", "Quyền POST_NOTIFICATIONS đã được cấp");
                if (tempTitle != null && tempContent != null) {
                    sendNotification(tempTitle, tempContent);
                    tempTitle = null;
                    tempContent = null;
                }
            } else {
                Log.e("PERMISSION", "Quyền POST_NOTIFICATIONS bị từ chối");
            }
        }
    }
}
