package com.puy.peach.service

import android.app.Service
import android.content.*
import android.media.MediaPlayer
import android.os.IBinder
import com.puy.peach.Peach
import com.puy.peach.configs.IPeachInterface
import com.puy.peach.configs.PeachConfig
import com.puy.peach.ext.*
import com.puy.peach.ext.getConfig
import com.puy.peach.ext.log
import com.puy.peach.ext.sTimes
import com.puy.peach.ext.startRemoteService
import com.puy.peach.utils.Constant

/**
 * 本地服务
 */
class LocalService : Service(), IBinder.DeathRecipient {

    /**
     * 配置信息
     */
    private lateinit var mPeachConfig: PeachConfig

    /**
     * 音乐播放器
     */
    private var mMediaPlayer: MediaPlayer? = null

    /**
     * 广播
     */
    private var mServiceReceiver: ServiceReceiver? = null

    /**
     * Service是否在运行
     */
    private var mIsServiceRunning = false

    /**
     * 音乐是否在播放
     */
    private var mIsMusicRunning = false

    /**
     * 服务连接次数
     */
    private var mConnectionTimes = sTimes

    /**
     * 停止标识符
     */
    private var mIsStop = false

    /**
     * 是否已经绑定
     */
    private var mIsBind = false

    /**
     * 是否已经注册linkToDeath
     */
    private var mIsDeathBind = false

    private lateinit var mLocalBinder: LocalBinder

    private var mIInterface: IPeachInterface? = null

