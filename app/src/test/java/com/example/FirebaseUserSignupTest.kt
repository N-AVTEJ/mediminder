package com.example

import com.example.data.firebase.FirebaseSyncRepository
import org.junit.Assert.*
import org.junit.Test

class FirebaseUserSignupTest {

    @Test
    fun `test writing user document on signup creates required fields`() {
        val repo = FirebaseSyncRepository.getInstance()
        val uid = "usr_test_signup_999"
        val name = "Sophia Martinez"
        val phone = "+1 (555) 987-6543"

        val createdUser = repo.writeUserOnSignup(uid, name, phone)

        assertNotNull(createdUser)
        assertEquals(uid, createdUser.uid)
        assertEquals(name, createdUser.name)
        assertEquals(phone, createdUser.phone)
        assertTrue(createdUser.createdAt.isNotBlank())
        assertTrue(createdUser.createdAt.contains("2026"))

        val currentFbUser = repo.currentUser.value
        assertEquals(uid, currentFbUser.uid)
        assertEquals(name, currentFbUser.name)
        assertEquals(phone, currentFbUser.phone)
    }
}
