package gui.components;
import gui.theme.Theme;
import gui.theme.ThemeManager;
import models.ExecutionSnapshot;
import models.VariableSnapshot;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
public class VariableHistoryPanel extends JPanel {
    private final Theme theme;
    private final JLabel titleLabel;
    private final JTable historyTable;
    private final DefaultTableModel tableModel;
    private final JLabel emptyLabel;
    private final JButton closeButton;
    private String currentVariableId;
    private String currentVariableName;
    public interface HistoryPanelListener {
        void onClose();
        void onStepSelected(int stepNumber);
    }
    private HistoryPanelListener listener;
    public VariableHistoryPanel() {
        this.theme = ThemeManager.getInstance().getTheme();
        setLayout(new BorderLayout(0, 5));
        setBackground(theme.getBackgroundSecondary());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(theme.getBackgroundSecondary());
        titleLabel = new JLabel("Variable History");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(theme.getTextPrimary());
        closeButton = new JButton("✕");
        closeButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        closeButton.setForeground(theme.getTextSecondary());
        closeButton.setBackground(theme.getBackgroundSecondary());
        closeButton.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        closeButton.setFocusPainted(false);
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> {
            if (listener != null) listener.onClose();
        });
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(closeButton, BorderLayout.EAST);
        String[] columns = {"Step", "Value", "Method", "Line"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        historyTable = new JTable(tableModel);
        historyTable.setBackground(theme.getBackgroundPrimary());
        historyTable.setForeground(theme.getTextPrimary());
        historyTable.setSelectionBackground(theme.getAccentPrimary());
        historyTable.setSelectionForeground(Color.WHITE);
        historyTable.setGridColor(theme.getBorderColor());
        historyTable.setRowHeight(25);
        historyTable.getTableHeader().setBackground(theme.getBackgroundSecondary());
        historyTable.getTableHeader().setForeground(theme.getTextPrimary());
        historyTable.setFont(new Font("Consolas", Font.PLAIN, 12));
        historyTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        historyTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        historyTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        historyTable.getColumnModel().getColumn(3).setPreferredWidth(50);
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        historyTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        historyTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        ValueChangeCellRenderer valueRenderer = new ValueChangeCellRenderer();
        historyTable.getColumnModel().getColumn(1).setCellRenderer(valueRenderer);
        historyTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = historyTable.getSelectedRow();
                    if (row >= 0 && listener != null) {
                        int stepNumber = (int) tableModel.getValueAt(row, 0);
                        int targetStep = Math.max(0, stepNumber - 1);
                        listener.onStepSelected(targetStep);
                    }
                }
            }
        });
        JScrollPane scrollPane = new JScrollPane(historyTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(theme.getBorderColor()));
        scrollPane.getViewport().setBackground(theme.getBackgroundPrimary());
        emptyLabel = new JLabel("Double-click a variable to see its history");
        emptyLabel.setForeground(theme.getTextMuted());
        emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        emptyLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        add(headerPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        showEmptyState();
    }
    public void setListener(HistoryPanelListener listener) {
        this.listener = listener;
    }
    public void showVariableHistory(String variableId, String variableName, List<ExecutionSnapshot> snapshots) {
        this.currentVariableId = variableId;
        this.currentVariableName = variableName;
        titleLabel.setText("History: " + variableName);
        tableModel.setRowCount(0);
        List<HistoryEntry> entries = extractVariableHistory(variableId, variableName, snapshots);
        String previousValue = null;
        int previousSize = -1;
        boolean isFirstEntry = true;

        for (HistoryEntry entry : entries) {
            boolean changed = previousValue != null && !previousValue.equals(entry.value);
            boolean isCollection = isCollectionValue(entry.value);
            boolean sizeIncreased = false;
            boolean sizeDecreased = false;

            // Si c'est une collection, vérifier le changement de taille
            if (isCollection && previousValue != null && isCollectionValue(previousValue)) {
                int currentSize = extractCollectionSize(entry.value);
                if (currentSize != -1 && previousSize != -1) {
                    if (currentSize > previousSize) {
                        sizeIncreased = true;
                    } else if (currentSize < previousSize) {
                        sizeDecreased = true;
                    }
                }
                previousSize = currentSize;
            } else if (isCollection) {
                previousSize = extractCollectionSize(entry.value);
            }

            Object[] row = {
                entry.stepNumber,
                new ValueCell(entry.value, changed, isCollection, sizeIncreased, sizeDecreased, isFirstEntry),
                entry.methodName,
                entry.lineNumber
            };
            tableModel.addRow(row);
            previousValue = entry.value;
            isFirstEntry = false;
        }
        if (entries.isEmpty()) {
            showEmptyState();
        } else {
            emptyLabel.setVisible(false);
            historyTable.setVisible(true);
        }
        revalidate();
        repaint();
    }
    private List<HistoryEntry> extractVariableHistory(String variableId, String variableName, List<ExecutionSnapshot> snapshots) {
        List<HistoryEntry> entries = new ArrayList<>();
        String lastValue = null;
        for (ExecutionSnapshot snapshot : snapshots) {
            VariableSnapshot found = null;
            if (variableId != null) {
                found = snapshot.getVariableById(variableId);
            }
            if (found == null) {
                for (VariableSnapshot vs : snapshot.getVariableSnapshots()) {
                    if (vs.getName().equals(variableName)) {
                        found = vs;
                        break;
                    }
                }
            }
            if (found != null) {
                String currentValue = found.getValue();
                if (lastValue == null || !lastValue.equals(currentValue)) {
                    entries.add(new HistoryEntry(
                        snapshot.getStepNumber(),
                        currentValue,
                        snapshot.getMethodName(),
                        snapshot.getLineNumber()
                    ));
                    lastValue = currentValue;
                }
            }
        }
        return entries;
    }

    /**
     * Extrait la taille d'une collection depuis sa représentation en string.
     * Exemples supportés:
     * - "ArrayList (size = 5)" -> 5
     * - "Array[10]" -> 10
     * - "[1, 2, 3]" -> 3
     * - "size=5" -> 5
     * @return La taille si détectée, sinon -1
     */
    private int extractCollectionSize(String value) {
        if (value == null) return -1;

        //Pattern: "size = X" ou "size=X"
        java.util.regex.Pattern sizePattern = java.util.regex.Pattern.compile("size\\s*=\\s*(\\d+)");
        java.util.regex.Matcher sizeMatcher = sizePattern.matcher(value);
        if (sizeMatcher.find()) {
            return Integer.parseInt(sizeMatcher.group(1));
        }

        // Pattern: "Array[X]"
        java.util.regex.Pattern arrayPattern = java.util.regex.Pattern.compile("Array\\[(\\d+)\\]");
        java.util.regex.Matcher arrayMatcher = arrayPattern.matcher(value);
        if (arrayMatcher.find()) {
            return Integer.parseInt(arrayMatcher.group(1));
        }

        // Pattern: "[a, b, c]" - compter les éléments séparés par des virgules
        if (value.startsWith("[") && value.endsWith("]")) {
            String content = value.substring(1, value.length() - 1).trim();
            if (content.isEmpty()) return 0;
            // Compter les virgules + 1
            return content.split(",").length;
        }

        return -1;
    }

    /**
     * Vérifie si une valeur représente une collection
     */
    private boolean isCollectionValue(String value) {
        if (value == null) return false;
        return value.contains("size =") || value.contains("size=") ||
               value.contains("Array[") || value.contains("List") ||
               value.contains("Collection") || value.contains("Set") ||
               value.contains("ArrayList") || value.contains("LinkedList") ||
               value.contains("HashSet") || value.contains("TreeSet") ||
               value.contains("Vector");
    }
    private void showEmptyState() {
        tableModel.setRowCount(0);
        titleLabel.setText("Variable History");
        emptyLabel.setVisible(true);
    }
    public void clear() {
        showEmptyState();
        currentVariableId = null;
        currentVariableName = null;
    }
    private static class HistoryEntry {
        final int stepNumber;
        final String value;
        final String methodName;
        final int lineNumber;
        HistoryEntry(int stepNumber, String value, String methodName, int lineNumber) {
            this.stepNumber = stepNumber;
            this.value = value;
            this.methodName = methodName;
            this.lineNumber = lineNumber;
        }
    }
    private static class ValueCell {
        final String value;
        final boolean changed;
        final boolean isCollection;
        final boolean sizeIncreased;
        final boolean sizeDecreased;
        final boolean isInitialization;

        ValueCell(String value, boolean changed) {
            this(value, changed, false, false, false, false);
        }

        ValueCell(String value, boolean changed, boolean isCollection, boolean sizeIncreased, boolean sizeDecreased) {
            this(value, changed, isCollection, sizeIncreased, sizeDecreased, false);
        }

        ValueCell(String value, boolean changed, boolean isCollection, boolean sizeIncreased, boolean sizeDecreased, boolean isInitialization) {
            this.value = value;
            this.changed = changed;
            this.isCollection = isCollection;
            this.sizeIncreased = sizeIncreased;
            this.sizeDecreased = sizeDecreased;
            this.isInitialization = isInitialization;
        }

        @Override
        public String toString() {
            return value;
        }
    }
    private class ValueChangeCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value instanceof ValueCell) {
                ValueCell cell = (ValueCell) value;
                setText(cell.value);
                if (!isSelected) {
                    // Priorité 1: Initialisation (première occurrence)
                    if (cell.isInitialization) {
                        // Orange pour l'initialisation
                        setBackground(new Color(100, 70, 30));
                        setForeground(new Color(255, 200, 120));
                    }
                    // Priorité 2: Changements de taille de collection
                    else if (cell.isCollection && cell.sizeDecreased) {
                        // Rouge pour diminution de taille
                        setBackground(new Color(100, 40, 40));
                        setForeground(new Color(255, 150, 150));
                    } else if (cell.isCollection && cell.sizeIncreased) {
                        // Vert pour augmentation de taille
                        setBackground(new Color(40, 100, 40));
                        setForeground(new Color(150, 255, 150));
                    } else {
                        // Pas de coloration pour les changements de variables classiques
                        setBackground(theme.getBackgroundPrimary());
                        setForeground(theme.getTextPrimary());
                    }
                }
            }
            return c;
        }
    }
}
