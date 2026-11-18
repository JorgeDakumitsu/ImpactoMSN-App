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
            metadata.getString(file.name, null)?.takeIf(::validShortcut)?.let {
                entries[it] = shortcut(it, file)
            }
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
        val key = sha1("$owner\n${normalized.lowercase(Locale.ROOT)}")
        val file = File(context.filesDir, "$DIRECTORY/$key.png")
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
        context.getSharedPreferences("impactomsn_custom_emoticons", Context.MODE_PRIVATE)
            .edit().putString(file.name, normalized).apply()
        return shortcut(normalized, file).also {
            entries[normalized.lowercase(Locale.ROOT)] = it
            if (normalized.startsWith("[") && normalized.endsWith("]")) {
                entries[normalized.substring(1, normalized.length - 1).lowercase(Locale.ROOT)] = it
            } else {
                entries["[$normalized]".lowercase(Locale.ROOT)] = it
            }
            mutableVersion.value++
        }
    }

    fun add(context: Context, metadata: MsnDisplayPicture, bytes: ByteArray): MsnEmoticonShortcut? {
        val candidates = buildList {
            MsnBase64.decode(metadata.friendly)?.toString(Charsets.UTF_16LE)
                ?.trimEnd('\u0000')?.let(::add)
            metadata.extraAttributes.values.forEach(::add)
            metadata.location.takeUnless { it.equals("TFR2C.tmp", true) || it.equals("TFR2C2.tmp", true) }
                ?.let(::add)
        }
        val token = candidates.firstOrNull { validShortcut(it) } ?: return null
        return add(context, token, bytes, metadata.creator)
    }

    fun all(context: Context): List<MsnEmoticonShortcut> {
        load(context)
        return entries.values.toList()
    }

    private fun shortcut(token: String, file: File) = MsnEmoticonShortcut(
        name = "Custom $token",
        shortcut = token,
        originalAsset = file.absolutePath,
        unicode = "",
        animated = false,
        menuToken = '\u0000',
        alternateShortcuts = listOfNotNull(
            token.takeIf { it.startsWith("[") && it.endsWith("]") }
                ?.substring(1, token.length - 1),
            token.takeIf { !it.startsWith("[") }?.let { "[$it]" },
        ),
    )

    private fun validShortcut(value: String): Boolean =
        value.isNotBlank() && value.length <= 64 &&
            value.none { it == '\r' || it == '\n' || it.isWhitespace() }

    private fun sha1(value: String): String =
        MessageDigest.getInstance("SHA-1").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}
