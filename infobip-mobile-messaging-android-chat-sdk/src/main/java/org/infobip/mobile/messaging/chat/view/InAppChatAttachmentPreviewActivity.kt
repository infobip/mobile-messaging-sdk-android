package org.infobip.mobile.messaging.chat.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import org.infobip.mobile.messaging.ConfigurationException
import org.infobip.mobile.messaging.api.chat.WidgetInfo
import org.infobip.mobile.messaging.chat.R
import org.infobip.mobile.messaging.chat.attachments.InAppChatWebAttachment
import org.infobip.mobile.messaging.chat.databinding.IbActivityChatAttachPreviewBinding
import org.infobip.mobile.messaging.chat.properties.MobileMessagingChatProperty
import org.infobip.mobile.messaging.chat.properties.PropertyHelper
import org.infobip.mobile.messaging.chat.utils.*
import org.infobip.mobile.messaging.chat.view.styles.InAppChatToolbarStyle
import org.infobip.mobile.messaging.chat.view.styles.apply
import org.infobip.mobile.messaging.logging.MobileMessagingLogger
import org.infobip.mobile.messaging.permissions.PermissionsRequestManager
import org.infobip.mobile.messaging.util.ResourceLoader
import org.infobip.mobile.messaging.util.SystemInformation

class InAppChatAttachmentPreviewActivity : AppCompatActivity(),
    PermissionsRequestManager.PermissionsRequester {

    companion object {
        private const val EXTRA_URL = "ib_chat_attachment_url"
        private const val EXTRA_TYPE = "ib_chat_attachment_type"
        private const val EXTRA_CAPTION = "ib_chat_attachment_caption"

        private const val RES_ID_IN_APP_CHAT_ATTACH_PREVIEW_URI = "ib_inappchat_attachment_preview_uri"

        @JvmStatic
        fun startIntent(context: Context, url: String?, type: String?, caption: String?): Intent {
            return Intent(context, InAppChatAttachmentPreviewActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                url?.let { this.putExtra(EXTRA_URL, it) }
                type?.let { this.putExtra(EXTRA_TYPE, it) }
                caption?.let { this.putExtra(EXTRA_CAPTION, it) }
            }
        }
    }

    var onFileDownloadingComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (::binding.isInitialized)
                binding.ibLcChatAttachPb.hide()
        }
    }

    private lateinit var binding: IbActivityChatAttachPreviewBinding
    private val permissionsRequestManager = PermissionsRequestManager(this, this)
    private var attachment: InAppChatWebAttachment? = null

    @ColorInt
    private var originalStatusBarColor: Int? = null

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LocalizationUtils.getInstance(newBase ?: this).updateContext())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(InAppChatThemeResolver.getChatAttachPreviewTheme(this))
        super.onCreate(savedInstanceState)
        binding = IbActivityChatAttachPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
        loadPreviewPage()
    }

    override fun onDestroy() {
        super.onDestroy()
        originalStatusBarColor?.let { setStatusBarColor(it) }
        unregisterReceiver(onFileDownloadingComplete)
    }

    private fun initViews() {
        initToolbar()
        initWebView()
    }

    private fun initToolbar() {
        originalStatusBarColor = getStatusBarColor()
        setSupportActionBar(binding.ibLcChatAttachTb)
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(false)
            it.setDisplayShowTitleEnabled(false)
        }
        binding.ibLcChatAttachTb.setNavigationOnClickListener {
            onBackPressed()
            binding.ibLcChatAttachWv.freeMemory()
            binding.ibLcChatAttachWv.removeAllViews()
            binding.ibLcChatAttachWv.destroy()
        }
        var style = InAppChatToolbarStyle.createChatAttachmentStyle(this, prepareWidgetInfo())
        if (style.titleText.isNullOrBlank()) {
            style = style.copy(titleText = this.intent.getStringExtra(EXTRA_CAPTION))
        }
        style.apply(binding.ibLcChatAttachTb)
        binding.ibLcChatAttachPb.setProgressTint(
            style.toolbarBackgroundColor.toColorStateList() ?: Color.WHITE.toColorStateList()
        )
        setStatusBarColor(style.statusBarBackgroundColor)
    }

    //region WebView
    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() = with(binding.ibLcChatAttachWv) {
        settings.javaScriptEnabled = true
        isClickable = true
        webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean = false

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                binding.ibLcChatAttachPb.show()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.ibLcChatAttachPb.hide()
            }
        }

        setDownloadListener { url, _, contentDisposition, mimetype, _ ->
            binding.ibLcChatAttachPb.show()
            attachment = InAppChatWebAttachment(url, contentDisposition, mimetype)
            downloadFile()
        }

        registerReceiver(onFileDownloadingComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    private fun downloadFile() {
        if (!permissionsRequestManager.isRequiredPermissionsGranted) {
            if (SystemInformation.isTiramisuOrAbove()) {
                MobileMessagingLogger.e(
                    "[InAppChat] Permissions required for attachments not granted",
                    ConfigurationException(
                        ConfigurationException.Reason.MISSING_REQUIRED_PERMISSION,
                        Manifest.permission.READ_MEDIA_IMAGES + ", " + Manifest.permission.READ_MEDIA_VIDEO + ", " + Manifest.permission.READ_MEDIA_AUDIO + ", " + Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ).message
                )
            } else {
                MobileMessagingLogger.e(
                    "[InAppChat] Permissions required for attachments not granted",
                    ConfigurationException(
                        ConfigurationException.Reason.MISSING_REQUIRED_PERMISSION,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ).message
                )
            }
            return
        }
        attachment?.let {
            val request = DownloadManager.Request(Uri.parse(it.url))
            request.allowScanningByMediaScanner()
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, it.fileName)
            val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
        }
    }

    private fun loadPreviewPage() {
        val previewPageUrl = ResourceLoader.loadStringResourceByName(this, RES_ID_IN_APP_CHAT_ATTACH_PREVIEW_URI)
        val attachmentUrl: String? = intent.getStringExtra(InAppChatAttachmentPreviewActivity.EXTRA_URL)
        val attachmentType: String? = intent.getStringExtra(InAppChatAttachmentPreviewActivity.EXTRA_TYPE)
        val attachmentCaption: String? = intent.getStringExtra(InAppChatAttachmentPreviewActivity.EXTRA_CAPTION)
        val resultUrl = Uri.Builder()
            .encodedPath(previewPageUrl)
            .appendQueryParameter("attachmentUrl", attachmentUrl)
            .appendQueryParameter("attachmentType", attachmentType)
            .appendQueryParameter("attachmentCaption", attachmentCaption)
            .build()
            .toString()
        binding.ibLcChatAttachWv.loadUrl(resultUrl)
    }
    //endregion

    //region PermissionsRequester
    override fun onPermissionGranted() {
        downloadFile()
    }

    override fun requiredPermissions(): Array<String> {
        return if (SystemInformation.isTiramisuOrAbove()) {
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    override fun shouldShowPermissionsNotGrantedDialogIfShownOnce(): Boolean = true

    override fun permissionsNotGrantedDialogTitle(): Int = R.string.ib_chat_permissions_not_granted_title

    override fun permissionsNotGrantedDialogMessage(): Int = R.string.ib_chat_permissions_not_granted_message
    //endregion

    private fun prepareWidgetInfo(): WidgetInfo {
        val prefs = PropertyHelper.getDefaultMMSharedPreferences(this)
        val widgetPrimaryColor =
            prefs.getString(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_PRIMARY_COLOR.key, null)
        val widgetBackgroundColor = prefs.getString(
            MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_BACKGROUND_COLOR.key,
            null
        )
        return WidgetInfo(null, null, widgetPrimaryColor, widgetBackgroundColor, 0L, null, false)
    }

}