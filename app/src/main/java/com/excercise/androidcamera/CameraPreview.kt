package com.excercise.androidcamera

import android.app.Activity
import android.content.Context
import android.graphics.Paint
import android.hardware.Camera
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.camera.core.CameraInfo
import java.io.IOException
import java.lang.Exception
import kotlin.math.abs

class CameraPreview (
    context: Context,
    private val activity: Activity,
    private val camera: Camera
): SurfaceView(context), SurfaceHolder.Callback {

    private val previewHolder: SurfaceHolder = holder.apply {

        // install surfaceholder.callback to get event
        addCallback(this@CameraPreview)

        setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS) // used for Android pre 3.0
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        // this handles surface view rotation or changes in size
        // make sure to stop the preview before resizing or reformatting it
        if (previewHolder.surface == null) return // preview surface doesn't exist

        stopPreview()

        // set preview size and make any resize, rotate, or reformatting
        // here
        println("surface view width: $width, height: $height")
        val supportedPreviewSizes = camera.parameters.supportedPreviewSizes
        println("supported preview size: ")
        supportedPreviewSizes.forEach {
            println("width: ${it.width}, height: ${it.height}")
        }

        val newWidth = width
        val newHeight = 768

        val surfaceViewParams = this.layoutParams
        surfaceViewParams.width = newWidth
        surfaceViewParams.height = newHeight
        this.layoutParams = surfaceViewParams

        val supportedPictureSizes= camera.parameters.supportedPictureSizes
        println("supported picture size: ")
        supportedPictureSizes.forEach {
            println("width: ${it.width}, height: ${it.height}")
        }

        setCameraDisplayOrientation(0)

        // start preview with new settings
        startPreview(previewHolder)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        // take care of releasing camera preview in activity
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        // The surface view has been created successfully, tell the camera where to draw
        startPreview(holder)
    }

    private fun startPreview(holder: SurfaceHolder?) {
        camera.apply {
            try {
                setPreviewDisplay(holder)
                startPreview()
            } catch (e: IOException) {
                Log.e(this@CameraPreview::class.java.simpleName, "Error setting camera preview: ${e.message}")
            }
        }
    }

    private fun stopPreview() {
        try {
            camera.stopPreview()
        } catch (e: Exception) {
            // ignore: try to stop non existent camera
        }
    }

    private fun setCameraDisplayOrientation(cameraId: Int) {
        val cameraInfo = Camera.CameraInfo()
        Camera.getCameraInfo(cameraId, cameraInfo)

        val rotation = activity.windowManager.defaultDisplay.rotation // device drawn rotation
        val cameraOrientation = cameraInfo.orientation
        val cameraFacing = cameraInfo.facing
        var degrees = when(rotation) {
            Surface.ROTATION_0 -> 0 // no rotation
            Surface.ROTATION_90 -> 90 // physical device rotation is 90 deg counter-clockwise, compenstate by 90 deg clockwise
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }

        var compensateRotation = 0

        if (cameraFacing == Camera.CameraInfo.CAMERA_FACING_BACK) {
            compensateRotation = cameraOrientation - degrees
        }

        println("camera facing: ${if (cameraFacing == Camera.CameraInfo.CAMERA_FACING_BACK) "back" else "front"}")
        println("display rotation: $degrees")
        println("camera orientation: $cameraOrientation")
        println("compensate rotation: $compensateRotation")

        // this affect to the preview and picture snapshot orientation
        camera.setDisplayOrientation(abs(compensateRotation)) // set the clockwise rotation of preview display in degrees
    }

    private fun getOptimalPreviewSize(maxWidth: Int, maxHeight: Int): Pair<Int, Int> {

        // setting the preview with smallest height
        var optimalHeight = maxHeight
        var optimalWidth = maxWidth
        val supportedPreviewSizes = camera.parameters.supportedPreviewSizes // get supported preview size
        println("supported preview size: ")
        for (previewSize in supportedPreviewSizes) {
            println("width: ${previewSize.width}, height: ${previewSize.height}")
            if (previewSize.width <= maxWidth) {
                optimalWidth= previewSize.width
                optimalHeight = previewSize.height
                break
            }
        }

        println("optimal width: $optimalWidth")
        println("optimal height: $optimalHeight")
        return Pair(optimalWidth, optimalHeight)
    }
}