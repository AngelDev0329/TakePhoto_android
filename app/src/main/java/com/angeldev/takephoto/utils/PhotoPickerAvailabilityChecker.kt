import android.os.Build
import android.os.ext.SdkExtensions.getExtensionVersion

private const val ANDROID_R_REQUIRED_EXTENSION_VERSION = 2

object PhotoPickerAvailabilityChecker {

    fun isPhotoPickerAvailable(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> true
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    getExtensionVersion(Build.VERSION_CODES.R) >= ANDROID_R_REQUIRED_EXTENSION_VERSION
                } else {
                    TODO("VERSION.SDK_INT < TIRAMISU")
                }
            }
            else -> false
        }
    }
}