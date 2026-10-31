package com.storm.lyricraft.utils

import android.content.Context
import android.content.Context.MODE_PRIVATE
import java.io.File

class ExportDirectoryManager(context: Context) {

    private val prefs = context.getSharedPreferences("app_settings", MODE_PRIVATE)

    fun getLastDirectory(): File? {
        return prefs.getString(KEY_LAST_EXPORT_DIRECTORY, null)
            ?.let(::File)
            ?.takeIf { it.isDirectory }
    }

    fun saveDirectory(directory: File) {
        prefs.edit().putString(KEY_LAST_EXPORT_DIRECTORY, directory.absolutePath).apply()
    }

    private companion object {
        const val KEY_LAST_EXPORT_DIRECTORY = "last_export_directory"
    }
}
