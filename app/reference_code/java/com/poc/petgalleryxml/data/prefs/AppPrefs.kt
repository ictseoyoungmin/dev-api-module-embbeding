package com.poc.petgalleryxml.data.prefs

import android.content.Context
import android.content.SharedPreferences

class AppPrefs(context: Context) {
    private val sp: SharedPreferences = context.getSharedPreferences("pet_gallery_prefs", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = sp.getString(KEY_BASE_URL, "http://10.0.2.2:8000") ?: "http://10.0.2.2:8000"
        set(value) { sp.edit().putString(KEY_BASE_URL, value.trim()).apply() }

    var daycareId: String
        get() = sp.getString(KEY_DAYCARE_ID, "dc_001") ?: "dc_001"
        set(value) { sp.edit().putString(KEY_DAYCARE_ID, value.trim()).apply() }

    var trainerId: String
        get() = sp.getString(KEY_TRAINER_ID, "trainer_kim") ?: "trainer_kim"
        set(value) { sp.edit().putString(KEY_TRAINER_ID, value.trim()).apply() }

    var selectedPetId: String?
        get() = sp.getString(KEY_SELECTED_PET_ID, null)
        set(value) { sp.edit().putString(KEY_SELECTED_PET_ID, value).apply() }

    companion object {
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_DAYCARE_ID = "daycare_id"
        private const val KEY_TRAINER_ID = "trainer_id"
        private const val KEY_SELECTED_PET_ID = "selected_pet_id"
    }
}
