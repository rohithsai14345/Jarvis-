package com.example.jarvis;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.KeyEvent;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JarvisService extends Service
implements TextToSpeech.OnInitListener {

private static final String CHANNEL_ID =
        "JARVIS_SERVICE_CHANNEL";

private SpeechRecognizer speechRecognizer;
private TextToSpeech textToSpeech;
private Intent speechIntent;

private boolean isListening = false;
private boolean isSpeaking = false;
private boolean serviceDestroyed = false;

private SharedPreferences memory;
private Handler handler;

@Override
public void onCreate() {

    super.onCreate();

    memory = getSharedPreferences(
            "JarvisMemory",
            MODE_PRIVATE
    );

    handler = new Handler(
            Looper.getMainLooper()
    );

    createNotificationChannel();

    Notification notification =
            new Notification.Builder(
                    this,
                    CHANNEL_ID
            )
            .setContentTitle(
                    "Jarvis is running"
            )
            .setContentText(
                    "Jarvis is ready"
            )
            .setSmallIcon(
                    android.R.drawable.ic_btn_speak_now
            )
            .setOngoing(true)
            .build();

    startForeground(
            1001,
            notification
    );

    textToSpeech =
            new TextToSpeech(
                    this,
                    this
            );

    if (
            ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
            )
                    ==
            PackageManager.PERMISSION_GRANTED
    ) {

        setupSpeechRecognizer();

    }
}

@Override
public int onStartCommand(
        Intent intent,
        int flags,
        int startId
) {

    return START_STICKY;
}

private void setupSpeechRecognizer() {

    try {

        if (
                !SpeechRecognizer
                        .isRecognitionAvailable(this)
        ) {

            return;
        }

        speechRecognizer =
                SpeechRecognizer
                        .createSpeechRecognizer(this);

        speechIntent =
                new Intent(
                        RecognizerIntent
                                .ACTION_RECOGNIZE_SPEECH
                );

        speechIntent.putExtra(
                RecognizerIntent
                        .EXTRA_LANGUAGE_MODEL,
                RecognizerIntent
                        .LANGUAGE_MODEL_FREE_FORM
        );

        speechIntent.putExtra(
                RecognizerIntent
                        .EXTRA_LANGUAGE,
                Locale.US
        );

        speechIntent.putExtra(
                RecognizerIntent
                        .EXTRA_PARTIAL_RESULTS,
                false
        );

        speechRecognizer.setRecognitionListener(
                new RecognitionListener() {

                    @Override
                    public void onReadyForSpeech(
                            Bundle params
                    ) {

                        isListening = true;
                    }

                    @Override
                    public void onBeginningOfSpeech() {
                    }

                    @Override
                    public void onRmsChanged(
                            float rmsdB
                    ) {
                    }

                    @Override
                    public void onBufferReceived(
                            byte[] buffer
                    ) {
                    }

                    @Override
                    public void onEndOfSpeech() {

                        isListening = false;
                    }

                    @Override
                    public void onError(
                            int error
                    ) {

                        isListening = false;

                        if (
                                !serviceDestroyed
                        ) {

                            handler.postDelayed(
                                    () -> startListening(),
                                    2000
                            );
                        }
                    }

                    @Override
                    public void onResults(
                            Bundle results
                    ) {

                        isListening = false;

                        ArrayList<String> matches =
                                results.getStringArrayList(
                                        SpeechRecognizer
                                                .RESULTS_RECOGNITION
                                );

                        if (
                                matches != null
                                &&
                                !matches.isEmpty()
                        ) {

                            handleCommand(
                                    matches.get(0)
                            );
                        }

                        if (
                                !serviceDestroyed
                        ) {

                            handler.postDelayed(
                                    () -> startListening(),
                                    1500
                            );
                        }
                    }

                    @Override
                    public void onPartialResults(
                            Bundle partialResults
                    ) {
                    }

                    @Override
                    public void onEvent(
                            int eventType,
                            Bundle params
                    ) {
                    }
                }
        );

    } catch (
            Exception e
    ) {

        speechRecognizer = null;
    }
}

