package com.puy.peach.service

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Process
import com.puy.peach.configs.IPeachInterface
import com.puy.peach.configs.PeachConfig
import com.puy.peach.ext.*
import com.puy.peach.ext.log
import com.puy.peach.ext.sTimes
import com.puy.peach.ext.setNotification
import com.puy.peach.ext.unlinkToDeath
import com.puy.peach.utils.Constant
import kotlin.system.exitProcess

/**
 * 远程服务
 *
 * @author geyifeng
 * @date 2019-08-28 17:05
 */
class RemoteService : Service(), IBinder.DeathRecipient {

    /**
     * 配置信息
     */
    private lateinit var mPeachConfig: PeachConfig

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

    private lateinit var remoteBinder: RemoteBinder

    private var mIInterface: IPeachInterface? = null

    private val mServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            log("onServiceDisconnected")
            if (!mIsStop) {
                mIsBind = startLocalService(this, mPeachConfig)
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
                                    asBinder().linkToDeath(this@RemoteService, 0)
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
                    mIsBind = startLocalService(mServiceConnection, mPeachConfig)
                }
            }
        } catch (e: Exception) {
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            log("handleNotification")
            mPeachConfig = getConfig()
            setNotification(mPeachConfig.notificationConfig)
        } catch (e: Exception) {
        }
        registerStopReceiver {
            mIsStop = true
            sTimes = mConnectionTimes
            stopService()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getParcelableExtra<PeachConfig>(Constant.PEACH_CONFIG)?.let {
            sPeachConfig = it
            mPeachConfig = it
        }
        setNotification(mPeachConfig.notificationConfig)
        mIsBind = startLocalService(mServiceConnection, mPeachConfig, false)
        log("RemoteService is running")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        stopBind()
        log("RemoteService has stopped")
        Process.killProcess(Process.myPid())
        exitProcess(10)
    }

    override fun onBind(intent: Intent?): IBinder? {
        remoteBinder = RemoteBinder()
        return remoteBinder
    }

    inner class RemoteBinder : IPeachInterface.Stub() {

        override fun wakeup(config: PeachConfig) {
            mPeachConfig = config
            setNotification(mPeachConfig.notificationConfig)
        }

        override fun connectionTimes(time: Int) {
            mConnectionTimes = time
            if (mConnectionTimes > 4 && mConnectionTimes % 2 == 1) {
                ++mConnectionTimes
            }
            sTimes = mConnectionTimes
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
}