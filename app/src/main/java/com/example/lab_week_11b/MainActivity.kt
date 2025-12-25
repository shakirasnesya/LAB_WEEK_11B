package com.example.lab_week_11b

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_PERMISSIONS = 100
    }

    private lateinit var providerFileManager: ProviderFileManager
    private var photoInfo: FileInfo? = null
    private var videoInfo: FileInfo? = null
    private var isCapturingVideo = false

    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var takeVideoLauncher: ActivityResultLauncher<Uri>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        providerFileManager = ProviderFileManager(
            applicationContext,
            FileHelper(applicationContext),
            contentResolver,
            Executors.newSingleThreadExecutor(),
            MediaContentHelper()
        )

        takePictureLauncher =
            registerForActivityResult(ActivityResultContracts.TakePicture()) {
                providerFileManager.insertImageToStore(photoInfo)
            }

        takeVideoLauncher =
            registerForActivityResult(ActivityResultContracts.CaptureVideo()) {
                providerFileManager.insertVideoToStore(videoInfo)
            }

        findViewById<Button>(R.id.photo_button).setOnClickListener {
            isCapturingVideo = false
            checkAllPermissions { openImageCapture() }
        }

        findViewById<Button>(R.id.video_button).setOnClickListener {
            isCapturingVideo = true
            checkAllPermissions { openVideoCapture() }
        }
    }

    private fun openImageCapture() {
        photoInfo = providerFileManager.generatePhotoUri(System.currentTimeMillis())
        takePictureLauncher.launch(photoInfo!!.uri)
    }

    private fun openVideoCapture() {
        videoInfo = providerFileManager.generateVideoUri(System.currentTimeMillis())
        takeVideoLauncher.launch(videoInfo!!.uri)
    }

    private fun checkAllPermissions(onGranted: () -> Unit) {
        val permissionsNeeded = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsNeeded.add(Manifest.permission.CAMERA)
        }

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsNeeded.toTypedArray(),
                REQUEST_PERMISSIONS
            )
        } else {
            onGranted()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_PERMISSIONS &&
            grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        ) {
            if (isCapturingVideo) {
                openVideoCapture()
            } else {
                openImageCapture()
            }
        }
    }
}
