package com.example.jarvis;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final int MICROPHONE_PERMISSION_CODE = 100;
    private static final int NOTIFICATION_PERMISSION_CODE = 101;

    private SharedPreferences memory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        memory = getSharedPreferences(
                "JarvisMemory",
                MODE_PRIVATE
        );

        TextView statusText =
                findViewById(R.id.statusText);

        Button startButton =
                findViewById(R.id.startButton);

        Button stopButton =
                findViewById(R.id.stopButton);

        Button deleteRemindersButton =
                findViewById(R.id.deleteRemindersButton);

        loadReminders();

        requestNotificationPermission();

        // START JARVIS
        startButton.setOnClickListener(v -> {

            if (checkMicrophonePermission()) {

                startJarvisService();

                statusText.setText(
                        "● Jarvis is running in the background"
                );

            } else {

                requestMicrophonePermission();
            }
        });

        // STOP JARVIS
        stopButton.setOnClickListener(v -> {

            Intent serviceIntent =
                    new Intent(
                            MainActivity.this,
                            JarvisService.class
                    );

            stopService(serviceIntent);

            statusText.setText(
                    "● Jarvis is stopped"
            );

            Toast.makeText(
                    MainActivity.this,
                    "Jarvis stopped",
                    Toast.LENGTH_SHORT
            ).show();
        });

        // DELETE ALL REMINDERS
        deleteRemindersButton.setOnClickListener(v -> {

            cancelAllReminderAlarms();

            memory.edit()
                    .remove("reminders")
                    .apply();

            loadReminders();

            Toast.makeText(
                    MainActivity.this,
                    "All reminders deleted",
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    // ==========================================
    // LOAD REMINDERS
    // ==========================================

    private void loadReminders() {

        TextView reminderText =
                findViewById(R.id.reminderText);

        Set<String> reminders =
                memory.getStringSet(
                        "reminders",
                        new HashSet<>()
                );

        if (
                reminders == null ||
                reminders.isEmpty()
        ) {

            reminderText.setText(
                    "No reminders yet."
            );

            return;
        }

        StringBuilder text =
                new StringBuilder();

        int number = 1;

        for (
                String savedReminder : reminders
        ) {

            String[] parts =
                    savedReminder.split(
                            "\\|",
                            2
                    );

            if (parts.length == 2) {

                text.append(number)
                        .append(". ")
                        .append(parts[1])
                        .append("\n\n");

                number++;
            }
        }

        if (text.length() == 0) {

            reminderText.setText(
                    "No reminders yet."
            );

        } else {

            reminderText.setText(
                    text.toString()
            );
        }
    }

    // ==========================================
    // CANCEL ALL REMINDER ALARMS
    // ==========================================

    private void cancelAllReminderAlarms() {

        AlarmManager alarmManager =
                (AlarmManager)
                        getSystemService(
                                ALARM_SERVICE
                        );

        if (alarmManager == null) {
            return;
        }

        Set<String> reminders =
                memory.getStringSet(
                        "reminders",
                        new HashSet<>()
                );

        if (
                reminders == null ||
                reminders.isEmpty()
        ) {
            return;
        }

        for (
                String savedReminder : reminders
        ) {

            try {

                String[] parts =
                        savedReminder.split(
                                "\\|",
                                2
                        );

                if (parts.length != 2) {
                    continue;
                }

                int reminderId =
                        Integer.parseInt(
                                parts[0]
                        );

                Intent intent =
                        new Intent(
                                this,
                                ReminderReceiver.class
                        );

                PendingIntent pendingIntent =
                        PendingIntent.getBroadcast(
                                this,
                                reminderId,
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT
                                        |
                                PendingIntent.FLAG_IMMUTABLE
                        );

                alarmManager.cancel(
                        pendingIntent
                );

                pendingIntent.cancel();

            } catch (Exception e) {

                e.printStackTrace();
            }
        }
    }

    // ==========================================
    // MICROPHONE PERMISSION
    // ==========================================

    private boolean checkMicrophonePermission() {

        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestMicrophonePermission() {

        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.RECORD_AUDIO
                },
                MICROPHONE_PERMISSION_CODE
        );
    }

    // ==========================================
    // NOTIFICATION PERMISSION
    // ==========================================

    private void requestNotificationPermission() {

        if (
                Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.TIRAMISU
        ) {

            if (
                    ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.POST_NOTIFICATIONS
                    )
                            !=
                    PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.POST_NOTIFICATIONS
                        },
                        NOTIFICATION_PERMISSION_CODE
                );
            }
        }
    }

    // ==========================================
    // START JARVIS SERVICE
    // ==========================================

    private void startjarvisService() {

        Intent serviceIntent =
                new Intent(
                        this,
                        JarvisService.class
                );

        if (
                Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.O
        ) {

            startForegroundService(
                    serviceIntent
            );

        } else {

            startService(
                    serviceIntent
            );
        }
    }
}