private void startListening() {

    if (
            serviceDestroyed
            ||
            speechRecognizer == null
            ||
            speechIntent == null
            ||
            isListening
            ||
            isSpeaking
    ) {

        return;
    }

    if (
            ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
            )
                    !=
            PackageManager.PERMISSION_GRANTED
    ) {

        return;
    }

    try {

        speechRecognizer.startListening(
                speechIntent
        );

    } catch (
            Exception e
    ) {

        isListening = false;

        if (
                !serviceDestroyed
        ) {

            handler.postDelayed(
                    () -> startListening(),
                    2500
            );
        }
    }
}

private void handleCommand(
        String command
) {

    command =
            command.toLowerCase(
                    Locale.ROOT
            )
            .trim();

    if (
            handleTimedReminder(command)
    ) {

        return;
    }

    if (
            command.equals("jarvis")
            ||
            command.equals("hello jarvis")
            ||
            command.equals("hey jarvis")
    ) {

        speak(
                "Yes sir. How can I help you?"
        );

    } else if (
            command.contains("time")
    ) {

        Calendar calendar =
                Calendar.getInstance();

        speak(
                "The time is "
                        +
                formatTime(
                        calendar.get(
                                Calendar.HOUR_OF_DAY
                        ),
                        calendar.get(
                                Calendar.MINUTE
                        )
                )
        );

    } else if (
            command.contains("date")
            ||
            command.contains("today")
    ) {

        speak(
                "Today is "
                        +
                java.text.DateFormat
                        .getDateInstance(
                                java.text.DateFormat.LONG
                        )
                        .format(
                                new java.util.Date()
                        )
        );

    } else if (
            command.contains(
                    "show my reminders"
            )
    ) {

        showReminders();

    } else if (
            command.contains(
                    "clear all reminders"
            )
    ) {

        clearAllReminders();

    } else if (
            command.startsWith(
                    "remind me to "
            )
    ) {

        String reminder =
                command.substring(
                        "remind me to ".length()
                )
                .trim();

        saveSimpleReminder(
                reminder
        );

        speak(
                "Reminder saved."
        );

    } else if (
            command.contains("open youtube")
    ) {

        openApp(
                "com.google.android.youtube",
                "YouTube"
        );

    } else if (
            command.contains("open whatsapp")
    ) {

        openApp(
                "com.whatsapp",
                "WhatsApp"
        );

    } else if (
            command.contains("open spotify")
    ) {

        openApp(
                "com.spotify.music",
                "Spotify"
        );

    } else if (
            command.contains("open chrome")
    ) {

        openApp(
                "com.android.chrome",
                "Chrome"
        );

    } else if (
            command.contains("open instagram")
    ) {

        openApp(
                "com.instagram.android",
                "Instagram"
        );

    } else if (
            command.contains("pause")
    ) {

        controlMusic(
                KeyEvent.KEYCODE_MEDIA_PAUSE
        );

        speak(
                "Music paused."
        );

    } else if (
            command.contains("resume")
            ||
            command.contains("continue")
    ) {

        controlMusic(
                KeyEvent.KEYCODE_MEDIA_PLAY
        );

        speak(
                "Music resumed."
        );

    } else if (
            command.contains("next song")
    ) {

        controlMusic(
                KeyEvent.KEYCODE_MEDIA_NEXT
        );

    } else if (
            command.contains("previous song")
    ) {

        controlMusic(
                KeyEvent.KEYCODE_MEDIA_PREVIOUS
        );

    } else if (
            command.contains("thank")
    ) {

        speak(
                "You are welcome."
        );

    } else {

        speak(
                "I do not understand that command yet."
        );
    }
}

