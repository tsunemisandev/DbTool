package com.example.dbmeta.ui

import com.example.dbmeta.model.DatabaseMetadataSnapshot
import com.example.dbmeta.model.ColumnMeta
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Toolkit
import javax.swing.JButton
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.JTextArea
import javax.swing.JComboBox
import javax.swing.table.DefaultTableModel
import javax.swing.ListSelectionModel
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.AbstractAction
import javax.swing.KeyStroke
import java.awt.event.InputEvent
import java.awt.event.KeyEvent

class LogicalNameSearchPanel : JPanel(BorderLayout()) {
    private val inputArea = JTextArea(4, 50)
    private val searchButton = JButton("検索")
    private val copyButton = JButton("コピー")
    private val exportButton = JButton("CSV エクスポート")
    private val modeCombo = JComboBox(arrayOf("完全一致", "部分一致"))
    private val schemaField = javax.swing.JTextField(12)
    private val tableField = javax.swing.JTextField(12)
    private val unmatchedLabel = JLabel("未マッチ: 0")

    private val tableModel = object : DefaultTableModel(
        arrayOf(
            "入力論理名",
            "論理名",
            "スキーマ",
            "テーブル論理名",
            "テーブル",
            "列名",
            "camelCase",
            "DBタイプ",
            "JDBCタイプ",
            "Javaタイプ",
            "Kotlinタイプ",
            "サイズ(COLUMN_SIZE)",
            "小数桁数(DECIMAL_DIGITS)",
            "文字バイト長(CHAR_OCTET_LENGTH)",
            "デフォルト値(COLUMN_DEF)"
        ), 0
    ) {
        override fun isCellEditable(row: Int, column: Int): Boolean = false
    }
    private val resultTable = JTable(tableModel).apply {
        autoResizeMode = JTable.AUTO_RESIZE_OFF
        rowSelectionAllowed = true
        columnSelectionAllowed = true
        selectionModel.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
        columnModel.selectionModel.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
    }
    private val resultScroll = JScrollPane(resultTable).apply {
        horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
    }

    private var snapshot: DatabaseMetadataSnapshot? = null

