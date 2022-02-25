package com.puy.peach.ext

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.puy.peach.R
import com.puy.peach.configs.NotificationConfig

/**
 * @author geyifeng
 * @date 2019-11-16 18:01
 */

/**
 * 小图标
 */
private val NotificationConfig.handleSmallIcon
    get() = if (hideNotification && Build.VERSION.SDK_INT != Build.VERSION_CODES.N_MR1) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !hideNotificationAfterO) {
            smallIcon
        } else {
            R.drawable.icon_peach_trans
        }
    } else {
        smallIcon
    }

/**
 * 设置通知栏信息
 *
 * @receiver Service
 * @param notificationConfig NotificationConfig
 */
internal fun Service.setNotification(
    notificationConfig: NotificationConfig,
) {
    val managerCompat = NotificationManagerCompat.from(this)
    val notification = getNotification(notificationConfig)
    notificationConfig.apply {
        //更新Notification
        managerCompat.notify(serviceId, notification)
        //设置前台服务Notification
        startForeground(serviceId, notification)
    }
}

/**
 * 获得Notification
 *
 * @param notificationConfig NotificationConfig
 * @return Notification
 */
internal fun Context.getNotification(notificationConfig: NotificationConfig): Notification =
    notificationConfig.run {
        val managerCompat = NotificationManagerCompat.from(this@getNotification)
        //构建Notification
        val notification =
            notification ?: NotificationCompat.Builder(this@getNotification, channelId)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(handleSmallIcon)
                .setLargeIcon(
                    largeIconBitmap ?: if (largeIcon == 0) {
                        null
                    } else {
                        BitmapFactory.decodeResource(
                            resources,
                            largeIcon
                        )
                    }
                )
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .apply {
                    remoteViews?.also {
                        setContent(it)
                    }
                    bigRemoteViews?.also {
                        setCustomBigContentView(it)
                    }
                }
                .build()
        //设置渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            managerCompat.getNotificationChannel(notification.channelId) == null
        ) {
            if (notificationChannel != null && notificationChannel is NotificationChannel) {
                (notificationChannel as NotificationChannel).apply {
                    if (id != notification.channelId) {
                        throw Exception(
                            "保证渠道相同(The id of the NotificationChannel " +
                                    "is different from the channel of the Notification.)"
                        )
                    }
                }
            } else {
                notificationChannel = NotificationChannel(
                    notification.channelId,
                    notificationConfig.channelName,
                    NotificationManager.IMPORTANCE_NONE
                )
            }
            managerCompat.createNotificationChannel(notificationChannel as NotificationChannel)
        }
        notification
    }