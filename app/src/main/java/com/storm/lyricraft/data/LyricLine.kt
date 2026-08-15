package com.storm.lyricraft.data

import com.storm.lyricraft.utils.formatMs

data class LyricLine(
    val timeMs: Long = 0,
    val text: String = ""
) {
    fun formatTime() = formatMs(timeMs)
}