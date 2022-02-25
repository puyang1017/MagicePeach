package com.puy.peach.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.puy.peach.ext.*
import com.puy.peach.ext.getConfig
import com.puy.peach.ext.isPeachRunning
import com.puy.peach.ext.log
import com.puy.peach.ext.register

/**
 *    author : puy
 *    date   : 2022/2/24 13:42
 *    desc   :
 */
class PeachWorker(val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    /**
     * 停止标识符
     */
    private var mIsStop = false

    init {
        context.registerStopReceiver {
            mIsStop = true
        }
    }

    override fun doWork(): Result {
        context.apply {
            val peachConfig = getConfig()
            log("${this@PeachWorker}-doWork")
            if (!isPeachRunning && !mIsStop && !isStopped) {
                register(peachConfig)
            }
        }
        return Result.success()
    }
}