package com.aichat.app.data.mcp

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Host service that allows this app to expose MCP-style tools to other apps or to the agent workspace.
 * Implements Mobile-MCP pattern via Android Intent + Messenger.
 */
class McpHostService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val toolName = intent?.getStringExtra("tool")
        val args = intent?.getStringExtra("args")
        // Handle tool invocation
        return START_NOT_STICKY
    }
}
