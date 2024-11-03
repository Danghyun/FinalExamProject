package com.example.ui;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;

import java.util.HashSet;
import java.util.Set;

public class MyAlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "expiration_channel";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onReceive(Context context, Intent intent) {

        // WakeLock을 사용하여 디바이스를 깨움
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "MyApp::MyWakelockTag");
        wakeLock.acquire(3000);  // 3초 동안 WakeLock 유지

        // 데이터베이스 초기화
        Database.DBHelper dbHelper = new Database.DBHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // 날짜 형식을 변환하고 업데이트
        updateDates(db);

        // 유효기간이 임박한 항목을 찾아 알림 생성
        createNotification(context, db);

        // 데이터베이스 닫기
        db.close();

        // WakeLock 해제
        wakeLock.release();
    }

    // 데이터를 변환하고 업데이트 하는 함수
    private void updateDates(SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT * FROM contacts", null);
        if (cursor.moveToFirst()) {
            do {
                String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                String formattedDate = convertDateFormat(date);

                String exchange = cursor.getString(cursor.getColumnIndexOrThrow("exchange"));
                if (exchange != null) {
                    db.execSQL("UPDATE contacts SET date = ? WHERE exchange = ?", new Object[]{formattedDate, exchange});
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    @SuppressLint("MutatingSharedPrefs")
    private void createNotification(Context context, SQLiteDatabase db) {
        // SharedPreferences 초기화
        SharedPreferences sharedPreferences = context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE);
        Set<String> notifiedItems = sharedPreferences.getStringSet("notifiedItems", new HashSet<>());

        // 유효기간이 임박한 항목을 찾는 쿼리 실행
        String query = "SELECT * FROM contacts WHERE date >= DATE('now') AND date <= DATE('now', '+3 days')";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            StringBuilder message = new StringBuilder("유효기간이 임박한 항목:\n");
            do {
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                String itemId = cursor.getString(cursor.getColumnIndexOrThrow("_id")); // 고유 ID로 _id 사용

                // 이미 알림을 보낸 항목인지 확인
                if (!notifiedItems.contains(itemId)) {
                    message.append(name).append(" (").append(date).append(")\n");
                    // 알림 ID 고유화
                    int notificationId = itemId.hashCode();

                    NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Expiration Notifications", NotificationManager.IMPORTANCE_HIGH);
                        notificationManager.createNotificationChannel(channel);
                    }

                    Intent notificationIntent = new Intent(context, Main.class);
                    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                    NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_notification)  // 알림에 사용할 작은 아이콘을 지정합니다.
                            .setContentTitle("유효기간 임박 알림")
                            .setContentText("유효기간이 임박한 항목이 있습니다.")
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(message.toString()))
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true);
                    notificationManager.notify(notificationId, builder.build());

                    // 알림 발송된 항목 기록
                    notifiedItems.add(itemId);
                }
            } while (cursor.moveToNext());

            // SharedPreferences에 알림 발송된 항목 저장
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putStringSet("notifiedItems", notifiedItems);
            editor.apply();
        }

        cursor.close();
    }

    // 날짜 형식 변환 함수
    private String convertDateFormat(String originalDate) {
        // 원래 날짜 형식: "YYYY년 MM월 DD일"
        // 새로운 날짜 형식: "YYYY-MM-DD"
        if (originalDate != null && originalDate.length() == 13) {
            String year = originalDate.substring(0, 4);
            String month = originalDate.substring(6, 8);
            String day = originalDate.substring(10, 12);
            return year + "-" + month + "-" + day;
        }
        return originalDate;
    }
}
