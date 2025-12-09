package com.example.dbmeta.service

import com.example.dbmeta.infra.ProfileFileRepository
import com.example.dbmeta.model.ConnectionProfile
import com.example.dbmeta.model.ProfileStore
import java.util.UUID

class ConnectionProfileService(
    private val repository: ProfileFileRepository = ProfileFileRepository()
)
{
    private var store: ProfileStore = repository.load()

    val profiles: List<ConnectionProfile>
        get() = store.profiles

    var lastSelectedProfileId: String?
        get() = store.lastSelectedProfileId
        private set(value) {
            store = store.copy(lastSelectedProfileId = value)
            persist()
        }

    fun reload(): List<ConnectionProfile> {
        store = repository.load()
        return store.profiles
    }

    fun addOrUpdate(profile: ConnectionProfile): ConnectionProfile {
        val updatedProfile = if (profile.id.isBlank()) {
            profile.copy(id = UUID.randomUUID().toString())
        } else profile

        val newList = store.profiles
            .filterNot { it.id == updatedProfile.id } + updatedProfile
        store = store.copy(profiles = newList.sortedBy { it.name })
        persist()
        return updatedProfile
    }

    fun delete(profileId: String) {
        store = store.copy(profiles = store.profiles.filterNot { it.id == profileId })
        if (store.lastSelectedProfileId == profileId) {
            store = store.copy(lastSelectedProfileId = store.profiles.firstOrNull()?.id)
        }
        persist()
    }

    fun setLastSelected(profileId: String?) {
        lastSelectedProfileId = profileId
    }

    private fun persist() {
        repository.save(store.profiles, store.lastSelectedProfileId)
    }
}
