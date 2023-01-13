package com.angeldev.takephoto.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.angeldev.takephoto.mvvms.AppViewModel


private const val MIME_TYPE_IMAGE = "image/*"

class PickSinglePhotoContract: ActivityResultContract<Void?, Uri?>() {
    override fun createIntent(context: Context, input: Void?): Intent {
        return Intent(if (PhotoPickerAvailabilityChecker.isPhotoPickerAvailable()) {
            Intent(MediaStore.ACTION_PICK_IMAGES)
//            Intent(Intent.ACTION_PICK)
        } else {
            Intent(Intent.ACTION_OPEN_DOCUMENT)
        }).apply { type = MIME_TYPE_IMAGE }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return intent.takeIf { resultCode == Activity.RESULT_OK }?.data
    }
}


class PhotoPickerActivity : ComponentActivity() {

    private val viewModel by viewModels<AppViewModel>()

    private val singlePhotoPickerLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { imageUri: Uri? ->
        imageUri?.let(viewModel::setImageUri)
    }

    private fun pickPhoto() {
        singlePhotoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}