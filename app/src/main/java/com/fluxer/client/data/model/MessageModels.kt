// =============================================================================
// !! DO NOT TOUCH MODEL DEFAULTS OR ChannelType SERIALIZER !!
// - ChannelType uses a custom KSerializer: API sends integers (0=TEXT,1=DM,2=VOICE,4=CATEGORY)
//   Do NOT revert to a plain @Serializable enum — it will break channel loading
// - Message.authorId, .content, .createdAt default to "" — API omits these in some contexts
// See CLAUDE.md for full details.
// =============================================================================
package com.fluxer.client.data.model

import com.fluxer.client.data.local.model.AuthorEntity
import com.fluxer.client.data.local.model.MessageEntity
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class Message(
    val id: String,
    @SerialName("channel_id")
    val channelId: String,
    @SerialName("author_id")
    val authorId: String = "",
    val author: User? = null,
    val content: String = "",
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("updated_at")
    val updatedAt: String? = null,
    val embeds: List<Embed> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
    val reactions: List<Reaction> = emptyList(),
    val mentions: List<User> = emptyList(),
    @SerialName("mention_everyone")
    val mentionEveryone: Boolean = false,
    @SerialName("mention_roles")
    val mentionRoles: List<String> = emptyList(),
    @SerialName("reply_to_id")
    val replyToId: String? = null,
    @SerialName("reply_to")
    val replyTo: Message? = null,
    @SerialName("is_edited")
    val isEdited: Boolean = false
)

@Serializable
data class Embed(
    val title: String? = null,
    val description: String? = null,
    val url: String? = null,
    val color: Int? = null,
    val image: EmbedImage? = null,
    val thumbnail: EmbedImage? = null,
    val author: EmbedAuthor? = null,
    val footer: EmbedFooter? = null,
    val timestamp: String? = null
)

@Serializable
data class EmbedAuthor(
    val name: String,
    @SerialName("icon_url")
    val iconUrl: String? = null
)

@Serializable
data class EmbedImage(
    val url: String,
    val width: Int? = null,
    val height: Int? = null
)

@Serializable
data class EmbedFooter(
    val text: String,
    @SerialName("icon_url")
    val iconUrl: String? = null
)

@Serializable
data class Attachment(
    val id: String,
    val filename: String,
    val size: Long,
    val url: String,
    @SerialName("content_type")
    val contentType: String? = null,
    val width: Int? = null,
    val height: Int? = null
)

@Serializable
data class Reaction(
    val emoji: Emoji,
    val count: Int,
    @SerialName("user_reacted")
    val userReacted: Boolean = false
)

@Serializable
data class Emoji(
    val id: String? = null,
    val name: String,
    val animated: Boolean = false
)

@Serializable
data class SendMessageRequest(
    val content: String,
    @SerialName("reply_to_id")
    val replyToId: String? = null,
    val embeds: List<Embed> = emptyList()
)

@Serializable
data class Channel(
    val id: String,
    val name: String,
    val type: ChannelType,
    @SerialName("server_id")
    val serverId: String? = null,
    @SerialName("parent_id")
    val parentId: String? = null,
    val position: Int = 0,
    val topic: String? = null,
    @SerialName("last_message_id")
    val lastMessageId: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable(with = ChannelTypeSerializer::class)
enum class ChannelType(val value: Int) {
    TEXT(0), DM(1), VOICE(2), CATEGORY(4), UNKNOWN(-1)
}

object ChannelTypeSerializer : KSerializer<ChannelType> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ChannelType", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: ChannelType) {
        encoder.encodeInt(value.value)
    }

    override fun deserialize(decoder: Decoder): ChannelType {
        val raw = decoder.decodeInt()
        return ChannelType.entries.firstOrNull { it.value == raw } ?: ChannelType.UNKNOWN
    }
}

@Serializable
data class Server(
    val id: String,
    val name: String = "",
    @SerialName("icon_url")
    val iconUrl: String? = null,
    @SerialName("owner_id")
    val ownerId: String = "",
    val channels: List<Channel> = emptyList(),
    @SerialName("member_count")
    val memberCount: Int = 0,
    @SerialName("online_count")
    val onlineCount: Int = 0,
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class MessageAuthor(
    val id: String,
    val username: String,
    @SerialName("display_name")
    val displayName: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null
)

// Extension functions for converting between API models and database entities

fun MessageAuthor.toEntity(): AuthorEntity =
    AuthorEntity(
        author_id = id,
        author_username = username,
        author_displayName = displayName,
        author_avatarUrl = avatarUrl
    )

fun AuthorEntity.toDomain(): MessageAuthor =
    MessageAuthor(
        id = author_id,
        username = author_username,
        displayName = author_displayName,
        avatarUrl = author_avatarUrl
    )

fun Message.toEntity(timestampMillis: Long): MessageEntity? {
    val msgAuthor = author ?: return null
    return MessageEntity(
        id = id,
        channelId = channelId,
        author = MessageAuthor(
            id = msgAuthor.id,
            username = msgAuthor.username,
            displayName = msgAuthor.displayName,
            avatarUrl = msgAuthor.avatarUrl
        ).toEntity(),
        content = content,
        timestamp = timestampMillis
    )
}

fun MessageEntity.toDomain(): Message =
    Message(
        id = id,
        channelId = channelId,
        authorId = author.author_id,
        author = author.toDomain().let { 
            User(
                id = it.id,
                email = "", // Email not stored in message cache
                username = it.username,
                displayName = it.displayName,
                avatarUrl = it.avatarUrl
            )
        },
        content = content,
        createdAt = "",
        updatedAt = null,
        embeds = emptyList(),
        attachments = emptyList(),
        reactions = emptyList(),
        replyToId = null,
        isEdited = false
    )
