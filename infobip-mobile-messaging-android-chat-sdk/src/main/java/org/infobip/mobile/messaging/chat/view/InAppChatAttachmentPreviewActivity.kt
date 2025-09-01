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
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.infobip.mobile.messaging.ConfigurationException
import org.infobip.mobile.messaging.api.chat.WidgetInfo
import org.infobip.mobile.messaging.chat.R
import org.infobip.mobile.messaging.chat.attachments.InAppChatWebAttachment
import org.infobip.mobile.messaging.chat.core.InAppChatAttachmentPreviewClient
import org.infobip.mobile.messaging.chat.core.InAppChatAttachmentPreviewClientImpl
import org.infobip.mobile.messaging.chat.databinding.IbActivityChatAttachPreviewBinding
import org.infobip.mobile.messaging.chat.properties.MobileMessagingChatProperty
import org.infobip.mobile.messaging.chat.properties.PropertyHelper
import org.infobip.mobile.messaging.chat.utils.applyInAppChatLanguage
import org.infobip.mobile.messaging.chat.utils.applyWindowInsets
import org.infobip.mobile.messaging.chat.utils.getDrawableCompat
import org.infobip.mobile.messaging.chat.utils.hide
import org.infobip.mobile.messaging.chat.utils.setProgressTint
import org.infobip.mobile.messaging.chat.utils.setStatusBarColor
import org.infobip.mobile.messaging.chat.utils.setSystemBarIconsColor
import org.infobip.mobile.messaging.chat.utils.setTint
import org.infobip.mobile.messaging.chat.utils.show
import org.infobip.mobile.messaging.chat.utils.toColorStateList
import org.infobip.mobile.messaging.chat.view.styles.InAppChatToolbarStyle
import org.infobip.mobile.messaging.chat.view.styles.apply
import org.infobip.mobile.messaging.chat.view.styles.factory.StyleFactory
import org.infobip.mobile.messaging.logging.MobileMessagingLogger
import org.infobip.mobile.messaging.permissions.PermissionsRequestManager
import java.util.UUID

