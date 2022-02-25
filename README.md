# MagicePeach
android 保护策略（jobservice、workmanger、双进程、锁屏后台音乐像素）



# Peach使用文档
### 1.导入aar
```kotlin
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}

dependencies {
	        implementation 'com.github.puyang1017:MagicePeach:1.0.0'
	}
```
### 2.在Application内初试化sdk
```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        //可选，设置通知栏点击事件 建议设置
        val pendingIntent =
            PendingIntent.getActivity(this, 0, Intent().apply {
                setClass(this@App, MainActivity::class.java)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }, if(Build.VERSION.SDK_INT >= 30) PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT)
        //初始化后默认直接启动
        PeachInit {
            val notificationConfig = NotificationConfig()
            //设置通知栏图标
            //notificationConfig.largeIcon = R.drawable.ic_launcher_foreground
            //notificationConfig.smallIcon = R.drawable.ic_launcher_foreground
            //设置通知栏名称 通知栏内容不要超过1mb，内部是走的intent传递
            notificationConfig.title = getString(R.string.app_name)
            //设置通知栏内容
            notificationConfig.content = getString(R.string.app_name) + " is running"
            //可选，设置通知栏配置
            setNotificationConfig(notificationConfig)
            //可选，设置通知栏点击事件
            setPendingIntent(pendingIntent)
            //可选，设置音乐  默认配置无声音乐
            //setMusicId(R.raw.main)
            //可选，是否是debug模式  debug模式会输出日志
            isDebug(true)
            //可选，运行时回调
            addCallback(object : PeachCallback {
                override fun doWork(times: Int) {
                }

                override fun onStop() {
                }
            })
            //可选，切后台切换回调
            addBackgroundCallback {
                Toast.makeText(this@App, if (it) "退到后台啦" else "跑到前台啦", Toast.LENGTH_SHORT).show()
            }
        }
    }
}




class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startPeach.setOnClickListener {
            //启动Peach
            PeachStart()
        }
        stopPeach.setOnClickListener {
            //停止Peach
            PeachStop()
        }
         updateNotification.setOnClickListener {
            //更新通知栏 
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
```