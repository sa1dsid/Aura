package com.aura.core.push

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.aura.MainActivity
import com.aura.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val manager = NotificationManagerCompat.from(context)

    private val nextId = AtomicInteger(FIRST_NOTIFICATION_ID)

    fun createChannel() {
        val channel = NotificationChannelCompat
            .Builder(channelId(), NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(context.getString(R.string.push_channel_name))
            .setDescription(context.getString(R.string.push_channel_description))
            .build()
        manager.createNotificationChannel(channel)
    }

    fun show(title: String?, body: String?, link: String? = null) {
        if (title.isNullOrBlank() && body.isNullOrBlank()) return
        if (!isAllowed()) return

        createChannel()

        val notification = NotificationCompat.Builder(context, channelId())
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.push_accent))
            .setContentTitle(
                title?.takeIf(String::isNotBlank) ?: context.getString(R.string.app_name)
            )
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body.orEmpty()))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(link))
            .build()

        manager.notify(nextId.getAndIncrement(), notification)
    }

    private fun channelId(): String = context.getString(R.string.push_channel_id)

    private fun isAllowed(): Boolean {
        if (!manager.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun contentIntent(link: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .apply { link?.takeIf(String::isNotBlank)?.let { data = Uri.parse(it) } }

        return PendingIntent.getActivity(
            context,
            nextId.get(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val FIRST_NOTIFICATION_ID = 1001
    }
}