private boolean handleTimedReminder(
        String command
) {

    Pattern pattern =
            Pattern.compile(
                    "remind me to (.+) at (\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?",
                    Pattern.CASE_INSENSITIVE
            );

    Matcher matcher =
            pattern.matcher(
                    command
            );

    if (
            !matcher.matches()
    ) {

        return false;
    }

    String reminderText =
            matcher.group(1).trim();

    int hour =
            Integer.parseInt(
                    matcher.group(2)
            );

    int minute = 0;

    if (
            matcher.group(3) != null
    ) {

        minute =
                Integer.parseInt(
                        matcher.group(3)
                );
    }

    String amPm =
            matcher.group(4);

    if (
            amPm != null
    ) {

        if (
                amPm.equalsIgnoreCase("pm")
                &&
                hour != 12
        ) {

            hour += 12;
        }

        if (
                amPm.equalsIgnoreCase("am")
                &&
                hour == 12
        ) {

            hour = 0;
        }
    }

    if (
            hour > 23
            ||
            minute > 59
    ) {

        speak(
                "Invalid reminder time."
        );

        return true;
    }

    setReminder(
            reminderText,
            hour,
            minute
    );

    return true;
}

private void setReminder(
        String reminderText,
        int hour,
        int minute
) {

    try {

        int reminderId =
                (int)
                        (
                                System.currentTimeMillis()
                                        / 1000
                        );

        Intent intent =
                new Intent(
                        this,
                        ReminderReceiver.class
                );

        intent.putExtra(
                "reminder",
                reminderText
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

        Calendar calendar =
                Calendar.getInstance();

        calendar.set(
                Calendar.HOUR_OF_DAY,
                hour
        );

        calendar.set(
                Calendar.MINUTE,
                minute
        );

        calendar.set(
                Calendar.SECOND,
                0
        );

        if (
                calendar.getTimeInMillis()
                <=
                System.currentTimeMillis()
        ) {

            calendar.add(
                    Calendar.DAY_OF_YEAR,
                    1
            );
        }

        AlarmManager alarmManager =
                (AlarmManager)
                        getSystemService(
                                ALARM_SERVICE
                        );

        if (
                alarmManager != null
        ) {

            if (
                    Build.VERSION.SDK_INT
                    >=
                    Build.VERSION_CODES.S
                    &&
                    !alarmManager
                            .canScheduleExactAlarms()
            ) {

                alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );

            } else {

                alarmManager
                        .setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                calendar
                                        .getTimeInMillis(),
                                pendingIntent
                        );
            }
        }

        saveReminder(
                reminderId,
                reminderText
        );

        speak(
                "Reminder set for "
                        +
                formatTime(
                        hour,
                        minute
                )
        );

    } catch (
            Exception e
    ) {

        speak(
                "I could not set the reminder."
        );
    }
}

private void saveReminder(
        int reminderId,
        String reminderText
) {

    Set<String> oldReminders =
            memory.getStringSet(
                    "reminders",
                    new HashSet<>()
            );

    Set<String> newReminders =
            new HashSet<>(
                    oldReminders
            );

    newReminders.add(
            reminderId
                    +
            "|"
                    +
            reminderText
    );

    memory.edit()
            .putStringSet(
                    "reminders",
                    newReminders
            )
            .apply();
}

private void saveSimpleReminder(
        String reminder
) {

    memory.edit()
            .putString(
                    "reminder",
                    reminder
            )
            .apply();
}

private void showReminders() {

    Set<String> reminders =
            memory.getStringSet(
                    "reminders",
                    new HashSet<>()
            );

    if (
            reminders.isEmpty()
    ) {

        speak(
                "You have no reminders."
        );

        return;
    }

    StringBuilder result =
            new StringBuilder(
                    "Your reminders are. "
            );

    for (
            String item : reminders
    ) {

        String[] parts =
                item.split(
                        "\\|",
                        2
                );

        if (
                parts.length == 2
        ) {

            result.append(
                    parts[1]
            )
            .append(". ");
        }
    }

    speak(
            result.toString()
    );
}

