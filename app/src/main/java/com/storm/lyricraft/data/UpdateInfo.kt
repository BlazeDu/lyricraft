package com.storm.lyricraft.data

import com.google.gson.annotations.SerializedName

data class UpdateInfo(
    @SerializedName("tag_name")
    val tagName: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("body")
    val body: String,
    @SerializedName("html_url")
    val htmlUrl: String,
    @SerializedName("published_at")
    val publishedAt: String,
    @SerializedName("assets")
    val assets: List<Asset>
)

data class Asset(
    @SerializedName("name")
    val name: String,
    @SerializedName("browser_download_url")
    val downloadUrl: String,
    @SerializedName("size")
    val size: Long
)