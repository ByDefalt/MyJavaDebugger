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
        this.theme = ThemeManager.getInstance().getTheme();
        setLayout(new BorderLayout());
        setBackground(theme.getBackgroundSecondary());
        outputArea = createOutputArea();
        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);
        applyTitledBorder("DEBUG CONSOLE");
    }
    private JTextArea createOutputArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setBackground(theme.getBackgroundPrimary());
        area.setForeground(new Color(150, 250, 150)); 
        area.setFont(new Font("Consolas", Font.PLAIN, 12));
        area.setCaretColor(theme.getTextPrimary());
        return area;
    }
    private void applyTitledBorder(String title) {
        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, theme.getBorderColor()),
                title);
        border.setTitleColor(theme.getTextMuted());
        border.setTitleFont(theme.getSmallFont());
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
