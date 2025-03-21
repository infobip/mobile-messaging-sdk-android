package org.infobip.mobile.messaging.chat.view

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.infobip.mobile.messaging.chat.R
import org.infobip.mobile.messaging.chat.attachments.InAppChatAttachmentFileProvider
import org.infobip.mobile.messaging.chat.models.AttachmentSource

/**
 * Handles activity results in Lifecycle aware manner as recommended by Google.
 * https://developer.android.com/training/basics/intents/result
 */
internal interface InAppChatFragmentActivityResultDelegate : DefaultLifecycleObserver {

    interface ResultListener {
        fun onAttachmentLauncherResult(uri: Uri, source: AttachmentSource)
    }

    fun capturePhoto()
    fun recordVideo()
    fun selectMedia()
    fun selectFile(mimeTypes: Array<String>? = null)
}

internal class InAppChatActivityResultDelegateImpl(
    private val activity: FragmentActivity,
    private val listener: InAppChatFragmentActivityResultDelegate.ResultListener
) : InAppChatFragmentActivityResultDelegate {

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var settingsActivityLauncher: ActivityResultLauncher<Intent>
    private lateinit var photoActionLauncher: ActivityResultLauncher<Uri>
    private lateinit var videoActionLauncher: ActivityResultLauncher<Uri>
    private lateinit var mediaPickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var filePickerLauncher: ActivityResultLauncher<Array<String>?>

    private var onSettingsResult: ((ActivityResult) -> Unit)? = null
    private var onRequestPermissionResult: ((Boolean) -> Unit)? = null
    private var onCameraResult: ((Boolean) -> Unit)? = null

    override fun onCreate(owner: LifecycleOwner) {
        requestPermissionLauncher = activity.activityResultRegistry.register(
            "requestPermissionLauncherKey${this.hashCode()}", //hashCode has to be used to avoid key duplication if multiple instances coexist in the same time
            owner,
            ActivityResultContracts.RequestPermission()
        ) { onRequestPermissionResult?.invoke(it) }

        settingsActivityLauncher = activity.activityResultRegistry.register(
            "settingsActivityLauncherKey${this.hashCode()}",
            owner,
            ActivityResultContracts.StartActivityForResult()
        ) { onSettingsResult?.invoke(it)}

        photoActionLauncher = activity.activityResultRegistry.register(
            "photoActionLauncherKey${this.hashCode()}",
            owner,
            ActivityResultContracts.TakePicture()
        ) { onCameraResult?.invoke(it) }

        videoActionLauncher = activity.activityResultRegistry.register(
            "videoActionLauncherKey${this.hashCode()}",
            owner,
            ActivityResultContracts.CaptureVideo()
        ) { onCameraResult?.invoke(it) }

        mediaPickerLauncher = activity.activityResultRegistry.register(
            "mediaPickerLauncherKey${this.hashCode()}",
            owner,
            PickVisualMedia()
        ) { it?.let { listener.onAttachmentLauncherResult(it, AttachmentSource.VisualMediaPicker) } }

        filePickerLauncher = activity.activityResultRegistry.register(
            "filePickerLauncherKey${this.hashCode()}",
            owner,
            GetContentContract()
        ) { it?.let { listener.onAttachmentLauncherResult(it, AttachmentSource.FilePicker) } }
    }

    //All DefaultLifecycleObserver methods must be overridden to avoid compilation error with different versions of Lifecycle library
    override fun onDestroy(owner: LifecycleOwner) {}

    override fun onPause(owner: LifecycleOwner) {}

    override fun onResume(owner: LifecycleOwner) {}

    override fun onStart(owner: LifecycleOwner) {}

    override fun onStop(owner: LifecycleOwner) {}

    override fun selectMedia() {
        mediaPickerLauncher.launch(PickVisualMediaRequest())
    }

    override fun selectFile(mimeTypes: Array<String>?) {
        filePickerLauncher.launch(mimeTypes)
    }

    override fun capturePhoto() {
        useCamera(photoActionLauncher, "Photo-${System.currentTimeMillis()}.jpeg", AttachmentSource.Camera)
    }

    override fun recordVideo() {
        useCamera(videoActionLauncher, "Video-${System.currentTimeMillis()}.mp4", AttachmentSource.VideoRecorder)
    }

    private fun useCamera(launcher: ActivityResultLauncher<Uri>, fileName: String, source: AttachmentSource) {
        val launchCameraAction: () -> Unit = {
            val file = InAppChatAttachmentFileProvider.createFile(activity, fileName)
            val uri = InAppChatAttachmentFileProvider.getFileUri(activity, file)
            uri?.let {
                onCameraResult = { success ->
                    if (success) {
                        listener.onAttachmentLauncherResult(it, source)
                    }
                    onCameraResult = null
                }
                launcher.launch(it)
            }
        }

        onSettingsResult = {
            if (isCameraPermissionGranted()) {
                launchCameraAction()
                onSettingsResult = null
            }
        }

        onRequestPermissionResult = {
            if (it) {
                launchCameraAction()
                onRequestPermissionResult = null
            }
        }

        when {
            isCameraPermissionGranted() -> launchCameraAction()
            shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA) -> showCameraPermissionRationale()
            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun isCameraPermissionGranted(): Boolean = ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PERMISSION_GRANTED

    private fun showCameraPermissionRationale() {
        AlertDialog.Builder(activity, R.style.IB_Chat_AlertDialog)
            .setTitle(R.string.ib_chat_permissions_not_granted_title)
            .setMessage(R.string.ib_chat_permissions_not_granted_message)
            .setCancelable(false)
            .setNegativeButton(org.infobip.mobile.messaging.resources.R.string.mm_button_cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(org.infobip.mobile.messaging.resources.R.string.mm_button_settings) { dialog, _ ->
                dialog.dismiss()
                openAppSettings(activity.packageName)
            }.show()
    }

    private fun openAppSettings(packageName: String) {
        settingsActivityLauncher.launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        })
    }
}

/**
 * Custom contract for getting content from the device.
 * It uses [Intent.ACTION_GET_CONTENT] intent and allows to specify multiple mime types of the content to be picked.
 */
private class GetContentContract : ActivityResultContract<Array<String>?, Uri?>() {

    override fun createIntent(context: Context, input: Array<String>?): Intent {
        return Intent(Intent.ACTION_GET_CONTENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("*/*")
            .apply {
                input?.let { putExtra(Intent.EXTRA_MIME_TYPES, it) }
            }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return intent.takeIf { resultCode == Activity.RESULT_OK }?.data
    }

}