    init {
        add(buildTopPanel(), BorderLayout.NORTH)
        add(resultScroll, BorderLayout.CENTER)
        add(buildBottomPanel(), BorderLayout.SOUTH)

        searchButton.addActionListener { performSearch() }
        copyButton.addActionListener { copyToClipboard() }
        exportButton.addActionListener { exportCsv() }
        resultTable.tableHeader.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val col = resultTable.columnModel.getColumnIndexAtX(e.x)
                if (col >= 0) {
                    resultTable.clearSelection()
                    resultTable.addColumnSelectionInterval(col, col)
                    if (tableModel.rowCount > 0) {
                        resultTable.setRowSelectionInterval(0, tableModel.rowCount - 1)
                    }
                }
            }
        })
        val inputMap = resultTable.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK), "copySelection")
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.META_DOWN_MASK), "copySelection") // macOS Cmd+C
        resultTable.actionMap.put("copySelection", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                copyToClipboard()
            }
        })
    }

    private fun buildTopPanel(): JPanel = JPanel(BorderLayout()).apply {
        val top = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JLabel("論理名 (1行1件):"))
        }
        val controls = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(searchButton)
            add(modeCombo)
            add(JLabel("スキーマ(任意):"))
            schemaField.columns = 10
            add(schemaField)
            add(JLabel("テーブル(任意):"))
            tableField.columns = 12
            add(tableField)
        }
        add(top, BorderLayout.NORTH)
        add(JScrollPane(inputArea), BorderLayout.CENTER)
        add(controls, BorderLayout.SOUTH)
    }

    private fun buildBottomPanel(): JPanel = JPanel(FlowLayout(FlowLayout.RIGHT)).apply {
        add(unmatchedLabel)
        add(copyButton)
        add(exportButton)
    }

    fun setMetadata(snapshot: DatabaseMetadataSnapshot) {
        this.snapshot = snapshot
    }

    private fun performSearch() {
        val metadata = snapshot ?: run {
            JOptionPane.showMessageDialog(this, "メタデータが読み込まれていません。", "Info", JOptionPane.INFORMATION_MESSAGE)
            return
        }
        val inputs = inputArea.text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val schemaFilter = schemaField.text.trim()
        val tableFilter = tableField.text.trim()
        val matchPartial = modeCombo.selectedItem == "部分一致"
        val rows = mutableListOf<Array<Any>>()
        val unmatched = mutableListOf<String>()

        if (inputs.isEmpty()) {
            // no logical name input -> list all (optionally schema-filtered)
            metadata.tables.forEach { table ->
                if (!matchesSchema(table.schemaName, schemaFilter)) return@forEach
                if (!matchesTable(table.tableName, tableFilter)) return@forEach
                table.columns.forEach { col ->
                    rows += toRow(col.logicalName ?: "", col)
                }
            }
        } else {
            inputs.forEach { input ->
                val hits = findMatches(metadata, input, matchPartial, schemaFilter, tableFilter)
                if (hits.isEmpty()) {
                    unmatched += input
                } else {
                    hits.forEach { col ->
                        rows += toRow(input, col)
                    }
                }
            }
        }

        tableModel.rowCount = 0
        rows.forEach { tableModel.addRow(it) }
        unmatchedLabel.text = "未マッチ: ${unmatched.size}"
        if (unmatched.isNotEmpty()) {
            JOptionPane.showMessageDialog(this, "未ヒット: ${unmatched.joinToString()}", "Info", JOptionPane.INFORMATION_MESSAGE)
        }
    }

    private fun findMatches(metadata: DatabaseMetadataSnapshot, input: String, partial: Boolean, schemaFilter: String, tableFilter: String): List<ColumnMeta> {
        val comparer: (String) -> Boolean = { target ->
            if (partial) target.contains(input, ignoreCase = true)
            else target.equals(input, ignoreCase = true)
        }
        return metadata.tables.flatMap { table ->
            if (!matchesSchema(table.schemaName, schemaFilter)) emptyList()
            else if (!matchesTable(table.tableName, tableFilter)) emptyList()
            else table.columns.filter { col ->
                val logical = col.logicalName ?: ""
                comparer(logical)
            }
        }
    }

    private fun toRow(input: String, col: ColumnMeta): Array<Any> {
        val sizeDisplay = col.columnSize?.toString() ?: ""
        val decimalDigitsDisplay = col.decimalDigits?.toString() ?: ""
        val octetDisplay = col.charOctetLength?.toString() ?: ""
        val defaultDisplay = col.columnDefault ?: ""
        val tableLogical = tableLogicalName(col)
        return arrayOf(
            input,
            col.logicalName ?: "",
            col.schemaName,
            tableLogical,
            col.tableName,
            col.columnName,
            col.camelCaseName,
            col.dbTypeName,
            col.jdbcType,
            col.javaType,
            col.kotlinType,
            sizeDisplay,
            decimalDigitsDisplay,
            octetDisplay,
            defaultDisplay
        )
    }

    private fun tableLogicalName(col: ColumnMeta): String {
        // Try to find table in current snapshot and pull its description (logical name) if available
        val current = snapshot ?: return ""
        val table = current.tables.firstOrNull { it.schemaName == col.schemaName && it.tableName == col.tableName }
        return table?.tableDescription ?: ""
    }

    private fun matchesSchema(schemaName: String, filter: String): Boolean {
        if (filter.isBlank()) return true
        return schemaName.equals(filter, ignoreCase = true)
    }

    private fun matchesTable(tableName: String, filter: String): Boolean {
        if (filter.isBlank()) return true
        return tableName.equals(filter, ignoreCase = true) || tableName.contains(filter, ignoreCase = true)
    }

    private fun copyToClipboard() {
        if (tableModel.rowCount == 0) return
        val sb = StringBuilder()
        val selectedCols = resultTable.selectedColumns
        val selectedRows = resultTable.selectedRows

        // Determine target columns and rows:
        val colsToCopy = if (selectedCols.isNotEmpty()) selectedCols else (0 until tableModel.columnCount).toList().toIntArray()
        val rowsToCopy = if (selectedRows.isNotEmpty()) selectedRows else (0 until tableModel.rowCount).toList().toIntArray()

        rowsToCopy.forEach { r ->
            colsToCopy.forEachIndexed { idx, c ->
                if (idx > 0) sb.append('\t')
                sb.append(tableModel.getValueAt(r, c))
            }
            sb.append('\n')
        }
        val selection = java.awt.datatransfer.StringSelection(sb.toString())
        Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
        JOptionPane.showMessageDialog(this, "結果をクリップボードへコピーしました。", "Copy", JOptionPane.INFORMATION_MESSAGE)
    }

    private fun exportCsv() {
        if (tableModel.rowCount == 0) return
        val chooser = JFileChooser()
        val result = chooser.showSaveDialog(this)
        if (result != JFileChooser.APPROVE_OPTION) return
        val file = chooser.selectedFile
        val lines = mutableListOf<String>()
        lines += (0 until tableModel.columnCount).joinToString(",") { escapeCsv(tableModel.getColumnName(it)) }
        for (r in 0 until tableModel.rowCount) {
            val line = (0 until tableModel.columnCount).joinToString(",") { c ->
                escapeCsv(tableModel.getValueAt(r, c).toString())
            }
            lines += line
        }
        file.writeText(lines.joinToString("\n"))
        JOptionPane.showMessageDialog(this, "CSV を保存しました: ${file.absolutePath}", "Export", JOptionPane.INFORMATION_MESSAGE)
    }

    private fun escapeCsv(value: String): String {
        val needsQuote = value.contains(",") || value.contains("\"") || value.contains("\n")
        var v = value.replace("\"", "\"\"")
        if (needsQuote) v = "\"$v\""
        return v
    }
}
