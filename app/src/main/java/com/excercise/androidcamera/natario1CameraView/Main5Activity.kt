package com.excercise.androidcamera.natario1CameraView

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.excercise.androidcamera.R
import kotlinx.android.synthetic.main.activity_main5.*
import java.io.File

class Main5Activity : AppCompatActivity() {

    private val RC_PERMISSION_TAKE_PICTURE = 1
    private val RC_START_TAKE_PICTURE = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main5)

        btnCamera.setOnClickListener {
            invokeActionCamera()
        }
    }

    private fun invokeActionCamera() {
        when {
            shouldShowPermissionsRationale() -> Toast.makeText(this, "Please allow permissions", Toast.LENGTH_SHORT).show()
            arePermissionsGranted() -> {
                startTakePhoto()
            }
            else -> requestPermissions()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_START_TAKE_PICTURE) {
            if (resultCode == Activity.RESULT_OK) {
                data?.also {
                    val imageFile = data.getSerializableExtra(SimpleCameraActivity.RESULT_FILE) as File
                    Glide.with(this@Main5Activity)
                        .load(imageFile)
                        .into(imageView)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RC_PERMISSION_TAKE_PICTURE) {
            invokeActionCamera()
        }
    }

    private fun arePermissionsGranted() =
        ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    private fun shouldShowPermissionsRationale() =
        ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) &&
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private fun requestPermissions() =
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), RC_PERMISSION_TAKE_PICTURE)

    private fun startTakePhoto() {
        val intent = Intent(this, WatermarkCameraActivity::class.java)
        startActivityForResult(intent, RC_START_TAKE_PICTURE)
    }
}
