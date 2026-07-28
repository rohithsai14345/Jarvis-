package com.example.jarvis;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID =
            "JARVIS_REMINDER_CHANNEL";

    @Override
    public void onReceive(
            Context context,
            Intent intent
    ) {

        createReminderChannel(context);

        String reminder =
                intent.getStringExtra(
                        "reminder"
                );

        if (
                reminder == null ||
                reminder.trim().isEmpty()
        ) {

            reminder =
                    "You have a reminder.";
        }

        Notification notification;

        if (
                Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.O
        ) {

            notification =
                    new Notification.Builder(
                            context,
                            CHANNEL_ID
                    )
                    .setContentTitle(
                            "Jarvis Reminder"
                    )
                    .setContentText(
                            reminder
                    )
                    .setSmallIcon(
                            android.R.drawable
                                    .ic_dialog_info
                    )
                    .setAutoCancel(true)
                    .build();

        } else {

            notification =
                    new Notification.Builder(
                            context
                    )
                    .setContentTitle(
                            "Jarvis Reminder"
                    )
                    .setContentText(
                            reminder
                    )
                    .setSmallIcon(
                            android.R.drawable
                                    .ic_dialog_info
                    )
                    .setAutoCancel(true)
                    .build();
        }

        NotificationManager manager =
                (NotificationManager)
                        context.getSystemService(
                                Context.NOTIFICATION_SERVICE
                        );

        if (manager != null) {

            int notificationId =
                    (int)
                            System.currentTimeMillis();

            manager.notify(
                    notificationId,
                    notification
            );
        }
    }

    private void createReminderChannel(
            Context context
    ) {

        if (
                Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.O
        ) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "Jarvis Reminders",
                            NotificationManager
                                    .IMPORTANCE_HIGH
                    );

            channel.setDescription(
                    "Notifications for Jarvis reminders"
            );

            NotificationManager manager =
                    context.getSystemService(
                            NotificationManager.class
                    );

            if (manager != null) {

                manager.createNotificationChannel(
                        channel
                );
            }
        }
    }
}