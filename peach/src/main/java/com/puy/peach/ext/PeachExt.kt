package com.puy.peach.ext

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.os.IInterface
import android.os.Process
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.puy.peach.receiver.StopReceiver
import com.puy.peach.Peach
import com.puy.peach.callback.AppBackgroundCallback
import com.puy.peach.configs.PeachConfig
import com.puy.peach.gson.Gson
import com.puy.peach.pix.OnePixActivity
import com.puy.peach.service.LocalService
import com.puy.peach.service.PeachJobService
import com.puy.peach.service.RemoteService
import com.puy.peach.utils.Constant
import com.puy.peach.worker.PeachWorker
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

/**
 *    author : puy
 *    date   : 2022/2/24 11:09
 *    desc   : 扩展方法
 */
fun Context.PeachInit(block: Peach.() -> Unit) =
    Peach.instance.apply { block() }.register(this)

/**
 * 注销
 *
 * @receiver Context
 */
fun Context.PeachStart() = Peach.instance.register(this)

/**
 * 注销
 *
 * @receiver Context
 */
fun Context.PeachStop() = Peach.instance.unregister(this)

/**
 * 更新通知栏
 *
 * @receiver Context
 * @param block 方法 {setNotificationConfig()}
 */
fun Context.PeachUpdateNotification(block: Peach.() -> Unit) =
    Peach.instance.apply { block() }.updateNotification(this)

/**
 * 注册Cactus服务
 *
 * @receiver Context
 * @param peachConfig PeachConfig
 */
internal fun Context.register(peachConfig: PeachConfig) {
    if (isMain) {
        try {
            if (sRegistered && isPeachRunning) {
                log("Peach is running，Please stop Peach before registering!!")
            } else {
                sRegistered = true
                handleRestartIntent(peachConfig)
                saveConfig(peachConfig)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    registerJobPeach(peachConfig)
                } else {
                    registerPeach(peachConfig)
                }
                if (this is Application && mAppBackgroundCallback == null) {
                    mAppBackgroundCallback = AppBackgroundCallback(this)
                    registerActivityLifecycleCallbacks(mAppBackgroundCallback)
                }
                mAppBackgroundCallback?.useCallback(true)
            }
        } catch (e: Exception) {
            log("Unable to open Peach service!!")
        }
    }
}

/**
 * 注销Peach
 *
 * @receiver Context
 */
internal fun Context.unregister() {
    try {
        if (isPeachRunning && sRegistered) {
            sRegistered = false
            sPeachConfig?.apply {
                unregisterWorker()
            }
            sendBroadcast(Intent("${Constant.PEACH_FLAG_STOP}.$packageName"))
            sMainHandler.postDelayed({
                mAppBackgroundCallback?.also {
                    it.useCallback(false)
                    if (this is Application) {
                        unregisterActivityLifecycleCallbacks(it)
                        mAppBackgroundCallback = null
                    }
                }
            }, 1000)
        } else {
            log("Peach is not running，Please make sure Peach is running!!")
        }
    } catch (e: Exception) {
    }
}

/**
 * 注册JobService
 *
 * @receiver Context
 * @param peachConfig PeachConfig
 */
internal fun Context.registerJobPeach(peachConfig: PeachConfig) {
    val intent = Intent(this, PeachJobService::class.java)
    intent.putExtra(Constant.PEACH_CONFIG, peachConfig)
    startInternService(intent)
}

/**
 * 注册Peach服务
 *
 * @receiver Context
 * @param peachConfig PeachConfig
 */
internal fun Context.registerPeach(peachConfig: PeachConfig) {
    val intent = Intent(this, LocalService::class.java)
    intent.putExtra(Constant.PEACH_CONFIG, peachConfig)
    startInternService(intent)
    sMainHandler.postDelayed({
        registerWorker()
    }, 5000)
}

/**
 * 开启WorkManager
 *
 * @receiver Context
 */
internal fun Context.registerWorker() {
    if (isPeachRunning && sRegistered) {
        try {
            val workRequest =
                PeriodicWorkRequest.Builder(PeachWorker::class.java, 15, TimeUnit.SECONDS)
                    .build()
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                PeachWorker::class.java.name,
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
        } catch (e: Exception) {
            unregisterWorker()
            log("WorkManager registration failed")
        }
    }
}

