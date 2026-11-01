package com.hermestower.voiceconsole.tower

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant

/**
 * TowerProfileStore (GRAPH 2 §8): metadata-only persistence plus SecretVault for
 * sensitive values. Enforced rules:
 *  - the metadata file NEVER contains a secret (validated by tests);
 *  - export is secret-clean with explicit placeholders;
 *  - import never silently overwrites an active configuration (conflict flow);
 *  - previous configuration is preserved for restore (rollback slot).
 */
interface TowerProfileStore {
    fun load(): TowerConnectionProfile?
    fun state(): ConnectionConfigState
    fun saveDraft(profile: TowerConnectionProfile): TowerProfileWriteResult
    fun saveValidated(profile: TowerConnectionProfile): TowerProfileWriteResult
    fun setSecret(profileId: String, secret: CharArray)
    fun setPairingCode(profileId: String, code: CharArray)
    fun secretFor(profileId: String): CharArray?
    fun clearSensitiveValues(profileId: String): Boolean
    fun exportSecretClean(profileId: String): ExportedProfile
    fun importExported(extracted: ExportedProfile, confirmOverwrite: Boolean): ImportResult
    fun restorePrevious(): TowerProfileWriteResult
    fun resetToMock(): TowerProfileWriteResult
    fun vaultAvailability(): VaultAvailability
}

@Serializable
data class ExportedProfile(
    val exportedFormatVersion: String,
    val profile: TowerConnectionProfile,
    // Required (no defaults): a secret-clean export must ALWAYS carry the explicit
    // placeholder strings, whatever JSON settings the receiver uses.
    val pairingSecret: String,
    val accessToken: String,
)

sealed class ImportResult {
    data class Imported(val profile: TowerConnectionProfile) : ImportResult()
    data class Conflict(val incoming: TowerConnectionProfile, val current: TowerConnectionProfile, val message: String) : ImportResult()
    data class Rejected(val reason: String) : ImportResult()
}

sealed class TowerProfileWriteResult {
    data class Written(val profile: TowerConnectionProfile) : TowerProfileWriteResult()
    data class Rejected(val reason: String) : TowerProfileWriteResult()
}

