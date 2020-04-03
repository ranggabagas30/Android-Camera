package com.excercise.androidcamera

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File

object FileUtil {

    fun getUriFromFile(
        context: Context,
        file: File
    ): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileProvider", file)
        } else {
            Uri.fromFile(file)
        }
    }

    fun createOrUseDir(dirParent: File, childDir: String): File {
        val fileDir = File(dirParent, childDir)
        if (!fileDir.exists()) fileDir.mkdirs()
        return fileDir
    }

    fun createFileName() = "${System.currentTimeMillis()}-photo-"

    fun createImageFile(dir: File, fileName: String) = File.createTempFile(fileName, ".jpg", dir)
}