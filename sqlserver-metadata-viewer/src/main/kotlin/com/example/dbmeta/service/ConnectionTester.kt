package com.example.dbmeta.service

import com.example.dbmeta.model.ConnectionProfile
import java.sql.DriverManager
import java.util.Properties

class ConnectionTester {
    fun test(profile: ConnectionProfile, password: String?): Boolean {
        val jdbcUrl =
            "jdbc:sqlserver://${profile.host}:${profile.port};databaseName=${profile.database};encrypt=false;trustServerCertificate=true;applicationName=SqlServerMetadataViewer"
        val props = Properties().apply {
            put("user", profile.username)
            password?.let { put("password", it) }
        }
        DriverManager.getConnection(jdbcUrl, props).use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("SELECT 1")
            }
        }
        return true
    }
}