/**
 * 取消WorkManager
 *
 * @receiver Context
 * @return Operation
 */
internal fun Context.unregisterWorker() =
    WorkManager.getInstance(this).cancelUniqueWork(PeachWorker::class.java.name)

/**
 * 前后台切换监听
 */
@SuppressLint("StaticFieldLeak")
private var mAppBackgroundCallback: AppBackgroundCallback? = null

/**
 * 获得带id值的字段值
 */
internal val String.fieldById get() = "${Constant.PEACH_PACKAGE}.${this}.$id"


/**
 * 获取id
 */
internal val id get() = if (Process.myUid() <= 0) Process.myPid() else Process.myUid()

/**
 * 配置信息
 */
internal var sPeachConfig: PeachConfig? = null

/**
 * 启动次数
 */
internal var sTimes = 0


/**
 * 用来表示是前台还是后台
 */
private var mIsForeground = false

/**
 * 是否注册过
 */
private var sRegistered = false

/**
 * 全局log
 *
 * @param msg String
 */
internal fun log(msg: String) {
    sPeachConfig?.defaultConfig?.apply {
        if (debug) {
            Log.d(Constant.PEACH_TAG, msg)
        }
    } ?: Log.v(Constant.PEACH_TAG, msg)
}

/**
 * 用以保存一像素Activity
 */
private var mWeakReference: WeakReference<Activity>? = null

/**
 * 销毁一像素
 */
internal fun finishOnePix() {
    mWeakReference?.apply {
        get()?.apply {
            finish()
        }
        mWeakReference = null
    }
}

/**
 * 保存一像素，方便销毁
 *
 * @receiver OnePixActivity
 */
internal fun OnePixActivity.setOnePix() {
    if (mWeakReference == null) {
        mWeakReference = WeakReference(this)
    }
}


/**
 * 开启一像素界面
 *
 * @receiver Context
 */
@SuppressLint("UnspecifiedImmutableFlag")
internal fun Context.startOnePixActivity() {
    if (!isScreenOn && Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        mIsForeground = isForeground
        log("isForeground:$mIsForeground")
        val onePixIntent = Intent(this, OnePixActivity::class.java)
        onePixIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        onePixIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent = PendingIntent.getActivity(this, 0, onePixIntent, 0)
        try {
            pendingIntent.send()
        } catch (e: Exception) {
        }
    }
}

/**
 * 获取配置信息
 *
 * @receiver Context
 * @return PeachConfig
 */
internal fun Context.getConfig() = sPeachConfig ?: getPreviousConfig() ?: PeachConfig()

/**
 * 保存配置信息
 *
 * @receiver Context
 * @param peachConfig PeachConfig
 */
internal fun Context.saveConfig(peachConfig: PeachConfig) {
    sPeachConfig = peachConfig
    val serviceId = getServiceId()
    if (serviceId > 0) {
        peachConfig.notificationConfig.serviceId = serviceId
    }
    getSharedPreferences(Constant.PEACH_TAG, Context.MODE_PRIVATE).edit().apply {
        putString(Constant.PEACH_CONFIG, Gson().toJson(peachConfig))
        if (serviceId <= 0) {
            putInt(Constant.PEACH_SERVICE_ID, peachConfig.notificationConfig.serviceId)
        }
    }.apply()
}

/**
 * 获得serviceId
 *
 * @receiver Context
 * @return Int
 */
private fun Context.getServiceId() = getSharedPreferences(
    Constant.PEACH_TAG,
    Context.MODE_PRIVATE
).getInt(Constant.PEACH_SERVICE_ID, -1)


/**
 * 获取Sp保存的配置信息
 *
 * @receiver Context
 * @return PeachConfig?
 */
internal fun Context.getPreviousConfig() = getSharedPreferences(
    Constant.PEACH_TAG,
    Context.MODE_PRIVATE
).getString(Constant.PEACH_CONFIG, null)?.run {
    Gson().fromJson(this, PeachConfig::class.java)
}

/**
 * 保存JobId
 *
 * @receiver Context
 * @param jobId Int
 */
@SuppressLint("CommitPrefEdits")
internal fun Context.saveJobId(jobId: Int) =
    getSharedPreferences(
        Constant.PEACH_TAG,
        Context.MODE_PRIVATE
    ).edit().putInt(Constant.PEACH_JOB_ID, jobId).apply()

