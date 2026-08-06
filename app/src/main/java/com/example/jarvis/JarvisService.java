package com.example.jarvis;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;

import android.content.Intent;
import android.content.SharedPreferences;

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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class jarvisService extends Service
        implements TextToSpeech.OnInitListener {

    private static final String CHANNEL_ID =
            "JARVIS_SERVICE_CHANNEL";

    private SpeechRecognizer speechRecognizer;

    private TextToSpeech textToSpeech;

    private Intent speechIntent;

    private boolean isListening = false;

    private boolean isSpeaking = false;

    private boolean serviceRunning = false;

    private SharedPreferences memory;

    private Handler handler;


    // ==========================================
    // SERVICE CREATED
    // ==========================================

    @Override
    public void onCreate() {

        super.onCreate();

        // IMPORTANT:
        // Initialize these before using them.

        handler =
                new Handler(
                        Looper.getMainLooper()
                );

        memory =
                getSharedPreferences(
                        "JarvisMemory",
                        MODE_PRIVATE
                );

        createNotificationChannel();

        Notification notification;

        if (
                Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.O
        ) {

            notification =
                    new Notification.Builder(
                            this,
                            CHANNEL_ID
                    )
                    .setContentTitle(
                            "Jarvis is running"
                    )
                    .setContentText(
                            "Listening for your commands"
                    )
                    .setSmallIcon(
                            android.R.drawable
                                    .ic_dialog_info
                    )
                    .setOngoing(true)
                    .build();

        } else {

            notification =
                    new Notification.Builder(
                            this
                    )
                    .setContentTitle(
                            "Jarvis is running"
                    )
                    .setContentText(
                            "Listening for your commands"
                    )
                    .setSmallIcon(
                            android.R.drawable
                                    .ic_dialog_info
                    )
                    .setOngoing(true)
                    .build();
        }

        startForeground(
                1001,
                notification
        );

        // Initialize text to speech.

        textToSpeech =
                new TextToSpeech(
                        this,
                        this
                );

        // Initialize speech recognition.

        setupSpeechRecognizer();

        serviceRunning = true;

        // Start listening after a small delay.

        handler.postDelayed(
                new Runnable() {

                    @Override
                    public void run() {

                        startListening();
                    }
                },
                1000
        );
    }


    // ==========================================
    // SERVICE START COMMAND
    // ==========================================

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {

        serviceRunning = true;

        return START_STICKY;
    }


    // ==========================================
    // SETUP SPEECH RECOGNIZER
    // ==========================================

    private void setupSpeechRecognizer() {

        if (
                !SpeechRecognizer
                        .isRecognitionAvailable(
                                this
                        )
        ) {

            return;
        }

        speechRecognizer =
                SpeechRecognizer
                        .createSpeechRecognizer(
                                this
                        );

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
                Locale.getDefault()
                        .toLanguageTag()
        );

        speechIntent.putExtra(
                RecognizerIntent
                        .EXTRA_PARTIAL_RESULTS,
                false
        );

        speechIntent.putExtra(
                RecognizerIntent
                        .EXTRA_MAX_RESULTS,
                1
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
                                serviceRunning
                        ) {

                            handler.postDelayed(
                                    new Runnable() {

                                        @Override
                                        public void run() {

                                            startListening();
                                        }
                                    },
                                    1500
                            );
                        }
                    }


                    @Override
                    public void onResults(
                            Bundle results
                    ) {

                        isListening = false;

                        ArrayList<String> matches =
                                results
                                        .getStringArrayList(
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

                        } else if (
                                serviceRunning
                        ) {

                            handler.postDelayed(
                                    new Runnable() {

                                        @Override
                                        public void run() {

                                            startListening();
                                        }
                                    },
                                    1000
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
    }


    // ==========================================
    // START LISTENING
    // ==========================================

    private void startListening() {

        if (
                !serviceRunning
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

        try {

            speechRecognizer.startListening(
                    speechIntent
            );

        } catch (
                Exception e
        ) {

            isListening = false;

            if (
                    serviceRunning
            ) {

                handler.postDelayed(
                        new Runnable() {

                            @Override
                            public void run() {

                                startListening();
                            }
                        },
                        2000
                );
            }
        }
    }


    // ==========================================
    // COMMAND HANDLER
    // ==========================================

    private void handleCommand(
            String command
    ) {

        if (
                command == null
        ) {

            startListening();

            return;
        }

        command =
                command.toLowerCase(
                        Locale.ROOT
                ).trim();


        if (
                handleTimedReminder(
                        command
                )
        ) {

            return;
        }


        if (
                command.equals(
                        "hey jarvis"
                )
                ||
                command.equals(
                        "hello jarvis"
                )
                ||
                command.equals(
                        "jarvis"
                )
                ||
                command.equals(
                        "hello"
                )
                ||
                command.equals(
                        "hi"
                )
        ) {

            speak(
                    "Yes sir. How can I help you?"
            );
        }


        else if (
                command.contains(
                        "time"
                )
        ) {

            Calendar calendar =
                    Calendar.getInstance();

            SimpleDateFormat format =
                    new SimpleDateFormat(
                            "hh:mm a",
                            Locale.US
                    );

            speak(
                    "The current time is "
                            +
                    format.format(
                            calendar.getTime()
                    )
            );
        }


        else if (
                command.contains(
                        "date"
                )
                ||
                command.contains(
                        "today"
                )
        ) {

            SimpleDateFormat format =
                    new SimpleDateFormat(
                            "dd MMMM yyyy",
                            Locale.US
                    );

            speak(
                    "Today is "
                            +
                    format.format(
                            Calendar
                                    .getInstance()
                                    .getTime()
                    )
            );
        }


        else if (
                command.contains(
                        "show my reminders"
                )
                ||
                command.contains(
                        "list my reminders"
                )
        ) {

            showReminders();
        }


        else if (
                command.contains(
                        "clear all reminders"
                )
                ||
                command.contains(
                        "delete all reminders"
                )
        ) {

            clearAllReminders();
        }


        else if (
                command.startsWith(
                        "remind me to "
                )
        ) {

            String reminder =
                    command.substring(
                            "remind me to "
                                    .length()
                    ).trim();

            if (
                    reminder.isEmpty()
            ) {

                speak(
                        "Please tell me the reminder."
                );

            } else {

                saveSimpleReminder(
                        reminder
                );

                speak(
                        "Okay sir. I saved your reminder."
                );
            }
        }


        else if (
                command.contains(
                        "open instagram"
                )
        ) {

            openApp(
                    "com.instagram.android",
                    "Instagram"
            );
        }


        else if (
                command.contains(
                        "open youtube"
                )
        ) {

            openApp(
                    "com.google.android.youtube",
                    "YouTube"
            );
        }


        else if (
                command.contains(
                        "open whatsapp"
                )
        ) {

            openApp(
                    "com.whatsapp",
                    "WhatsApp"
            );
        }


        else if (
                command.contains(
                        "open spotify"
                )
        ) {

            openApp(
                    "com.spotify.music",
                    "Spotify"
            );
        }


        else if (
                command.contains(
                        "open chrome"
                )
        ) {

            openApp(
                    "com.android.chrome",
                    "Chrome"
            );
        }


        else if (
                command.contains(
                        "open settings"
                )
        ) {

            openApp(
                    "com.android.settings",
                    "Settings"
            );
        }


        else if (
                command.contains(
                        "open google"
                )
        ) {

            openWebsite(
                    "https://www.google.com",
                    "Google"
            );
        }


        else if (
                command.contains(
                        "chatgpt"
                )
                ||
                command.contains(
                        "chat gpt"
                )
        ) {

            openWebsite(
                    "https://chatgpt.com",
                    "ChatGPT"
            );
        }


        else if (
                command.contains(
                        "thank you"
                )
                ||
                command.contains(
                        "thanks"
                )
        ) {

            speak(
                    "You are welcome, sir."
            );
        }


        else if (
                command.contains(
                        "how are you"
                )
        ) {

            speak(
                    "I am doing great, sir."
            );
        }


        else if (
                command.contains(
                        "who are you"
                )
        ) {

            speak(
                    "I am Jarvis, your Android voice assistant."
            );
        }


        else if (
                command.equals(
                        "go back"
                )
                ||
                command.equals(
                        "back"
                )
        ) {

            goBack();
        }


        else {

            speak(
                    "I do not understand that command yet, sir."
            );
        }
    }


    // ==========================================
    // TIMED REMINDER
    // ==========================================

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
                matcher.group(1)
                        .trim();

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
                    amPm.equalsIgnoreCase(
                            "pm"
                    )
                    &&
                    hour != 12
            ) {

                hour += 12;
            }

            if (
                    amPm.equalsIgnoreCase(
                            "am"
                    )
                    &&
                    hour == 12
            ) {

                hour = 0;
            }
        }

        if (
                hour < 0
                ||
                hour > 23
                ||
                minute < 0
                ||
                minute > 59
        ) {

            speak(
                    "That is not a valid time."
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


    // ==========================================
    // SET REMINDER
    // ==========================================

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
                            PendingIntent
                                    .FLAG_UPDATE_CURRENT
                                    |
                            PendingIntent
                                    .FLAG_IMMUTABLE
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

            calendar.set(
                    Calendar.MILLISECOND,
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

                alarmManager
                        .setAndAllowWhileIdle(
                                AlarmManager
                                        .RTC_WAKEUP,
                                calendar
                                        .getTimeInMillis(),
                                pendingIntent
                        );
            }

            saveReminder(
                    reminderId,
                    reminderText
            );

            speak(
                    "Okay sir. I will remind you at "
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


    // ==========================================
    // FORMAT TIME
    // ==========================================

    private String formatTime(
            int hour,
            int minute
    ) {

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

        SimpleDateFormat format =
                new SimpleDateFormat(
                        "hh:mm a",
                        Locale.US
                );

        return format.format(
                calendar.getTime()
        );
    }


    // ==========================================
    // SAVE REMINDER
    // ==========================================

    private void saveReminder(
            int reminderId,
            String reminderText
    ) {

        Set<String> oldReminders =
                memory.getStringSet(
                        "reminders",
                        new HashSet<>()
                );

        Set<String> reminders =
                new HashSet<>(
                        oldReminders
                );

        reminders.add(
                reminderId
                        +
                "|"
                        +
                reminderText
        );

        memory.edit()
                .putStringSet(
                        "reminders",
                        reminders
                )
                .apply();
    }


    // ==========================================
    // SIMPLE REMINDER
    // ==========================================

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


    // ==========================================
    // SHOW REMINDERS
    // ==========================================

    private void showReminders() {

        Set<String> reminders =
                memory.getStringSet(
                        "reminders",
                        new HashSet<>()
                );

        String simpleReminder =
                memory.getString(
                        "reminder",
                        ""
                );

        if (
                reminders.isEmpty()
                &&
                simpleReminder.isEmpty()
        ) {

            speak(
                    "You have no saved reminders."
            );

            return;
        }

        StringBuilder result =
                new StringBuilder(
                        "Your reminders are. "
                );

        for (
                String reminder :
                reminders
        ) {

            String[] parts =
                    reminder.split(
                            "\\|",
                            2
                    );

            if (
                    parts.length == 2
            ) {

                result.append(
                        parts[1]
                );

                result.append(
                        ". "
                );
            }
        }

        if (
                !simpleReminder.isEmpty()
        ) {

            result.append(
                    simpleReminder
            );

            result.append(
                    ". "
            );
        }

        speak(
                result.toString()
        );
    }


    // ==========================================
    // CLEAR REMINDERS
    // ==========================================

    private void clearAllReminders() {

        memory.edit()
                .remove(
                        "reminders"
                )
                .remove(
                        "reminder"
                )
                .apply();

        speak(
                "All reminders have been cleared."
        );
    }


    // ==========================================
    // OPEN APPLICATION
    // ==========================================

    private void openApp(
            String packageName,
            String appName
    ) {

        Intent launchIntent =
                getPackageManager()
                        .getLaunchIntentForPackage(
                                packageName
                        );

        if (
                launchIntent == null
        ) {

            speak(
                    appName
                            +
                    " is not installed."
            );

            return;
        }

        launchIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
        );

        startActivity(
                launchIntent
        );

        speak(
                "Opening "
                        +
                appName
        );
    }


    // ==========================================
    // OPEN WEBSITE
    // ==========================================

    private void openWebsite(
            String url,
            String name
    ) {

        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW
                    );

            intent.setData(
                    android.net.Uri.parse(
                            url
                    )
            );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            startActivity(
                    intent
            );

            speak(
                    "Opening "
                            +
                    name
            );

        } catch (
                Exception e
        ) {

            speak(
                    "I could not open "
                            +
                    name
            );
        }
    }


    // ==========================================
    // SPEAK
    // ==========================================

    private void speak(
            String text
    ) {

        isSpeaking = true;

        if (
                speechRecognizer != null
        ) {

            try {

                speechRecognizer
                        .cancel();

            } catch (
                    Exception ignored
            ) {
            }
        }

        if (
                textToSpeech != null
        ) {

            textToSpeech.speak(
                    text,
                    TextToSpeech
                            .QUEUE_FLUSH,
                    null,
                    "JARVIS_SPEECH"
            );
        }

        handler.postDelayed(
                new Runnable() {

                    @Override
                    public void run() {

                        isSpeaking = false;

                        startListening();
                    }
                },
                3000
        );
    }


    // ==========================================
    // TEXT TO SPEECH READY
    // ==========================================

    @Override
    public void onInit(
            int status
    ) {

        if (
                status ==
                TextToSpeech.SUCCESS
        ) {

            textToSpeech.setLanguage(
                    Locale.US
            );
        }
    }


    // ==========================================
    // GO HOME
    // ==========================================

    private void goBack() {

        Intent intent =
                new Intent(
                        Intent.ACTION_MAIN
                );

        intent.addCategory(
                Intent.CATEGORY_HOME
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
        );

        startActivity(
                intent
        );
    }


    // ==========================================
    // NOTIFICATION CHANNEL
    // ==========================================

    private void createNotificationChannel() {

        if (
                Build.VERSION.SDK_INT
                        >=
                Build.VERSION_CODES.O
        ) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "Jarvis Background Service",
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

                manager
                        .createNotificationChannel(
                                channel
                        );
            }
        }
    }


    // ==========================================
    // DESTROY SERVICE
    // ==========================================

    @Override
    public void onDestroy() {

        serviceRunning = false;

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

            speechRecognizer.destroy();

            speechRecognizer = null;
        }

        if (
                textToSpeech != null
        ) {

            textToSpeech.stop();

            textToSpeech.shutdown();

            textToSpeech = null;
        }

        stopForeground(
                true
        );

        super.onDestroy();
    }


    @Override
    public IBinder onBind(
            Intent intent
    ) {

        return null;
    }
}