package com.puy.peach.configs

import android.os.Parcel
import android.os.Parcelable

/**
 *    author : puy
 *    date   : 2022/2/24 11:15
 *    desc   : 用户配置的信息
 */
data class PeachConfig(
    /**
     * 通知栏信息
     */
    var notificationConfig: NotificationConfig = NotificationConfig(),
    /**
     * 默认配置信息
     */
    val defaultConfig: DefaultConfig = DefaultConfig()
) : Parcelable {
    constructor(source: Parcel) : this(
        source.readParcelable<NotificationConfig>(NotificationConfig::class.java.classLoader)!!,
        source.readParcelable<DefaultConfig>(DefaultConfig::class.java.classLoader)!!
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeParcelable(notificationConfig, 0)
        writeParcelable(defaultConfig, 0)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<PeachConfig> = object : Parcelable.Creator<PeachConfig> {
            override fun createFromParcel(source: Parcel): PeachConfig =
                PeachConfig(source)

            override fun newArray(size: Int): Array<PeachConfig?> = arrayOfNulls(size)
        }
    }
}