private void clearAllReminders() {

    memory.edit()
            .remove("reminders")
            .remove("reminder")
            .apply();

    speak(
            "All reminders cleared."
    );
}

private void openApp(
        String packageName,
        String appName
) {

    try {

        Intent intent =
                getPackageManager()
                        .getLaunchIntentForPackage(
                                packageName
                        );

        if (
                intent == null
        ) {

            speak(
                    appName
                            +
                    " is not installed."
            );

            return;
        }

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
        );

        startActivity(
                intent
        );

        speak(
                "Opening "
                        +
                appName
        );

    } catch (
            Exception e
    ) {

        speak(
                "I could not open "
                        +
                appName
        );
    }
}

private void controlMusic(
        int keyCode
) {

    try {

        AudioManager audioManager =
                (AudioManager)
                        getSystemService(
                                AUDIO_SERVICE
                        );

        if (
                audioManager == null
        ) {

            return;
        }

        long time =
                android.os.SystemClock
                        .uptimeMillis();

        audioManager
                .dispatchMediaKeyEvent(
                        new KeyEvent(
                                time,
                                time,
                                KeyEvent.ACTION_DOWN,
                                keyCode,
                                0
                        )
                );

        audioManager
                .dispatchMediaKeyEvent(
                        new KeyEvent(
                                time,
                                time,
                                KeyEvent.ACTION_UP,
                                keyCode,
                                0
                        )
                );

    } catch (
            Exception ignored
    ) {
    }
}

private void speak(
        String text
) {

    if (
            serviceDestroyed
    ) {

        return;
    }

    isSpeaking = true;

    if (
            textToSpeech != null
    ) {

        textToSpeech.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "JARVIS"
        );
    }

    handler.removeCallbacksAndMessages(
            null
    );

    handler.postDelayed(
            () -> {

                isSpeaking = false;

                startListening();

            },
            3000
    );
}

@Override
public void onInit(
        int status
) {

    if (
            status
            ==
            TextToSpeech.SUCCESS
    ) {

        textToSpeech.setLanguage(
                Locale.US
        );

        speak(
                "Hello. Jarvis is ready."
        );
    }
}

private String formatTime(
        int hour,
        int minute
) {

    String period =
            hour >= 12
                    ?
            "PM"
                    :
            "AM";

    int displayHour =
            hour % 12;

    if (
            displayHour == 0
    ) {

        displayHour = 12;
    }

    return String.format(
            Locale.US,
            "%d:%02d %s",
            displayHour,
            minute,
            period
    );
}

private void createNotificationChannel() {

    if (
            Build.VERSION.SDK_INT
            >=
            Build.VERSION_CODES.O
    ) {

        NotificationChannel channel =
                new NotificationChannel(
                        CHANNEL_ID,
                        "Jarvis Service",
                        NotificationManager
                                .IMPORTANCE_LOW
                );

        NotificationManager manager =
                getSystemService(
                        NotificationManager.class
                );

        if (
                manager != null
        ) {

            manager.createNotificationChannel(
                    channel
            );
        }
    }
}

@Override
public void onDestroy() {

    serviceDestroyed = true;

    if (
            handler != null
    ) {

        handler.removeCallbacksAndMessages(
                null
        );
    }

    if (
            speechRecognizer != null
    ) {

        try {

            speechRecognizer.cancel();

            speechRecognizer.destroy();

        } catch (
                Exception ignored
        ) {
        }

        speechRecognizer = null;
    }

    if (
            textToSpeech != null
    ) {

        textToSpeech.stop();

        textToSpeech.shutdown();

        textToSpeech = null;
    }

    super.onDestroy();
}

@Override
public IBinder onBind(
        Intent intent
) {

    return null;
}

}