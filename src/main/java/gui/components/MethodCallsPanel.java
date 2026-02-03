package gui.components;
import gui.theme.Theme;
import gui.theme.ThemeManager;
import models.ExecutionSnapshot;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
public class MethodCallsPanel extends JPanel {
    private final JList<MethodCallEntry> callsList;
    private final DefaultListModel<MethodCallEntry> callsModel;
    private final Theme theme;
    private final JButton closeButton;
    private MethodCallsListener listener;
    private List<MethodCallEntry> allCalls = new ArrayList<>();
    public interface MethodCallsListener {
        void onMethodCallSelected(int stepNumber);
        void onClose();
    }
    public MethodCallsPanel() {
        this.theme = ThemeManager.getInstance().getTheme();
        this.callsModel = new DefaultListModel<>();
        setLayout(new BorderLayout());
        setBackground(theme.getBackgroundSecondary());
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(theme.getBackgroundTertiary());
        JLabel titleLabel = new JLabel("  📞 All Method Calls");
        titleLabel.setForeground(theme.getTextPrimary());
        titleLabel.setFont(theme.getUIFont().deriveFont(Font.BOLD));
        closeButton = new JButton("✕");
        closeButton.setForeground(theme.getTextMuted());
        closeButton.setBackground(theme.getBackgroundTertiary());
        closeButton.setBorderPainted(false);
        closeButton.setFocusPainted(false);
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> {
            if (listener != null) listener.onClose();
        });
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        headerPanel.add(closeButton, BorderLayout.EAST);
        callsList = createList();
        JScrollPane scrollPane = new JScrollPane(callsList);
        scrollPane.setBorder(null);
        add(headerPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        applyTitledBorder("METHOD CALLS");
    }
    private JList<MethodCallEntry> createList() {
        JList<MethodCallEntry> list = new JList<>(callsModel);
        list.setBackground(theme.getBackgroundPrimary());
        list.setForeground(theme.getTextSecondary());
        list.setSelectionBackground(new Color(theme.getAccentPrimary().getRed(),
                theme.getAccentPrimary().getGreen(),
                theme.getAccentPrimary().getBlue(), 150));
        list.setSelectionForeground(Color.WHITE);
        list.setFont(theme.getUIFont());
        list.setCellRenderer(new MethodCallCellRenderer());
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = list.getSelectedIndex();
                    if (index >= 0 && listener != null) {
                        MethodCallEntry entry = callsModel.getElementAt(index);
                        int targetStep = entry.isExternal ? entry.stepNumber : Math.max(0, entry.stepNumber - 1);
                        listener.onMethodCallSelected(targetStep);
                    }
                }
            }
        });
        return list;
    }
    private void applyTitledBorder(String title) {
        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, theme.getBorderColor()),
                title);
        border.setTitleColor(theme.getTextMuted());
        border.setTitleFont(theme.getSmallFont());
        setBorder(border);
    }
    public void setListener(MethodCallsListener listener) {
        this.listener = listener;
    }
    public void updateMethodCalls(List<ExecutionSnapshot> snapshots) {
        SwingUtilities.invokeLater(() -> {
            callsModel.clear();
            allCalls.clear();
            if (snapshots == null || snapshots.isEmpty()) {
                return;
            }
            Set<String> seenCalls = new HashSet<>();
            for (int i = 0; i < snapshots.size(); i++) {
                ExecutionSnapshot snapshot = snapshots.get(i);
                List<ExecutionSnapshot.StackFrameSnapshot> frames = snapshot.getStackFrames();
                if (frames.isEmpty()) continue;
                if (frames.size() >= 2) {
                    ExecutionSnapshot.StackFrameSnapshot callee = frames.get(0);
                    ExecutionSnapshot.StackFrameSnapshot caller = frames.get(1);
                    String uniqueKey = caller.getClassName() + "." + caller.getMethodName()
                            + "@" + caller.getLineNumber()
                            + "->" + callee.getClassName() + "." + callee.getMethodName();
                    if (!seenCalls.contains(uniqueKey)) {
                        seenCalls.add(uniqueKey);
                        MethodCallEntry entry = new MethodCallEntry(
                                snapshot.getStepNumber(),
                                callee.getClassName(),
                                callee.getMethodName(),
                                caller.getSourceFile(),
                                caller.getLineNumber()  
                        );
                        allCalls.add(entry);
                        callsModel.addElement(entry);
                    }
                }
                if (i > 0) {
                    ExecutionSnapshot prevSnapshot = snapshots.get(i - 1);
                    detectStepOverCalls(prevSnapshot, snapshot, seenCalls);
                }
            }
        });
    }
    private void detectStepOverCalls(ExecutionSnapshot prev, ExecutionSnapshot current, Set<String> seenCalls) {
        if (!prev.getClassName().equals(current.getClassName()) ||
            !prev.getMethodName().equals(current.getMethodName())) {
            return; 
        }
        int prevLine = prev.getLineNumber();
        int currLine = current.getLineNumber();
        if (currLine > prevLine || currLine < prevLine) {
            String sourceCode = getSourceLine(prev.getSourceFile(), prev.getClassName(), prevLine);
            if (sourceCode != null) {
                List<String> methodCalls = extractMethodCalls(sourceCode);
                for (String methodCall : methodCalls) {
                    String uniqueKey = prev.getClassName() + "." + prev.getMethodName()
                            + "@" + prevLine + "->EXTERNAL." + methodCall;
                    if (!seenCalls.contains(uniqueKey)) {
                        seenCalls.add(uniqueKey);
                        MethodCallEntry entry = new MethodCallEntry(
                                prev.getStepNumber(),
                                "(external)",
                                methodCall,
                                prev.getSourceFile(),
                                prevLine,
                                true  
                        );
                        allCalls.add(entry);
                        callsModel.addElement(entry);
                    }
                }
            }
        }
    }
    private String getSourceLine(String sourceFile, String className, int lineNumber) {
        String packagePath = className.contains(".")
                ? className.substring(0, className.lastIndexOf('.')).replace('.', '/') + "/"
                : "";
        String[] possiblePaths = {
                "src/main/java/" + packagePath + sourceFile,
                "src/main/java/dbg/" + sourceFile,
                "src/main/java/" + sourceFile,
                "src/" + packagePath + sourceFile,
                sourceFile
        };
        for (String path : possiblePaths) {
            try {
                java.nio.file.Path p = java.nio.file.Paths.get(path);
                if (java.nio.file.Files.exists(p)) {
                    List<String> lines = java.nio.file.Files.readAllLines(p);
                    if (lineNumber > 0 && lineNumber <= lines.size()) {
                        return lines.get(lineNumber - 1);
                    }
                }
            } catch (Exception e) {
            }
        }
        return null;
    }
    private List<String> extractMethodCalls(String line) {
        List<String> calls = new ArrayList<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "\\.\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\("
        );
        java.util.regex.Matcher matcher = pattern.matcher(line);
        while (matcher.find()) {
            String methodName = matcher.group(1);
            if (!methodName.equals("class") && !methodName.equals("new")) {
                calls.add(methodName);
            }
        }
        return calls;
    }
    public void filterByClassAndMethod(String className, String methodName) {
        SwingUtilities.invokeLater(() -> {
            callsModel.clear();
            for (MethodCallEntry entry : allCalls) {
                boolean matchesClass = className == null || className.isEmpty() ||
                        entry.className.equals(className) ||
                        entry.className.endsWith("." + className);
                boolean matchesMethod = methodName == null || methodName.isEmpty() ||
                        entry.methodName.equals(methodName);
                if (matchesClass && matchesMethod) {
                    callsModel.addElement(entry);
                }
            }
        });
    }
    public void setTitle(String title) {
        SwingUtilities.invokeLater(() -> {
            TitledBorder border = BorderFactory.createTitledBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, theme.getBorderColor()),
                    title);
            border.setTitleColor(theme.getTextMuted());
            border.setTitleFont(theme.getSmallFont());
            setBorder(border);
        });
    }
    private class MethodCallCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof MethodCallEntry) {
                MethodCallEntry entry = (MethodCallEntry) value;
                setText(entry.toDisplayString());
                if (!isSelected) {
                    setBackground(index % 2 == 0 ?
                            theme.getBackgroundPrimary() :
                            theme.getBackgroundSecondary());
                }
            }
            return this;
        }
    }
    public static class MethodCallEntry {
        public final int stepNumber;
        public final String className;
        public final String methodName;
        public final String sourceFile;
        public final int lineNumber;
        public final boolean isExternal;
        public MethodCallEntry(int stepNumber, String className, String methodName,
                String sourceFile, int lineNumber) {
            this(stepNumber, className, methodName, sourceFile, lineNumber, false);
        }
        public MethodCallEntry(int stepNumber, String className, String methodName,
                String sourceFile, int lineNumber, boolean isExternal) {
            this.stepNumber = stepNumber;
            this.className = className;
            this.methodName = methodName;
            this.sourceFile = sourceFile;
            this.lineNumber = lineNumber;
            this.isExternal = isExternal;
        }
        public String toDisplayString() {
            String shortClassName = className.contains(".") ?
                    className.substring(className.lastIndexOf('.') + 1) : className;
            return String.format("%s.%s() called at line %d",
                    shortClassName, methodName, lineNumber);
        }
        @Override
        public String toString() {
            return toDisplayString();
        }
    }
}
