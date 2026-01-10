package com.castle.domain.dto.request

import com.castle.domain.enums.ChatType
import com.castle.domain.enums.ChatVisibility

data class CreateChatRequest(
    val createdBy: Long,
    val description: String,
    val memberIds: Set<Long>,
    val name: String,
    val type: ChatType,
    val visibility: ChatVisibility,
)
