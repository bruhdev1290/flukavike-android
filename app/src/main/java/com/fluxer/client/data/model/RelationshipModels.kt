package com.fluxer.client.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Relationship(
    val id: String = "",
    /** 1=friend, 2=blocked, 3=incoming request, 4=outgoing request */
    val type: Int = 0,
    val user: User
)

@Serializable
data class AddFriendRequest(val username: String)

@Serializable
data class RelationshipTypeRequest(val type: Int)
