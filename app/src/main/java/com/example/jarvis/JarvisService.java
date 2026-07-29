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


    // ==========================================
    // SERVICE CREATED
    // ==========================================

    @Override
    public void onCreate() {

        super.onCreate();

        memory =
                getSharedPreferences(
                        "JarvisMemory",
                        MODE_PRIVATE
                );

        handler =
                new Handler(
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
                        "Jarvis is starting"
                )
                .setSmallIcon(
                        android.R.drawable
                                .ic_btn_speak_now
                )
                .setOngoing(true)
                .build();

        startForeground(
                1001,
                notification
        );

        // Start Text-to-Speech.
        // Speech recognition starts only after
        // Text-to-Speech is ready.
        textToSpeech =
                new TextToSpeech(
                        this,
                        this
                );
    }


    // ==========================================
    // TEXT TO SPEECH INITIALIZED
    // ==========================================

    @Override
    public void onInit(
            int status
    ) {

        if (
                status !=
                        TextToSpeech.SUCCESS
        ) {

            return;
        }

        if (
                textToSpeech == null
        ) {

            return;
        }

        int languageResult =
                textToSpeech.setLanguage(
                        Locale.US
                );

        if (
                languageResult ==
                        TextToSpeech
                                .LANG_MISSING_DATA
                ||
                languageResult ==
                        TextToSpeech
                                .LANG_NOT_SUPPORTED
        ) {

            return;
        }

        speak(
                "Hello sir. I am ready."
        );

        handler.postDelayed(
                () -> {

                    if (
                            !serviceDestroyed
                    ) {

                        setupSpeechRecognizer();

                        startListening();
                    }

                },
                2500
        );
    }


    // ==========================================
    // SETUP SPEECH RECOGNIZER
    // ==========================================

    private void setupSpeechRecognizer() {

        if (
                serviceDestroyed
        ) {

            return;
        }

        if (
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission
                                .RECORD_AUDIO
                )
                !=
                PackageManager
                        .PERMISSION_GRANTED
        ) {

            return;
        }

        if (
                !SpeechRecognizer
                        .isRecognitionAvailable(
                                this
                        )
        ) {

            speak(
                    "Speech recognition is not available."
            );

            return;
        }

        try {

            if (
                    speechRecognizer != null
            ) {

                speechRecognizer.destroy();

                speechRecognizer = null;
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

            speechRecognizer
                    .setRecognitionListener(
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
                                                () ->
                                                        startListening(),
                                                1500
                                        );
                                    }
                                }

                                @Override
                                public void onResults(
                                        Bundle results
                                ) {

                                    isListening = false;

                                    ArrayList<String>
                                            matches =
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

                                    } else {

                                        scheduleListening(
                                                1200
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

            e.printStackTrace();
        }
    }


    // ==========================================
    // START LISTENING
    // ==========================================

    private void startListening() {

        if (
                serviceDestroyed
        ) {

            return;
        }

        if (
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission
                                .RECORD_AUDIO
                )
                !=
                PackageManager
                        .PERMISSION_GRANTED
        ) {

            return;
        }

        if (
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

            speechRecognizer
                    .startListening(
                            speechIntent
                    );

        } catch (
                Exception e
        ) {

            isListening = false;

            e.printStackTrace();

            scheduleListening(
                    2000
            );
        }
    }


    // ==========================================
    // SCHEDULE LISTENING
    // ==========================================

    private void scheduleListening(
            long delay
    ) {

        if (
                handler == null
                ||
                serviceDestroyed
        ) {

            return;
        }

        handler.postDelayed(
                () -> startListening(),
                delay
        );
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

            scheduleListening(
                    1000
            );

            return;
        }

        command =
                command.toLowerCase(
                        Locale.ROOT
                )
                .trim();


        // TIMED REMINDER

        if (
                handleTimedReminder(
                        command
                )
        ) {

            return;
        }


        // GREETING

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


        // TIME

        else if (
                command.contains(
                        "time"
                )
        ) {

            Calendar calendar =
                    Calendar.getInstance();

            int hour =
                    calendar.get(
                            Calendar.HOUR_OF_DAY
                    );

            int minute =
                    calendar.get(
                            Calendar.MINUTE
                    );

            speak(
                    "The current time is "
                            +
                    formatTime(
                            hour,
                            minute
                    )
            );
        }


        // DATE

        else if (
                command.contains(
                        "date"
                )
                ||
                command.contains(
                        "today"
                )
        ) {

            java.time.LocalDate date =
                    java.time.LocalDate.now(
                            java.time.ZoneId.of(
                                    "Asia/Kolkata"
                            )
                    );

            speak(
                    "Today is "
                            +
                    date
            );
        }


        // SHOW REMINDERS

        else if (
                command.contains(
                        "show my reminders"
                )
                ||
                command.contains(
                        "list my reminders"
                )
                ||
                command.contains(
                        "what are my reminders"
                )
        ) {

            showReminders();
        }


        // CLEAR ALL REMINDERS

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


        // SIMPLE REMINDER

        else if (
                command.startsWith(
                        "remind me to "
                )
        ) {

            String reminder =
                    command.substring(
                            "remind me to "
                                    .length()
                    )
                    .trim();

            if (
                    reminder.isEmpty()
            ) {

                speak(
                        "Please tell me what to remember."
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


        // CLEAR SIMPLE REMINDER

        else if (
                command.contains(
                        "clear my reminder"
                )
                ||
                command.contains(
                        "delete my reminder"
                )
        ) {

            memory.edit()
                    .remove(
                            "reminder"
                    )
                    .apply();

            speak(
                    "Your reminder has been cleared, sir."
            );
        }


        // PAUSE MUSIC

        else if (
                command.equals(
                        "pause"
                )
                ||
                command.contains(
                        "pause music"
                )
        ) {

            controlMusic(
                    KeyEvent
                            .KEYCODE_MEDIA_PAUSE
            );

            speak(
                    "Music paused, sir."
            );
        }


        // RESUME MUSIC

        else if (
                command.equals(
                        "resume"
                )
                ||
                command.contains(
                        "resume music"
                )
                ||
                command.contains(
                        "continue music"
                )
        ) {

            controlMusic(
                    KeyEvent
                            .KEYCODE_MEDIA_PLAY
            );

            speak(
                    "Resuming music, sir."
            );
        }


        // NEXT SONG

        else if (
                command.equals(
                        "next"
                )
                ||
                command.contains(
                        "next song"
                )
        ) {

            controlMusic(
                    KeyEvent
                            .KEYCODE_MEDIA_NEXT
            );

            speak(
                    "Playing the next song, sir."
            );
        }


        // PREVIOUS SONG

        else if (
                command.equals(
                        "previous"
                )
                ||
                command.contains(
                        "previous song"
                )
        ) {

            controlMusic(
                    KeyEvent
                            .KEYCODE_MEDIA_PREVIOUS
            );

            speak(
                    "Playing the previous song, sir."
            );
        }


        // INSTAGRAM

        else if (
                command.equals(
                        "open instagram"
                )
                ||
                command.equals(
                        "instagram"
                )
        ) {

            openApp(
                    "com.instagram.android",
                    "Instagram"
            );
        }


        // YOUTUBE

        else if (
                command.equals(
                        "open youtube"
                )
                ||
                command.equals(
                        "youtube"
                )
        ) {

            openApp(
                    "com.google.android.youtube",
                    "YouTube"
            );
        }


        // WHATSAPP

        else if (
                command.equals(
                        "open whatsapp"
                )
                ||
                command.equals(
                        "whatsapp"
                )
        ) {

            openApp(
                    "com.whatsapp",
                    "WhatsApp"
            );
        }


        // SPOTIFY

        else if (
                command.equals(
                        "open spotify"
                )
                ||
                command.equals(
                        "spotify"
                )
        ) {

            openApp(
                    "com.spotify.music",
                    "Spotify"
            );
        }


        // CHROME

        else if (
                command.equals(
                        "open chrome"
                )
                ||
                command.equals(
                        "chrome"
                )
        ) {

            openApp(
                    "com.android.chrome",
                    "Chrome"
            );
        }


        // SETTINGS

        else if (
                command.equals(
                        "open settings"
                )
                ||
                command.equals(
                        "settings"
                )
        ) {

            openApp(
                    "com.android.settings",
                    "Settings"
            );
        }


        // CHATGPT

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


        // GOOGLE

        else if (
                command.equals(
                        "open google"
                )
                ||
                command.equals(
                        "google"
                )
        ) {

            openWebsite(
                    "https://www.google.com",
                    "Google"
            );
        }


        // THANKS

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


        // HOW ARE YOU

        else if (
                command.contains(
                        "how are you"
                )
        ) {

            speak(
                    "I am doing great, sir. Ready to help you."
            );
        }


        // WHO ARE YOU

        else if (
                command.contains(
                        "who are you"
                )
        ) {

            speak(
                    "I am Jarvis, your Android voice assistant."
            );
        }


        // GO BACK

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

            speak(
                    "Going back."
            );
        }


        // UNKNOWN COMMAND

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
                matcher.group(3)
                        != null
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
                    "That is not a valid time, sir."
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

            intent.putExtra(
                    "reminder_id",
                    reminderId
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

                if (
                        Build.VERSION.SDK_INT
                                >=
                        Build.VERSION_CODES.S
                        &&
                        alarmManager
                                .canScheduleExactAlarms()
                ) {

                    alarmManager
                            .setExactAndAllowWhileIdle(
                                    AlarmManager
                                            .RTC_WAKEUP,
                                    calendar
                                            .getTimeInMillis(),
                                    pendingIntent
                            );

                } else {

                    alarmManager
                            .setAndAllowWhileIdle(
                                    AlarmManager
                                            .RTC_WAKEUP,
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

            e.printStackTrace();

            speak(
                    "I could not set the reminder, sir."
            );
        }
    }


    // ==========================================
    // SAVE REMINDER
    // ==========================================

    private void saveReminder(
            int reminderId,
            String reminderText
    ) {

        Set<String> reminders =
                memory.getStringSet(
                        "reminders",
                        new HashSet<>()
                );

        Set<String> updatedReminders =
                new HashSet<>(
                        reminders
                );

        updatedReminders.add(
                reminderId
                        +
                "|"
                        +
                reminderText
        );

        memory.edit()
                .putStringSet(
                        "reminders",
                        updatedReminders
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
                reminders == null
                ||
                reminders.isEmpty()
        ) {

            if (
                    simpleReminder.isEmpty()
            ) {

                speak(
                        "You have no saved reminders, sir."
                );

                return;
            }
        }


        StringBuilder result =
                new StringBuilder(
                        "Your reminders are: "
                );


        if (
                reminders != null
        ) {

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
                    )
                    .append(
                            ". "
                    );
                }
            }
        }


        if (
                !simpleReminder.isEmpty()
        ) {

            result.append(
                    simpleReminder
            )
            .append(
                    ". "
            );
        }


        speak(
                result.toString()
        );
    }


    // ==========================================
    // CLEAR ALL REMINDERS
    // ==========================================

    private void clearAllReminders() {

        Set<String> reminders =
                memory.getStringSet(
                        "reminders",
                        new HashSet<>()
                );

        AlarmManager alarmManager =
                (AlarmManager)
                        getSystemService(
                                ALARM_SERVICE
                        );


        if (
                reminders != null
                &&
                alarmManager != null
        ) {

            for (
                    String savedReminder :
                    reminders
            ) {

                try {

                    String[] parts =
                            savedReminder.split(
                                    "\\|",
                                    2
                            );

                    if (
                            parts.length != 2
                    ) {

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
                                    PendingIntent
                                            .FLAG_UPDATE_CURRENT
                                            |
                                    PendingIntent
                                            .FLAG_IMMUTABLE
                            );

                    alarmManager.cancel(
                            pendingIntent
                    );

                    pendingIntent.cancel();

                } catch (
                        Exception e
                ) {

                    e.printStackTrace();
                }
            }
        }


        memory.edit()
                .remove(
                        "reminders"
                )
                .remove(
                        "reminder"
                )
                .apply();


        speak(
                "All reminders have been cleared, sir."
        );
    }


    // ==========================================
    // OPEN APP
    // ==========================================

    private void openApp(
            String packageName,
            String appName
    ) {

        try {

            Intent launchIntent =
                    getPackageManager()
                            .getLaunchIntentForPackage(
                                    packageName
                            );

            if (
                    launchIntent != null
            ) {

                launchIntent.addFlags(
                        Intent
                                .FLAG_ACTIVITY_NEW_TASK
                );

                startActivity(
                        launchIntent
                );

                speak(
                        "Opening "
                                +
                        appName
                                +
                        "."
                );

            } else {

                speak(
                        appName
                                +
                        " is not installed."
                );
            }

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
                    Intent
                            .FLAG_ACTIVITY_NEW_TASK
            );

            startActivity(
                    intent
            );

            speak(
                    "Opening "
                            +
                    name
                            +
                    "."
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
    // TEXT TO SPEECH
    // ==========================================

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

            try {

                textToSpeech.speak(
                        text,
                        TextToSpeech
                                .QUEUE_FLUSH,
                        null,
                        "JARVIS_SPEECH"
                );

            } catch (
                    Exception e
            ) {

                e.printStackTrace();
            }
        }


        handler.postDelayed(
                () -> {

                    if (
                            !serviceDestroyed
                    ) {

                        isSpeaking = false;

                        startListening();
                    }

                },
                2500
        );
    }


    // ==========================================
    // MUSIC CONTROL
    // ==========================================

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

            long eventTime =
                    android.os.SystemClock
                            .uptimeMillis();


            KeyEvent downEvent =
                    new KeyEvent(
                            eventTime,
                            eventTime,
                            KeyEvent.ACTION_DOWN,
                            keyCode,
                            0
                    );


            KeyEvent upEvent =
                    new KeyEvent(
                            eventTime,
                            eventTime,
                            KeyEvent.ACTION_UP,
                            keyCode,
                            0
                    );


            audioManager
                    .dispatchMediaKeyEvent(
                            downEvent
                    );

            audioManager
                    .dispatchMediaKeyEvent(
                            upEvent
                    );

        } catch (
                Exception e
        ) {

            speak(
                    "I could not control the music, sir."
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

        intent.setFlags(
                Intent
                        .FLAG_ACTIVITY_NEW_TASK
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
    // FORMAT TIME
    // ==========================================

    private String formatTime(
            int hour,
            int minute
    ) {

        String period =
                "AM";

        if (
                hour >= 12
        ) {

            period =
                    "PM";
        }

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


    // ==========================================
    // SERVICE DESTROYED
    // ==========================================

    @Override
    public void onDestroy() {

        serviceDestroyed = true;

        isListening = false;

        isSpeaking = false;

        if (
                handler != null
        ) {

            handler
                    .removeCallbacksAndMessages(
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
                    Exception e
            ) {

                e.printStackTrace();
            }

            speechRecognizer = null;
        }

        if (
                textToSpeech != null
        ) {

            try {

                textToSpeech.stop();

                textToSpeech.shutdown();

            } catch (
                    Exception e
            ) {

                e.printStackTrace();
            }

            textToSpeech = null;
        }

        if (
                Build.VERSION.SDK_INT
                        >=
                Build.VERSION_CODES.N
        ) {

            stopForeground(
                    STOP_FOREGROUND_REMOVE
            );

        } else {

            stopForeground(
                    true
            );
        }

        super.onDestroy();
    }


    // ==========================================
    // SERVICE BINDING
    // ==========================================

    @Override
    public IBinder onBind(
            Intent intent
    ) {

        return null;
    }
}