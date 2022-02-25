package com.puy.peach.service

import android.annotation.SuppressLint
import android.app.Service
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.puy.peach.configs.PeachConfig
import com.puy.peach.ext.*
import com.puy.peach.ext.getConfig
import com.puy.peach.ext.registerPeach
import com.puy.peach.ext.registerStopReceiver
import com.puy.peach.ext.setNotification
import com.puy.peach.utils.Constant

@SuppressLint("SpecifyJobSchedulerIdRange")
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class PeachJobService : JobService() {

    private lateinit var mJobScheduler: JobScheduler

    private lateinit var mPeachConfig: PeachConfig

    private var mJobId = 100

    /**
     * 停止标识符
     */
    private var mIsStop = false

    override fun onCreate() {
        super.onCreate()
        mPeachConfig = getConfig()
        registerJob()
        registerStopReceiver {
            mIsStop = true
            stopService()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getParcelableExtra<PeachConfig>(Constant.PEACH_CONFIG)?.let {
            mPeachConfig = it
        }
        setNotification(mPeachConfig.notificationConfig)
        registerPeach(mPeachConfig)
        return Service.START_STICKY
    }

    override fun onDestroy() {
        stopForeground(true)
        mJobScheduler.cancel(mJobId)
        saveJobId(-1)
        super.onDestroy()
    }

    override fun onStartJob(jobParameters: JobParameters): Boolean {
        log("onStartJob")
        if (!isPeachRunning && !mIsStop) {
            registerPeach(mPeachConfig)
        }
        return false
    }

    override fun onStopJob(jobParameters: JobParameters): Boolean {
        log("onStopJob")
        if (!isPeachRunning && !mIsStop) {
            registerPeach(mPeachConfig)
        }
        return false
    }

    /**
     * 开始Job
     */
    private fun registerJob() {
        mJobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        mJobId = getJobId()
        if (mJobId != -1) {
            mJobScheduler.cancel(mJobId)
        }
        mJobId = id
        saveJobId(mJobId)
        val builder = JobInfo.Builder(
            mJobId,
            ComponentName(packageName, PeachJobService::class.java.name)
        ).apply {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    setMinimumLatency(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS) //执行的最小延迟时间
                    setOverrideDeadline(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS)  //执行的最长延时时间
                    setMinimumLatency(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS)
                    setBackoffCriteria(
                        JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS,
                        JobInfo.BACKOFF_POLICY_LINEAR
                    )//线性重试方案
                } else {
                    setPeriodic(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS)
                    setRequiresDeviceIdle(true)
                }
                setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                setRequiresCharging(true) // 当插入充电器，执行该任务
                setPersisted(true)//手机重启
            } catch (e: Exception) {
            }
        }
        mJobScheduler.schedule(builder.build())
    }
}