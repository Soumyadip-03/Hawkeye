package com.soumyadip.haykeye.classifier

import com.soumyadip.haykeye.data.UrgentMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

        // Alex on WhatsApp -> Urgent
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

        // Alex on Gmail -> Not urgent (since VIP sender is scoped to WhatsApp)
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
        // Multiple exclamation marks
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

        // Excessive uppercase text
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
        // Urgent subjectless Gmail
        val urgentSubjectless = UrgentMessage(
            provider = "Gmail",
            senderName = "Support",
            preview = "ASAP need server access",
            fullText = "ASAP need server access",
            timestamp = 1000L,
        )
        val urgentResult = UrgencyClassifier.classify(urgentSubjectless)
        assertTrue("Subjectless email with ASAP should be classified as urgent", urgentResult.isUrgent)

        // Ordinary subjectless Gmail
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
