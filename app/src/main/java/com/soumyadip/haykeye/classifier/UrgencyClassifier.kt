package com.soumyadip.haykeye.classifier

import com.soumyadip.haykeye.data.UrgentMessage

object UrgencyClassifier {

    val DEFAULT_KEYWORDS = setOf(
        // English
        "urgent",
        "immediate",
        "priority",
        "asap",
        "as soon as possible",
        "emergency",
        "call me",
        "important",
        "immediate help",
        "do it now",
        "right now",
        "you have to complete this",
        "immediately",
        "critical",
        "time-sensitive",

        // Bengali script
        "জরুরি",
        "এখনই সাহায্য দরকার",
        "এখনই ফোন করো",
        "বিপদে আছি",
        "সাহায্য করুন",

        // Hindi script
        "आपातकाल",
        "तुरंत मदद चाहिए",
        "अभी फोन करो",
        "मदद चाहिए",
        "मैं खतरे में हूँ",

        // Banglish
        "khub bipode achi",
        "ekhoni call kor",
        "ekhoni phone kor",
        "ekhoni help dorkar",
        "khub joruri",
        "help kor",
        "dorkar ache bhai",

        // Hinglish
        "abhi call karo",
        "turant madad chahiye",
        "bahut zaroori hai",
        "abhi help chahiye",
        "main khatre mein hoon",
    )

    fun classify(
        message: UrgentMessage,
        config: ClassifierConfig = ClassifierConfig(),
    ): ClassificationResult {
        // 1. Check @mention
        if (message.wasMentioned) {
            return ClassificationResult(
                isUrgent = true,
                matchedSignal = UrgencySignal.MENTION,
                matchedReason = "User was explicitly mentioned in notification",
            )
        }

        // 2. Check provider-scoped VIP Sender
        val providerVipSenders = config.vipSenders[message.provider] ?: emptySet()
        if (providerVipSenders.any { it.equals(message.senderName, ignoreCase = true) }) {
            return ClassificationResult(
                isUrgent = true,
                matchedSignal = UrgencySignal.VIP_SENDER,
                matchedReason = "Sender matches VIP list for provider '${message.provider}'",
            )
        }

        // 3. Check provider-scoped VIP Group
        if (!message.groupName.isNullOrBlank()) {
            val providerVipGroups = config.vipGroups[message.provider] ?: emptySet()
            if (providerVipGroups.any { it.equals(message.groupName, ignoreCase = true) }) {
                return ClassificationResult(
                    isUrgent = true,
                    matchedSignal = UrgencySignal.VIP_GROUP,
                    matchedReason = "Group matches VIP group list for provider '${message.provider}'",
                )
            }
        }

        // 4. Check Keywords (Default + Custom)
        val allKeywords = DEFAULT_KEYWORDS + config.customKeywords
        for (keyword in allKeywords) {
            val trimmedKeyword = keyword.trim()
            if (trimmedKeyword.isBlank()) continue

            val isMatched = if (isAsciiOnly(trimmedKeyword)) {
                val pattern = Regex("""\b${Regex.escape(trimmedKeyword)}\b""", RegexOption.IGNORE_CASE)
                pattern.containsMatchIn(message.fullText) || pattern.containsMatchIn(message.preview)
            } else {
                message.fullText.contains(trimmedKeyword, ignoreCase = true) ||
                        message.preview.contains(trimmedKeyword, ignoreCase = true)
            }

            if (isMatched) {
                return ClassificationResult(
                    isUrgent = true,
                    matchedSignal = UrgencySignal.KEYWORD,
                    matchedReason = "Matched urgency keyword: '$trimmedKeyword'",
                )
            }
        }

        // 5. Check Text Heuristics (if enabled)
        if (config.enableTextHeuristics) {
            val text = message.fullText.ifBlank { message.preview }

            // Heuristic A: Multiple Exclamation Marks ("!!" or "!!!")
            if (text.contains("!!")) {
                return ClassificationResult(
                    isUrgent = true,
                    matchedSignal = UrgencySignal.TEXT_HEURISTIC,
                    matchedReason = "Contains multiple exclamation marks",
                )
            }

            // Heuristic B: Excessive Uppercase Text (length >= 6 and uppercase letters >= 70% of letters)
            val letters = text.filter { it.isLetter() }
            if (letters.length >= 6) {
                val upperCount = letters.count { it.isUpperCase() }
                val upperPercentage = upperCount.toDouble() / letters.length.toDouble()
                if (upperPercentage >= 0.70) {
                    return ClassificationResult(
                        isUrgent = true,
                        matchedSignal = UrgencySignal.TEXT_HEURISTIC,
                        matchedReason = "Excessive uppercase text (${(upperPercentage * 100).toInt()}% uppercase)",
                    )
                }
            }
        }

        return ClassificationResult.NOT_URGENT
    }

    private fun isAsciiOnly(text: String): Boolean = text.all { it.code in 0..127 }
}
