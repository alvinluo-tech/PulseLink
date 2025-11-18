package com.alvin.pulselink.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class LocalDataSource @Inject constructor(
    private val context: Context
) {
    private val dataStore = context.dataStore
    
    companion object {
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USERNAME_KEY = stringPreferencesKey("username")
        private val USER_ROLE_KEY = stringPreferencesKey("user_role")
    }
    
    suspend fun saveUser(id: String, username: String, role: String) {
        dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = id
            preferences[USERNAME_KEY] = username
            preferences[USER_ROLE_KEY] = role
        }
    }
    
    suspend fun getUser(): Triple<String?, String?, String?>? {
        val preferences = dataStore.data.first()
        val id = preferences[USER_ID_KEY]
        val username = preferences[USERNAME_KEY]
        val role = preferences[USER_ROLE_KEY]
        
        return if (id != null && username != null && role != null) {
            Triple(id, username, role)
        } else {
            null
        }
    }
    
    suspend fun clearUser() {
        dataStore.edit { preferences ->
            preferences.remove(USER_ID_KEY)
            preferences.remove(USERNAME_KEY)
            preferences.remove(USER_ROLE_KEY)
        }
    }
}
