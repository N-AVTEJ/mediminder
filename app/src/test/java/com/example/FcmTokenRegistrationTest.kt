package com.example

import com.example.data.firebase.FirebaseSyncRepository
import com.example.notifications.FcmTokenManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FcmTokenRegistrationTest {

    @Test
    fun `test register FCM token updates currentUser doc with fcmToken and fcm_token`() {
        val repo = FirebaseSyncRepository.getInstance()
        val testToken = "test_fcm_token_xyz_12345"

        repo.registerFcmToken(testToken)

        val currentUser = repo.currentUser.value
        assertEquals(testToken, currentUser.fcmToken)
        assertEquals(testToken, currentUser.fcm_token)
    }

    @Test
    fun `test registerUserOnSignup includes fcmToken in created user document`() {
        val repo = FirebaseSyncRepository.getInstance()
        val uid = "user_fcm_signup_99"
        val name = "Sophia Martinez"
        val phone = "+1 (555) 987-6543"
        val customToken = "fcm_signup_token_9999"

        val user = repo.writeUserOnSignup(
            uid = uid,
            name = name,
            phone = phone,
            fcmToken = customToken
        )

        assertEquals(uid, user.uid)
        assertEquals(name, user.name)
        assertEquals(phone, user.phone)
        assertEquals(customToken, user.fcmToken)
        assertEquals(customToken, user.fcm_token)

        val storedUser = repo.currentUser.value
        assertEquals(customToken, storedUser.fcmToken)
        assertEquals(customToken, storedUser.fcm_token)
    }

    @Test
    fun `test FcmTokenManager registerDeviceFcmToken saves token to user doc`() {
        val repo = FirebaseSyncRepository.getInstance()
        val customToken = "device_fcm_registered_token_abc"

        val registeredToken = FcmTokenManager.registerDeviceFcmToken(customToken = customToken)

        assertEquals(customToken, registeredToken)
        assertEquals(customToken, repo.currentUser.value.fcmToken)
        assertEquals(customToken, repo.currentUser.value.fcm_token)
    }
}
