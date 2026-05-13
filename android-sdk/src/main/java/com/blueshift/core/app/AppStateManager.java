package com.blueshift.core.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import com.blueshift.BlueshiftLogger;
import com.blueshift.batch.BulkEventManager;
import com.blueshift.rich_push.Message;
import com.blueshift.rich_push.NotificationFactory;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.List;
import java.util.Map;

/**
 * Manages app state transitions and handles recovery from Android 15+ package stopped state.
 * When an app enters the stopped state in Android 15+, all PendingIntents are automatically
 * cancelled. This class provides mechanisms to detect when the app resumes and restore
 * any cancelled scheduled notifications and alarms.
 */
public class AppStateManager {
    private static final String TAG = "AppStateManager";
    private static final String PREFS_NAME = "blueshift_app_state";
    private static final String PREFS_SCHEDULED_NOTIFICATIONS = "blueshift_scheduled_notifications";
    private static final String KEY_NOTIFICATION_PREFIX = "notification_";
    private static final String KEY_SCHEDULE_TIME_PREFIX = "schedule_time_";
    private static final String KEY_LAST_RESUME_TIME = "last_resume_time";

    /**
     * Called when the app resumes from background or stopped state.
     * Handles recovery of cancelled PendingIntents for Android 15+.
     *
     * @param context Application context
     */
    public static void onAppResumed(Context context) {
        if (context == null) return;

        BlueshiftLogger.d(TAG, "App resumed, checking for Android 15+ recovery needs");
        
        // Update last resume time for tracking
        updateLastResumeTime(context);

        // Handle Android 15+ stopped state recovery
        if (Build.VERSION.SDK_INT >= 35) { // Android 15+ API level
            checkAndRescheduleNotifications(context);
            BulkEventManager.ensureAlarmIsScheduled(context);
        }
    }

    /**
     * Stores scheduled notification data for recovery purposes.
     * This allows rescheduling if the PendingIntent gets cancelled.
     *
     * @param context Application context
     * @param message Notification message
     * @param scheduleTime Time when notification should be displayed
     */
    public static void trackScheduledNotification(Context context, Message message, long scheduleTime) {
        if (context == null || message == null) return;

        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_SCHEDULED_NOTIFICATIONS, Context.MODE_PRIVATE);
            String notificationData = new Gson().toJson(message);
            String messageId = message.getId();
            
