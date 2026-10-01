package com.example

import com.example.data.firebase.FirebaseSyncRepository
import com.example.data.firebase.GuardianInviteService
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class GuardianAcceptFlowTest {

    @Test
    fun `test extracting invite token from various URLs and schemes`() {
        val testToken = "tok_abc123xyz"

        // 1. Standard web URL
        val webUrl = "https://medremind.app/invite?token=$testToken"
        assertEquals(testToken, GuardianInviteService.extractToken(webUrl))

        // 2. HTTP web URL
        val httpUrl = "http://medremind.app/invite?token=$testToken"
        assertEquals(testToken, GuardianInviteService.extractToken(httpUrl))

        // 3. Custom scheme deep link
        val deepLink = "medremind://invite?token=$testToken"
        assertEquals(testToken, GuardianInviteService.extractToken(deepLink))

        // 4. Raw token input
        assertEquals(testToken, GuardianInviteService.extractToken(testToken))
    }

    @Test
    fun `test caregiver web html generation includes token and bridge hooks`() {
        val token = "tok_test_caregiver_88"
        val patientName = "Eleanor Vance"
        val guardianUid = "guardian_user_test_99"

        val html = GuardianInviteService.generateCaregiverWebHtml(
            token = token,
            patientName = patientName,
            guardianPhone = "+1 (555) 777-8888",
            guardianUid = guardianUid,
            isLinked = false
        )

        assertTrue(html.contains(token))
        assertTrue(html.contains(patientName))
        assertTrue(html.contains(guardianUid))
        assertTrue(html.contains("AndroidBridge"))
        assertTrue(html.contains("MedRemind Guardian Portal"))
        assertTrue(html.contains("Confirm & Link as Guardian"))
    }

    @Test
    fun `test guardian accept flow updates status to linked and links guardianUid in Firestore collections`() {
        val repo = FirebaseSyncRepository.getInstance()
        val token = "tok_unique_" + UUID.randomUUID().toString().take(6)
        val initialPhone = "+1 (555) 444-3322"
        val patientId = repo.currentUser.value.id

        // 1. Create pending guardian invite
        val pendingGuardian = repo.addGuardianToFirebase(
            patientId = patientId,
            guardianPhone = initialPhone,
            guardianUid = "temp_unlinked_uid",
            status = "pending",
            name = "Marcus Vance",
            relationship = "Caregiver",
            inviteToken = token
        )

        assertEquals("pending", pendingGuardian.status)
        assertEquals(token, pendingGuardian.inviteToken)

        // 2. Confirm acceptance via token
        val actualGuardianUid = "guardian_active_user_555"
        val linkedGuardian = repo.linkGuardianWithToken(
            token = token,
            guardianUid = actualGuardianUid,
            guardianName = "Marcus Vance",
            guardianPhone = initialPhone
        )

        assertNotNull("Linked guardian should not be null", linkedGuardian)
        assertEquals("linked", linkedGuardian!!.status)
        assertEquals(actualGuardianUid, linkedGuardian.guardianUid)
        assertNotNull(linkedGuardian.linkedAt)
        assertTrue(linkedGuardian.linkedAt!!.isNotBlank())

        // 3. Verify user doc in users collection has guardianUid linked in guardian_ids
        val userGuardianIds = repo.currentUser.value.guardian_ids
        assertTrue("User guardian_ids must include linked guardianUid", userGuardianIds.contains(actualGuardianUid))

        // 4. Verify lookup by token returns updated linked status
        val queried = repo.findGuardianByToken(token)
        assertNotNull(queried)
        assertEquals("linked", queried!!.status)
        assertEquals(actualGuardianUid, queried.guardianUid)
    }
}
