package com.excercise.androidcamera.cropphoto


import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.Camera
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment

import com.excercise.androidcamera.R
import kotlinx.android.synthetic.main.fragment_photo.*
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.round

/**
 * A simple [Fragment] subclass.
 */
class PhotoFragment : Fragment(), SurfaceHolder.Callback {

    private var camera: Camera? = null
    private var previewing = false
    private var onPhotoFragmentListener: OnPhotoFragmentListener? = null

    companion object {
        fun newInstance(onPhotoFragmentListener: OnPhotoFragmentListener? = null): PhotoFragment {
            val fragment = PhotoFragment()
            fragment.onPhotoFragmentListener = onPhotoFragmentListener
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_photo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }

    private fun makePhoto() {
        camera?.also {
            it.takePicture(null, null, Camera.PictureCallback { data, camera ->

                val bitmapPicture = BitmapFactory.decodeByteArray(data, 0, data.size)
                var croppedBitmap: Bitmap? = null

                val display = (requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay

                if (display.rotation == Surface.ROTATION_0) {

                    // rotate bitmap
                    val matrix = Matrix()
                    matrix.postRotate(90f)
                    val rotatedBitmap = Bitmap.createBitmap(bitmapPicture, 0, 0, bitmapPicture.width, bitmapPicture.height, matrix, true)

                    // save file
                    createImageFile(rotatedBitmap)

                    // calculate aspect ratio
                    val koefX = rotatedBitmap.width.toFloat() / preview_layout.width.toFloat()
                    val koefY = rotatedBitmap.height.toFloat() / preview_layout.height.toFloat()

                    // get viewfinder border size and position on the screen
                    val x1 = border_camera.left
                    val y1 = border_camera.top
                    val x2 = border_camera.width
                    val y2 = border_camera.height

                    // calculate position and size for cropping
                    val cropStartX = round(x1 * koefX).toInt()
                    val cropStartY = round(y1 * koefY).toInt()
                    val cropWidthX = round(x2 * koefX).toInt()
                    val cropHeightY = round(y2 * koefY).toInt()

                    // check limits and make crop
                    if (cropStartX + cropWidthX <= rotatedBitmap.width &&
                            cropStartY + cropHeightY <= rotatedBitmap.height) {
                        croppedBitmap = Bitmap.createBitmap(rotatedBitmap, cropStartX, cropStartY, cropWidthX, cropHeightY)
                        createImageFile(croppedBitmap)
                    }
                } else if (display.rotation == Surface.ROTATION_270) {
                    // for landscape
                }

                croppedBitmap?.also {
                    onPhotoFragmentListener?.onPhotoResult(it)
                }

                camera?.startPreview()
            })
        }
    }

    private fun createImageFile(bitmap: Bitmap) {

        val path = File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_PICTURES)
        val timestamp = SimpleDateFormat("MMdd_HHmmssSSSS").format(Date())
        val imageFileName = "region_$timestamp.jpg"
        val file = File(path, imageFileName)

        try {
            if (path.mkdirs()) {
                Toast.makeText(requireContext(), "Not exist: ${path.name}", Toast.LENGTH_SHORT).show()
            }

            val os = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
            os.flush()
            os.close()
            println("Writed $path${file.name}")

            // Tell the media scanner
            MediaScannerConnection.scanFile(requireContext(),
                arrayOf(file.toString()), null,
                object : MediaScannerConnection.OnScanCompletedListener {
                    override fun onScanCompleted(path: String?, uri: Uri?) {
                        println("Scanned $path: ")
                        println("-> uri = $uri")
                    }
                })
            Toast.makeText(requireContext(), file.name, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            println("Error writing $file")
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        if (previewing) {
            stopPreview()
        }

        camera?.also {
            val parameters = it.parameters
            val supportedPreviewSizes = parameters.supportedPreviewSizes

            val optimalPreviewSize = getOptimalPreviewSize(supportedPreviewSizes, parameters.pictureSize.width, parameters.pictureSize.height)

            if (optimalPreviewSize != null) {
                parameters.setPreviewSize(optimalPreviewSize.width, optimalPreviewSize.height)
            }

            if (parameters.focusMode.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
            }

            if (parameters.flashMode.contains(Camera.Parameters.FLASH_MODE_AUTO)) {
                parameters.flashMode = Camera.Parameters.FLASH_MODE_AUTO
            }

            it.parameters = parameters

            // rotate screen
            val display = (requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
            when(display.rotation) {
                Surface.ROTATION_0 -> it.setDisplayOrientation(90)
                Surface.ROTATION_270 -> it.setDisplayOrientation(180)
            }

            // write some info
            val x1 = preview_layout.width
            val y1 = preview_layout.height

            val x2 = border_camera.width
            val y2 = border_camera.height

            val info = "Preview width: $x1 \n" +
                    "Preview height: $y1 \n " +
                    "Border width: $x2 \n" +
                    "Border height: $y2"

            res_border_size.text = info
        }

        startPreview(holder)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        stopPreview()
        releaseCamera()
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        openCamera()
    }

    private fun openCamera() {
        try {
            camera = Camera.open()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseCamera() {
        camera?.release()
        camera = null
    }

    private fun startPreview(holder: SurfaceHolder?) {
        camera?.apply {
            try {
                setPreviewDisplay(holder)
                startPreview()
                previewing = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopPreview() {
        try {
            camera?.stopPreview()
            previewing = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getOptimalPreviewSize(sizes: List<Camera.Size>?, width: Int, height: Int): Camera.Size? {
        val ASPECT_TOLERANCE = 0.1f
        val targetRatio: Double = width.toDouble() / height.toDouble()
        if (sizes == null) return null

        var optimalSize: Camera.Size? = null
        var minDiff = Double.MAX_VALUE

        val targetHeight = height

        println("target ratio: $targetRatio")
        println("width: $width, height: $height")

        for (size in sizes) {
            val ratio: Double = size.width.toDouble() / size.height.toDouble()
            if (abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue
            if (abs(size.height - targetHeight) < minDiff) {
                optimalSize = size
                minDiff = abs(size.height.toDouble() - targetHeight)
            }
        }

        // cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE
            for (size in sizes) {
                if (abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size
                    minDiff = abs(size.height.toDouble() - targetHeight)
                }
            }
        }

        println("optimal size width: ${optimalSize?.width}, height: ${optimalSize?.height}")
        return optimalSize
    }

    interface OnPhotoFragmentListener {
        fun onPhotoResult(croppedBitmap: Bitmap)
    }
}
