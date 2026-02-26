package com.tdull.webdavviewer.app.data.model

/**
 * 播放器设置数据类
 * 用于存储播放器相关的全局设置
 */
data class PlayerSettings(
    val seekSeconds: Int = 10,  // 快进快退秒数，默认10秒
    val playbackSpeed: Float = 1f  // 默认播放速度，默认1倍速
)
