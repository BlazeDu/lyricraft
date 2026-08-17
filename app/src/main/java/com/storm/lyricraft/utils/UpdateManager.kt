package com.storm.lyricraft.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.google.gson.Gson
import com.storm.lyricraft.data.UpdateInfo
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class UpdateManager(private val context: Context) {

    companion object {
        private const val GITHUB_API = "https://api.github.com/repos/BlazeDu/lyricraft/releases/latest"
    }

    private var onProgressUpdate: ((Int) -> Unit)? = null
    private var onDownloadComplete: (() -> Unit)? = null
    private var onDownloadFailed: ((String) -> Unit)? = null
    private var isDownloading = false
    private var isCancelled = false
    private var downloadThread: Thread? = null

    private val client: OkHttpClient by lazy {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, SecureRandom())

            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .build()
        } catch (_: Exception) {
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val gson = Gson()

    interface UpdateCallback {
        fun onUpdateAvailable(updateInfo: UpdateInfo)
        fun onNoUpdate()
        fun onError(message: String)
    }

    fun checkUpdate(callback: UpdateCallback) {
        Thread {
            try {
                val request = Request.Builder()
                    .url(GITHUB_API)
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "Lyricraft-UpdateChecker")
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    val errorMsg = when (response.code) {
                        403 -> "API 访问限制，请稍后再试"
                        404 -> "未找到 Release"
                        503 -> "GitHub 服务暂时不可用"
                        else -> "检查更新失败 (${response.code})"
                    }
                    mainHandler.post { callback.onError(errorMsg) }
                    return@Thread
                }

                val json = response.body?.string() ?: ""
                val updateInfo = gson.fromJson(json, UpdateInfo::class.java)

                val currentVersion = getCurrentVersion()
                val latestVersion = updateInfo.tagName.removePrefix("v")

                mainHandler.post {
                    if (compareVersions(latestVersion, currentVersion) > 0) {
                        callback.onUpdateAvailable(updateInfo)
                    } else {
                        callback.onNoUpdate()
                    }
                }

            } catch (_: Exception) {
                mainHandler.post {
                    callback.onError("网络错误，请检查网络连接")
                }
            }
        }.start()
    }

    private fun getCurrentVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(parts1.size, parts2.size)

        for (i in 0 until maxLen) {
            val p1 = if (i < parts1.size) parts1[i] else 0
            val p2 = if (i < parts2.size) parts2[i] else 0
            if (p1 != p2) return p1 - p2
        }
        return 0
    }

    fun downloadAndInstall(
        updateInfo: UpdateInfo,
        onProgress: (Int) -> Unit,
        onComplete: () -> Unit,
        onFailed: (String) -> Unit
    ) {
        if (isDownloading) {
            Toast.makeText(context, "正在下载中...", Toast.LENGTH_SHORT).show()
            return
        }

        val asset = updateInfo.assets.firstOrNull { it.name.endsWith(".apk") }
        if (asset == null) {
            mainHandler.post {
                onFailed("未找到 APK 文件")
            }
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pm = context.packageManager
            if (!pm.canRequestPackageInstalls()) {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                intent.data = Uri.parse("package:${context.packageName}")
                context.startActivity(intent)
                mainHandler.post {
                    onFailed("请先开启「允许安装未知来源应用」权限")
                }
                return
            }
        }

        isDownloading = true
        isCancelled = false
        this.onProgressUpdate = onProgress
        this.onDownloadComplete = onComplete
        this.onDownloadFailed = onFailed

        startDownload(asset.downloadUrl)
    }

    fun cancelDownload() {
        isCancelled = true
        isDownloading = false
        downloadThread?.interrupt()
        onProgressUpdate = null
        onDownloadComplete = null
        onDownloadFailed = null
        mainHandler.post {
            Toast.makeText(context, "已取消下载", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startDownload(downloadUrl: String) {
        val downloadDir = File(context.getExternalFilesDir(null), "downloads")
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        val apkFile = File(downloadDir, "lyricraft_latest.apk")

        if (apkFile.exists()) {
            apkFile.delete()
        }

        downloadThread = Thread {
            try {
                val url = URL(downloadUrl)
                val connection = url.openConnection()
                connection.connectTimeout = 30000
                connection.readTimeout = 60000
                connection.setRequestProperty("User-Agent", "Lyricraft-Downloader")

                val inputStream = connection.getInputStream()
                val outputStream = FileOutputStream(apkFile)
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalRead = 0L
                val contentLength = connection.contentLength.toLong()

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    if (isCancelled) {
                        inputStream.close()
                        outputStream.close()
                        apkFile.delete()
                        mainHandler.post {
                            isDownloading = false
                            downloadThread = null
                            onDownloadFailed?.invoke("下载已取消")
                            onProgressUpdate = null
                            onDownloadComplete = null
                            onDownloadFailed = null
                        }
                        return@Thread
                    }
                    outputStream.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    if (contentLength > 0) {
                        val progress = (totalRead * 100 / contentLength).toInt()
                        mainHandler.post {
                            onProgressUpdate?.invoke(progress)
                        }
                    }
                }

                outputStream.close()
                inputStream.close()

                mainHandler.post {
                    isDownloading = false
                    downloadThread = null
                    if (!isCancelled) {
                        onDownloadComplete?.invoke()
                        onProgressUpdate = null
                        onDownloadComplete = null
                        onDownloadFailed = null
                        installApk(apkFile)
                    }
                }

            } catch (e: Exception) {
                mainHandler.post {
                    isDownloading = false
                    downloadThread = null
                    if (!isCancelled) {
                        val msg = onDownloadFailed
                        onProgressUpdate = null
                        onDownloadComplete = null
                        onDownloadFailed = null
                        msg?.invoke("下载失败: ${e.message}")
                    }
                }
            }
        }
        downloadThread?.start()
    }

    private fun installApk(file: File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else {
                Uri.fromFile(file)
            }

            intent.setDataAndType(uri, "application/vnd.android.package-archive")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            context.startActivity(intent)
        } catch (e: Exception) {
            mainHandler.post {
                Toast.makeText(context, "安装失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}