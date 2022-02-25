package com.puy.peach.callback

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import com.puy.peach.Peach
import com.puy.peach.ext.sMainHandler
import com.puy.peach.pix.OnePixActivity
import java.lang.ref.WeakReference

/**
 *
 * 处理前后台切换
 */
class AppBackgroundCallback @JvmOverloads constructor(
    private var context: Context? = null,
    private var block: ((Boolean) -> Unit)? = null
) :
    Application.ActivityLifecycleCallbacks {

    private var mContext: WeakReference<Context>? = null

    /**
     * 前台Activity数量
     */
    private var mFrontActivityCount = 0
    /**
     * 当Activity数量大于0的时候，标识是否已经发出前后台广播
     */
    private var mIsSend = false
    /**
     * 是否是第一次发送前后台广播
     */
    private var mIsFirst = true
    /**
     * 是否使用监听回调
     */
    private var mUseCallback = true

    companion object {
        private const val FIRST_TIME = 1000L
    }

    init {
        sMainHandler.postDelayed({
            if (mFrontActivityCount == 0) {
                post()
            }
        }, FIRST_TIME)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity !is OnePixActivity) {
            mContext = WeakReference(activity)
        }
    }

    override fun onActivityStarted(activity: Activity) {
        if (activity !is OnePixActivity) {
            mFrontActivityCount++
            post()
        }
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
        if (activity !is OnePixActivity) {
            mFrontActivityCount--
            post()
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    internal fun useCallback(useCallback: Boolean) {
        mUseCallback = useCallback
    }

    /**
     * 处理广播
     */
    private fun post() {
        (mContext?.get() ?: context)?.apply {
            if (mUseCallback) {
                if (mFrontActivityCount == 0) {
                    mIsSend = false
                    sMainHandler.postDelayed {
                        sendBroadcast(Intent().setAction(Peach.PEACH_BACKGROUND))
                        block?.let { it(true) }
                    }
                } else {
                    if (!mIsSend) {
                        mIsSend = true
                        sMainHandler.postDelayed {
                            sendBroadcast(Intent().setAction(Peach.PEACH_FOREGROUND))
                            block?.let { it(false) }
                        }
                    }
                }
            }
        }
    }

    private inline fun Handler.postDelayed(crossinline block: () -> Unit) {
        if (mIsFirst) {
            postDelayed(
                {
                    block()
                    mIsFirst = false
                },
                FIRST_TIME
            )
        } else {
            block()
        }
    }
}