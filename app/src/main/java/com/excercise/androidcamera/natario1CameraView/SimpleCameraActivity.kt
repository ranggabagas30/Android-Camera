package com.excercise.androidcamera.natario1CameraView

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import com.excercise.androidcamera.FileUtil
import com.excercise.androidcamera.R
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.PictureResult
import kotlinx.android.synthetic.main.simple_camera_activity.*
import java.io.File

class SimpleCameraActivity : AppCompatActivity() {

    private var photoDir: File? = null
    private var imageFile: File? = null
    private var imageUri: Uri? = null
    private val photoFolderName = "SimpleCamera"

    companion object {
        const val RESULT_FILE = "RESULT_FILE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.simple_camera_activity)

        photoDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        cameraView.setLifecycleOwner(this)
        cameraView.addCameraListener(object : CameraListener(){
            override fun onPictureTaken(result: PictureResult) {
                println("picture was taken")
                photoDir = FileUtil.createOrUseDir(photoDir!!, photoFolderName)
                imageFile = File(photoDir, "${FileUtil.createFileName()}.jpg").also {
                    result.toFile(it) { file ->
                        file?.also {
                            Intent().apply {
                                println("intent result: ")
                                println("imagefile: ${it.path}")
                                Toast.makeText(this@SimpleCameraActivity, "Saved photo success", Toast.LENGTH_SHORT).show()
                                putExtra(RESULT_FILE, it)
                                setResult(Activity.RESULT_OK, this)
                                finish()
                            }
                        }
                    }
                }
            }
        })

        btnTakePhoto.setOnClickListener {
            cameraView.takePicture()
        }
    }

}
