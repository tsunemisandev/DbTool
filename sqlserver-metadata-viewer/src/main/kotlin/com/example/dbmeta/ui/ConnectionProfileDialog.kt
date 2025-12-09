package com.example.dbmeta.ui

import com.example.dbmeta.model.ConnectionProfile
import com.example.dbmeta.service.ConnectionProfileService
import com.example.dbmeta.service.ConnectionTester
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JPasswordField
import javax.swing.JScrollPane
import javax.swing.JSpinner
import javax.swing.JTextField
import javax.swing.SpinnerNumberModel
import javax.swing.border.EmptyBorder

class ConnectionProfileDialog(
    parent: java.awt.Frame,
    private val profileService: ConnectionProfileService,
    private val onUpdated: () -> Unit
) : JDialog(parent, "接続設定", true) {

    private val listModel = DefaultListModel<ConnectionProfile>()
    private val profileList = JList(listModel)

    private val nameField = JTextField(18)
    private val hostField = JTextField(18)
    private val portField = JSpinner(SpinnerNumberModel(1433, 1, 65535, 1))
    private val dbField = JTextField(18)
    private val userField = JTextField(18)
    private val passwordField = JPasswordField(18)
    private val savePasswordCheck = javax.swing.JCheckBox("パスワードを保存")
    private val tester = ConnectionTester()

    init {
        setSize(640, 420)
        setLocationRelativeTo(parent)
        layout = BorderLayout()
        add(buildMainPanel(), BorderLayout.CENTER)
        add(buildButtonPanel(), BorderLayout.SOUTH)
        profileList.addListSelectionListener { if (!it.valueIsAdjusting) loadSelectedProfile() }
        loadProfiles()
        loadSelectedProfile()
    }

    private fun buildMainPanel(): JPanel = JPanel(BorderLayout()).apply {
        border = EmptyBorder(8, 8, 8, 8)
        add(JScrollPane(profileList), BorderLayout.WEST)
        add(buildFormPanel(), BorderLayout.CENTER)
    }

    private fun buildFormPanel(): JPanel = JPanel().apply {
        layout = java.awt.GridBagLayout()
        val c = java.awt.GridBagConstraints().apply {
            anchor = java.awt.GridBagConstraints.WEST
            insets = java.awt.Insets(4, 4, 4, 4)
        }
        fun addRow(row: Int, label: String, component: java.awt.Component) {
            c.gridx = 0
            c.gridy = row
            add(JLabel(label), c)
            c.gridx = 1
            add(component, c)
        }
        addRow(0, "プロファイル名", nameField)
        addRow(1, "ホスト", hostField)
        addRow(2, "ポート", portField)
        addRow(3, "DB名", dbField)
        addRow(4, "ユーザー名", userField)
        addRow(5, "パスワード", passwordField)
        addRow(6, "", savePasswordCheck)
    }

    private fun buildButtonPanel(): JPanel = JPanel(FlowLayout(FlowLayout.RIGHT)).apply {
        val newBtn = JButton("新規")
        val saveBtn = JButton("保存")
        val deleteBtn = JButton("削除")
        val testBtn = JButton("接続テスト")
        val closeBtn = JButton("閉じる")

        newBtn.addActionListener { clearForm() }
        saveBtn.addActionListener { saveProfile() }
        deleteBtn.addActionListener { deleteProfile() }
        testBtn.addActionListener { testConnection() }
        closeBtn.addActionListener {
            onUpdated()
            dispose()
        }

        add(newBtn)
        add(saveBtn)
        add(deleteBtn)
        add(testBtn)
        add(closeBtn)
    }

    private fun loadProfiles() {
        listModel.clear()
        profileService.profiles.forEach { listModel.addElement(it) }
        if (listModel.size() > 0) {
            profileList.selectedIndex = 0
        }
    }

    private fun loadSelectedProfile() {
        val profile = profileList.selectedValue ?: return
        nameField.text = profile.name
        hostField.text = profile.host
        portField.value = profile.port
        dbField.text = profile.database
        userField.text = profile.username
        passwordField.text = profile.decodedPassword() ?: ""
        savePasswordCheck.isSelected = profile.savePassword
    }

    private fun clearForm() {
        profileList.clearSelection()
        nameField.text = ""
        hostField.text = ""
        portField.value = 1433
        dbField.text = ""
        userField.text = ""
        passwordField.text = ""
        savePasswordCheck.isSelected = false
    }

    private fun saveProfile() {
        val profile = buildProfileFromForm() ?: return
        val saved = profileService.addOrUpdate(profile)
        loadProfiles()
        profileList.setSelectedValue(saved, true)
    }

    private fun buildProfileFromForm(): ConnectionProfile? {
        val name = nameField.text.trim()
        val host = hostField.text.trim()
        val port = (portField.value as? Number)?.toInt() ?: 1433
        val db = dbField.text.trim()
        val user = userField.text.trim()
        if (name.isEmpty() || host.isEmpty() || db.isEmpty() || user.isEmpty()) {
            JOptionPane.showMessageDialog(this, "必須項目を入力してください", "Validation", JOptionPane.WARNING_MESSAGE)
            return null
        }
        val selectedId = profileList.selectedValue?.id ?: ""
        val rawPassword = String(passwordField.password)
        val encrypted = if (savePasswordCheck.isSelected && rawPassword.isNotEmpty()) {
            java.util.Base64.getEncoder().encodeToString(rawPassword.toByteArray())
        } else null

        return ConnectionProfile(
            id = selectedId,
            name = name,
            host = host,
            port = port,
            database = db,
            username = user,
            savePassword = savePasswordCheck.isSelected,
            encryptedPassword = encrypted,
            runtimePassword = rawPassword.ifEmpty { null }
        )
    }

    private fun testConnection() {
        val profile = buildProfileFromForm() ?: return
        val password = profile.runtimePassword ?: profile.decodedPassword()
        try {
            tester.test(profile, password)
            JOptionPane.showMessageDialog(this, "接続成功しました。", "Connection Test", JOptionPane.INFORMATION_MESSAGE)
        } catch (ex: Exception) {
            JOptionPane.showMessageDialog(this, "接続に失敗しました: ${ex.message}", "Connection Test", JOptionPane.ERROR_MESSAGE)
        }
    }

    private fun deleteProfile() {
        val target = profileList.selectedValue ?: return
        val result = JOptionPane.showConfirmDialog(
            this,
            "プロファイル '${target.name}' を削除しますか？",
            "Confirm",
            JOptionPane.OK_CANCEL_OPTION
        )
        if (result == JOptionPane.OK_OPTION) {
            profileService.delete(target.id)
            loadProfiles()
            clearForm()
        }
    }
}
