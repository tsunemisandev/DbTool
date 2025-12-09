#!/bin/bash
# ============================================
# SQL Server メタデータビューア (Kotlin + Swing)
# Gradle プロジェクト生成スクリプト (macOS / Linux)
# ============================================

PROJECT_NAME="sqlserver-metadata-viewer"

if [ -d "$PROJECT_NAME" ]; then
  echo "フォルダ '$PROJECT_NAME' は既に存在します。処理を中止します。"
  exit 1
fi

echo "プロジェクトフォルダ '$PROJECT_NAME' を作成します..."
mkdir -p "$PROJECT_NAME/src/main/kotlin/com/example/dbmeta/ui"
mkdir -p "$PROJECT_NAME/src/main/kotlin/com/example/dbmeta/model"
mkdir -p "$PROJECT_NAME/src/main/kotlin/com/example/dbmeta/service"

cd "$PROJECT_NAME"

echo "settings.gradle.kts を生成..."
cat << 'EOF' > settings.gradle.kts
rootProject.name = "sqlserver-metadata-viewer"
EOF

echo "build.gradle.kts を生成..."
cat << 'EOF' > build.gradle.kts
plugins {
    kotlin("jvm") version "2.0.0"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))

    implementation("com.microsoft.sqlserver:mssql-jdbc:12.6.1.jre11")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
}

application {
    mainClass = "com.example.dbmeta.MainKt"
}

kotlin {
    jvmToolchain(17)
}
EOF

echo "Main.kt を生成..."
cat << 'EOF' > src/main/kotlin/com/example/dbmeta/Main.kt
package com.example.dbmeta

import com.example.dbmeta.ui.MainFrame
import javax.swing.SwingUtilities
import javax.swing.UIManager

fun main() {
    try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (_: Exception) {}

    SwingUtilities.invokeLater {
        MainFrame().isVisible = true
    }
}
EOF

echo "MainFrame.kt を生成..."
cat << 'EOF' > src/main/kotlin/com/example/dbmeta/ui/MainFrame.kt
package com.example.dbmeta.ui

import javax.swing.*
import java.awt.BorderLayout

class MainFrame : JFrame("SQL Server Metadata Viewer") {

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(1024, 768)
        setLocationRelativeTo(null)

        layout = BorderLayout()

        val label = JLabel("SQL Server メタデータビューア (Kotlin + Swing)")
        val panel = JPanel(BorderLayout())
        panel.add(label, BorderLayout.NORTH)

        contentPane.add(panel, BorderLayout.CENTER)
    }
}
EOF

echo "MetadataModels.kt を生成..."
cat << 'EOF' > src/main/kotlin/com/example/dbmeta/model/MetadataModels.kt
package com.example.dbmeta.model

data class ConnectionProfile(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val database: String,
    val username: String,
    val savePassword: Boolean,
    val encryptedPassword: String? = null
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
    val kotlinType: String
)
EOF

echo "MetadataService.kt を生成..."
cat << 'EOF' > src/main/kotlin/com/example/dbmeta/service/MetadataService.kt
package com.example.dbmeta.service

import com.example.dbmeta.model.*

class MetadataService {

    fun fetchAndSaveMetadata(profile: ConnectionProfile) {
        // TODO: SQL Server 接続 & メタデータ取得処理を実装
    }

    fun loadMetadata(profileId: String): DatabaseMetadataSnapshot? {
        // TODO: JSON 読み込み処理
        return null
    }
}
EOF

echo ""
echo "============================================"
echo "Gradle プロジェクトの生成が完了しました。"
echo "次の手順:"
echo "  1. gradle wrapper を作成:"
echo "        gradle wrapper"
echo "  2. 実行:"
echo "        ./gradlew run"
echo "============================================"
echo ""
