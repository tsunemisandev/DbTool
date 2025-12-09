package com.example.dbmeta.service

import com.example.dbmeta.infra.MetadataFileRepository
import com.example.dbmeta.model.ConnectionProfile
import com.example.dbmeta.model.DatabaseMetadataSnapshot
import com.example.dbmeta.model.TableMeta
import com.example.dbmeta.util.NameUtils
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MetadataService(
    private val repository: MetadataFileRepository = MetadataFileRepository(),
    private val fetcher: JdbcMetadataFetcher = JdbcMetadataFetcher()
) {

    fun loadCached(profileId: String): DatabaseMetadataSnapshot? = repository.load(profileId)

    fun fetchAndSave(profile: ConnectionProfile, password: String?): DatabaseMetadataSnapshot {
        val snapshot = fetcher.fetch(profile, password)
        repository.save(snapshot)
        return snapshot
    }

    fun refreshCamelCase(snapshot: DatabaseMetadataSnapshot): DatabaseMetadataSnapshot {
        val updatedTables = snapshot.tables.map { table ->
            val updatedColumns = table.columns.map { col ->
                if (col.camelCaseName.isNotBlank()) col else col.copy(camelCaseName = NameUtils.toCamelCase(col.columnName))
            }
            table.copy(columns = updatedColumns)
        }
        val refreshed = snapshot.copy(
            tables = updatedTables,
            fetchedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )
        repository.save(refreshed)
        return refreshed
    }
}
