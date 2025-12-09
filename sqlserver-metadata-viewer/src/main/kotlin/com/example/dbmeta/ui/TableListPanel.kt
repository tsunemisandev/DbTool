package com.example.dbmeta.ui

import com.example.dbmeta.model.DatabaseMetadataSnapshot
import com.example.dbmeta.model.TableMeta
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.table.DefaultTableModel
import java.awt.BorderLayout

class TableListPanel : JPanel(BorderLayout()) {
    private val tableModel = object : DefaultTableModel(arrayOf("スキーマ", "テーブル", "説明"), 0) {
        override fun isCellEditable(row: Int, column: Int): Boolean = false
    }
    private val columnsModel = object : DefaultTableModel(
        arrayOf("列名", "camelCase", "論理名", "列説明", "DBタイプ", "JDBCタイプ", "Javaタイプ", "Kotlinタイプ"), 0
    ) {
        override fun isCellEditable(row: Int, column: Int): Boolean = false
    }

    private val tablesTable = JTable(tableModel)
    private val columnsTable = JTable(columnsModel)

    private var tables: List<TableMeta> = emptyList()

    init {
        tablesTable.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        tablesTable.selectionModel.addListSelectionListener {
            if (!it.valueIsAdjusting) showColumns(tablesTable.selectedRow)
        }

        add(JScrollPane(tablesTable), BorderLayout.WEST)
        add(JScrollPane(columnsTable), BorderLayout.CENTER)

        val westWidth = 360
        tablesTable.preferredScrollableViewportSize = java.awt.Dimension(westWidth, 500)
    }

    fun setMetadata(snapshot: DatabaseMetadataSnapshot) {
        tables = snapshot.tables
        refreshTables()
    }

    private fun refreshTables() {
        tableModel.rowCount = 0
        tables.forEach {
            tableModel.addRow(arrayOf(it.schemaName, it.tableName, it.tableDescription ?: ""))
        }
        if (tables.isNotEmpty()) {
            tablesTable.setRowSelectionInterval(0, 0)
            showColumns(0)
        } else {
            columnsModel.rowCount = 0
        }
    }

    private fun showColumns(row: Int) {
        if (row !in tables.indices) {
            columnsModel.rowCount = 0
            return
        }
        val table = tables[row]
        columnsModel.rowCount = 0
        table.columns.forEach { c ->
            columnsModel.addRow(
                arrayOf(
                    c.columnName,
                    c.camelCaseName,
                    c.logicalName ?: "",
                    c.columnDescription ?: "",
                    c.dbTypeName,
                    c.jdbcType,
                    c.javaType,
                    c.kotlinType
                )
            )
        }
    }
}
