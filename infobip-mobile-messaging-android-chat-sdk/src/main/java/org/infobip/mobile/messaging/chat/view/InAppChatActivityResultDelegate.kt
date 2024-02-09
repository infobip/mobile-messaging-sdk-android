package org.infobip.mobile.messaging.chat.view

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Handles activity results in Lifecycle aware manner as recommended by Google.
 * https://developer.android.com/training/basics/intents/result
 */
internal interface InAppChatFragmentActivityResultDelegate : DefaultLifecycleObserver {

    interface ResultListener {
        fun onCameraPermissionResult(isGranted: Boolean)
        fun onSettingsResult(result: ActivityResult)
        fun onAttachmentChooserResult(result: ActivityResult)
    }

    fun requestCameraPermission()
    fun openAppSettings(packageName: String)
    fun openAttachmentChooser(intent: Intent)
}

internal class InAppChatActivityResultDelegateImpl(
    private val registry: ActivityResultRegistry,
    private val listener: InAppChatFragmentActivityResultDelegate.ResultListener
) : InAppChatFragmentActivityResultDelegate {

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var settingsActivityLauncher: ActivityResultLauncher<Intent>
    private lateinit var attachmentChooserLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(owner: LifecycleOwner) {
        requestPermissionLauncher = registry.register(
            "requestPermissionLauncherKey",
            owner,
            ActivityResultContracts.RequestPermission()
        ) { listener.onCameraPermissionResult(it) }

        settingsActivityLauncher = registry.register(
            "settingsActivityLauncherKey",
            owner,
            ActivityResultContracts.StartActivityForResult()
        ) { listener.onSettingsResult(it)}

        attachmentChooserLauncher = registry.register(
            "attachmentChooserLauncherKey",
            owner,
            ActivityResultContracts.StartActivityForResult()
        ) { listener.onAttachmentChooserResult(it) }
    }

    override fun requestCameraPermission() {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    override fun openAppSettings(packageName: String) {
        settingsActivityLauncher.launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        })
    }

    override fun openAttachmentChooser(intent: Intent) {
        attachmentChooserLauncher.launch(intent)
    }

}