            if (messageId != null) {
                prefs.edit()
                    .putString(KEY_NOTIFICATION_PREFIX + messageId, notificationData)
                    .putLong(KEY_SCHEDULE_TIME_PREFIX + messageId, scheduleTime)
                    .apply();
                
                BlueshiftLogger.d(TAG, "Tracked scheduled notification: " + messageId + " for time: " + scheduleTime);
            }
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, "Failed to track scheduled notification: " + e.getMessage());
        }
    }

    /**
     * Removes tracking data for a notification that has been delivered or cancelled.
     *
     * @param context Application context
     * @param messageId ID of the message to stop tracking
     */
    public static void untrackScheduledNotification(Context context, String messageId) {
        if (context == null || messageId == null) return;

        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_SCHEDULED_NOTIFICATIONS, Context.MODE_PRIVATE);
            prefs.edit()
                .remove(KEY_NOTIFICATION_PREFIX + messageId)
                .remove(KEY_SCHEDULE_TIME_PREFIX + messageId)
                .apply();
            
            BlueshiftLogger.d(TAG, "Untracked scheduled notification: " + messageId);
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, "Failed to untrack scheduled notification: " + e.getMessage());
        }
    }

    /**
     * Checks for scheduled notifications that may have been cancelled and reschedules them.
     * This is called when the app resumes on Android 15+.
     *
     * @param context Application context
     */
    private static void checkAndRescheduleNotifications(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_SCHEDULED_NOTIFICATIONS, Context.MODE_PRIVATE);
            Map<String, ?> allEntries = prefs.getAll();
            
            if (allEntries.isEmpty()) {
                BlueshiftLogger.d(TAG, "No scheduled notifications to check");
                return;
            }

            BlueshiftLogger.d(TAG, "Checking " + allEntries.size() + " tracked notifications for rescheduling");
            
            SharedPreferences.Editor editor = prefs.edit();
            int rescheduledCount = 0;
            int deliveredCount = 0;
            int cleanedCount = 0;
            
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                String key = entry.getKey();
                
                if (key.startsWith(KEY_NOTIFICATION_PREFIX)) {
                    String messageId = key.replace(KEY_NOTIFICATION_PREFIX, "");
                    long scheduleTime = prefs.getLong(KEY_SCHEDULE_TIME_PREFIX + messageId, 0);
                    
                    if (scheduleTime > System.currentTimeMillis()) {
                        // Notification is still in future, attempt to reschedule it
                        try {
                            String messageData = (String) entry.getValue();
                            if (messageData != null) {
                                Message message = new Gson().fromJson(messageData, Message.class);
                                if (message != null) {
                                    NotificationFactory.scheduleNotificationWithFallback(context, message, scheduleTime);
                                    BlueshiftLogger.d(TAG, "Rescheduled notification: " + messageId);
                                    rescheduledCount++;
                                } else {
                                    BlueshiftLogger.e(TAG, "Failed to parse message data for: " + messageId);
                                    // Clean up invalid data
                                    editor.remove(key).remove(KEY_SCHEDULE_TIME_PREFIX + messageId);
                                    cleanedCount++;
                                }
                            }
                        } catch (JsonSyntaxException e) {
                            BlueshiftLogger.e(TAG, "Failed to parse notification data for " + messageId + ": " + e.getMessage());
                            // Clean up corrupted data
                            editor.remove(key).remove(KEY_SCHEDULE_TIME_PREFIX + messageId);
                            cleanedCount++;
                        } catch (Exception e) {
                            BlueshiftLogger.e(TAG, "Failed to reschedule notification " + messageId + ": " + e.getMessage());
                        }
                    } else {
                        // Notification time has passed, deliver it as a missed notification
                        try {
                            String messageData = (String) entry.getValue();
                            if (messageData != null) {
                                BlueshiftLogger.d(TAG, "Processing missed notification data: " + messageData.substring(0, Math.min(200, messageData.length())) + "...");
                                Message message = new Gson().fromJson(messageData, Message.class);
                                if (message != null) {
                                    // Get the individual notification from the parent message
                                    List<Message> notifications = message.getNotifications();
                                    if (notifications != null && !notifications.isEmpty()) {
                                        Message notification = notifications.get(0);
                                        if (notification != null) {
                                            BlueshiftLogger.d(TAG, "Delivering missed notification: " + messageId);
                                            // Deliver the missed notification immediately
                                            NotificationFactory.handleMessage(context, notification);
                                            deliveredCount++;
                                        } else {
                                            BlueshiftLogger.e(TAG, "Notification object is null in message: " + messageId);
                                        }
                                    } else {
                                        BlueshiftLogger.e(TAG, "No notifications array found in message: " + messageId + ". Trying to deliver parent message directly.");
                                        // Debug: Check what content is available in the parent message
                                        BlueshiftLogger.d(TAG, "Parent message content_title: " + message.getContentTitle());
                                        BlueshiftLogger.d(TAG, "Parent message content_text: " + message.getContentText());
                                        BlueshiftLogger.d(TAG, "Parent message JSON: " + message.toJson());
                                        // Fallback: try to deliver the parent message directly if it has notification content
                                        if (message.getContentTitle() != null || message.getContentText() != null) {
                                            BlueshiftLogger.d(TAG, "Delivering parent message as missed notification: " + messageId);
                                            NotificationFactory.handleMessage(context, message);
                                            deliveredCount++;
                                        } else {
                                            BlueshiftLogger.e(TAG, "Parent message also has no notification content: " + messageId);
                                        }
                                    }
                                } else {
                                    BlueshiftLogger.e(TAG, "Failed to parse missed notification data for: " + messageId);
                                }
                            }
                        } catch (Exception e) {
                            BlueshiftLogger.e(TAG, "Failed to deliver missed notification " + messageId + ": " + e.getMessage());
                        }
                        
                        // Clean up the tracking data
                        editor.remove(key).remove(KEY_SCHEDULE_TIME_PREFIX + messageId);
                    }
                }
            }
            
            // Apply all cleanup operations
            editor.apply();
            
            BlueshiftLogger.d(TAG, "Notification recovery complete. Rescheduled: " + rescheduledCount + ", Delivered: " + deliveredCount + ", Cleaned: " + cleanedCount);
            
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, "Failed to check and reschedule notifications: " + e.getMessage());
        }
    }

    /**
     * Updates the last resume time for tracking purposes.
     *
     * @param context Application context
     */
    private static void updateLastResumeTime(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                .putLong(KEY_LAST_RESUME_TIME, System.currentTimeMillis())
                .apply();
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, "Failed to update last resume time: " + e.getMessage());
        }
    }
}