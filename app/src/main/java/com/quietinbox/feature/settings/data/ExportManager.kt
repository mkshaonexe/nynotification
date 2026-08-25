package com.quietinbox.feature.settings.data

import android.content.ContentResolver
import android.net.Uri
import com.quietinbox.data.db.entity.NotificationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles streaming export of notification history in JSON and CSV formats
 * directly to a user-specified destination without buffering all rows in memory.
 */
@Singleton
class ExportManager @Inject constructor(
    private val settingsDao: SettingsDao
) {

    companion object {
        const val CHUNK_SIZE = 500
    }

    /**
     * Streams JSON export to the specified [uri] using [contentResolver].
     */
    suspend fun exportJsonToUri(contentResolver: ContentResolver, uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                exportJsonToStream(outputStream)
            } ?: throw IllegalStateException("Could not open output stream for $uri")
        }
    }

    /**
     * Streams CSV export to the specified [uri] using [contentResolver].
     */
    suspend fun exportCsvToUri(contentResolver: ContentResolver, uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                exportCsvToStream(outputStream)
            } ?: throw IllegalStateException("Could not open output stream for $uri")
        }
    }

    /**
     * Streams all notifications to [outputStream] in formatted JSON array format.
     * Returns total exported row count.
     */
    suspend fun exportJsonToStream(outputStream: OutputStream): Int = withContext(Dispatchers.IO) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream, Charsets.UTF_8))
        var offset = 0
        var totalExported = 0
        var firstItem = true

        writer.write("[\n")

        while (true) {
            val chunk = settingsDao.getNotificationsChunk(limit = CHUNK_SIZE, offset = offset)
            if (chunk.isEmpty()) break

            for (item in chunk) {
                if (!firstItem) {
                    writer.write(",\n")
                } else {
                    firstItem = false
                }
                writer.write(formatNotificationAsJson(item, indent = "  "))
                totalExported++
            }
            offset += chunk.size
            writer.flush()
        }

        writer.write("\n]\n")
        writer.flush()
        totalExported
    }

    /**
     * Streams all notifications to [outputStream] in RFC 4180 compliant CSV format.
     * Returns total exported row count.
     */
    suspend fun exportCsvToStream(outputStream: OutputStream): Int = withContext(Dispatchers.IO) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream, Charsets.UTF_8))
        var offset = 0
        var totalExported = 0

        // CSV Header
        val header = listOf(
            "id", "sbnKey", "packageName", "appLabel", "title", "body", "subText",
            "senderName", "senderDigits", "channelId", "androidCategory", "importance",
            "signalClass", "contentHash", "firstSeenAt", "lastSeenAt", "endedAt",
            "updateCount", "wasRateLimited", "isSeen", "isStarred", "removalReason"
        ).joinToString(separator = ",", postfix = "\r\n")

        writer.write(header)

        while (true) {
            val chunk = settingsDao.getNotificationsChunk(limit = CHUNK_SIZE, offset = offset)
            if (chunk.isEmpty()) break

            for (item in chunk) {
                val row = listOf(
                    item.id.toString(),
                    escapeCsv(item.sbnKey),
                    escapeCsv(item.packageName),
                    escapeCsv(item.appLabel),
                    escapeCsv(item.title),
                    escapeCsv(item.body),
                    escapeCsv(item.subText),
                    escapeCsv(item.senderName),
                    escapeCsv(item.senderDigits),
                    escapeCsv(item.channelId),
                    escapeCsv(item.androidCategory),
                    item.importance?.toString() ?: "",
                    escapeCsv(item.signalClass),
                    escapeCsv(item.contentHash),
                    item.firstSeenAt.toString(),
                    item.lastSeenAt.toString(),
                    item.endedAt?.toString() ?: "",
                    item.updateCount.toString(),
                    if (item.wasRateLimited) "1" else "0",
                    if (item.isSeen) "1" else "0",
                    if (item.isStarred) "1" else "0",
                    item.removalReason?.toString() ?: ""
                ).joinToString(separator = ",", postfix = "\r\n")

                writer.write(row)
                totalExported++
            }
            offset += chunk.size
            writer.flush()
        }

        writer.flush()
        totalExported
    }

    /**
     * Formats a single [NotificationEntity] as a JSON object string.
     */
    fun formatNotificationAsJson(n: NotificationEntity, indent: String = ""): String {
        val i2 = "$indent  "
        val sb = StringBuilder()
        sb.append(indent).append("{\n")
        sb.append(i2).append("\"id\": ").append(n.id).append(",\n")
        sb.append(i2).append("\"sbnKey\": ").append(jsonString(n.sbnKey)).append(",\n")
        sb.append(i2).append("\"packageName\": ").append(jsonString(n.packageName)).append(",\n")
        sb.append(i2).append("\"appLabel\": ").append(jsonString(n.appLabel)).append(",\n")
        sb.append(i2).append("\"title\": ").append(jsonNullableString(n.title)).append(",\n")
        sb.append(i2).append("\"body\": ").append(jsonNullableString(n.body)).append(",\n")
        sb.append(i2).append("\"subText\": ").append(jsonNullableString(n.subText)).append(",\n")
        sb.append(i2).append("\"senderName\": ").append(jsonNullableString(n.senderName)).append(",\n")
        sb.append(i2).append("\"senderDigits\": ").append(jsonNullableString(n.senderDigits)).append(",\n")
        sb.append(i2).append("\"channelId\": ").append(jsonNullableString(n.channelId)).append(",\n")
        sb.append(i2).append("\"androidCategory\": ").append(jsonNullableString(n.androidCategory)).append(",\n")
        sb.append(i2).append("\"importance\": ").append(n.importance ?: "null").append(",\n")
        sb.append(i2).append("\"signalClass\": ").append(jsonString(n.signalClass)).append(",\n")
        sb.append(i2).append("\"contentHash\": ").append(jsonString(n.contentHash)).append(",\n")
        sb.append(i2).append("\"firstSeenAt\": ").append(n.firstSeenAt).append(",\n")
        sb.append(i2).append("\"lastSeenAt\": ").append(n.lastSeenAt).append(",\n")
        sb.append(i2).append("\"endedAt\": ").append(n.endedAt ?: "null").append(",\n")
        sb.append(i2).append("\"updateCount\": ").append(n.updateCount).append(",\n")
        sb.append(i2).append("\"wasRateLimited\": ").append(n.wasRateLimited).append(",\n")
        sb.append(i2).append("\"isSeen\": ").append(n.isSeen).append(",\n")
        sb.append(i2).append("\"isStarred\": ").append(n.isStarred).append(",\n")
        sb.append(i2).append("\"removalReason\": ").append(n.removalReason ?: "null").append("\n")
        sb.append(indent).append("}")
        return sb.toString()
    }

    private fun jsonString(value: String): String {
        return buildString {
            append('"')
            for (c in value) {
                when (c) {
                    '"' -> append("\\\"")
                    '\\' -> append("\\\\")
                    '\b' -> append("\\b")
                    '\u000C' -> append("\\f")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> {
                        if (c.code < 0x20) {
                            append(String.format("\\u%04x", c.code))
                        } else {
                            append(c)
                        }
                    }
                }
            }
            append('"')
        }
    }

    private fun jsonNullableString(value: String?): String {
        return if (value == null) "null" else jsonString(value)
    }

    /**
     * Escapes a value for CSV: if it contains commas, newlines, or quotes,
     * wraps in quotes and doubles any existing internal quotes.
     */
    fun escapeCsv(value: String?): String {
        if (value == null) return ""
        val containsSpecial = value.contains(',') ||
                value.contains('"') ||
                value.contains('\n') ||
                value.contains('\r')
        return if (containsSpecial) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