    private val mServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            log("onServiceDisconnected")
            if (!mIsStop) {
                mIsBind = startRemoteService(this, mPeachConfig)
            }
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            log("onServiceConnected")
            service?.let {
                mIInterface = IPeachInterface.Stub.asInterface(it)
                    ?.apply {
                        if (asBinder().isBinderAlive && asBinder().pingBinder()) {
                            try {
                                ++mConnectionTimes
                                wakeup(mPeachConfig)
                                connectionTimes(mConnectionTimes)
                                if (!mIsDeathBind) {
                                    mIsDeathBind = true
                                    asBinder().linkToDeath(this@LocalService, 0)
                                }
                            } catch (e: Exception) {
                                --mConnectionTimes
                            }
                        }
                    }
            }
        }
    }

    override fun binderDied() {
        log("binderDied")
        try {
            unlinkToDeath(mIInterface) {
                mIsDeathBind = false
                mIInterface = null
                if (!mIsStop) {
                    mIsBind = startRemoteService(mServiceConnection, mPeachConfig)
                }
            }
        } catch (e: Exception) {
        }
    }

    override fun onCreate() {
        super.onCreate()
        mPeachConfig = getConfig()
        registerStopReceiver {
            mIsStop = true
            sTimes = mConnectionTimes
            stopService()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getParcelableExtra<PeachConfig>(Constant.PEACH_CONFIG)?.let {
            mPeachConfig = it
        }
        setNotification(mPeachConfig.notificationConfig)
        mIsBind = startRemoteService(mServiceConnection, mPeachConfig)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        stopBind()
        stopService(Intent(this, RemoteService::class.java))
        onStop()
    }

    override fun onBind(intent: Intent?): IBinder? {
        mLocalBinder = LocalBinder()
        return mLocalBinder
    }

    inner class LocalBinder : IPeachInterface.Stub() {

        override fun wakeup(config: PeachConfig) {
            mPeachConfig = config
        }

        override fun connectionTimes(time: Int) {
            mConnectionTimes = time
            if (mConnectionTimes > 3 && mConnectionTimes % 2 == 0) {
                ++mConnectionTimes
            }
            sTimes = mConnectionTimes
            doWork((mConnectionTimes + 1) / 2)
        }
    }

    /**
     * 屏幕息屏亮屏与前后台切换广播
     */
    inner class ServiceReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.action?.apply {
                when (this) {
                    Intent.ACTION_SCREEN_OFF -> {
                        // 熄屏，打开1像素Activity
                        log("screen off")
                        openOnePix()
                        playMusic()
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        //亮屏，关闭1像素Activity
                        log("screen on")
                        closeOnePix()
                        if (!mPeachConfig.defaultConfig.backgroundMusicEnabled) {
                            pauseMusic()
                        }
                    }
                    Peach.PEACH_BACKGROUND -> {
                        log("background")
                        if (mPeachConfig.defaultConfig.backgroundMusicEnabled) {
                            playMusic()
                        }
                        onBackground(true)
                    }
                    Peach.PEACH_FOREGROUND -> {
                        log("foreground")
                        pauseMusic()
                        onBackground(false)
                    }
                }
            }
        }
    }

    /**
     * 解除相关绑定
     */
    private fun stopBind() {
        try {
            if (mIsDeathBind) {
                mIsDeathBind = false
                unlinkToDeath(mIInterface)
            }
            if (mIsBind) {
                unbindService(mServiceConnection)
                mIsBind = false
            }
        } catch (e: Exception) {
        }
    }

    /**
     * 处理外部事情
     *
     * @param times Int，启动次数
     */
    private fun doWork(times: Int) {
        if (!mIsServiceRunning) {
            mIsServiceRunning = true
            log("LocalService is run >>>> do work times = $times")
            registerMedia()
            registerBroadcastReceiver()
            sendBroadcast(
                Intent(Peach.PEACH_WORK).putExtra(Peach.PEACH_TIMES, times)
            )
            if (Constant.CALLBACKS.isNotEmpty()) {
                Constant.CALLBACKS.forEach {
                    if (mPeachConfig.defaultConfig.workOnMainThread) {
                        sMainHandler.post { it.doWork(times) }
                    } else {
                        it.doWork(times)
                    }
                }
            }
        }
    }

    /**
     * 停止回调
     */
    private fun onStop() {
        if (mIsServiceRunning) {
            mIsServiceRunning = false
            log("LocalService is stop!")
            unregisterReceiver()
            sendBroadcast(Intent(Peach.PEACH_STOP))
            pauseMusic()
            mMediaPlayer = null
            if (Constant.CALLBACKS.isNotEmpty()) {
                Constant.CALLBACKS.forEach {
                    it.onStop()
                }
            }
        }
    }

    /**
     * 打开一像素
     */
    private fun openOnePix() {
        if (mPeachConfig.defaultConfig.onePixEnabled) {
            sMainHandler.postDelayed({ startOnePixActivity() }, 1000)
        }
    }

    /**
     * 关闭一像素
     */
    private fun closeOnePix() {
        mPeachConfig.defaultConfig.apply {
            if (onePixEnabled) {
                backBackground()
                finishOnePix()
            }
        }
    }

    /**
     * 是否是在后台
     *
     * @param background Boolean
     */
    private fun onBackground(background: Boolean) {
        if (Constant.BACKGROUND_CALLBACKS.isNotEmpty()) {
            Constant.BACKGROUND_CALLBACKS.forEach {
                it.onBackground(background)
            }
        }
    }

    /**
     * 注册息屏亮屏、前后台切换广播监听
     */
    private fun registerBroadcastReceiver() {
        if (mServiceReceiver == null) {
            mServiceReceiver = ServiceReceiver()
        }
        mServiceReceiver?.let {
            registerReceiver(it, IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Peach.PEACH_BACKGROUND)
                addAction(Peach.PEACH_FOREGROUND)
            })
        }
    }

    /**
     * 注销息屏亮屏、前后台切换广播监听
     */
    private fun unregisterReceiver() {
        mServiceReceiver?.let {
            unregisterReceiver(it)
            mServiceReceiver = null
        }
    }

    /**
     * 注册音乐播放器
     */
    private fun registerMedia() {
        if (mPeachConfig.defaultConfig.musicEnabled) {
            if (mMediaPlayer == null) {
                mMediaPlayer = MediaPlayer.create(this, mPeachConfig.defaultConfig.musicId)
            }
            mMediaPlayer?.apply {
                if (!mPeachConfig.defaultConfig.debug) {
                    setVolume(0f, 0f)
                }
                setOnCompletionListener {
                    sMainHandler.postDelayed(
                        {
                            mIsMusicRunning = false
                            playMusic()
                        },
                        mPeachConfig.defaultConfig.repeatInterval
                    )
                }
                setOnErrorListener { _, _, _ -> false }
                if (!isScreenOn) {
                    playMusic()
                }
            }
        }
    }

    /**
     * 播放音乐
     */
    private fun playMusic() {
        mMediaPlayer?.apply {
            if (mPeachConfig.defaultConfig.musicEnabled) {
                if (!mIsMusicRunning) {
                    start()
                    mIsMusicRunning = true
                    log("music is playing")
                }
            }
        }
    }

    /**
     * 暂停音乐
     */
    private fun pauseMusic() {
        mMediaPlayer?.apply {
            if (mIsMusicRunning) {
                pause()
                mIsMusicRunning = false
                log("music is pause")
            }
        }
    }
}