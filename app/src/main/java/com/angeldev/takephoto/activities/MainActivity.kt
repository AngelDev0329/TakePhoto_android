package com.angeldev.takephoto.activities

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.result.launch
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.navigation.ui.AppBarConfiguration
import com.angeldev.takephoto.BuildConfig
import com.angeldev.takephoto.R
import com.angeldev.takephoto.databinding.ActivityMainBinding
import com.angeldev.takephoto.mvvms.AppViewModel
import com.angeldev.takephoto.utils.PickSinglePhotoContract
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.lang.Math.max
import java.lang.Math.min
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var currentPhotoPath: String
    private val storagePermissionCode  = 1000
    private val cameraPermissionCode  = 1001
    private val viewModel by viewModels<AppViewModel>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.fabCamera.setOnClickListener(this)
        binding.fabGallery.setOnClickListener(this)

//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//        appBarConfiguration = AppBarConfiguration(navController.graph)
//        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    override fun onClick(view: View?) {
        when (view!!.id) {
            R.id.fab_camera -> {
                launchTakeImageIntent(CAMERA)
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                    .setAnchorView(R.id.fab)
//                    .setAction("Action", null).show()
            }
            R.id.fab_gallery -> {
                launchTakeImageIntent(GALLERY)
            }
            else -> {
                Snackbar.make(view, "Replace with your own action" + view.id.toString(), Snackbar.LENGTH_LONG).setAction("Action", null).show()
            }
        }
    }

    private fun isDisabledStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
    }

    private fun isDisabledCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
    }

    private fun launchTakeImageIntent(type: String) {
        if ((type == CAMERA && isDisabledCameraPermission()) || (type == GALLERY && isDisabledStoragePermission())) {
            if (type == CAMERA) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), cameraPermissionCode)
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), storagePermissionCode)
            }
        } else  {

            val intent: Intent
            val photoFile = createImageFile()
            if (type == CAMERA) {
                intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                val photoURI: Uri = FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}$AUTHORITY_SUFFIX", photoFile)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                takePictureIntent.launch(intent)
            } else {
                pickSinglePhoto()
//                intent = Intent(MediaStore.ACTION_PICK_IMAGES)
//                intent = Intent(Intent.ACTION_PICK)
//                intent.type = "image/*"
//                intent.action = Intent.ACTION_GET_CONTENT
            }
//            takePictureIntent.launch(Intent.createChooser(intent, "Select Picture"))
//            takePictureIntent.launch(intent)
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

//    private val singlePhotoPickerLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { imageUri: Uri? ->
//        imageUri?.let(viewModel::setImageUri)
//    }
//
//    private fun pickPhoto() {
//        singlePhotoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
//    }

    private val singlePhotoPickerLauncher = registerForActivityResult(PickSinglePhotoContract()) { imageUri: Uri? ->
        imageUri?.let(viewModel::setImageUri)
        binding.imgView.setImageURI(imageUri)
    }

    private fun pickSinglePhoto() = singlePhotoPickerLauncher.launch()

    private val takePictureIntent = registerForActivityResult(StartActivityForResult()) { activity ->
        if (activity.resultCode == RESULT_OK) {
//            val list = activity.data
            val data = activity.data

            if (data?.data != null) {
                setPictureWithUri(data.data!!)
            } else {
                setPicture()
            }

//            data?.data?.let {
//                setPictureWithUri(data.data!!)
//            } ?: run {
//                setPicture()
//            }
        } else {
            deleteLocalPhotoFile(currentPhotoPath)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            storagePermissionCode -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    launchTakeImageIntent(GALLERY)
                } else {
                    gotoSystemSetting(R.string.we_need_to_access)
                }
                return
            }
            cameraPermissionCode -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    launchTakeImageIntent(CAMERA)
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

    private fun setPictureWithUri(imageUri: Uri) {
        try {
            val imageStream: InputStream? = contentResolver.openInputStream(imageUri)
            val selectedImage = BitmapFactory.decodeStream(imageStream)
            val degree: Int = getRotateDegreeFromExif(imageUri.path!!)
            val matrix = Matrix()
            matrix.setRotate(degree.toFloat())
            val rotatedImage: Bitmap = Bitmap.createBitmap(
                selectedImage, 0, 0,
                selectedImage.width, selectedImage.height, matrix, true
            )
            binding.imgView.setImageBitmap(rotatedImage)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show()
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
        const val TAG  = "MainActivity"
        const val GALLERY = "gallery"
        const val CAMERA = "camera"
        const val DATE_FORMAT = "yyyyMMdd_HHmmss"
        const val FILE_NAMING_PREFIX = "JPEG_"
        const val FILE_NAMING_SUFFIX = "_"
        const val FILE_FORMAT = ".jpg"
        const val AUTHORITY_SUFFIX = ".fileprovider"

//        const val AUTHORITY_SUFFIX = ".cropper.fileprovider"
    }
}