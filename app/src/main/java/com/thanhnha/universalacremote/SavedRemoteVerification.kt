package com.thanhnha.universalacremote

import com.thanhnha.universalacremote.ir.CatalogTransmitter
import com.thanhnha.universalacremote.ir.RemoteCandidate
import com.thanhnha.universalacremote.ir.RemoteControls
import com.thanhnha.universalacremote.ir.VerificationCheck

enum class SavedVerificationState {
    NONE,
    PARTIAL,
    FULL,
}

fun SavedRemote.verifiedChecksSet(): Set<VerificationCheck> =
    verifiedCapabilities.mapNotNull { raw ->
        runCatching { VerificationCheck.valueOf(raw) }.getOrNull()
    }.toSet()

fun SavedRemote.verificationState(candidate: RemoteCandidate?): SavedVerificationState {
    if (importedCommandsJson.isNotBlank()) return SavedVerificationState.NONE
    if (candidate == null || !CatalogTransmitter.supports(candidate)) return SavedVerificationState.NONE

    val verified = verifiedChecksSet()
    if (verified.isEmpty()) return SavedVerificationState.NONE

    val required = RemoteControls.from(candidate).verificationRequirements()
    return if (required.isNotEmpty() && verified.containsAll(required)) {
        SavedVerificationState.FULL
    } else {
        SavedVerificationState.PARTIAL
    }
}
