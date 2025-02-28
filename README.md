# ỨNG DỤNG NHẬN API VÀ HIỂN THỊ THÔNG BÁO


  Đây là ứng dụng Android sử dụng Retrofit để lấy danh sách bài viết từ API https://jsonplaceholder.typicode.com/posts và hiển thị thông báo (Notification) khi có bài viết mới.

(Do api của thầy bị lỗi nên em sử dụng api này để test)

Ứng dụng sẽ kiểm tra nếu thông báo đã hiển thị trước đó, thì không gửi thông báo. Nếu có thông báo mới, ứng dụng sẽ hiển thị title thông báo và sẽ mở NotificationDetailActivity(toàn bộ body thông báo) khi người dùng nhấn vào thông báo.


Retrofit (Gọi API)

GsonConverterFactory (Chuyển đổi JSON)

NotificationManager (Hiển thị thông báo)

SharedPreferences (Lưu trạng thái thông báo cuối cùng)

## Quy trình làm bài
yêu cầu cấp quyền:

1 Quyền truy cập Internet (INTERNET)

Dùng để kết nối mạng và gọi API từ https://jsonplaceholder.typicode.com/posts.
```java
<!-- Quyền truy cập Internet để gọi API -->
    <uses-permission android:name="android.permission.INTERNET"/>
```
2 Quyền hiển thị thông báo (POST_NOTIFICATIONS)
```java
    <!-- Quyền gửi thông báo (chỉ cần trên Android 13+) -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
```


### 1️⃣ **Gọi API để lấy danh sách bài viết**

Sử dụng Retrofit để gửi request đến API https://jsonplaceholder.typicode.com/posts.

Khi API phản hồi, lấy tiêu đề (title) và nội dung (body) của bài viết đầu tiên.
```java
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
```
### 2️⃣ **Kiểm tra thông báo mới**

Sử dụng SharedPreferences để lưu tiêu đề và nội dung của bài viết cuối cùng.

So sánh bài viết mới với bài viết đã lưu: nếu khác nhau tức là có dữ liệu mới -> hiển thị thông báo, nếu giống nhau tức là dữ liệu này đã thông báo rồi không hiển thị nữa
```java
private boolean isNewNotification(String title, String body) {
    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    String lastTitle = prefs.getString(PREF_LAST_TITLE, "");
    String lastBody = prefs.getString(PREF_LAST_BODY, "");
    return !title.equals(lastTitle) || !body.equals(lastBody);
}
```
Lưu bài viết mới:
```java
private void saveLastNotification(String title, String body) {
    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    SharedPreferences.Editor editor = prefs.edit();
    editor.putString(PREF_LAST_TITLE, title);
    editor.putString(PREF_LAST_BODY, body);
    editor.apply();
}
```
### 3️⃣ **Hiển thị thông báo (Notification)**

Tạo Notification Channel (chỉ cần chạy một lần).

Kiểm tra quyền POST_NOTIFICATIONS trên Android 13+ (API 33 trở lên).

Nếu chưa có quyền, yêu cầu người dùng cấp quyền.

Nếu có quyền, hiển thị thông báo chứa tiêu đề bài viết.

Khi người dùng nhấn vào thông báo, mở NotificationDetailActivity để hiển thị nội dung bài viết.

Tạo Notification Channel:
```java
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
```
Kiểm tra quyền và gửi thông báo:
```java
private void showNotification(String title, String content) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            tempTitle = title;
            tempContent = content;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
            return;
        }
    }
    sendNotification(title, content);
}
```
Gửi thông báo
```java
private void sendNotification(String title, String content) {
    Intent intent = new Intent(this, NotificationDetailActivity.class);
    intent.putExtra("title", title);
    intent.putExtra("body", content);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText("Nhấn để xem chi tiết")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true);
    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
    notificationManager.notify(1, builder.build());
}
```
Xử lý cấp quyền:
```java
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
```
