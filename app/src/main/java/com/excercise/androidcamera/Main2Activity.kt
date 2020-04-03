package com.excercise.androidcamera

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.exifinterface.media.ExifInterface
import kotlinx.android.synthetic.main.activity_main2.*
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class Main2Activity : AppCompatActivity() {

    private val TAG = Main2Activity::class.java.simpleName
    private val RC_TAKE_PICTURE = 1
    private val RC_PERMISSION_COMPRESS = 1
    private val RC_PERMISSION_TAKE_PICTURE = 2
    private lateinit var imageUri: Uri
    private var imageFile: File? = null
    private var dirParent: File? = null
    private val dirPhoto = "photo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        btnTakePhoto.setOnClickListener {
            invokeActionCamera()
        }
        
        btnCompressImage.setOnClickListener { 
            invokeActionCompress()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_TAKE_PICTURE) {
            if (resultCode == Activity.RESULT_OK) {
                imageFile?.also {
                    contentResolver.notifyChange(imageUri, null)
                    try {
                        var bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                        val rotationInDegrees = rotateBitmap(it)
                        val matrixRotation = Matrix()
                        matrixRotation.preRotate(rotationInDegrees.toFloat())
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrixRotation, true)
                        imageView.setImageBitmap(bitmap)
                        Toast.makeText(this@Main2Activity, "Uri: $imageUri", Toast.LENGTH_LONG).show()
                        
                        btnCompressImage.isEnabled = true
                        
                    } catch (e: Exception) {
                        Toast.makeText(this@Main2Activity, "error: ${e.message}", Toast.LENGTH_LONG).show()
                        e.printStackTrace()
                    }
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

    private fun startTakePhoto() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            val fullDir = "$dirParent${File.separator}$dirPhoto"
            val file = File(fullDir, createFileName())
            val uri = FileUtil.getUriFromFile(this@Main2Activity, file)
            this.putExtra(MediaStore.EXTRA_OUTPUT, uri)
            this.resolveActivity(packageManager)?.also {
                startActivityForResult(this, RC_TAKE_PICTURE)
            }
        }
    }

    private fun startTakePhoto2() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            dirParent = getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.also {
                val dirFull = createOrUseDir(it, dirPhoto)
                val fileName = createFileName()
                try {
                    imageFile = createImageFile(dirFull, fileName).also {
                        imageUri = FileUtil.getUriFromFile(this@Main2Activity, it)
                        this.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                        this.resolveActivity(packageManager)?.also {
                            startActivityForResult(this, RC_TAKE_PICTURE)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@Main2Activity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }

            }
        }
    }

    private fun createOrUseDir(dirParent: File, childDir: String): File {
        val fileDir = File(dirParent, childDir)
        if (!fileDir.exists()) fileDir.mkdirs()
        return fileDir
    }
    
    private fun createImageFile(dir: File, fileName: String) = File.createTempFile(fileName, ".jpg", dir)

    private fun createFileName() = "${System.currentTimeMillis()}-photo-"

    private fun invokeActionCamera() {
        when {
            shouldShowPermissionsRationale() -> Toast.makeText(this, "Please allow permissions", Toast.LENGTH_SHORT).show()
            arePermissionsGranted() -> {
                //startTakePhoto()
                startTakePhoto2()
            }
            else -> requestPermissions()
        }
    }

    private fun invokeActionCompress() {
        when {
            shouldShowPermissionsRationale() -> Toast.makeText(this, "Please allow permissions", Toast.LENGTH_SHORT).show()
            arePermissionsGranted() -> {
                if (dirParent == null) {
                    Toast.makeText(this@Main2Activity, "Parent directory is not found", Toast.LENGTH_SHORT).show()
                    return
                }

                if (imageFile != null) {
                    val destinationDir = createOrUseDir(dirParent!!, dirPhoto)
                    val bitmap = BitmapFactory.decodeFile(imageFile!!.path)
                    val imageRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                    val desiredMaxHeight = bitmap.height.toFloat() * 0.25f
                    val desiredMaxWidth = imageRatio * desiredMaxHeight
                    val fileCompressed = compressImage(imageFile!!, desiredMaxWidth, desiredMaxHeight, destinationDir)
                    if (fileCompressed != null) {
                        imageView.setImageURI(FileUtil.getUriFromFile(this@Main2Activity, fileCompressed))
                        Toast.makeText(this@Main2Activity, "Image compression success", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@Main2Activity, "Image compression failed", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this@Main2Activity, "Image file is not found", Toast.LENGTH_SHORT).show()
                }
            }
            else -> Toast.makeText(this@Main2Activity, "Please allow permission write external storage from device settings", Toast.LENGTH_LONG).show()
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

    private fun rotateBitmap(imageFile: File): Int {
        return imageFile.let {
            val exifInterface = ExifInterface(it.absolutePath)
            val orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val rotation = when(orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
            println("orientation: $orientation")
            println("rotation: $rotation")
            rotation
        }
    }

    private fun compressImage(imageFile: File, desiredMaxWidth: Float, desiredMaxHeight: Float, destinationDir: File): File? {

        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true // make sure that bitmap pixels are not loaded to the memory
                                          // only the bounds
        var sourceBmp = BitmapFactory.decodeFile(imageFile.absolutePath, options) // apply options into source bitmap

        var scaledBitmap: Bitmap? = null // declare scaled bitmap as bitmap result

        // obtaining source boundaries, like height and width
        val actualHeight = options.outHeight.toFloat()
        val actualWidth = options.outWidth.toFloat()

        // get ratio for both height and width compared between the actual and desired
        val heightRatio = actualHeight / desiredMaxHeight
        val widthRatio = actualWidth / desiredMaxWidth

        var resultHeight = actualHeight
        var resultWidth = actualWidth

        // if the actual height is greater than the desired, than scaled it down using 1/ratio
        if (actualHeight > desiredMaxHeight) {
            resultHeight = 1/heightRatio * actualHeight
        }

        // idem for width
        if (actualWidth > desiredMaxWidth) {
            resultWidth = 1/widthRatio * actualWidth
        }

        // options to allow scaled down version of bitmap
        options.inSampleSize = calculateInSampleSize(actualWidth, actualHeight, resultWidth, resultHeight)

        // allow to load bitmap pixels
        options.inJustDecodeBounds = false

        //this options allow android to claim the bitmap memory if it runs low on memory
        options.inTempStorage = ByteArray(16 * 1024)

        //load original bitmap from file
        try {
            sourceBmp = BitmapFactory.decodeFile(imageFile.absolutePath, options)
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
        }

        val actualHeightUsingSampleSize = options.outHeight.toFloat()
        val actualWidthUsingSampleSize = options.outWidth.toFloat()
        val resultHeightRatio = resultHeight / actualHeightUsingSampleSize
        val resultWidthRatio = resultWidth / actualWidthUsingSampleSize

        // load scaled bitmap
        try {
            scaledBitmap = Bitmap.createBitmap(resultWidth.toInt(), resultHeight.toInt(), Bitmap.Config.ARGB_8888)
            val middleX = resultWidth / 2f
            val middleY = resultHeight / 2f
            val scaleMatrix = Matrix()
            scaleMatrix.setScale(resultWidthRatio, resultHeightRatio, middleX, middleY)

            val canvas = Canvas(scaledBitmap)
            canvas.apply {
                setMatrix(scaleMatrix)
                drawBitmap(sourceBmp, middleX - sourceBmp.width / 2, middleY - sourceBmp.height / 2, Paint())
            }
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
        }

        if (scaledBitmap != null) {
            try {
                val rotation = rotateBitmap(imageFile)
                val matrixRotation = Matrix()
                matrixRotation.postRotate(rotation.toFloat())
                scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.width, scaledBitmap.height, matrixRotation, true)

                val compressedImageFileName = "${imageFile.nameWithoutExtension}-compress.jpg"
                val compressedImageFile = File(destinationDir, compressedImageFileName)
                val out = FileOutputStream(compressedImageFile)
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                
                return compressedImageFile
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        return null
    }

    private fun calculateInSampleSize(width: Float, height: Float, desiredMaxWidth: Float, desiredMaxHeight: Float): Int {
        val totalReqPixels = desiredMaxWidth * desiredMaxHeight * 2
        val totalPixels = width * height
        var inSampleSize = 1
        while ((totalPixels / (inSampleSize * inSampleSize) < totalReqPixels)) {
            inSampleSize++
        }
        return inSampleSize
    }
}
