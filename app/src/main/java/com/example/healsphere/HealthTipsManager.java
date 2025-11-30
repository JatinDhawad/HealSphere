package com.example.healsphere;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class HealthTipsManager {

    public static List<String> getHealthTips() {
        List<String> tips = new ArrayList<>();
        tips.add("Drink 2–3 liters of water daily to stay hydrated.");
        tips.add("Sleep 7–8 hours every night for a healthy mind.");
        tips.add("Start your morning with deep breathing or meditation.");
        tips.add("Eat fresh fruits and vegetables daily.");
        tips.add("Avoid skipping breakfast — it fuels your day!");
        tips.add("Take short walks every hour if you sit for long periods.");
        tips.add("Add turmeric and ginger to your diet to reduce inflammation.");
        tips.add("Limit sugar and processed food for better immunity.");
        tips.add("Stretch for 5 minutes after waking up to improve circulation.");
        tips.add("Smile more — it boosts your mood and reduces stress.");
        return tips;
    }

    public static void scheduleDailyTip(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, HealthTipReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 3001, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 9);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);

        if (cal.before(Calendar.getInstance())) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        am.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, pi);
    }

    public static String getRandomTip() {
        List<String> tips = getHealthTips();
        return tips.get(new Random().nextInt(tips.size()));
    }
}
