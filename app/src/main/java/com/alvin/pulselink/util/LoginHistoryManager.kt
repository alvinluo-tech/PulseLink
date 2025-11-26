package com.alvin.pulselink.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * 登录历史管理器
 * 用于保存和读取用户的登录历史记录（账号+密码组合）
 */
object LoginHistoryManager {
    
    private const val PREF_NAME = "login_history"
    private const val KEY_EMAIL_HISTORY = "email_history"
    private const val KEY_VIRTUAL_ID_HISTORY = "virtual_id_history"
    private const val MAX_HISTORY_SIZE = 5
    private const val SEPARATOR = "|||"
    
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * 登录凭证数据类
     */
    @Serializable
    data class LoginCredential(
        val account: String,  // 邮箱或SNR-ID
        val password: String  // 密码
    )
    
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 保存邮箱登录凭证（账号+密码）
     */
    fun saveEmailCredential(context: Context, email: String, password: String) {
        if (email.isBlank() || password.isBlank()) return
        
        val prefs = getPreferences(context)
        val currentHistory = getEmailCredentials(context).toMutableList()
        
        // 移除相同账号的旧记录
        currentHistory.removeAll { it.account == email }
        // 添加新凭证到最前面
        currentHistory.add(0, LoginCredential(email, password))
        // 限制最大数量
        if (currentHistory.size > MAX_HISTORY_SIZE) {
            currentHistory.removeAt(currentHistory.size - 1)
        }
        
        // 序列化为JSON
        val jsonString = currentHistory.joinToString(SEPARATOR) { json.encodeToString(it) }
        prefs.edit()
            .putString(KEY_EMAIL_HISTORY, jsonString)
            .apply()
    }
    
    /**
     * 获取邮箱登录凭证列表
     */
    fun getEmailCredentials(context: Context): List<LoginCredential> {
        val prefs = getPreferences(context)
        val historyString = prefs.getString(KEY_EMAIL_HISTORY, "") ?: ""
        
        if (historyString.isBlank()) {
            return emptyList()
        }
        
        return try {
            historyString.split(SEPARATOR)
                .filter { it.isNotBlank() }
                .map { json.decodeFromString<LoginCredential>(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 获取邮箱账号列表（仅用于UI显示）
     */
    fun getEmailHistory(context: Context): List<String> {
        return getEmailCredentials(context).map { it.account }
    }
    
    /**
     * 根据邮箱获取密码
     */
    fun getPasswordForEmail(context: Context, email: String): String? {
        return getEmailCredentials(context).find { it.account == email }?.password
    }
    
    /**
     * 保存虚拟ID登录凭证（账号+密码）
     */
    fun saveVirtualIdCredential(context: Context, virtualId: String, password: String) {
        if (virtualId.isBlank() || password.isBlank()) return
        
        val prefs = getPreferences(context)
        val currentHistory = getVirtualIdCredentials(context).toMutableList()
        
        // 移除相同账号的旧记录
        currentHistory.removeAll { it.account == virtualId }
        // 添加新凭证到最前面
        currentHistory.add(0, LoginCredential(virtualId, password))
        // 限制最大数量
        if (currentHistory.size > MAX_HISTORY_SIZE) {
            currentHistory.removeAt(currentHistory.size - 1)
        }
        
        // 序列化为JSON
        val jsonString = currentHistory.joinToString(SEPARATOR) { json.encodeToString(it) }
        prefs.edit()
            .putString(KEY_VIRTUAL_ID_HISTORY, jsonString)
            .apply()
    }
    
    /**
     * 获取虚拟ID登录凭证列表
     */
    fun getVirtualIdCredentials(context: Context): List<LoginCredential> {
        val prefs = getPreferences(context)
        val historyString = prefs.getString(KEY_VIRTUAL_ID_HISTORY, "") ?: ""
        
        if (historyString.isBlank()) {
            return emptyList()
        }
        
        return try {
            historyString.split(SEPARATOR)
                .filter { it.isNotBlank() }
                .map { json.decodeFromString<LoginCredential>(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 获取虚拟ID账号列表（仅用于UI显示）
     */
    fun getVirtualIdHistory(context: Context): List<String> {
        return getVirtualIdCredentials(context).map { it.account }
    }
    
    /**
     * 根据虚拟ID获取密码
     */
    fun getPasswordForVirtualId(context: Context, virtualId: String): String? {
        return getVirtualIdCredentials(context).find { it.account == virtualId }?.password
    }
    
    /**
     * 清空邮箱历史
     */
    fun clearEmailHistory(context: Context) {
        getPreferences(context).edit()
            .remove(KEY_EMAIL_HISTORY)
            .apply()
    }
    
    /**
     * 清空虚拟ID历史
     */
    fun clearVirtualIdHistory(context: Context) {
        getPreferences(context).edit()
            .remove(KEY_VIRTUAL_ID_HISTORY)
            .apply()
    }
}
