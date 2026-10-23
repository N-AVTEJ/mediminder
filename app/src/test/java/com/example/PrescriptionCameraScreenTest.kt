package com.example

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.screens.PrescriptionCameraScreen
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PrescriptionCameraScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testCameraPreviewScreenAndControlsRender() {
        var photoCapturedUri: Uri? = null
        var isClosed = false

        composeTestRule.setContent {
            PrescriptionCameraScreen(
                onPhotoCaptured = { uri -> photoCapturedUri = uri },
                onClose = { isClosed = true }
            )
        }

        // Verify camera screen is rendered
        composeTestRule.onNodeWithTag("prescription_camera_screen").assertIsDisplayed()

        // Verify top bar buttons
        composeTestRule.onNodeWithTag("camera_back_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("camera_torch_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("camera_switch_btn").assertIsDisplayed()

        // Verify bottom bar buttons: gallery, shutter, and permission dialog buttons
        composeTestRule.onNodeWithTag("camera_gallery_picker_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("camera_capture_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("camera_permission_dialog_btn").assertIsDisplayed()

        // Test Back Button interaction
        composeTestRule.onNodeWithTag("camera_back_btn").performClick()
        assertTrue("onClose callback should be triggered on back click", isClosed)
    }

    @Test
    fun testAllowAllPhotosAlertDialogDisplayAndDismiss() {
        composeTestRule.setContent {
            PrescriptionCameraScreen(
                onPhotoCaptured = {},
                onClose = {}
            )
        }

        // Open permission dialog if not already open
        composeTestRule.onNodeWithTag("camera_permission_dialog_btn").performClick()

        // Verify permission dialog content is displayed
        composeTestRule.onNodeWithTag("permission_alert_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("allow_all_photos_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("allow_while_using_app_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("deny_photo_access_btn").assertIsDisplayed()

        // Test clicking "Allow All Photos" dismisses or proceeds
        composeTestRule.onNodeWithTag("allow_all_photos_btn").performClick()
        composeTestRule.onNodeWithTag("permission_alert_dialog").assertDoesNotExist()
    }

    @Test
    fun testShutterButtonClickTriggersPhotoCapture() {
        var capturedUri: Uri? = null

        composeTestRule.setContent {
            PrescriptionCameraScreen(
                onPhotoCaptured = { uri -> capturedUri = uri },
                onClose = {}
            )
        }

        // Dismiss alert if open so shutter is clickable
        try {
            composeTestRule.onNodeWithTag("allow_all_photos_btn").performClick()
        } catch (ignored: Exception) {}

        // Click shutter button
        composeTestRule.onNodeWithTag("camera_capture_btn").performClick()

        // Verify photo capture was triggered with a valid URI
        assertNotNull("Photo captured URI should not be null", capturedUri)
        assertTrue("Captured URI scheme should be valid", capturedUri.toString().isNotEmpty())
    }
}
