package com.puy.peach.callback

/**
 * 前后台切换回调
 */
interface PeachBackgroundCallback {
    /**
     * 前后台切换回调
     * @param background Boolean
     */
    fun onBackground(background: Boolean)
}