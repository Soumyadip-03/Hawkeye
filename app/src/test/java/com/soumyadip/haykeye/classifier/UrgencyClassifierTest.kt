package com.soumyadip.haykeye.classifier

import com.soumyadip.haykeye.data.UrgentMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UrgencyClassifierTest {

    @Test
    fun testCaseInsensitiveKeywordMatching() {
        val msg1 = UrgentMessage(
            provider = "WhatsApp",
            senderName = "John",
            preview = "This is URGENT please respond",
            fullText = "This is URGENT please respond",
            timestamp = 1000L,
        )
        val result1 = UrgencyClassifier.classify(msg1)
        assertTrue(result1.isUrgent)
        assertEquals(UrgencySignal.KEYWORD, result1.matchedSignal)

        val msg2 = UrgentMessage(
            provider = "Gmail",
            senderName = "Boss",
            preview = "Call me as soon as possible",
            fullText = "Please call me as soon as possible",
            timestamp = 1000L,
        )
        val result2 = UrgencyClassifier.classify(msg2)
        assertTrue(result2.isUrgent)
        assertEquals(UrgencySignal.KEYWORD, result2.matchedSignal)
    }

    @Test
    fun testMultilingualKeywordsMatching() {
        // Bengali script
        val msgBengali = UrgentMessage(
            provider = "WhatsApp",
            senderName = "Ami",
            preview = "এখনই ফোন করো, খুব দরকার",
            fullText = "এখনই ফোন করো, খুব দরকার",
            timestamp = 1000L,
        )
        val resultBengali = UrgencyClassifier.classify(msgBengali)
        assertTrue("Bengali keyword should be classified as urgent", resultBengali.isUrgent)
        assertEquals(UrgencySignal.KEYWORD, resultBengali.matchedSignal)

        // Hindi script
        val msgHindi = UrgentMessage(
            provider = "Google Messages",
            senderName = "Rahul",
            preview = "तुरंत मदद चाहिए",
            fullText = "तुरंत मदद चाहिए",
            timestamp = 1000L,
        )
        val resultHindi = UrgencyClassifier.classify(msgHindi)
        assertTrue("Hindi keyword should be classified as urgent", resultHindi.isUrgent)
        assertEquals(UrgencySignal.KEYWORD, resultHindi.matchedSignal)

        // Banglish
        val msgBanglish = UrgentMessage(
            provider = "WhatsApp",
            senderName = "Rono",
            preview = "khub bipode achi help kor",
            fullText = "khub bipode achi help kor",
            timestamp = 1000L,
        )
        val resultBanglish = UrgencyClassifier.classify(msgBanglish)
        assertTrue("Banglish keyword should be classified as urgent", resultBanglish.isUrgent)
        assertEquals(UrgencySignal.KEYWORD, resultBanglish.matchedSignal)

        // Hinglish
        val msgHinglish = UrgentMessage(
            provider = "WhatsApp",
            senderName = "Vikram",
            preview = "abhi call karo jaldi",
            fullText = "abhi call karo jaldi",
            timestamp = 1000L,
        )
        val resultHinglish = UrgencyClassifier.classify(msgHinglish)
        assertTrue("Hinglish keyword should be classified as urgent", resultHinglish.isUrgent)
        assertEquals(UrgencySignal.KEYWORD, resultHinglish.matchedSignal)
    }

    @Test
    fun testNonUrgentOrdinaryMessage() {
        val msg = UrgentMessage(
            provider = "WhatsApp",
            senderName = "Friend",
            preview = "Hey, what are you doing tonight?",
            fullText = "Hey, what are you doing tonight? Let's get coffee tomorrow.",
            timestamp = 1000L,
        )
        val result = UrgencyClassifier.classify(msg)
        assertFalse(result.isUrgent)
        assertEquals(null, result.matchedSignal)
    }

    @Test
    fun testUrgentFollowedByOrdinaryMessageInSameConversation() {
        // Step 1: Urgent message
        val msgUrgent = UrgentMessage(
            provider = "WhatsApp",
            senderName = "Alex",
            preview = "urgent please call me",
            fullText = "urgent please call me",
            timestamp = 1000L,
            dedupKey = "WhatsApp||Alex|1000|urgent please call me",
        )
        val resultUrgent = UrgencyClassifier.classify(msgUrgent)
        assertTrue("First message 'urgent' must be classified as urgent", resultUrgent.isUrgent)
        assertEquals(UrgencySignal.KEYWORD, resultUrgent.matchedSignal)

        // Step 2: Ordinary message from same sender
        val msgOrdinary = UrgentMessage(
            provider = "WhatsApp",
            senderName = "Alex",
            preview = "Okay, thanks",
            fullText = "Okay, thanks",
            timestamp = 2000L,
            dedupKey = "WhatsApp||Alex|2000|Okay, thanks",
        )
        val resultOrdinary = UrgencyClassifier.classify(msgOrdinary)
        assertFalse("Subsequent ordinary message 'Okay, thanks' must NOT be classified as urgent", resultOrdinary.isUrgent)
        assertNull("Subsequent ordinary message must have null matchedSignal", resultOrdinary.matchedSignal)
    }

    @Test
    fun testUpdatedMessagingStyleNotificationWithUrgentAndOrdinary() {
        // Simulated MessagingStyle notification buffer containing two messages from Alex
        val msg1 = UrgentMessage(
            provider = "WhatsApp",
            senderName = "Alex",
            preview = "urgent",
            fullText = "urgent",
            timestamp = 1000L,
            dedupKey = "WhatsApp||Alex|1000|urgent",
        )
        val msg2 = UrgentMessage(
            provider = "WhatsApp",
            senderName = "Alex",
            preview = "Okay, thanks",
            fullText = "Okay, thanks",
            timestamp = 2000L,
            dedupKey = "WhatsApp||Alex|2000|Okay, thanks",
        )

        val buffer = listOf(msg1, msg2)

        val classifications = buffer.map { msg -> msg to UrgencyClassifier.classify(msg) }

        // msg1 ("urgent") is urgent
        assertTrue(classifications[0].second.isUrgent)
        // msg2 ("Okay, thanks") is NOT urgent
        assertFalse(classifications[1].second.isUrgent)
    }

    @Test
    fun testSecondUrgentMessageAfterOrdinaryMessage() {
        val msgOrdinary = UrgentMessage(
            provider = "Google Messages",
            senderName = "Contact",
            preview = "See you tomorrow",
            fullText = "See you tomorrow",
            timestamp = 1000L,
        )
        val resultOrdinary = UrgencyClassifier.classify(msgOrdinary)
        assertFalse(resultOrdinary.isUrgent)

        val msgUrgent = UrgentMessage(
            provider = "Google Messages",
            senderName = "Contact",
            preview = "Emergency call me ASAP",
            fullText = "Emergency call me ASAP",
            timestamp = 2000L,
        )
        val resultUrgent = UrgencyClassifier.classify(msgUrgent)
        assertTrue("Subsequent urgent message must be classified as urgent", resultUrgent.isUrgent)
        assertEquals(UrgencySignal.KEYWORD, resultUrgent.matchedSignal)
    }

    @Test
    fun testRepeatedNotificationUpdatesDeduplication() {
        val msg1 = UrgentMessage(
            provider = "WhatsApp",
            senderName = "Alex",
            preview = "urgent meeting",
            fullText = "urgent meeting",
            timestamp = 1000L,
            dedupKey = "WhatsApp||Alex|1000|urgent meeting",
        )

        val msg2 = UrgentMessage(
            provider = "WhatsApp",
            senderName = "Alex",
            preview = "urgent meeting",
            fullText = "urgent meeting",
            timestamp = 1000L,
            dedupKey = "WhatsApp||Alex|1000|urgent meeting",
        )

        assertEquals("Reprocessed messages from notification updates must have identical dedupKeys", msg1.dedupKey, msg2.dedupKey)
    }

    @Test
    fun testAvoidsAccidentalSubstringMatches() {
        // "detergent" contains "urgent"
        val msg1 = UrgentMessage(
            provider = "WhatsApp",
            senderName = "Store",
            preview = "Buy laundry detergent on the way home",
            fullText = "Buy laundry detergent on the way home",
            timestamp = 1000L,
        )
        val result1 = UrgencyClassifier.classify(msg1)
        assertFalse("Substrings like detergent must not trigger keyword matches", result1.isUrgent)

        // "passages" or "passengers"
        val msg2 = UrgentMessage(
            provider = "Google Messages",
            senderName = "Bus Co",
            preview = "Bus for passengers departs at 5 PM",
            fullText = "Bus for passengers departs at 5 PM",
            timestamp = 1000L,
        )
        val result2 = UrgencyClassifier.classify(msg2)
        assertFalse("Substrings like passengers must not trigger ASAP matches", result2.isUrgent)
    }

    @Test
    fun testProviderScopedVipSenderMatching() {
        val config = ClassifierConfig(
            vipSenders = mapOf("WhatsApp" to setOf("Alex")),
        )

        val msgWhatsApp = UrgentMessage(
            provider = "WhatsApp",
            senderName = "Alex",
            preview = "Hey there",
            fullText = "Hey there",
            timestamp = 1000L,
        )
        val resultWhatsApp = UrgencyClassifier.classify(msgWhatsApp, config)
        assertTrue(resultWhatsApp.isUrgent)
        assertEquals(UrgencySignal.VIP_SENDER, resultWhatsApp.matchedSignal)

        val msgGmail = UrgentMessage(
            provider = "Gmail",
            senderName = "Alex",
            preview = "Hey there",
            fullText = "Hey there",
            timestamp = 1000L,
        )
        val resultGmail = UrgencyClassifier.classify(msgGmail, config)
        assertFalse("VIP sender on WhatsApp must not mark same name on Gmail as VIP", resultGmail.isUrgent)
    }

    @Test
    fun testProviderScopedVipGroupMatching() {
        val config = ClassifierConfig(
            vipGroups = mapOf("WhatsApp" to setOf("Family Group")),
        )

        val msgGroup = UrgentMessage(
            provider = "WhatsApp",
            senderName = "Bob",
            groupName = "Family Group",
            preview = "Dinner at 8",
            fullText = "Dinner at 8",
            timestamp = 1000L,
        )
        val result = UrgencyClassifier.classify(msgGroup, config)
        assertTrue(result.isUrgent)
        assertEquals(UrgencySignal.VIP_GROUP, result.matchedSignal)
    }

    @Test
    fun testMentionAsUrgencySignal() {
        val msg = UrgentMessage(
            provider = "WhatsApp",
            senderName = "Member",
            groupName = "General Chat",
            preview = "Hey check this out",
            fullText = "Hey check this out",
            timestamp = 1000L,
            wasMentioned = true,
        )
        val result = UrgencyClassifier.classify(msg)
        assertTrue(result.isUrgent)
        assertEquals(UrgencySignal.MENTION, result.matchedSignal)
    }

    @Test
    fun testTextHeuristics() {
        val msgExclamation = UrgentMessage(
            provider = "WhatsApp",
            senderName = "Friend",
            preview = "Look at this!!",
            fullText = "Look at this!!",
            timestamp = 1000L,
        )
        val resultExclamation = UrgencyClassifier.classify(msgExclamation)
        assertTrue(resultExclamation.isUrgent)
        assertEquals(UrgencySignal.TEXT_HEURISTIC, resultExclamation.matchedSignal)

        val msgCaps = UrgentMessage(
            provider = "Google Messages",
            senderName = "Alert",
            preview = "PLEASE READ THIS NOW",
            fullText = "PLEASE READ THIS NOW",
            timestamp = 1000L,
        )
        val resultCaps = UrgencyClassifier.classify(msgCaps)
        assertTrue(resultCaps.isUrgent)
        assertEquals(UrgencySignal.TEXT_HEURISTIC, resultCaps.matchedSignal)
    }

    @Test
    fun testSubjectlessGmailMessages() {
        val urgentSubjectless = UrgentMessage(
            provider = "Gmail",
            senderName = "Support",
            preview = "ASAP need server access",
            fullText = "ASAP need server access",
            timestamp = 1000L,
        )
        val urgentResult = UrgencyClassifier.classify(urgentSubjectless)
        assertTrue("Subjectless email with ASAP should be classified as urgent", urgentResult.isUrgent)

        val ordinarySubjectless = UrgentMessage(
            provider = "Gmail",
            senderName = "Newsletter",
            preview = "No subject",
            fullText = "No subject",
            timestamp = 1000L,
        )
        val ordinaryResult = UrgencyClassifier.classify(ordinarySubjectless)
        assertFalse("Subjectless email with ordinary text should NOT be classified as urgent", ordinaryResult.isUrgent)
    }
}