class InAppChatAttachmentPreviewActivity : AppCompatActivity(),
    PermissionsRequestManager.PermissionsRequester {

    companion object {
        private const val EXTRA_URL = "ib_chat_attachment_url"
        private const val EXTRA_TYPE = "ib_chat_attachment_type"
        private const val EXTRA_CAPTION = "ib_chat_attachment_caption"

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

    private var onFileDownloadingComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (::binding.isInitialized)
                binding.ibLcChatAttachPb.hide()
        }
    }

    private lateinit var binding: IbActivityChatAttachPreviewBinding
    private val permissionsRequestManager = PermissionsRequestManager(this, this)
    private var attachment: InAppChatWebAttachment? = null
    private var toolbarStyle: InAppChatToolbarStyle? = null
    private lateinit var webViewClient: InAppChatAttachmentPreviewClient
    private var isWebViewLoaded = false

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.applyInAppChatLanguage())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(InAppChatThemeResolver.getChatViewTheme(this))
        super.onCreate(savedInstanceState)
        binding = IbActivityChatAttachPreviewBinding.inflate(layoutInflater)
        webViewClient = InAppChatAttachmentPreviewClientImpl(binding.ibLcChatAttachWv)
        ContextCompat.registerReceiver(
            this@InAppChatAttachmentPreviewActivity,
            onFileDownloadingComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
        setContentView(binding.root)
        applyWindowInsets()
        initViews()
        loadPreviewPage()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_attachment_preview, menu)
        applyToolbarMenuStyle(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.save_attachment) {
            triggerDownload()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onFileDownloadingComplete)
    }

    private fun initViews() {
        initToolbar()
        initWebView()
    }

    private fun initToolbar() {
        setSupportActionBar(binding.ibLcChatAttachTb)
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(false)
            it.setDisplayShowTitleEnabled(false)
        }
        binding.ibLcChatAttachTb.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
            binding.ibLcChatAttachWv.clearCache(false)
            binding.ibLcChatAttachWv.clearHistory()
            binding.ibLcChatAttachWv.removeAllViews()
            binding.ibLcChatAttachWv.destroy()
        }
        val style = getToolbarStyle()
        style.apply(binding.ibLcChatAttachTb)
        binding.ibLcChatAttachPb.setProgressTint(
            style.toolbarBackgroundColor.toColorStateList() ?: Color.WHITE.toColorStateList()
        )
        setStatusBarColor(style.statusBarBackgroundColor)
        setSystemBarIconsColor(style.lightStatusBarIcons)
    }

    private fun applyToolbarMenuStyle(menu: Menu?) {
        val style = getToolbarStyle()
        runCatching {
            val saveAttachmentMenuItem = menu?.findItem(R.id.save_attachment)
            style.saveAttachmentMenuItemIcon?.let { icon -> saveAttachmentMenuItem?.setIcon(icon) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                saveAttachmentMenuItem?.iconTintList = style.menuItemsIconTint.toColorStateList()
            } else {
                saveAttachmentMenuItem?.setIcon(saveAttachmentMenuItem.icon.setTint(style.menuItemsIconTint))
            }
        }
    }

    //region WebView
    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() = with(binding.ibLcChatAttachWv) {
        settings.javaScriptEnabled = true
        settings.cacheMode = WebSettings.LOAD_NO_CACHE
        isClickable = true
        webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean = false

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                binding.ibLcChatAttachPb.show()
                isWebViewLoaded = false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.ibLcChatAttachPb.hide()
                isWebViewLoaded = true
            }

        }

        setDownloadListener { url, _, contentDisposition, mimetype, _ ->
            binding.ibLcChatAttachPb.show()
            this@InAppChatAttachmentPreviewActivity.attachment = InAppChatWebAttachment(
                url,
                contentDisposition,
                mimetype
            )
            downloadFile()
        }
    }

    private fun triggerDownload() {
        /**
         * Prefer to fetch using WebView's DownloadManager, it provides contentDisposition we can parse fileName from.
         * If WebView is not loaded there is a fallback to caption.
         */
        if (isWebViewLoaded && ::webViewClient.isInitialized) {
            webViewClient.downloadAttachment()
        } else {
            attachment = InAppChatWebAttachment(
                url = intent.getStringExtra(EXTRA_URL).orEmpty(),
                fileName = intent.getStringExtra(EXTRA_CAPTION) ?: UUID.randomUUID().toString()
            )
            downloadFile()
        }
    }

    private fun downloadFile() {
        if (!permissionsRequestManager.isRequiredPermissionsGranted) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
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
            binding.ibLcChatAttachPb.show()
            val request = DownloadManager.Request(Uri.parse(it.url))
            request.allowScanningByMediaScanner()
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, it.fileName)
            val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
        }
    }

    private fun loadPreviewPage() {
        val previewPageUrl = getString(R.string.ib_inappchat_attachment_preview_uri)
        val attachmentUrl: String? = intent.getStringExtra(EXTRA_URL)
        val attachmentType: String? = intent.getStringExtra(EXTRA_TYPE)
        val resultUrl = Uri.Builder()
            .encodedPath(previewPageUrl)
            .appendQueryParameter("attachmentUrl", attachmentUrl)
            .appendQueryParameter("attachmentType", attachmentType)
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
        /**
         * We use DownloadManager.Request().setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS)
         * by the documentation - https://developer.android.com/reference/android/app/DownloadManager.Request#setDestinationInExternalPublicDir(java.lang.String,%20java.lang.String)
         * apps targeting above Q don't need permission.
         */
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            emptyArray()
        }
    }

    override fun shouldShowPermissionsNotGrantedDialogIfShownOnce(): Boolean = true

    override fun permissionsNotGrantedDialogTitle(): Int =
        R.string.ib_chat_permissions_not_granted_title

    override fun permissionsNotGrantedDialogMessage(): Int =
        R.string.ib_chat_permissions_not_granted_message
    //endregion

    private fun prepareWidgetInfo(): WidgetInfo {
        val prefs = PropertyHelper.getDefaultMMSharedPreferences(this)
        val widgetPrimaryColor =
            prefs.getString(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_PRIMARY_COLOR.key, null)
        val widgetBackgroundColor = prefs.getString(
            MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_BACKGROUND_COLOR.key,
            null
        )
        val widgetPrimaryTextColor = prefs.getString(
            MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_PRIMARY_TEXT_COLOR.key,
            null
        )
        return WidgetInfo(
            null,
            null,
            widgetPrimaryColor,
            widgetBackgroundColor,
            widgetPrimaryTextColor,
            false,
            false,
            false,
            null,
            null,
        )
    }

    private fun getToolbarStyle(): InAppChatToolbarStyle {
        return this.toolbarStyle ?: run {
            var style =
                StyleFactory.create(this, widgetInfo = prepareWidgetInfo()).attachmentToolbarStyle()
            if (style.titleText.isNullOrBlank() && style.titleTextRes == null) {
                style = style.copy(
                    titleText = this.intent.getStringExtra(EXTRA_CAPTION),
                    titleTextRes = null
                )
            }
            if (style.saveAttachmentMenuItemIcon == null) {
                style = style.copy(
                    saveAttachmentMenuItemIcon = getDrawableCompat(R.drawable.ib_chat_attachment_save_btn_icon)
                )
            }
            style
        }.also { this.toolbarStyle = it }
    }

}