package de.piobyte.barnearby.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.google.android.gms.nearby.messages.Message;

import java.util.ArrayList;
import java.util.List;

import de.piobyte.barnearby.R;

public class BackgroundSubscribeIntentService extends IntentService {

    private static final String TAG = BackgroundSubscribeIntentService.class.getSimpleName();

    private static final int MESSAGES_NOTIFICATION_ID = 1;
    private static final int NUM_MESSAGES_IN_NOTIFICATION = 5;

    public BackgroundSubscribeIntentService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        updateNotification(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
//        if (intent == null) {
//            return;
//        }
//        Nearby.Messages.handleIntent(intent, new MessageListener() {
//            // Called each time a new message is discovered nearby.
//            @Override
//            public void onFound(Message message) {
//                Log.i(TAG, "Found message: " + message);
//                CacheUtils.saveFoundMessage(getApplicationContext(), message);
//                updateNotification(BackgroundSubscribeIntentService.this);
//            }
//
//            // Called when a message is no longer nearby.
//            @Override
//            public void onLost(Message message) {
//                Log.i(TAG, "Lost message: " + message);
//                CacheUtils.removeLostMessage(getApplicationContext(), message);
//                updateNotification(BackgroundSubscribeIntentService.this);
//            }
//        });
    }

    public static void updateNotification(Context context) {
        List<Message> messages = new ArrayList<>();
//        List<Message> messages = CacheUtils.getCachedMessage(context);
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//        Intent intent = new Intent(context, MainActivity.class);
//        intent.setAction(Intent.ACTION_MAIN);
//        intent.addCategory(Intent.CATEGORY_LAUNCHER);
//        PendingIntent pendingIntent =
//                PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//        Intent stopIntent = new Intent(context, MainActivity.class);
//        stopIntent.setAction(MainActivity.ACTION_STOP);
//        PendingIntent pendingStopIntent =
//                PendingIntent.getActivity(context, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        CharSequence title = getContentTitle(context, messages);
        CharSequence text = getContentText(messages);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_nearby_white_24dp)
                .setContentTitle(context.getString(R.string.nearby_running))
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text));
//                .setOngoing(true);
//                .setContentIntent(pendingIntent)
//                .addAction(0, context.getString(R.string.nearby_stop), pendingStopIntent);
        notificationManager.notify(MESSAGES_NOTIFICATION_ID, builder.build());
    }

    public static void cancelNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(MESSAGES_NOTIFICATION_ID);
    }

    private static String getContentTitle(Context context, List<Message> messages) {
        int size = messages.size();
        return context.getResources().getQuantityString(R.plurals.messages_found, size, size);
    }

    private static String getContentText(List<Message> messages) {
        List<String> tokens = new ArrayList<>(messages.size());
        for (Message message : messages) {
            tokens.add(new String(message.getContent()));
        }

        String newline = System.getProperty("line.separator");
        if (messages.size() < NUM_MESSAGES_IN_NOTIFICATION) {
            return TextUtils.join(newline, tokens);
        }
        return TextUtils.join(newline, tokens.subList(0, NUM_MESSAGES_IN_NOTIFICATION))
                + newline + "&#8230;";
    }
}