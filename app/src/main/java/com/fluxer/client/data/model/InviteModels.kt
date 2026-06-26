package com.fluxer.client.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InviteInfo(
    val code: String = "",
    val guild: InviteGuild? = null,
    val channel: InviteChannel? = null,
    val inviter: User? = null,
    @SerialName("approximate_member_count")
    val memberCount: Int = 0,
    @SerialName("approximate_presence_count")
    val onlineCount: Int = 0
)

@Serializable
data class InviteGuild(
    val id: String,
    val name: String = "",
    @SerialName("icon_url")
    val iconUrl: String? = null,
    val description: String? = null
)

@Serializable
data class InviteChannel(
    val id: String,
    val name: String = "",
    val type: ChannelType = ChannelType.TEXT
)
