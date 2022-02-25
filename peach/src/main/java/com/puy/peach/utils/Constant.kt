package com.puy.peach.utils

import com.puy.peach.callback.PeachBackgroundCallback
import com.puy.peach.callback.PeachCallback


/**
 *    author : puy
 *    date   : 2022/2/24 11:07
 *    desc   : 常量
 */
internal object Constant {
    /**
     * 包名
     */
    internal const val PEACH_TAG = "peach_log"

    /**
     * 包名
     */
    internal const val PEACH_PACKAGE = "com.puy.peach"

    /**
     * 停止标识符
     */
    internal const val PEACH_FLAG_STOP = "$PEACH_PACKAGE.flag.stop"

    /**
     * 进程名字
     */
    internal const val PEACH_EMOTE_SERVICE = "peachRemoteService"

    /**
     * 配置信息
     */
    internal const val PEACH_CONFIG = "peachConfig"

    /**
     * 服务ID key
     */
    internal const val PEACH_SERVICE_ID = "serviceId"
    /**
     * 回调集合
     */
    internal val CALLBACKS = arrayListOf<PeachCallback>()
    /**
     * 前后台回调集合
     */
    internal val BACKGROUND_CALLBACKS = arrayListOf<PeachBackgroundCallback>()
    /**
     * JobID key
     */
    internal const val PEACH_JOB_ID = "jobId"
}