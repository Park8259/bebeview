package com.example.bebeview.record.data

import android.content.Context

/** 로그인한 보호자 세션. parentId를 그대로 childId로 씀 (서버 쪽과 동일한 가정) */
data class ParentSession(val parentId: String, val name: String, val email: String, val token: String)

object ParentSessionStore {
    private const val PREFS = "bebeview_session"
    private const val KEY_PARENT_ID = "parent_id"
    private const val KEY_NAME = "name"
    private const val KEY_EMAIL = "email"
    private const val KEY_TOKEN = "token"

    fun save(context: Context, session: ParentSession) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_PARENT_ID, session.parentId)
            .putString(KEY_NAME, session.name)
            .putString(KEY_EMAIL, session.email)
            .putString(KEY_TOKEN, session.token)
            .apply()
    }

    fun load(context: Context): ParentSession? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val parentId = prefs.getString(KEY_PARENT_ID, null) ?: return null
        val name = prefs.getString(KEY_NAME, "") ?: ""
        val email = prefs.getString(KEY_EMAIL, "") ?: ""
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        return ParentSession(parentId, name, email, token)
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
