package com.thanhnha.universalacremote.ir

enum class VerificationCheck { POWER, TEMPERATURE_CHANGED, MODE, FAN, SWING_VERTICAL, SWING_HORIZONTAL }
enum class ScanResult { FULL_MATCH, PARTIAL_MATCH, NO_MATCH }
enum class ScanState { READY, TRANSMITTING, AWAITING_FEEDBACK, VERIFYING, MATCHED, STOPPED, ERROR, COMPLETE }
enum class VerificationStatus { VERIFIED, FAILED, SKIPPED, UNSUPPORTED }

/** Safe, UI-driven scanner: a candidate is transmitted once, then waits for explicit user action. */
class UniversalAcScanner(
    val candidates: List<RemoteCandidate>,
    private val minimumDelayMillis: Long = 1_000,
    private val transmit: (RemoteCandidate) -> Unit,
) {
    var state: ScanState = ScanState.READY
        private set
    var cursor: Int = 0
        private set
    var selected: RemoteCandidate? = null
        private set
    var verifiedCapabilities: Set<VerificationCheck> = emptySet()
        private set
    var verificationStatuses: Map<VerificationCheck, VerificationStatus> = emptyMap()
        private set
    var result: ScanResult? = null
        private set
    private var lastSentAt: Long? = null

    init { require(minimumDelayMillis >= 0) }

    fun tryCurrent(nowMillis: Long) {
        check(state == ScanState.READY) { "Scanner is not ready for a transmission." }
        check(cursor < candidates.size) { "No candidate remains." }
        val previous = lastSentAt
        require(previous == null || nowMillis - previous >= minimumDelayMillis) { "Please wait before transmitting again." }
        transmit(candidates[cursor])
        lastSentAt = nowMillis
        state = ScanState.AWAITING_FEEDBACK
    }

    fun reportNoReaction() {
        check(state == ScanState.AWAITING_FEEDBACK)
        cursor++
        state = if (cursor < candidates.size) ScanState.READY else ScanState.COMPLETE
        if (state == ScanState.COMPLETE) result = ScanResult.NO_MATCH
    }

    /** UI timeout is an explicit negative response; scanner never schedules transmissions itself. */
    fun onFeedbackTimeout() = reportNoReaction()

    fun reportReaction() {
        check(state == ScanState.AWAITING_FEEDBACK)
        selected = candidates[cursor]
        verifiedCapabilities = emptySet()
        verificationStatuses = emptyMap()
        state = ScanState.VERIFYING
    }

    fun recordVerification(check: VerificationCheck, supported: Boolean = true) {
        check(state == ScanState.VERIFYING)
        val candidateSupportsCheck = selected?.let { check in RemoteControls.from(it).verificationRequirements() } == true
        val status = when {
            !candidateSupportsCheck -> VerificationStatus.UNSUPPORTED
            supported -> VerificationStatus.VERIFIED
            else -> VerificationStatus.FAILED
        }
        verificationStatuses = verificationStatuses + (check to status)
        if (supported && candidateSupportsCheck) verifiedCapabilities = verifiedCapabilities + check
    }

    fun skipVerification(check: VerificationCheck) {
        check(state == ScanState.VERIFYING)
        val candidateSupportsCheck = selected?.let { check in RemoteControls.from(it).verificationRequirements() } == true
        verificationStatuses = verificationStatuses + (check to if (candidateSupportsCheck) VerificationStatus.SKIPPED else VerificationStatus.UNSUPPORTED)
    }

    fun nextVerificationCheck(): VerificationCheck? {
        check(state == ScanState.VERIFYING)
        return RemoteControls.from(selected!!).verificationOrder().firstOrNull { it !in verificationStatuses }
    }

    fun finishVerification(): ScanResult {
        check(state == ScanState.VERIFYING)
        result = RemoteControls.from(selected!!).verificationResult(verifiedCapabilities)
        state = ScanState.COMPLETE
        return result!!
    }

    fun continueAfterNoMatch(): Boolean {
        check(state == ScanState.COMPLETE && result == ScanResult.NO_MATCH) { "There is no failed verification to continue from." }
        return continueAfterResult()
    }

    fun continueAfterResult(): Boolean {
        check(state == ScanState.COMPLETE) { "There is no completed candidate to continue from." }
        cursor++
        selected = null
        verifiedCapabilities = emptySet()
        verificationStatuses = emptyMap()
        result = null
        state = if (cursor < candidates.size) ScanState.READY else ScanState.COMPLETE
        if (state == ScanState.COMPLETE) result = ScanResult.NO_MATCH
        return state == ScanState.READY
    }

    fun stop() {
        if (state != ScanState.COMPLETE) state = ScanState.STOPPED
    }
}
