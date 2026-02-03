package gui.components;
import gui.theme.Theme;
import gui.theme.ThemeManager;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class SourceCodePanel extends JPanel {
    private List<String> sourceLines;
    private int currentLine = -1;
    private final Set<Integer> breakpoints;
    private final Theme theme;
    private static final int LINE_HEIGHT = 22;
    private static final int GUTTER_WIDTH = 55;
    private final CodeDisplayPanel codePanel;
    private BreakpointListener breakpointListener;
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "public", "private", "protected", "class", "interface", "enum",
            "void", "static", "final", "abstract", "synchronized",
            "new", "return", "if", "else", "for", "while", "do", "switch", "case", "break",
            "int", "double", "float", "boolean", "long", "short", "byte", "char",
            "import", "package", "this", "super", "extends", "implements",
            "true", "false", "null", "try", "catch", "finally", "throw", "throws"
    ));
    public interface BreakpointListener {
        void onBreakpointToggle(int lineNumber) throws Exception;
    }
    public SourceCodePanel() {
        this.theme = ThemeManager.getInstance().getTheme();
        this.sourceLines = new ArrayList<>();
        this.breakpoints = new HashSet<>();
        setLayout(new BorderLayout());
        setBackground(theme.getBackgroundPrimary());
        codePanel = new CodeDisplayPanel();
        JScrollPane scrollPane = new JScrollPane(codePanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }
    public void setSourceLines(List<String> lines) {
        this.sourceLines = new ArrayList<>(lines);
        SwingUtilities.invokeLater(() -> {
            codePanel.updateSize();
            codePanel.revalidate();
            codePanel.repaint();
            revalidate();
            repaint();
        });
    }
    public void setCurrentLine(int line) {
        this.currentLine = line;
        SwingUtilities.invokeLater(() -> {
            codePanel.repaint();
            scrollToLine(line);
        });
    }
    private void scrollToLine(int line) {
        if (line > 0 && line <= sourceLines.size()) {
            Rectangle rect = new Rectangle(0, (line - 1) * LINE_HEIGHT - 100,
                    codePanel.getWidth(), LINE_HEIGHT + 200);
            codePanel.scrollRectToVisible(rect);
        }
    }
    public void toggleBreakpoint(int line) {
        if (breakpoints.contains(line)) {
            breakpoints.remove(line);
        } else {
            breakpoints.add(line);
        }
        codePanel.repaint();
    }
    public void removeBreakpoint(int line) {
        breakpoints.remove(line);
        codePanel.repaint();
    }
    public void addBreakpoint(int line) {
        breakpoints.add(line);
        codePanel.repaint();
    }
    public void setBreakpointListener(BreakpointListener listener) {
        this.breakpointListener = listener;
    }
    public Set<Integer> getBreakpoints() {
        return Collections.unmodifiableSet(breakpoints);
    }
    private class CodeDisplayPanel extends JPanel {
        public CodeDisplayPanel() {
            setBackground(theme.getBackgroundPrimary());
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    handleClick(e);
                }
            });
        }
        private void handleClick(MouseEvent e) {
            int clickedLine = (e.getY() / LINE_HEIGHT) + 1;
            if (clickedLine > 0 && clickedLine <= sourceLines.size() && e.getX() <= GUTTER_WIDTH) {
                toggleBreakpoint(clickedLine);
                if (breakpointListener != null) {
                    try {
                        breakpointListener.onBreakpointToggle(clickedLine);
                    } catch (Exception ex) {
                    }
                }
            }
        }
        public void updateSize() {
            int width = calculateMaxWidth();
            int height = sourceLines.size() * LINE_HEIGHT + 50;
            setPreferredSize(new Dimension(width, height));
            revalidate();
        }
        private int calculateMaxWidth() {
            FontMetrics fm = getFontMetrics(theme.getCodeFont());
            int maxWidth = 800;
            for (String line : sourceLines) {
                int lineWidth = fm.stringWidth(line) + GUTTER_WIDTH + 50;
                maxWidth = Math.max(maxWidth, lineWidth);
            }
            return maxWidth;
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(theme.getCodeFont());
            for (int i = 0; i < sourceLines.size(); i++) {
                paintLine(g2, i);
            }
        }
        private void paintLine(Graphics2D g2, int index) {
            int y = index * LINE_HEIGHT;
            int lineNum = index + 1;
            if (lineNum == currentLine) {
                g2.setColor(theme.getCurrentLineHighlight());
                g2.fillRect(0, y, getWidth(), LINE_HEIGHT);
                g2.setColor(theme.getCurrentLineMarker());
                g2.fillRect(0, y, 3, LINE_HEIGHT);
            }
            g2.setColor(theme.getLineNumberBackground());
            g2.fillRect(0, y, GUTTER_WIDTH, LINE_HEIGHT);
            g2.setColor(theme.getLineNumberForeground());
            g2.drawString(String.format("%3d", lineNum), 10, y + 16);
            if (breakpoints.contains(lineNum)) {
                g2.setColor(theme.getBreakpointColor());
                g2.fillOval(38, y + 5, 10, 10);
            }
            drawSyntaxHighlightedLine(g2, sourceLines.get(index), GUTTER_WIDTH + 15, y + 16);
        }
        private void drawSyntaxHighlightedLine(Graphics2D g2, String line, int x, int y) {
            FontMetrics fm = g2.getFontMetrics();
            Pattern pattern = Pattern.compile("\"[^\"]*\"|'[^']*'|//.*|\\b\\w+\\b|@\\w+|[^\\s]");
            Matcher matcher = pattern.matcher(line);
            int currentX = x;
            int lastEnd = 0;

            while (matcher.find()) {
                // Dessiner les espaces avant le token (pour préserver l'indentation)
                if (matcher.start() > lastEnd) {
                    String spaces = line.substring(lastEnd, matcher.start());
                    currentX += fm.stringWidth(spaces);
                }

                String token = matcher.group();
                g2.setColor(getTokenColor(token, line, matcher.end()));
                g2.drawString(token, currentX, y);
                currentX += fm.stringWidth(token);
                lastEnd = matcher.end();
            }

            // Dessiner les espaces restants à la fin de la ligne
            if (lastEnd < line.length()) {
                String trailing = line.substring(lastEnd);
                currentX += fm.stringWidth(trailing);
            }
        }
        private Color getTokenColor(String token, String line, int tokenEnd) {
            if (token.startsWith("//")) {
                return theme.getCodeComment();
            }
            if (token.startsWith("\"") || token.startsWith("'")) {
                return theme.getCodeString();
            }
            if (token.matches("\\d+(\\.\\d+)?")) {
                return theme.getCodeNumber();
            }
            if (KEYWORDS.contains(token)) {
                return theme.getCodeKeyword();
            }
            if (token.startsWith("@")) {
                return theme.getCodeAnnotation();
            }
            if (token.matches("\\w+") && tokenEnd < line.length() && line.charAt(tokenEnd) == '(') {
                return theme.getCodeMethod();
            }
            return theme.getCodeDefault();
        }
    }
}
