package org.infobip.mobile.messaging.chat.attachments

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.webkit.MimeTypeMap
import androidx.activity.result.contract.ActivityResultContracts
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.assertj.core.api.Assertions
import org.infobip.mobile.messaging.chat.models.AttachmentSourceSpecification
import org.junit.After
import org.junit.Before
import org.junit.Test

class InAppChatAttachmentTest {

    private val mimeTypeMap: MimeTypeMap = mockk()

    @Before
    fun setup(){
        mockkStatic(MimeTypeMap::class)
        every { MimeTypeMap.getSingleton() } returns mimeTypeMap
    }

    @After
    fun after() {
        unmockkStatic(MimeTypeMap::class)
    }

    //region getAvailableSourcesSpecifications()
    @Test
    fun `getAvailableSourcesSpecifications() allowed extensions set is empty`(){
        val specifications = InAppChatAttachment.getAvailableSourcesSpecifications(mockk(), emptySet())
        Assertions.assertThat(specifications)
            .describedAs("No specifications for empty allowed extensions set")
            .isEmpty()
    }

    @Test
    fun `getAvailableSourcesSpecifications() all specifications available`(){
        val extensions = setOf("jpg", "mp4", "txt")
        val context = getContextMock(true)
        every { mimeTypeMap.getMimeTypeFromExtension("jpg") } returns "image/jpg"
        every { mimeTypeMap.getMimeTypeFromExtension("mp4") } returns "video/mp4"
        every { mimeTypeMap.getMimeTypeFromExtension("txt") } returns "text/plain"

        val specifications = InAppChatAttachment.getAvailableSourcesSpecifications(context, extensions)

        Assertions.assertThat(specifications)
            .describedAs("All specifications available")
            .isEqualTo(
                setOf(
                    AttachmentSourceSpecification.Camera("jpg"),
                    AttachmentSourceSpecification.VideoRecorder("mp4"),
                    AttachmentSourceSpecification.FilePicker(setOf("image/jpg", "video/mp4", "text/plain")),
                    AttachmentSourceSpecification.VisualMediaPicker(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
                )
            )
    }

    @Test
    fun `getAvailableSourcesSpecifications() camera feature is not available`(){
        val extensions = setOf("jpg", "mp4", "txt")
        val context = getContextMock(false)
        every { mimeTypeMap.getMimeTypeFromExtension("jpg") } returns "image/jpg"
        every { mimeTypeMap.getMimeTypeFromExtension("mp4") } returns "video/mp4"
        every { mimeTypeMap.getMimeTypeFromExtension("txt") } returns "text/plain"

        val specifications = InAppChatAttachment.getAvailableSourcesSpecifications(context, extensions)

        Assertions.assertThat(specifications)
            .describedAs("Only non camera specifications available")
            .isEqualTo(
                setOf(
                    AttachmentSourceSpecification.FilePicker(setOf("image/jpg", "video/mp4", "text/plain")),
                    AttachmentSourceSpecification.VisualMediaPicker(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
                )
            )
    }

    @Test
    fun `getAvailableSourcesSpecifications() pickers not preset when mimeTypes are missing`(){
        val extensions = setOf("jpg", "mp4", "txt")
        val context = getContextMock(true)
        every { mimeTypeMap.getMimeTypeFromExtension(any()) } returns null

        val specifications = InAppChatAttachment.getAvailableSourcesSpecifications(context, extensions)

        Assertions.assertThat(specifications)
            .describedAs("No pickers specifications available")
            .isEqualTo(
                setOf(
                    AttachmentSourceSpecification.Camera("jpg"),
                    AttachmentSourceSpecification.VideoRecorder("mp4")
                )
            )
    }

    @Test
    fun `getAvailableSourcesSpecifications() only images`(){
        val extensions = setOf("jpg", "png")
        val context = getContextMock(true)
        every { mimeTypeMap.getMimeTypeFromExtension("jpg") } returns "image/jpg"
        every { mimeTypeMap.getMimeTypeFromExtension("png") } returns "image/png"

        val specifications = InAppChatAttachment.getAvailableSourcesSpecifications(context, extensions)

        Assertions.assertThat(specifications)
            .describedAs("Only image specifications available")
            .isEqualTo(
                setOf(
                    AttachmentSourceSpecification.Camera("jpg"),
                    AttachmentSourceSpecification.FilePicker(setOf("image/jpg", "image/png")),
                    AttachmentSourceSpecification.VisualMediaPicker(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            )
    }

    @Test
    fun `getAvailableSourcesSpecifications() preferred photo extension is picked for camera spec`(){
        val extensions = setOf("jpeg", "bmp", "jpg", "png")
        val context = getContextMock(true)
        every { mimeTypeMap.getMimeTypeFromExtension("jpeg") } returns "image/jpeg"
        every { mimeTypeMap.getMimeTypeFromExtension("bmp") } returns "image/bmp"
        every { mimeTypeMap.getMimeTypeFromExtension("jpg") } returns "image/jpg"
        every { mimeTypeMap.getMimeTypeFromExtension("png") } returns "image/png"

        val specifications = InAppChatAttachment.getAvailableSourcesSpecifications(context, extensions)

        Assertions.assertThat(specifications)
            .describedAs("Only image specifications available")
            .isEqualTo(
                setOf(
                    AttachmentSourceSpecification.Camera("jpg"),
                    AttachmentSourceSpecification.FilePicker(setOf("image/jpeg", "image/bmp", "image/jpg", "image/png")),
                    AttachmentSourceSpecification.VisualMediaPicker(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            )
    }

    @Test
    fun `getAvailableSourcesSpecifications() only videos`(){
        val extensions = setOf("mp4", "3gp")
        val context = getContextMock(true)
        every { mimeTypeMap.getMimeTypeFromExtension("mp4") } returns "video/mp4"
        every { mimeTypeMap.getMimeTypeFromExtension("3gp") } returns "video/3gp"

        val specifications = InAppChatAttachment.getAvailableSourcesSpecifications(context, extensions)

        Assertions.assertThat(specifications)
            .describedAs("Only video specifications available")
            .isEqualTo(
                setOf(
                    AttachmentSourceSpecification.VideoRecorder("mp4"),
                    AttachmentSourceSpecification.FilePicker(setOf("video/mp4", "video/3gp")),
                    AttachmentSourceSpecification.VisualMediaPicker(ActivityResultContracts.PickVisualMedia.VideoOnly),
                )
            )
    }

    @Test
    fun `getAvailableSourcesSpecifications() preferred video extension is picked for video recorder spec`(){
        val extensions = setOf("webm", "m4v", "mp4", "3gp")
        val context = getContextMock(true)
        every { mimeTypeMap.getMimeTypeFromExtension("webm") } returns "video/webm"
        every { mimeTypeMap.getMimeTypeFromExtension("m4v") } returns "video/m4v"
        every { mimeTypeMap.getMimeTypeFromExtension("mp4") } returns "video/mp4"
        every { mimeTypeMap.getMimeTypeFromExtension("3gp") } returns "video/3gp"

        val specifications = InAppChatAttachment.getAvailableSourcesSpecifications(context, extensions)

        Assertions.assertThat(specifications)
            .describedAs("Only video specifications available")
            .isEqualTo(
                setOf(
                    AttachmentSourceSpecification.VideoRecorder("mp4"),
                    AttachmentSourceSpecification.FilePicker(setOf("video/webm", "video/m4v", "video/mp4", "video/3gp")),
                    AttachmentSourceSpecification.VisualMediaPicker(ActivityResultContracts.PickVisualMedia.VideoOnly),
                )
            )
    }

    @Test
    fun `getAvailableSourcesSpecifications() only non visual media types`(){
        val extensions = setOf("txt", "pdf")
        val context = getContextMock(true)
        every { mimeTypeMap.getMimeTypeFromExtension("txt") } returns "text/plain"
        every { mimeTypeMap.getMimeTypeFromExtension("pdf") } returns "application/pdf"

        val specifications = InAppChatAttachment.getAvailableSourcesSpecifications(context, extensions)

        Assertions.assertThat(specifications)
            .describedAs("Only non visual media specifications available")
            .isEqualTo(
                setOf(
                    AttachmentSourceSpecification.FilePicker(setOf("text/plain", "application/pdf"))
                )
            )
    }
    //endregion

    private fun getContextMock(hasCameraFeature: Boolean): Context {
        val context: Context = mockk()
        every { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) } returns hasCameraFeature
        val cameraManager: CameraManager = mockk()
        every { cameraManager.cameraIdList } returns arrayOf("camera1")
        every { context.getSystemService(Context.CAMERA_SERVICE) } returns cameraManager
        return context
    }

}