package com.innovatewithomer.myshelf.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {

    companion object {
        private val OFFLINE_MODE_KEY = booleanPreferencesKey("offline_mode")
    }

    val isOfflineMode: Flow<Boolean> = context.dataStore.data
        .map { it[OFFLINE_MODE_KEY] == true }

    suspend fun setOfflineMode(enabled: Boolean) {
        context.dataStore.edit { it[OFFLINE_MODE_KEY] = enabled }
    }
}
