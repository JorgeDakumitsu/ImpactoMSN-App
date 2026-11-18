package com.impactomsn.app

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Locale
import java.security.MessageDigest
import com.impactomsn.app.protocol.MsnDisplayPicture
import com.impactomsn.app.protocol.MsnBase64
import com.impactomsn.app.data.LocalDisplayPicture
import android.util.Log

/** Stores MSN type-2 objects received from Windows clients. */
internal object CustomEmoticonCatalog {
    private const val DIRECTORY = "custom_emoticons"
    private val mutableVersion = MutableStateFlow(0)
    val version: StateFlow<Int> = mutableVersion.asStateFlow()
    private val entries = LinkedHashMap<String, MsnEmoticonShortcut>()
    private var loadedPath: String? = null

    @Synchronized
    fun load(context: Context) {
        val directory = File(context.filesDir, DIRECTORY)
        if (loadedPath == directory.absolutePath) return
        loadedPath = directory.absolutePath
        entries.clear()
        val metadata = context.getSharedPreferences("impactomsn_custom_emoticons", Context.MODE_PRIVATE)
        directory.listFiles()?.filter(File::isFile)?.forEach { file ->
            metadata.getString(file.name, null)
                ?.split('\u001f')
                ?.filter(::validShortcut)
                ?.takeIf { it.isNotEmpty() }
                ?.let { aliases -> registerAliases(aliases, file) }
        }
        mutableVersion.value++
    }

    @Synchronized
    fun add(
        context: Context,
        token: String,
        bytes: ByteArray,
        owner: String = "",
    ): MsnEmoticonShortcut? {
        val normalized = token.trim().takeIf(::validShortcut) ?: return null
        if (bytes.isEmpty()) return null
        load(context)
        val key = sha1(bytes)
        val file = File(context.filesDir, "$DIRECTORY/$key.png")
        file.parentFile?.mkdirs()
        if (!file.exists()) file.writeBytes(bytes)
        val preferences = context.getSharedPreferences("impactomsn_custom_emoticons", Context.MODE_PRIVATE)
        val aliases = (preferences.getString(file.name, null)?.split('\u001f').orEmpty() +
            normalized).filter(::validShortcut).distinctBy { it.lowercase(Locale.ROOT) }
        preferences.edit().putString(file.name, aliases.joinToString("\u001f")).apply()
        registerAliases(aliases, file)
        return entries[normalized.lowercase(Locale.ROOT)]
    }

    fun add(context: Context, metadata: MsnDisplayPicture, bytes: ByteArray): MsnEmoticonShortcut? {
        val candidates = buildList {
            MsnBase64.decode(metadata.friendly)?.toString(Charsets.UTF_16LE)
                ?.trimEnd('\u0000')?.let(::add)
            metadata.extraAttributes.values.forEach(::add)
            metadata.location.takeUnless { it.equals("TFR2C.tmp", true) || it.equals("TFR2C2.tmp", true) }
                ?.let(::add)
        }
        if (candidates.none(::validShortcut)) {
            Log.w("CustomEmoticon", "Objeto tipo 2 recibido sin alias reconocible")
            return null
        }
        Log.d("CustomEmoticon", "Guardando alias=${candidates.filter(::validShortcut)} bytes=${bytes.size}")
        var result: MsnEmoticonShortcut? = null
        candidates.filter(::validShortcut).forEach { token ->
            result = add(context, token, bytes, metadata.creator) ?: result
        }
        return result
    }

    fun all(context: Context): List<MsnEmoticonShortcut> {
        load(context)
        return entries.values.toList()
    }

    fun announcements(context: Context, creator: String): List<Pair<String, MsnDisplayPicture>> {
        load(context)
        val preferences = context.getSharedPreferences("impactomsn_custom_emoticons", Context.MODE_PRIVATE)
        return File(context.filesDir, DIRECTORY).listFiles().orEmpty().filter(File::isFile).flatMap { file ->
            val bytes = runCatching { file.readBytes() }.getOrNull() ?: return@flatMap emptyList()
            preferences.getString(file.name, null).orEmpty().split('\u001f')
                .filter(::validShortcut).map { token ->
                    token to MsnDisplayPicture.create(creator, bytes, 2, location = "0")
                        .copy(extraAttributes = mapOf("Shortcut" to token))
                }
        }
    }

    fun localObject(context: Context, metadata: MsnDisplayPicture): LocalDisplayPicture? {
        val file = File(context.filesDir, "$DIRECTORY/${sha1FromMsn(metadata.sha1d)}.png")
        if (!file.isFile) {
            val match = File(context.filesDir, DIRECTORY).listFiles().orEmpty().firstOrNull {
                it.nameWithoutExtension.equals(sha1FromMsn(metadata.sha1d), true)
            } ?: return null
            return LocalDisplayPicture(metadata, match.readBytes())
        }
        return LocalDisplayPicture(metadata, file.readBytes())
    }

    private fun shortcut(token: String, file: File, aliases: List<String> = emptyList()) = MsnEmoticonShortcut(
        name = "Custom $token",
        shortcut = token,
        originalAsset = file.absolutePath,
        unicode = "",
        animated = false,
        menuToken = '\u0000',
        alternateShortcuts = (aliases + listOfNotNull(
            token.takeIf { it.startsWith("[") && it.endsWith("]") }
                ?.substring(1, token.length - 1),
            token.takeIf { !it.startsWith("[") }?.let { "[$it]" },
        )).distinctBy { it.lowercase(Locale.ROOT) },
    )

    @Synchronized
    private fun registerAliases(aliases: List<String>, file: File) {
        val normalizedAliases = aliases.filter(::validShortcut).distinctBy { it.lowercase(Locale.ROOT) }
        if (normalizedAliases.isEmpty()) return
        val item = shortcut(normalizedAliases.first(), file, normalizedAliases.drop(1))
        normalizedAliases.flatMap { token ->
            listOf(
                token,
                if (token.startsWith("[") && token.endsWith("]")) {
                    token.substring(1, token.length - 1)
                } else {
                    "[$token]"
                },
            )
        }.forEach { token ->
            entries[token.lowercase(Locale.ROOT)] = item
        }
        mutableVersion.value++
    }

    private fun validShortcut(value: String): Boolean =
        value.isNotBlank() && value.length <= 64 &&
            value.none { it == '\r' || it == '\n' || it.isWhitespace() }

    private fun sha1(value: String): String =
        MessageDigest.getInstance("SHA-1").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun sha1(value: ByteArray): String =
        MessageDigest.getInstance("SHA-1").digest(value).joinToString("") { "%02x".format(it) }

    private fun sha1FromMsn(value: String): String =
        runCatching {
            // SHA1D is the base64-encoded digest itself, not the source bytes.
            MsnBase64.decode(value)?.joinToString("") { "%02x".format(it) } ?: ""
        }.getOrDefault("")
}
