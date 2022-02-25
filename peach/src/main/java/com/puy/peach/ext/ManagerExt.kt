package com.puy.peach.ext

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.Process

/**
 *    author : puy
 *    date   : 2022/2/24 11:15
 *    desc   : PowerManager和ActivityManager
 */

/**
 * 屏幕是否亮屏
 */
internal val Context.isScreenOn
    get() = run {
        try {
            val pm = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isScreenOn
        } catch (e: Exception) {
            false
        }
    }

/**
 * 获得主进程的pid
 */
internal val Context.mainPid
    get() = run {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        var pid = Process.myPid()
        activityManager.runningAppProcesses?.forEach {
            if (it.processName == packageName) {
                pid = it.pid
                return@forEach
            }
        }
        pid
    }

/**
 * 是否主进程
 */
internal val Context.isMain
    get() = run {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.runningAppProcesses.forEach {
            if (it.pid == Process.myPid() && it.processName == packageName) {
                return@run true
            }
        }
        false
    }

/**
 * 是否在前台
 */
internal val Context.isForeground
    @SuppressLint("NewApi")
    get() = run {
        var foreground = false
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getRunningTasks(1)?.apply {
            if (isNotEmpty()) {
                this[0].topActivity?.let {
                    foreground = (it.packageName == packageName)
                }
            }
        }
        foreground
    }

/**
 * 主Handler
 */
internal val sMainHandler by lazy {
    Handler(Looper.getMainLooper())
}

/**
 * 判断服务是否在运行
 *
 * @receiver Context
 * @param className String
 * @return Boolean
 */
fun Context.isServiceRunning(className: String): Boolean {
    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    activityManager.getRunningServices(Integer.MAX_VALUE)?.also {
        val l = it.iterator()
        while (l.hasNext()) {
            val si = l.next()
            if (className == si.service.className) {
                return true
            }
        }
    }
    return false
}

/**
 * 判断任务是否在运行
 *
 * @receiver Context
 * @param processName String
 * @return Boolean
 */
fun Context.isRunningTaskExist(processName: String): Boolean {
    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    activityManager.runningAppProcesses?.forEach {
        if (it.processName == "$packageName:$processName") {
            return true
        }
    }
    return false
}