package com.example.dbmeta.model

import com.fasterxml.jackson.annotation.JsonIgnore

data class ConnectionProfile(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val database: String,
    val username: String,
    val savePassword: Boolean,
    val encryptedPassword: String? = null,
    @JsonIgnore
    var runtimePassword: String? = null
) {
    fun decodedPassword(): String? =
        encryptedPassword?.let { String(java.util.Base64.getDecoder().decode(it)) }

    override fun toString(): String = name
}

data class ProfileStore(
    val profiles: List<ConnectionProfile> = emptyList(),
    val lastSelectedProfileId: String? = null
)

data class DatabaseMetadataSnapshot(
    val profileId: String,
    val databaseName: String,
    val fetchedAt: String,
    val tables: List<TableMeta>
)

data class TableMeta(
    val schemaName: String,
    val tableName: String,
    val tableDescription: String?,
    val columns: List<ColumnMeta>
)

data class ColumnMeta(
    val schemaName: String,
    val tableName: String,
    val columnName: String,
    val camelCaseName: String,
    val columnDescription: String?,
    val logicalName: String?,
    val dbTypeName: String,
    val jdbcType: String,
    val javaType: String,
    val kotlinType: String,
    val columnSize: Int? = null,
    val decimalDigits: Int? = null,
    val charOctetLength: Int? = null,
    val columnDefault: String? = null
)
