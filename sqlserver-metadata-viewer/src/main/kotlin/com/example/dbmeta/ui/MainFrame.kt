package com.example.dbmeta.ui

import com.example.dbmeta.model.ConnectionProfile
import com.example.dbmeta.model.DatabaseMetadataSnapshot
import com.example.dbmeta.service.ConnectionProfileService
import com.example.dbmeta.service.MetadataService
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.SwingWorker
import javax.swing.border.EmptyBorder

class MainFrame : JFrame("SQL Server Metadata Viewer") {
    private val profileService = ConnectionProfileService()
    private val metadataService = MetadataService()

    private var currentProfile: ConnectionProfile? = null
    private var currentSnapshot: DatabaseMetadataSnapshot? = null

    private val profileCombo = JComboBox<ConnectionProfile>()
    private val refreshButton = JButton("メタデータ再取得")
    private val settingsButton = JButton("接続設定")
    private val statusLabel = JLabel("Ready")

    private val tableListPanel = TableListPanel()
    private val searchPanel = LogicalNameSearchPanel()
    private val dataClassPanel = DataClassGeneratorPanel()

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(1100, 780)
        setLocationRelativeTo(null)
        layout = BorderLayout()

        add(buildTopPanel(), BorderLayout.NORTH)
        add(buildTabbedPane(), BorderLayout.CENTER)
        add(buildStatusPanel(), BorderLayout.SOUTH)

        bindActions()
        reloadProfiles()
    }

    private fun buildTopPanel(): JPanel = JPanel(BorderLayout()).apply {
        border = EmptyBorder(8, 8, 4, 8)
        val left = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JLabel("プロファイル:"))
            add(profileCombo)
        }
        val right = JPanel(FlowLayout(FlowLayout.RIGHT)).apply {
            add(settingsButton)
            add(refreshButton)
        }
        add(left, BorderLayout.WEST)
        add(right, BorderLayout.EAST)
    }

    private fun buildTabbedPane(): JTabbedPane = JTabbedPane().apply {
        addTab("テーブル・列一覧", tableListPanel)
        addTab("論理名検索", searchPanel)
        addTab("データクラス生成", dataClassPanel)
    }

    private fun buildStatusPanel(): JPanel = JPanel(BorderLayout()).apply {
        border = BorderFactory.createEmptyBorder(4, 8, 4, 8)
        add(statusLabel, BorderLayout.WEST)
    }

    private fun bindActions() {
        profileCombo.addActionListener {
            val selected = profileCombo.selectedItem as? ConnectionProfile ?: return@addActionListener
            profileService.setLastSelected(selected.id)
            currentProfile = selected
            loadCachedMetadata(selected)
        }
        settingsButton.addActionListener { openProfileDialog() }
        refreshButton.addActionListener { refreshMetadata(force = true) }
    }

    private fun reloadProfiles() {
        val profiles = profileService.reload()
        profileCombo.removeAllItems()
        profiles.forEach { profileCombo.addItem(it) }
        val toSelect = profiles.firstOrNull { it.id == profileService.lastSelectedProfileId } ?: profiles.firstOrNull()
        profileCombo.selectedItem = toSelect
        currentProfile = toSelect
        toSelect?.let { loadCachedMetadata(it) }
    }

    private fun loadCachedMetadata(profile: ConnectionProfile) {
        val cached = metadataService.loadCached(profile.id)
        if (cached != null) {
            applySnapshot(cached)
            setStatus("Profile: ${profile.name} | DB: ${cached.databaseName} | Last fetched: ${cached.fetchedAt}")
        } else {
            setStatus("Profile: ${profile.name} | DB: ${profile.database} | No cached metadata")
        }
    }

    private fun refreshMetadata(force: Boolean) {
        val profile = currentProfile ?: return
        val password = resolvePassword(profile) ?: return
        setStatus("Fetching metadata...")
        refreshButton.isEnabled = false
        object : SwingWorker<Unit, Unit>() {
            private var success = false
            override fun doInBackground() {
                try {
                    val snapshot = metadataService.fetchAndSave(profile, password)
                    currentSnapshot = snapshot
                    tableListPanel.setMetadata(snapshot)
                    searchPanel.setMetadata(snapshot)
                    dataClassPanel.setMetadata(snapshot)
                    setStatus("Profile: ${profile.name} | DB: ${snapshot.databaseName} | Last fetched: ${snapshot.fetchedAt}")
                    success = true
                } catch (ex: Exception) {
                    showError("メタデータ取得に失敗しました: ${ex.message}")
                    setStatus("Failed to fetch metadata")
                }
            }

            override fun done() {
                refreshButton.isEnabled = true
                if (success) {
                    JOptionPane.showMessageDialog(
                        this@MainFrame,
                        "メタデータの再取得が完了しました。",
                        "情報",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                }
            }
        }.execute()
    }

    private fun resolvePassword(profile: ConnectionProfile): String? {
        val saved = if (profile.savePassword) profile.decodedPassword() else null
        if (!saved.isNullOrBlank()) return saved
        val passwordField = javax.swing.JPasswordField()
        val result = JOptionPane.showConfirmDialog(
            this,
            passwordField,
            "パスワードを入力してください (${profile.name})",
            JOptionPane.OK_CANCEL_OPTION
        )
        return if (result == JOptionPane.OK_OPTION) {
            passwordField.password?.joinToString("") ?: ""
        } else null
    }

    private fun applySnapshot(snapshot: DatabaseMetadataSnapshot) {
        currentSnapshot = snapshot
        tableListPanel.setMetadata(snapshot)
        searchPanel.setMetadata(snapshot)
        dataClassPanel.setMetadata(snapshot)
    }

    private fun openProfileDialog() {
        ConnectionProfileDialog(this, profileService) {
            reloadProfiles()
        }.isVisible = true
    }

    private fun setStatus(text: String) {
        statusLabel.text = text
    }

    private fun showError(message: String) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE)
    }
}
