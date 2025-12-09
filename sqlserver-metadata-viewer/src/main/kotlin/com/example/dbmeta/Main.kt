package com.example.dbmeta

import com.example.dbmeta.ui.MainFrame
import javax.swing.SwingUtilities
import javax.swing.UIManager

fun main() {
    try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (_: Exception) {
        // keep default L&F
    }
    SwingUtilities.invokeLater {
        MainFrame().isVisible = true
    }
}
