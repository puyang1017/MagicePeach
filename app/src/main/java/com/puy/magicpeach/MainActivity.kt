package com.puy.magicpeach

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.puy.peach.configs.NotificationConfig
import com.puy.peach.ext.PeachStart
import com.puy.peach.ext.PeachStop
import com.puy.peach.ext.PeachUpdateNotification
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startPeach.setOnClickListener {
            PeachStart()
        }
        stopPeach.setOnClickListener {
            PeachStop()
        }
        updateNotification.setOnClickListener {
            PeachUpdateNotification {
                val notificationConfig = NotificationConfig()
                //设置通知栏图标
                notificationConfig.largeIcon = R.drawable.ic_launcher_foreground
                notificationConfig.smallIcon = R.drawable.ic_launcher_foreground
                setNotificationConfig(notificationConfig)
            }
        }
    }
}