/**
 * 获得JobId
 *
 * @receiver Context
 * @return Int
 */
internal fun Context.getJobId() =
    getSharedPreferences(
        Constant.PEACH_TAG,
        Context.MODE_PRIVATE
    ).getInt(Constant.PEACH_JOB_ID, -1)

/**
 * 开启远程服务
 *
 * @receiver Service
 * @param serviceConnection ServiceConnection
 * @param peachConfig PeachConfig
 */
internal fun Service.startRemoteService(
    serviceConnection: ServiceConnection,
    peachConfig: PeachConfig
) = startAndBindService(RemoteService::class.java, serviceConnection, peachConfig)

/**
 * 开启本地服务
 *
 * @receiver Service
 * @param serviceConnection ServiceConnection
 * @param peachConfig PeachConfig
 * @param isStart Boolean
 */
internal fun Service.startLocalService(
    serviceConnection: ServiceConnection,
    peachConfig: PeachConfig,
    isStart: Boolean = true
) = startAndBindService(LocalService::class.java, serviceConnection, peachConfig, isStart)

/**
 * 开启并绑定服务
 *
 * @receiver Service
 * @param cls Class<*>
 * @param serviceConnection ServiceConnection
 * @param peachConfig PeachConfig
 * @param isStart Boolean
 * @return Boolean
 */
private fun Service.startAndBindService(
    cls: Class<*>,
    serviceConnection: ServiceConnection,
    peachConfig: PeachConfig,
    isStart: Boolean = true
) = run {
    val intent = Intent(this, cls)
    intent.putExtra(Constant.PEACH_CONFIG, peachConfig)
    if (isStart) {
        startInternService(intent)
    }
    val bindService = bindService(intent, serviceConnection, Context.BIND_IMPORTANT)
    bindService
}

/**
 * 开启Service
 *
 * @receiver Context
 * @param intent Intent
 */
internal fun Context.startInternService(intent: Intent) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(intent)
    } else {
        startService(intent)
    }
}

/**
 * 停止服务
 *
 * @receiver Service
 */
internal fun Service.stopService() {
    sMainHandler.postDelayed({
        try {
            this.stopSelf()
        } catch (e: Exception) {
        }
    }, 1000)
}

/**
 * 解除DeathRecipient绑定
 *
 * @receiver IBinder.DeathRecipient
 * @param iInterface IInterface?
 * @param block Function0<Unit>?
 */
internal fun IBinder.DeathRecipient.unlinkToDeath(
    iInterface: IInterface? = null,
    block: (() -> Unit)? = null
) {
    iInterface?.asBinder()?.unlinkToDeath(this, 0)
    block?.invoke()
}

/**
 * kotlin里使用注册Receiver
 *
 * @receiver Context
 * @param block Function0<Unit>
 */
internal fun Context.registerStopReceiver(block: () -> Unit) =
    StopReceiver.newInstance(this).register(block)


/**
 * 退到后台
 *
 * @receiver Context
 */
internal fun backBackground() {
    mWeakReference?.apply {
        get()?.apply {
            if (!mIsForeground && isScreenOn) {
                val home = Intent(Intent.ACTION_MAIN)
                home.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                home.addCategory(Intent.CATEGORY_HOME)
                startActivity(home)
            }
        }
    }
}


/**
 * 是否在运行中
 */
internal val Context.isPeachRunning
    get() = run {
        isServiceRunning(LocalService::class.java.name) and isRunningTaskExist(Constant.PEACH_EMOTE_SERVICE)
    }


/**
 * 设置重启Intent
 *
 * @receiver Context
 * @param peachConfig PeachConfig
 */
private fun Context.handleRestartIntent(peachConfig: PeachConfig) {
    peachConfig.defaultConfig.apply {
        restartIntent = packageManager.getLaunchIntentForPackage(packageName)
        restartIntent?.apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}

/**
 * 更新通知栏
 * @receiver Context
 * @param peachConfig PeachConfig
 */
internal fun Context.updateNotification(peachConfig: PeachConfig) {
    if (!getConfig().notificationConfig.canUpdate(peachConfig.notificationConfig)) {
        return
    }
    saveConfig(peachConfig)
    val managerCompat = NotificationManagerCompat.from(this)
    val notification = getNotification(peachConfig.notificationConfig)
    managerCompat.notify(peachConfig.notificationConfig.serviceId, notification)
}


