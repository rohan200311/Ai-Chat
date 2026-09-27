package com.aichat.app.data.local.db

import androidx.room.TypeConverter
import com.aichat.app.domain.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromRole(role: Role): String = role.name
    @TypeConverter
    fun toRole(name: String): Role = Role.valueOf(name)

    @TypeConverter
    fun fromAttachmentType(type: AttachmentType): String = type.name
    @TypeConverter
    fun toAttachmentType(name: String): AttachmentType = AttachmentType.valueOf(name)

    @TypeConverter
    fun fromProviderType(type: ProviderType): String = type.name
    @TypeConverter
    fun toProviderType(name: String): ProviderType = ProviderType.valueOf(name)

    @TypeConverter
    fun fromStringList(list: List<String>): String = json.encodeToString(list)
    @TypeConverter
    fun toStringList(data: String): List<String> = try { json.decodeFromString(data) } catch (_: Exception) { emptyList() }

    @TypeConverter
    fun fromStringMap(map: Map<String, String>): String = json.encodeToString(map)
    @TypeConverter
    fun toStringMap(data: String): Map<String, String> = try { json.decodeFromString(data) } catch (_: Exception) { emptyMap() }

    @TypeConverter
    fun fromAttachments(list: List<Attachment>): String = json.encodeToString(list)
    @TypeConverter
    fun toAttachments(data: String): List<Attachment> = try { json.decodeFromString(data) } catch (_: Exception) { emptyList() }

    @TypeConverter
    fun fromToolCalls(list: List<ToolCall>): String = json.encodeToString(list)
    @TypeConverter
    fun toToolCalls(data: String): List<ToolCall> = try { json.decodeFromString(data) } catch (_: Exception) { emptyList() }
}
