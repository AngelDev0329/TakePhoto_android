package com.angeldev.takephoto.activities

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
//import androidx.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.navigation.ui.AppBarConfiguration
import com.angeldev.takephoto.BuildConfig
import com.angeldev.takephoto.R
import com.angeldev.takephoto.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.io.IOException
import java.lang.Math.max
import java.lang.Math.min
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var currentPhotoPath: String
    private val cameraPermissionCode  = 1001
    private val TAG  = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.fab.setOnClickListener(this)

//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//        appBarConfiguration = AppBarConfiguration(navController.graph)
//        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    override fun onClick(view: View?) {
        when (view!!.id) {
            R.id.fab -> {
                launchTakeImageIntent()
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                    .setAnchorView(R.id.fab)
//                    .setAction("Action", null).show()
            }
            else -> {
                Snackbar.make(view, "Replace with your own action" + view.id.toString(), Snackbar.LENGTH_LONG).setAction("Action", null).show()
            }
        }
    }

    private fun launchTakeImageIntent() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), cameraPermissionCode)
        } else {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val photoURI: Uri = FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}$AUTHORITY_SUFFIX", createImageFile())
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            takePictureIntent.launch(intent)
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "$FILE_NAMING_PREFIX${timeStamp}$FILE_NAMING_SUFFIX",
            FILE_FORMAT,
            storageDir
        )
            .apply { currentPhotoPath = absolutePath }
    }

    private val takePictureIntent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
//            val list = it.data
            val uri = it?.data
            Log.d(TAG, uri.toString())
            setPicture()
        } else {
            deleteLocalPhotoFile(currentPhotoPath)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            cameraPermissionCode -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    launchTakeImageIntent()
                } else {
                    gotoSystemSetting(R.string.we_need_to_access)
                }
                return
            }
        }
    }

    private fun setPicture() {
        // Get the dimensions of the View
        val targetW: Int = binding.imgView.width
        val targetH: Int = binding.imgView.height

        val bmOptions = BitmapFactory.Options().apply {
            // Get the dimensions of the bitmap
            inJustDecodeBounds = true

            BitmapFactory.decodeFile(currentPhotoPath, this)

            // Determine how much to scale down the image
            val scaleFactor: Int = max(1, min(outWidth / targetW, outHeight / targetH))

            // Decode the image file into a Bitmap sized to fill the View
            inJustDecodeBounds = false
            inSampleSize = scaleFactor
        }

        BitmapFactory.decodeFile(currentPhotoPath, bmOptions)?.also { bitmap ->
            val degree: Int = getRotateDegreeFromExif(currentPhotoPath)
            val matrix = Matrix()
            matrix.setRotate(degree.toFloat())
            val rotatedImage: Bitmap = Bitmap.createBitmap(
                bitmap, 0, 0,
                bitmap.width, bitmap.height, matrix, true
            )
            binding.imgView.setImageBitmap(rotatedImage)
        }
    }

    private fun getRotateDegreeFromExif(filePath: String): Int {
        var degree = 0
        try {
            val exifInterface = ExifInterface(filePath)
            when (exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> {
                    degree = 90
                }
                ExifInterface.ORIENTATION_ROTATE_180 -> {
                    degree = 180
                }
                ExifInterface.ORIENTATION_ROTATE_270 -> {
                    degree = 270
                }
            }
            if (degree != 0) {
                exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, "0")
                exifInterface.saveAttributes()
            }
        } catch (e: IOException) {
            degree = -1
            e.printStackTrace()
        }
        return degree
    }

    private fun gotoSystemSetting(message: Int) {
        gotoSystemSetting(getString(message))
    }
    private fun gotoSystemSetting(message: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.app_name)
            .setMessage(message)
            .setPositiveButton(R.string.OK) { dialog, _ ->
                val intent = Intent()
                intent.data = Uri.fromParts("package", packageName, null)
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.Cancel) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun deleteLocalPhotoFile (uriString: String) {
        val tempPhoto = File(uriString)
        if (tempPhoto.exists()) {
            if (tempPhoto.delete()) {
                Log.d("deleteLocalPhotoFile", "file Deleted :$uriString")
            } else {
                Log.d("deleteLocalPhotoFile", "file not Deleted :$uriString")
            }
        }
    }

//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        menuInflater.inflate(R.menu.menu_main, menu)
//        return true
//    }

//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return when (item.itemId) {
//            R.id.action_settings -> true
//            else -> super.onOptionsItemSelected(item)
//        }
//    }
//
//    override fun onSupportNavigateUp(): Boolean {
//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
//    }

    companion object {
        const val DATE_FORMAT = "yyyyMMdd_HHmmss"
        const val FILE_NAMING_PREFIX = "JPEG_"
        const val FILE_NAMING_SUFFIX = "_"
        const val FILE_FORMAT = ".jpg"
        const val AUTHORITY_SUFFIX = ".fileprovider"
//        const val AUTHORITY_SUFFIX = ".cropper.fileprovider"
    }
}