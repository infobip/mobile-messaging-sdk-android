/*
 * InAppChatAttachmentPreviewActivity.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import org.infobip.mobile.messaging.ConfigurationException
import org.infobip.mobile.messaging.api.chat.WidgetInfo
import org.infobip.mobile.messaging.chat.R
import org.infobip.mobile.messaging.chat.attachments.InAppChatPdfDownloader
import org.infobip.mobile.messaging.chat.attachments.InAppChatPdfPageAdapter
import org.infobip.mobile.messaging.chat.attachments.InAppChatPdfRenderer
import org.infobip.mobile.messaging.chat.attachments.InAppChatWebAttachment
import org.infobip.mobile.messaging.chat.attachments.InAppChatWebAttachmentResolver
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
import java.io.File

class InAppChatAttachmentPreviewActivity : AppCompatActivity(),
    PermissionsRequestManager.PermissionsRequester {

    companion object {
        private const val TAG = "InAppChatAttachmentPreview"
        private const val EXTRA_URL = "ib_chat_attachment_url"
        private const val EXTRA_TYPE = "ib_chat_attachment_type"
        private const val EXTRA_CAPTION = "ib_chat_attachment_caption"
        private const val PDF_CACHE_DIR_NAME = "ib_chat_attachments"
        private const val PDF_CACHE_FILE_NAME = "preview.pdf"
        private const val STATE_PDF_PAGE_POSITION = "ib_chat_pdf_page_position"

        private const val TYPE_IMAGE = "IMAGE"
        private const val TYPE_VIDEO = "VIDEO"
        private const val TYPE_AUDIO = "AUDIO"
        private const val TYPE_DOCUMENT = "DOCUMENT"

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
    private val attachmentResolver = InAppChatWebAttachmentResolver()
    private val pdfDownloader = InAppChatPdfDownloader()
    private var pdfRenderer: InAppChatPdfRenderer? = null
    private var pdfPagePosition = 0
    private var attachment: InAppChatWebAttachment? = null
    private var toolbarStyle: InAppChatToolbarStyle? = null

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.applyInAppChatLanguage())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(InAppChatThemeResolver.getChatViewTheme(this))
        super.onCreate(savedInstanceState)
        binding = IbActivityChatAttachPreviewBinding.inflate(layoutInflater)
        ContextCompat.registerReceiver(
            this@InAppChatAttachmentPreviewActivity,
            onFileDownloadingComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
        setContentView(binding.root)
        applyWindowInsets()
        initToolbar()
        pdfPagePosition = savedInstanceState?.getInt(STATE_PDF_PAGE_POSITION) ?: 0
        val url = intent.getStringExtra(EXTRA_URL)
        if (!url.isNullOrBlank() && intent.getStringExtra(EXTRA_TYPE) == TYPE_DOCUMENT) {
            loadPdfPreview(url)
        } else {
            loadPreviewPage()
        }
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val layoutManager = binding.ibLcChatAttachRv.layoutManager as? LinearLayoutManager
        val position = layoutManager?.findFirstVisibleItemPosition() ?: RecyclerView.NO_POSITION
        if (position != RecyclerView.NO_POSITION) {
            outState.putInt(STATE_PDF_PAGE_POSITION, position)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onFileDownloadingComplete)
        binding.ibLcChatAttachRv.adapter = null
        pdfRenderer?.close()
        pdfRenderer = null
        clearPdfCache()
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
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
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
    }

    private fun triggerDownload() {
        val url = intent.getStringExtra(EXTRA_URL).orEmpty()
        val fallbackFileName = fallbackFileName()
        binding.ibLcChatAttachPb.show()
        lifecycleScope.launch {
            attachment = attachmentResolver.resolve(url, fallbackFileName)
            downloadFile()
        }
    }

    /**
     * File name used when the real one can not be resolved from the attachment's response headers.
     *
     * The caption usually carries the original file name, so it is preferred, but only when it has
     * an extension. Otherwise a generated name is used, with the extension derived from the
     * attachment type - a file without one is not recognized by the system.
     */
    private fun fallbackFileName(): String {
        val caption = intent.getStringExtra(EXTRA_CAPTION)?.trim()
        if (!caption.isNullOrEmpty() && caption.substringAfterLast('.', "").isNotEmpty()) {
            return caption
        }
        val extension = when (intent.getStringExtra(EXTRA_TYPE)?.uppercase()) {
            TYPE_IMAGE -> ".jpg"
            TYPE_VIDEO -> ".mp4"
            TYPE_AUDIO -> ".mp3"
            TYPE_DOCUMENT -> ".pdf"
            else -> ""
        }
        return "attachment_${System.currentTimeMillis()}$extension"
    }

    private fun downloadFile() {
        if (!permissionsRequestManager.isRequiredPermissionsGranted) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                MobileMessagingLogger.w(
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
            val fileName = it.fileName.replace('/', '_').replace('\\', '_').trim()
            val request = DownloadManager.Request(Uri.parse(it.url))
            request.allowScanningByMediaScanner()
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setTitle(fileName)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
        }
    }

    private fun loadPreviewPage() {
        initWebView()
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

    //region PDF preview
    /**
     * Renders the document locally.
     *
     * The attachment url is signed and may carry credentials, so it must not be handed to a
     * third party viewer. The document is downloaded into the application's cache instead and
     * rendered by [InAppChatPdfRenderer].
     */
    private fun loadPdfPreview(url: String) {
        binding.ibLcChatAttachWv.hide()
        binding.ibLcChatAttachPb.show()
        lifecycleScope.launch {
            runCatching {
                val file = pdfDownloader.download(url, pdfCacheFile())
                InAppChatPdfRenderer.open(file)
            }.onSuccess { renderer ->
                pdfRenderer?.close()
                pdfRenderer = renderer
                showPdfPages(renderer)
            }.onFailure {
                MobileMessagingLogger.e(TAG, "Could not render document preview.", it)
                showPreviewError()
            }
            binding.ibLcChatAttachPb.hide()
        }
    }

    private fun showPdfPages(renderer: InAppChatPdfRenderer) {
        binding.ibLcChatAttachRv.apply {
            val layoutManager = LinearLayoutManager(this@InAppChatAttachmentPreviewActivity)
            this.layoutManager = layoutManager
            adapter = InAppChatPdfPageAdapter(renderer, lifecycleScope)
            // The pages are rendered asynchronously, the position can only be restored once
            // the adapter has them.
            layoutManager.scrollToPosition(pdfPagePosition)
            show()
        }
    }

    private fun showPreviewError() {
        binding.ibLcChatAttachRv.hide()
        binding.ibLcChatAttachErrorTv.apply {
            setText(R.string.ib_chat_attachment_preview_failed)
            show()
        }
    }

    /**
     * File the previewed document is cached in.
     *
     * A fixed name is enough, the preview shows a single document and [clearPdfCache] removes
     * the file once the preview is closed.
     */
    private fun pdfCacheFile(): File = File(File(cacheDir, PDF_CACHE_DIR_NAME), PDF_CACHE_FILE_NAME)

    /**
     * Removes documents cached for previewing, they are kept only while the preview is open.
     */
    private fun clearPdfCache() {
        runCatching { File(cacheDir, PDF_CACHE_DIR_NAME).deleteRecursively() }
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