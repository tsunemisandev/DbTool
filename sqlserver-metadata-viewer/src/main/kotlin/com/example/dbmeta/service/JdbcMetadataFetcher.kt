package com.example.dbmeta.service

import com.example.dbmeta.model.ColumnMeta
import com.example.dbmeta.model.ConnectionProfile
import com.example.dbmeta.model.DatabaseMetadataSnapshot
import com.example.dbmeta.model.TableMeta
import com.example.dbmeta.util.KotlinTypeMapper
import com.example.dbmeta.util.NameUtils
import java.sql.Connection
import java.sql.DriverManager
import java.sql.JDBCType
import java.sql.ResultSet
import java.sql.Types
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties

class JdbcMetadataFetcher {

    fun fetch(profile: ConnectionProfile, password: String?): DatabaseMetadataSnapshot {
        val jdbcUrl =
            "jdbc:sqlserver://${profile.host}:${profile.port};databaseName=${profile.database};encrypt=false;trustServerCertificate=true;applicationName=SqlServerMetadataViewer"
        val props = Properties().apply {
            put("user", profile.username)
            password?.let { put("password", it) }
        }

        DriverManager.getConnection(jdbcUrl, props).use { conn ->
            val tableDescriptions = loadTableDescriptions(conn)
            val columnDescriptions = loadColumnDescriptions(conn)
            val tables = loadTables(conn, tableDescriptions, columnDescriptions)
            return DatabaseMetadataSnapshot(
                profileId = profile.id,
                databaseName = profile.database,
                fetchedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                tables = tables.sortedWith(compareBy({ it.schemaName }, { it.tableName }))
            )
        }
    }

    private fun loadTables(
        conn: Connection,
        tableDescriptions: Map<String, String?>,
        columnDescriptions: Map<String, String?>
    ): List<TableMeta> {
        val meta = conn.metaData
        val result = mutableListOf<TableMeta>()
        meta.getTables(null, null, "%", arrayOf("TABLE", "VIEW")).use { rs ->
            while (rs.next()) {
                val schema = rs.getString("TABLE_SCHEM") ?: "dbo"
                val table = rs.getString("TABLE_NAME")
                val key = "$schema.$table"
                val columns = loadColumns(meta, schema, table, columnDescriptions)
                result += TableMeta(
                    schemaName = schema,
                    tableName = table,
                    tableDescription = tableDescriptions[key],
                    columns = columns.sortedBy { it.columnName }
                )
            }
        }
        return result
    }

    private fun loadColumns(
        meta: java.sql.DatabaseMetaData,
        schema: String,
        table: String,
        columnDescriptions: Map<String, String?>
    ): List<ColumnMeta> {
        val columns = mutableListOf<ColumnMeta>()
        meta.getColumns(null, schema, table, "%").use { rs ->
            while (rs.next()) {
                val columnName = rs.getString("COLUMN_NAME")
                val jdbcTypeCode = rs.getInt("DATA_TYPE")
                val jdbcType = toJdbcTypeName(jdbcTypeCode)
                val typeName = rs.getString("TYPE_NAME") ?: jdbcType
                val javaType = mapJavaType(jdbcTypeCode, typeName)
                val kotlinType = KotlinTypeMapper.fromJavaType(javaType)
                val key = "$schema.$table.$columnName"
                val columnDescription = columnDescriptions[key] ?: rs.getString("REMARKS")
                val logicalName = columnDescription ?: columnName
                val columnSize = rs.getInt("COLUMN_SIZE").takeIf { !rs.wasNull() }
                val decimalDigits = rs.getInt("DECIMAL_DIGITS").takeIf { !rs.wasNull() }
                val charOctetLength = rs.getInt("CHAR_OCTET_LENGTH").takeIf { !rs.wasNull() }
                val columnDef = rs.getString("COLUMN_DEF")
                columns += ColumnMeta(
                    schemaName = schema,
                    tableName = table,
                    columnName = columnName,
                    camelCaseName = NameUtils.toCamelCase(columnName),
                    columnDescription = columnDescription,
                    logicalName = logicalName,
                    dbTypeName = typeName,
                    jdbcType = jdbcType,
                    javaType = javaType,
                    kotlinType = kotlinType,
                    columnSize = columnSize,
                    decimalDigits = decimalDigits,
                    charOctetLength = charOctetLength,
                    columnDefault = columnDef
                )
            }
        }
        return columns
    }

    private fun loadTableDescriptions(conn: Connection): Map<String, String?> {
        val sql = """
            SELECT s.name AS schema_name, t.name AS table_name, CAST(ep.value AS NVARCHAR(4000)) AS description
            FROM sys.tables t
            INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
            LEFT JOIN sys.extended_properties ep
              ON ep.major_id = t.object_id
              AND ep.minor_id = 0
              AND ep.name = 'MS_Description'
        """.trimIndent()
        return conn.createStatement().use { stmt ->
            stmt.executeQuery(sql).use { rs ->
                buildMap {
                    while (rs.next()) {
                        val key = "${rs.getString("schema_name")}.${rs.getString("table_name")}"
                        put(key, rs.getString("description"))
                    }
                }
            }
        }
    }

    private fun loadColumnDescriptions(conn: Connection): Map<String, String?> {
        val sql = """
            SELECT
                s.name AS schema_name,
                t.name AS table_name,
                c.name AS column_name,
                CAST(ep.value AS NVARCHAR(4000)) AS description
            FROM sys.columns c
            INNER JOIN sys.tables t ON c.object_id = t.object_id
            INNER JOIN sys.schemas s ON t.schema_id = s.schema_id
            LEFT JOIN sys.extended_properties ep
              ON ep.major_id = c.object_id
              AND ep.minor_id = c.column_id
              AND ep.name = 'MS_Description'
        """.trimIndent()
        return conn.createStatement().use { stmt ->
            stmt.executeQuery(sql).use { rs ->
                buildMap {
                    while (rs.next()) {
                        val key = "${rs.getString("schema_name")}.${rs.getString("table_name")}.${rs.getString("column_name")}"
                        put(key, rs.getString("description"))
                    }
                }
            }
        }
    }

    private fun mapJavaType(jdbcType: Int, typeName: String): String = when (jdbcType) {
        Types.VARCHAR, Types.NVARCHAR, Types.CHAR, Types.NCHAR, Types.LONGNVARCHAR, Types.LONGVARCHAR -> "java.lang.String"
        Types.INTEGER, Types.SMALLINT, Types.TINYINT -> "java.lang.Integer"
        Types.BIGINT -> "java.lang.Long"
        Types.DECIMAL, Types.NUMERIC -> "java.math.BigDecimal"
        Types.DOUBLE -> "java.lang.Double"
        Types.FLOAT, Types.REAL -> "java.lang.Float"
        Types.DATE -> "java.time.LocalDate"
        Types.TIME -> "java.time.LocalTime"
        Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> "java.time.LocalDateTime"
        Types.BIT, Types.BOOLEAN -> "java.lang.Boolean"
        else -> when (typeName.uppercase()) {
            "UNIQUEIDENTIFIER" -> "java.util.UUID"
            "VARBINARY", "BINARY" -> "java.sql.Blob"
            else -> "java.lang.Object"
        }
    }

    private fun toJdbcTypeName(type: Int): String =
        try {
            JDBCType.valueOf(type).name
        } catch (_: Exception) {
            type.toString()
        }
}
