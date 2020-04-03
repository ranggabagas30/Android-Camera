package com.excercise.androidcamera.cropphoto

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import com.excercise.androidcamera.R

class Main3Activity : AppCompatActivity(), PhotoFragment.OnPhotoFragmentListener {

    val PERMISSION_ALL = 1
    var flagPermissions = false
    val permissions = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main3)
        checkPermission()
    }

    override fun onPhotoResult(croppedBitmap: Bitmap) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun checkPermission() {
        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_ALL)
            flagPermissions = false
        }
        flagPermissions = true
    }

    private fun hasPermissions(): Boolean {
        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) !=
                    PackageManager.PERMISSION_GRANTED) return false
        }
        return true
    }
}
