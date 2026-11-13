package com.example.localization

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appPrefsDataStore by preferencesDataStore(name = "my_duty_in_ncr_prefs")

data class NotificationPreferences(
    val punchAlerts: Boolean = true,
    val overtimeAlerts: Boolean = true,
    val salaryAlerts: Boolean = true,
    val pfEsiAlerts: Boolean = true,
    val jobAlerts: Boolean = true
)

class LanguageStore(private val context: Context) {

    private val keyLang = stringPreferencesKey("app_language")
    private val keyPunchAlerts = booleanPreferencesKey("notif_punch")
    private val keyOvertimeAlerts = booleanPreferencesKey("notif_overtime")
    private val keySalaryAlerts = booleanPreferencesKey("notif_salary")
    private val keyPfEsiAlerts = booleanPreferencesKey("notif_pf_esi")
    private val keyJobAlerts = booleanPreferencesKey("notif_job")

    // Hindi ("hi") is the default language per requirement
    val languageFlow: Flow<String> = context.appPrefsDataStore.data.map { prefs ->
        prefs[keyLang] ?: "hi"
    }

    val notificationPrefsFlow: Flow<NotificationPreferences> = context.appPrefsDataStore.data.map { prefs ->
        NotificationPreferences(
            punchAlerts = prefs[keyPunchAlerts] ?: true,
            overtimeAlerts = prefs[keyOvertimeAlerts] ?: true,
            salaryAlerts = prefs[keySalaryAlerts] ?: true,
            pfEsiAlerts = prefs[keyPfEsiAlerts] ?: true,
            jobAlerts = prefs[keyJobAlerts] ?: true
        )
    }

    suspend fun toggleLanguage(current: String) {
        val next = if (current == "hi") "en" else "hi"
        context.appPrefsDataStore.edit { it[keyLang] = next }
    }

    suspend fun updateNotificationPrefs(prefs: NotificationPreferences) {
        context.appPrefsDataStore.edit {
            it[keyPunchAlerts] = prefs.punchAlerts
            it[keyOvertimeAlerts] = prefs.overtimeAlerts
            it[keySalaryAlerts] = prefs.salaryAlerts
            it[keyPfEsiAlerts] = prefs.pfEsiAlerts
            it[keyJobAlerts] = prefs.jobAlerts
        }
    }
}
