package gui.components;
import gui.theme.Theme;
import gui.theme.ThemeManager;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
public class OutputPanel extends JPanel {
    private final JTextArea outputArea;
    private final Theme theme;
    public OutputPanel() {
        this("Console");
    }

    public OutputPanel(String title) {
        this.theme = ThemeManager.getInstance().getTheme();
        setLayout(new BorderLayout());
        setBackground(theme.getBackgroundPrimary());
        outputArea = createOutputArea();
        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
        applyTitledBorder(title);
    }
    private JTextArea createOutputArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setBackground(theme.getBackgroundPrimary());
        area.setForeground(theme.getTextPrimary());
        area.setFont(theme.getCodeFont());
        area.setCaretColor(theme.getTextPrimary());
        area.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        area.setLineWrap(false);
        return area;
    }
    private void applyTitledBorder(String title) {
        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, theme.getBorderColor()),
                    BorderFactory.createEmptyBorder(4, 8, 4, 8)
                ),
                title);
        border.setTitleColor(theme.getTextMuted());
        border.setTitleFont(theme.getUIFontBold());
        setBorder(border);
    }
    public void appendOutput(String text) {
        SwingUtilities.invokeLater(() -> {
            outputArea.append(text);
            outputArea.setCaretPosition(outputArea.getDocument().getLength());
        });
    }
    public void clear() {
        SwingUtilities.invokeLater(() -> outputArea.setText(""));
    }
}
