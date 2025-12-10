package com.example.dbmeta.ui

import com.example.dbmeta.model.DatabaseMetadataSnapshot
import com.example.dbmeta.util.NameUtils
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Toolkit
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.JTextField

class DataClassGeneratorPanel : JPanel(BorderLayout()) {
    private val classNameField = JTextField("ResultRow", 18)
    private val inputArea = JTextArea(8, 80)
    private val outputArea = JTextArea(10, 80).apply { isEditable = false }
    private val generateButton = JButton("生成")
    private val copyButton = JButton("コピー")
    private var logicalNameMap: Map<String, String> = emptyMap()

    init {
        add(buildTopPanel(), BorderLayout.NORTH)
        add(JScrollPane(inputArea), BorderLayout.CENTER)
        add(buildBottomPanel(), BorderLayout.SOUTH)

        generateButton.addActionListener { generate() }
        copyButton.addActionListener { copyResult() }
    }

    fun setMetadata(snapshot: DatabaseMetadataSnapshot?) {
        logicalNameMap = snapshot?.tables
            ?.flatMap { table -> table.columns.map { it.columnName.lowercase() to (it.logicalName ?: it.columnName) } }
            ?.toMap()
            ?: emptyMap()
    }

    private fun buildTopPanel(): JPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
        add(JLabel("クラス名:"))
        add(classNameField)
        add(generateButton)
        add(copyButton)
    }

    private fun buildBottomPanel(): JPanel = JPanel(BorderLayout()).apply {
        add(JLabel("生成結果:"), BorderLayout.NORTH)
        add(JScrollPane(outputArea), BorderLayout.CENTER)
    }

    private fun generate() {
        val raw = inputArea.text.trim()
        if (raw.isEmpty()) {
            JOptionPane.showMessageDialog(this, "SELECT の結果を貼り付けてください（ヘッダー行必須）。", "Info", JOptionPane.INFORMATION_MESSAGE)
            return
        }
        val lines = raw.lines().filter { it.isNotBlank() }
        val firstLine = lines.firstOrNull()?.trim().orEmpty()
        if (lines.isEmpty()) {
            JOptionPane.showMessageDialog(this, "有効な行がありません。", "Info", JOptionPane.INFORMATION_MESSAGE)
            return
        }
        val splitter = buildSplitter(lines.first())
        var headers = splitter(lines.first()).map { it.trim() }.filter { it.isNotEmpty() }

        // If it looks like a SELECT statement pasted (not tabular result), parse the select list.
        if (firstLine.startsWith("select", ignoreCase = true)) {
            headers = parseSelectList(raw)
        }
        if (headers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "ヘッダー行が解釈できませんでした。", "Error", JOptionPane.ERROR_MESSAGE)
            return
        }
        val samples = lines.drop(1).map { splitter(it) }
        val headerInfos = headers.mapIndexed { idx, h -> toHeaderInfo(h, idx) }
        val types = headers.mapIndexed { idx, _ -> inferType(samples, idx) }

        val className = classNameField.text.trim().ifEmpty { "ResultRow" }
        val properties = headerInfos.mapIndexed { idx, info ->
            val type = types[idx]
            "    /** ${info.comment}*/\n    var ${info.propName}: $type"
        }.joinToString(",\n\n")

        val result = buildString {
            appendLine("data class $className(")
            appendLine(properties)
            appendLine(")")
        }
        outputArea.text = result
    }

    private fun buildSplitter(header: String): (String) -> List<String> =
        when {
            header.contains('\t') -> { line -> line.split('\t') }
            header.contains(',') -> { line -> line.split(',') }
            else -> { line -> line.trim().split(Regex("\\s+")) }
        }

    private data class HeaderInfo(val propName: String, val comment: String)

    private fun toHeaderInfo(raw: String, index: Int): HeaderInfo {
        var token = raw.trim()
        token = token.removePrefixIgnoreCase("select").trim().removePrefix(",").trim()
        token = stripWrapper(token, "`", "`")
        token = stripWrapper(token, "\"", "\"")
        token = stripWrapper(token, "[", "]")

        val aliasMatch = Regex("(?i)\\s+as\\s+([A-Za-z0-9_]+)$").find(token)
        val alias = aliasMatch?.groupValues?.get(1)
        val base = alias ?: token.substringAfterLast('.').ifBlank { token }
        val key = base.lowercase()
        val comment = logicalNameMap[key] ?: base
        val prop = NameUtils.toCamelCase(base.replace(Regex("[^A-Za-z0-9_]"), "_")).ifBlank { "field$index" }
        return HeaderInfo(prop, comment)
    }

    private fun parseSelectList(sqlText: String): List<String> {
        val lower = sqlText.lowercase()
        val start = lower.indexOf("select")
        if (start == -1) return listOf(sqlText.trim())

        val fromMatch = Regex("\\bfrom\\b", RegexOption.IGNORE_CASE).find(lower, start)
        val fromIdx = fromMatch?.range?.first ?: -1
        if (fromIdx <= start) return listOf(sqlText.trim())

        val selectPart = sqlText.substring(start + "select".length, fromIdx)
        val parts = mutableListOf<String>()
        val sb = StringBuilder()
        var depth = 0
        selectPart.forEach { ch ->
            when (ch) {
                '(' -> {
                    depth++
                    sb.append(ch)
                }
                ')' -> {
                    depth--
                    sb.append(ch)
                }
                ',' -> {
                    if (depth == 0) {
                        parts += sb.toString()
                        sb.clear()
                    } else sb.append(ch)
                }
                else -> sb.append(ch)
            }
        }
        if (sb.isNotEmpty()) parts += sb.toString()
        return parts.map { it.trim().trimEnd(';') }.filter { it.isNotEmpty() }
    }

    private fun stripWrapper(value: String, prefix: String, suffix: String): String =
        if (value.startsWith(prefix) && value.endsWith(suffix) && value.length >= prefix.length + suffix.length) {
            value.substring(prefix.length, value.length - suffix.length)
        } else value

    private fun String.removePrefixIgnoreCase(prefix: String): String =
        if (this.startsWith(prefix, ignoreCase = true)) this.substring(prefix.length) else this

    private fun inferType(samples: List<List<String>>, index: Int): String {
        val first = samples.firstOrNull { it.size > index && it[index].isNotBlank() }?.get(index)?.trim() ?: return "String"
        val value = first
        return when {
            value.matches(Regex("^-?\\d+\$")) -> {
                return try {
                    val longVal = value.toLong()
                    if (longVal in Int.MIN_VALUE..Int.MAX_VALUE) "Int" else "Long"
                } catch (_: Exception) {
                    "Long"
                }
            }
            value.matches(Regex("^-?\\d+\\.\\d+\$")) -> "Double"
            value.equals("true", true) || value.equals("false", true) -> "Boolean"
            value.matches(Regex("^\\d{4}-\\d{2}-\\d{2}\$")) -> "java.time.LocalDate"
            value.matches(Regex("^\\d{2}:\\d{2}:\\d{2}\$")) -> "java.time.LocalTime"
            value.matches(Regex("^\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}")) -> "java.time.LocalDateTime"
            else -> "String"
        }
    }

    private fun copyResult() {
        val text = outputArea.text
        if (text.isBlank()) return
        val selection = java.awt.datatransfer.StringSelection(text)
        Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
        JOptionPane.showMessageDialog(this, "生成した data class をコピーしました。", "Copy", JOptionPane.INFORMATION_MESSAGE)
    }
}
