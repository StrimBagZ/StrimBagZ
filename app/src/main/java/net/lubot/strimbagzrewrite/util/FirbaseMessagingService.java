package net.lubot.strimbagzrewrite.util;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import net.lubot.strimbagzrewrite.R;
import net.lubot.strimbagzrewrite.ui.activity.MainActivity;

import java.util.Map;

public class FirbaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FirebaseMsgService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
        }

        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, remoteMessage.getNotification().getTitle());
            Log.d(TAG, remoteMessage.getNotification().getBody());
        }

        if (remoteMessage.getNotification() != null || remoteMessage.getData() != null) {
            sendNotification(remoteMessage);
        }
    }

    private void sendNotification(RemoteMessage notification) {
        Map<String, String> data = notification.getData();
        int color = getResources().getColor(R.color.colorAccent);
        Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentTitle(data.get("title"))
                .setContentText(data.get("body"))
                .setAutoCancel(true)
                .setColor(color)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(data.get("body")))
                .setSound(defaultSound)
                .setContentIntent(pendingIntent);

        if (notification.getData().size() > 0) {
            String type = data.get("type");
            switch (type) {
                case "stream":
                    addWatchButton(builder, data);
                    break;
                default:
                    builder.setSmallIcon(R.drawable.ic_notification);
                    break;
            }
        }
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(1234, builder.build());
    }

    private void addWatchButton(NotificationCompat.Builder builder, Map<String, String> data) {
        Intent stream = new Intent(this, MainActivity.class);
        stream.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        stream.putExtra("startStream", true);
        stream.putExtra("channel", data.get("channel"));
        PendingIntent pendingStream = PendingIntent
                .getActivity(this, 42541, stream, PendingIntent.FLAG_ONE_SHOT);
        builder.setSmallIcon(R.drawable.ic_marathon_notification);
        builder.addAction(R.drawable.ic_channel,
                getString(R.string.notification_watch), pendingStream);
    }
}