class FileTowerProfileStore(
    private val metadataFile: Path,
    private val vault: SecretVault,
    private val clock: () -> Instant = { Instant.now() },
) : TowerProfileStore {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
    private var cachedState: ConnectionConfigState = ConnectionConfigState.EMPTY

    init {
        val loaded = readProfile()
        cachedState = if (loaded == null) ConnectionConfigState.EMPTY else ConnectionConfigState.VALIDATED
    }

    override fun load(): TowerConnectionProfile? = readProfile()

    override fun state(): ConnectionConfigState = cachedState

    override fun saveDraft(profile: TowerConnectionProfile): TowerProfileWriteResult {
        val report = TowerProfileValidator.validate(profile, vault.contains(VaultKeys.secretKeyFor(profile.profileId)), clock().toString())
        val stateToStore = if (report.passed) ConnectionConfigState.DRAFT else ConnectionConfigState.INVALID
        writeWithBackup(profile.copy(updatedAt = clock().toString(), lastValidationAt = clock().toString()))
        cachedState = stateToStore
        return TowerProfileWriteResult.Written(profile)
    }

    override fun saveValidated(profile: TowerConnectionProfile): TowerProfileWriteResult {
        val report = TowerProfileValidator.validate(profile, vault.contains(VaultKeys.secretKeyFor(profile.profileId)), clock().toString())
        if (!report.passed) {
            cachedState = ConnectionConfigState.INVALID
            return TowerProfileWriteResult.Rejected(report.issues.joinToString("; ") { it.toString() })
        }
        writeWithBackup(profile.copy(updatedAt = clock().toString(), lastValidationAt = clock().toString()))
        cachedState = ConnectionConfigState.VALIDATED
        return TowerProfileWriteResult.Written(profile)
    }

    override fun setSecret(profileId: String, secret: CharArray) {
        require(secret.isNotEmpty()) { "secret must not be empty" }
        vault.put(VaultKeys.secretKeyFor(profileId), secret)
    }

    override fun setPairingCode(profileId: String, code: CharArray) {
        require(code.isNotEmpty()) { "pairing code must not be empty" }
        vault.put(VaultKeys.pairingKeyFor(profileId), code)
    }

    override fun secretFor(profileId: String): CharArray? =
        vault.get(VaultKeys.secretKeyFor(profileId))
            ?: vault.get(VaultKeys.pairingKeyFor(profileId))

    override fun clearSensitiveValues(profileId: String): Boolean {
        val a = vault.remove(VaultKeys.secretKeyFor(profileId))
        val b = vault.remove(VaultKeys.pairingKeyFor(profileId))
        return a || b
    }

    override fun exportSecretClean(profileId: String): ExportedProfile {
        val profile = readProfile() ?: throw IllegalStateException("No profile to export.")
        require(profile.profileId == profileId) { "profile id mismatch on export" }
        return ExportedProfile(
            exportedFormatVersion = EXPORT_FORMAT_VERSION,
            profile = profile,
            pairingSecret = PLACEHOLDER_PAIRING,
            accessToken = PLACEHOLDER_TOKEN,
        )
    }

    override fun importExported(extracted: ExportedProfile, confirmOverwrite: Boolean): ImportResult {
        if (extracted.exportedFormatVersion != EXPORT_FORMAT_VERSION) {
            return ImportResult.Rejected("Unsupported export format '${extracted.exportedFormatVersion}'.")
        }
        if (extracted.pairingSecret != PLACEHOLDER_PAIRING || extracted.accessToken != PLACEHOLDER_TOKEN) {
            // Refuse to import a file that claims to carry a real secret: secrets never travel in exports.
            return ImportResult.Rejected("Refused: export files must be secret-clean; no real secret is accepted on import.")
        }
        val current = readProfile()
        if (current != null && !confirmOverwrite) {
            val differs = current != extracted.profile
            if (differs) {
                return ImportResult.Conflict(
                    incoming = extracted.profile,
                    current = current,
                    message = "An existing configuration exists. Confirm to replace it (previous configuration is preserved for restore).",
                )
            }
        }
        writeWithBackup(extracted.profile.copy(updatedAt = clock().toString()))
        cachedState = ConnectionConfigState.VALIDATED
        return ImportResult.Imported(extracted.profile)
    }

    override fun restorePrevious(): TowerProfileWriteResult {
        if (!Files.exists(backupFile())) {
            return TowerProfileWriteResult.Rejected("No previous configuration preserved.")
        }
        val backup = readJson(backupFile()) ?: return TowerProfileWriteResult.Rejected("Previous configuration unreadable.")
        writeWithBackup(backup)
        cachedState = ConnectionConfigState.ROLLBACK_AVAILABLE
        return TowerProfileWriteResult.Written(backup)
    }

    override fun resetToMock(): TowerProfileWriteResult {
        val current = readProfile()
        if (current != null) writeWithBackup(current)
        val mock = TowerConnectionProfile.emptyMock().copy(
            createdAt = clock().toString(),
            updatedAt = clock().toString(),
        )
        writeJson(metadataFile, mock)
        cachedState = ConnectionConfigState.VALIDATED
        return TowerProfileWriteResult.Written(mock)
    }

    override fun vaultAvailability(): VaultAvailability = vault.available()

    private fun backupFile(): Path = metadataFile.resolveSibling(metadataFile.fileName.toString() + ".previous")

    private fun writeWithBackup(profile: TowerConnectionProfile) {
        if (Files.exists(metadataFile)) {
            Files.createDirectories(backupFile().parent)
            Files.copy(metadataFile, backupFile(), StandardCopyOption.REPLACE_EXISTING)
        }
        writeJson(metadataFile, profile)
    }

    private fun readProfile(): TowerConnectionProfile? = readJson(metadataFile)

    private fun readJson(path: Path): TowerConnectionProfile? = try {
        if (!Files.exists(path)) null
        else json.decodeFromString(TowerConnectionProfile.serializer(), String(Files.readAllBytes(path), Charsets.UTF_8))
    } catch (_: Exception) {
        null
    }

    private fun writeJson(path: Path, profile: TowerConnectionProfile) {
        Files.createDirectories(path.parent)
        val tmp = path.resolveSibling(path.fileName.toString() + ".tmp")
        Files.write(tmp, json.encodeToString(TowerConnectionProfile.serializer(), profile).toByteArray(Charsets.UTF_8))
        Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    companion object {
        const val EXPORT_FORMAT_VERSION = "hermes.connection.profile.v1"
        const val PLACEHOLDER_PAIRING = "PAIRING_SECRET_REQUIRED"
        const val PLACEHOLDER_TOKEN = "ACCESS_TOKEN_REQUIRED"
    }
}
