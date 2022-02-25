package com.puy.peach

import android.app.PendingIntent
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.puy.peach.callback.PeachBackgroundCallback
import com.puy.peach.callback.PeachCallback
import com.puy.peach.configs.DefaultConfig
import com.puy.peach.configs.NotificationConfig
import com.puy.peach.configs.PeachConfig
import com.puy.peach.ext.*
import com.puy.peach.utils.Constant

/**
 *    author : puy
 *    date   : 2022/2/24 11:05
 *    desc   : Peach 保活方案
 */
class Peach private constructor() {
    /**
     * 配置信息
     */
    private var mPeachConfig = PeachConfig()

    /**
     * 通知栏信息
     */
    private var mNotificationConfig = NotificationConfig()

    /**
     * 默认配置信息
     */
    private val mDefaultConfig = DefaultConfig()

    /**
     * 是否使用上一次保存的配置信息
     */
    private var mUsePreviousConfig = false

    companion object {
        /**
         * 运行时回调广播ACTION
         */
        @JvmField
        val PEACH_WORK = "work".fieldById

        /**
         * 停止时回调广播ACTION
         */
        @JvmField
        val PEACH_STOP = "stop".fieldById

        /**
         * 后台回调广播ACTION
         */
        @JvmField
        val PEACH_BACKGROUND = "background".fieldById

        /**
         * 前台后调广播ACTION
         */
        @JvmField
        val PEACH_FOREGROUND = "foreground".fieldById

        /**
         * key，通过广播形式获取启动次数
         */
        const val PEACH_TIMES = "times"

        @JvmStatic
        val instance by lazy { Peach() }
    }

    /**
     * 主Handler
     */
    internal val sMainHandler by lazy {
        Handler(Looper.getMainLooper())
    }


    /**
     * 必须调用，建议在Application里初始化，使用Kotlin扩展函数不需要调用此方法
     *
     * @param context Context
     */
    fun register(context: Context) {
        val peachConfig = PeachConfig(
            mNotificationConfig,
            mDefaultConfig
        )
        mPeachConfig = if (mUsePreviousConfig) {
            context.getPreviousConfig() ?: peachConfig
        } else peachConfig
        context.register(peachConfig)
    }

    /**
     * 设置PendingIntent，用来处理通知栏点击事件，非必传
     *
     * @param pendingIntent PendingIntent
     * @return Peach
     */
    fun setPendingIntent(pendingIntent: PendingIntent) = apply {
        mNotificationConfig.pendingIntent = pendingIntent
    }

    /**
     * 设置通知栏配置
     *
     * @param notificationConfig NotificationConfig
     * @return Peach
     */
    fun setNotificationConfig(notificationConfig: NotificationConfig) = apply {
        mNotificationConfig = notificationConfig
    }

    /**
     * 设置自定义音乐，默认是无声音乐，非必传
     *
     * @param musicId Int
     * @return Peach
     */
    fun setMusicId(musicId: Int) = apply {
        mDefaultConfig.musicId = musicId
    }


    /**
     * 设置debug模式
     *
     * @param musicId Int
     * @return Peach
     */
    fun isDebug(debug: Boolean) = apply {
        mDefaultConfig.debug = debug
    }

    /**
     * 后台是否可以播放音乐，默认不可以后台播放音乐，非必传
     *
     * @param enabled Boolean
     * @return Peach
     */
    fun setBackgroundMusicEnabled(enabled: Boolean) = apply {
        mDefaultConfig.backgroundMusicEnabled = enabled
    }

    /**
     * 增加回调，用于处理一些额外的工作，非必传
     *
     * @param peachCallback PeachCallback
     * @return Peach
     */
    fun addCallback(peachCallback: PeachCallback) = apply {
        Constant.CALLBACKS.add(peachCallback)
    }

    /**
     * 前后台切换回调，用于处理app前后台切换，非必传
     *
     * @param block Function1<Boolean, Unit>
     * @return Peach
     */
    fun addBackgroundCallback(block: (Boolean) -> Unit) = apply {
        Constant.BACKGROUND_CALLBACKS.add(object : PeachBackgroundCallback {
            override fun onBackground(background: Boolean) {
                block(background)
            }
        })
    }

    /**
     * 注销，并不会立马停止，而是在1s之后停止，非必须调用，比如可以在app完全退出的时候可以调用，根据你的需求调用
     *
     * @param context Context
     */
    fun unregister(context: Context) = context.unregister()


    /**
     * 更新通知栏
     *
     * @param context Context
     */
    fun updateNotification(context: Context) {
        mPeachConfig.notificationConfig = mNotificationConfig
        context.updateNotification(mPeachConfig)
    }

}