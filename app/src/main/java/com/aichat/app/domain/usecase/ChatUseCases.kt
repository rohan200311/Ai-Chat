package com.aichat.app.domain.usecase

import com.aichat.app.data.repository.ChatRepository
import com.aichat.app.domain.model.Message
import com.aichat.app.domain.model.Role
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(conversationId: String, content: String, parentId: String? = null): Message {
        val msg = Message(
            conversationId = conversationId,
            parentId = parentId,
            role = Role.USER,
            content = content
        )
        chatRepository.addMessage(msg)
        return msg
    }
}

class BranchConversationUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(fromMessageId: String, newContent: String): Message {
        return chatRepository.createBranch(fromMessageId, newContent)
    }
}

class GetConversationBranchUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(conversationId: String, leafId: String): List<Message> {
        return chatRepository.getBranch(conversationId, leafId)
    }
}
