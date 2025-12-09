package com.example.dbmeta.infra

import com.example.dbmeta.model.ConnectionProfile
import com.example.dbmeta.model.DatabaseMetadataSnapshot
import com.example.dbmeta.model.ProfileStore
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime

object AppPaths {
    private val baseDir: Path = Path.of(System.getProperty("user.home"), ".sqlserver-metadata-viewer")

    fun ensureBaseDir(): Path {
        if (!Files.exists(baseDir)) {
            Files.createDirectories(baseDir)
        }
        return baseDir
    }

    fun metadataFile(profileId: String): Path {
        ensureBaseDir()
        return baseDir.resolve("metadata_$profileId.json")
    }

    fun profilesFile(): Path {
        ensureBaseDir()
        return baseDir.resolve("profiles.json")
    }
}

private val mapper: ObjectMapper = ObjectMapper()
    .registerModule(KotlinModule.Builder().build())
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .enable(SerializationFeature.INDENT_OUTPUT)

class MetadataFileRepository {
    fun save(snapshot: DatabaseMetadataSnapshot) {
        val path = AppPaths.metadataFile(snapshot.profileId)
        Files.writeString(path, mapper.writeValueAsString(snapshot))
    }

    fun load(profileId: String): DatabaseMetadataSnapshot? {
        val path = AppPaths.metadataFile(profileId)
        return if (Files.exists(path)) {
            mapper.readValue<DatabaseMetadataSnapshot>(Files.readString(path))
        } else null
    }
}

class ProfileFileRepository {
    fun save(profiles: List<ConnectionProfile>, lastSelectedId: String?): ProfileStore {
        val store = ProfileStore(profiles, lastSelectedId)
        Files.writeString(AppPaths.profilesFile(), mapper.writeValueAsString(store.copy(lastSelectedProfileId = lastSelectedId)))
        return store
    }

    fun load(): ProfileStore {
        val path = AppPaths.profilesFile()
        if (!Files.exists(path)) {
            return ProfileStore(emptyList(), null)
        }
        return mapper.readValue(Files.readString(path))
    }
}
