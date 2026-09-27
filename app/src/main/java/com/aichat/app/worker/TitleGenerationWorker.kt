package com.aichat.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aichat.app.data.repository.ChatRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class TitleGenerationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val chatRepository: ChatRepository
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val conversationId = inputData.getString("conversationId") ?: return Result.failure()
        return try {
            val messages = chatRepository.getMessages(conversationId)
            if (messages.isNotEmpty()) {
                val title = messages.first { it.role == com.aichat.app.domain.model.Role.USER }.content.take(50)
                chatRepository.updateConversationTitle(conversationId, title)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
