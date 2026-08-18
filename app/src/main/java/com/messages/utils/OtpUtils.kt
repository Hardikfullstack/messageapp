package com.messages.utils

// Same keyword set SmsRepository.determineCategory() uses to classify a message as OTP — kept in
// sync so "this message got the OTP category" and "this message has a code we can extract" agree.
private val otpKeywordRegex = Regex("\\b(otp|one time password|verification code)\\b", RegexOption.IGNORE_CASE)
private val otpCodeRegex = Regex("\\b\\d{4,8}\\b")

/**
 * Pulls the standalone 4-8 digit code out of an OTP message body, e.g. "123456 is your OTP" ->
 * "123456". Only looks for a code at all when an OTP-ish keyword is present, so a random message
 * that happens to contain a lone number (a date, a short amount) doesn't get treated as one.
 * Returns null if there's no OTP keyword or no matching digit run.
 */
fun extractOtpCode(body: String): String? {
    if (!otpKeywordRegex.containsMatchIn(body)) return null
    return otpCodeRegex.find(body)?.value
}
