package com.storm.lyricraft.utils

fun formatMs(ms: Long): String {
    if (ms <= 0) return "00:00.00"
    val secAll = ms / 1000
    val min = secAll / 60
    val sec = secAll % 60
    val msPart = (ms % 1000) / 10
    return "%02d:%02d.%02d".format(min, sec, msPart)
}