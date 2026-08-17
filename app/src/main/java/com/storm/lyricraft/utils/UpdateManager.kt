package com.storm.lyricraft.utils

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.storm.lyricraft.R
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
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "update_channel"
    }

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
        } catch (e: Exception) {
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

            } catch (e: Exception) {
                mainHandler.post {
                    callback.onError("网络错误: ${e.message}")
                }
            }
        }.start()
    }

    private fun getCurrentVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
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

    fun downloadAndInstall(updateInfo: UpdateInfo) {
        val asset = updateInfo.assets.firstOrNull { it.name.endsWith(".apk") }
        if (asset == null) {
            mainHandler.post {
                Toast.makeText(context, "未找到 APK 文件", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // Android 8+ 检查安装权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pm = context.packageManager
            if (!pm.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                intent.data = Uri.parse("package:${context.packageName}")
                context.startActivity(intent)
                mainHandler.post {
                    Toast.makeText(context, "请先开启「允许安装未知来源应用」权限", Toast.LENGTH_LONG).show()
                }
                return
            }
        }

        // 检查通知权限 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                // 请求通知权限
                (context as? androidx.activity.ComponentActivity)?.requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1002
                )
            }
        }

        startDownload(asset.downloadUrl)
    }

    private fun startDownload(downloadUrl: String) {
        // 使用应用私有目录，避免 Android 11+ 权限问题
        val downloadDir = File(context.getExternalFilesDir(null), "downloads")
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        val apkFile = File(downloadDir, "lyricraft_latest.apk")

        // 如果文件已存在，删除重新下载
        if (apkFile.exists()) {
            apkFile.delete()
        }

        createNotification()

        Thread {
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
                    outputStream.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    if (contentLength > 0) {
                        val progress = (totalRead * 100 / contentLength).toInt()
                        updateNotification(progress)
                    }
                }

                outputStream.close()
                inputStream.close()

                mainHandler.post {
                    Toast.makeText(context, "下载完成，开始安装", Toast.LENGTH_SHORT).show()
                    // 延迟一点确保通知已更新
                    Handler(Looper.getMainLooper()).postDelayed({
                        installApk(apkFile)
                    }, 500)
                }

            } catch (e: Exception) {
                mainHandler.post {
                    Toast.makeText(context, "下载失败: ${e.message}", Toast.LENGTH_LONG).show()
                    cancelNotification()
                }
            }
        }.start()
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

    private fun createNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "更新下载",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("正在下载更新")
            .setContentText("0%")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun updateNotification(progress: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("正在下载更新")
            .setContentText("$progress%")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun cancelNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}