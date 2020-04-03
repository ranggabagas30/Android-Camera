package com.excercise.androidcamera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Camera
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_camera.*
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

// Control camera hardware using the framework API
class CameraActivity : AppCompatActivity() {

    private val RC_PERMISSION_TAKE_PICTURE = 2

    private var parentDir: File? = null
    private var childDir: File? = null
    private var imageFile: File? = null
    private var imageUri: Uri? = null

    private var camera: Camera? = null
    private var preview: CameraPreview? = null
    private val pictureCallback: Camera.PictureCallback by lazy {
        Camera.PictureCallback { data, camera ->
            imageFile = FileUtil.createImageFile(File(parentDir, childDir.toString()), FileUtil.createFileName())

            try {
                val os = FileOutputStream(imageFile)
                os.write(data)
                os.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        if (!checkCameraHardware(this)) {
            Toast.makeText(this, "No hardware camera found", Toast.LENGTH_LONG).show()
            return
        }

        // setup dir for saving photos
        childDir = File("Photo2")
        parentDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.also {
            FileUtil.createOrUseDir(it, childDir.toString())
        }

        // get camera instance
        camera = getCameraInstance()
        preview = camera?.let {
            // create preview view
            CameraPreview(this, this, it)
        }

        // set the preview view as the content of activity
        preview?.also {
            val frameLayout: FrameLayout = findViewById(R.id.cameraPreview)
            frameLayout.addView(it)
        }

        btnCapture.setOnClickListener {
            invokeActionCamera()
        }
    }

    override fun onPause() {
        super.onPause()
        releaseCamera()
    }

    // Detect camera existence
    private fun checkCameraHardware(context: Context): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)

    // get camera instance
    private fun getCameraInstance(): Camera? {
        return try {
            Camera.open() // accessing primary camera (back-facing camera)
        } catch (e: Exception) {
            null
        }
    }

    private fun releaseCamera() {
        camera?.release()
        camera = null
    }

    private fun invokeActionCamera() {
        when {
            shouldShowPermissionsRationale() -> Toast.makeText(this, "Please allow permissions", Toast.LENGTH_SHORT).show()
            arePermissionsGranted() -> {
                camera?.takePicture(null, null, pictureCallback)
            }
            else -> requestPermissions()
        }
    }

    // permissions
    private fun arePermissionsGranted() =
        ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    private fun shouldShowPermissionsRationale() =
        ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) &&
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private fun requestPermissions() =
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), RC_PERMISSION_TAKE_PICTURE)

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
 }
