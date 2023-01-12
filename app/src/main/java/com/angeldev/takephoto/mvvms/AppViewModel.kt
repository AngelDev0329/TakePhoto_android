package com.angeldev.takephoto.mvvms

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream


class AppViewModel(application: Application) : AndroidViewModel(application) {

    data class ViewState(
        val imageBitmap: ImageBitmap? = null,
    )

    private val _viewState = MutableStateFlow(ViewState())
    val viewState = _viewState.asStateFlow()

    fun setImageUri_2(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            getContext().contentResolver.openInputStream(uri)?.use { inputStream: InputStream ->
                _viewState.update { currentState: ViewState ->
                    currentState.copy(imageBitmap = BitmapFactory.decodeStream(inputStream).asImageBitmap())
                }
            }
        }
    }

    fun setImageUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            getContext().contentResolver.let { contentResolver: ContentResolver ->
                val readUriPermission: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, readUriPermission)
                contentResolver.openInputStream(uri)?.use { inputStream: InputStream ->
                    _viewState.update { currentState: ViewState ->
                        currentState.copy(imageBitmap = BitmapFactory.decodeStream(inputStream).asImageBitmap())
                    }
                }
            }
        }
    }

    private fun getContext(): Context = getApplication<Application>().applicationContext
}