package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SourceCodePanel extends JPanel {
    private List<String> sourceLines;
    private int currentLine = -1;
    private Set<Integer> breakpoints;
    private BreakpointClickListener breakpointListener;

    // Palette de couleurs "Modern Dark"
    private static final Color BG_COLOR = new Color(30, 30, 30);
    private static final Color LINE_NUM_BG = new Color(35, 35, 35);
    private static final Color LINE_NUM_FG = new Color(133, 133, 133);
    private static final Color CODE_FG = new Color(212, 212, 212);
    private static final Color HIGHLIGHT_LINE = new Color(45, 45, 45);
    private static final Color CURRENT_LINE_MARKER = new Color(255, 230, 0);
    private static final Color BREAKPOINT_RED = new Color(230, 50, 50);

    private static final int LINE_HEIGHT = 22;
    private static final int GUTTER_WIDTH = 55;

    private JScrollPane scrollPane;
    private CodeDisplayPanel codePanel;

    public interface BreakpointClickListener {
        void onBreakpointToggle(int lineNumber) throws Exception;
    }

    public SourceCodePanel() {
        sourceLines = new ArrayList<>();
        breakpoints = new HashSet<>();
        setLayout(new BorderLayout());
        setBackground(BG_COLOR);

        codePanel = new CodeDisplayPanel();
        scrollPane = new JScrollPane(codePanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void setSourceLines(List<String> lines) {
        this.sourceLines = new ArrayList<>(lines);
        codePanel.updateSize();
        codePanel.repaint();
    }

    public void setCurrentLine(int line) {
        this.currentLine = line;
        codePanel.repaint();
        if (line > 0 && line <= sourceLines.size()) {
            Rectangle rect = new Rectangle(0, (line - 1) * LINE_HEIGHT - 100, codePanel.getWidth(), LINE_HEIGHT + 200);
            codePanel.scrollRectToVisible(rect);
        }
    }

    public void toggleBreakpoint(int line) {
        if (breakpoints.contains(line)) breakpoints.remove(line);
        else breakpoints.add(line);
        codePanel.repaint();
    }

    public void setBreakpointListener(BreakpointClickListener listener) {
        this.breakpointListener = listener;
    }

    private class CodeDisplayPanel extends JPanel {
        public CodeDisplayPanel() {
            setBackground(BG_COLOR);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    int clickedLine = (e.getY() / LINE_HEIGHT) + 1;
                    if (clickedLine > 0 && clickedLine <= sourceLines.size() && e.getX() <= GUTTER_WIDTH) {
                        toggleBreakpoint(clickedLine);
                        if (breakpointListener != null) {
                            try {
                                breakpointListener.onBreakpointToggle(clickedLine);
                            } catch (Exception ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    }
                }
            });
        }

        public void updateSize() {
            setPreferredSize(new Dimension(1000, sourceLines.size() * LINE_HEIGHT + 50));
            revalidate();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Font font = new Font("JetBrains Mono", Font.PLAIN, 13);
            if (font.getFamily().equals("Dialog")) font = new Font("Consolas", Font.PLAIN, 13);
            g2.setFont(font);

            for (int i = 0; i < sourceLines.size(); i++) {
                int y = i * LINE_HEIGHT;
                int lineNum = i + 1;

                // Highlight current line
                if (lineNum == currentLine) {
                    g2.setColor(HIGHLIGHT_LINE);
                    g2.fillRect(0, y, getWidth(), LINE_HEIGHT);
                    g2.setColor(CURRENT_LINE_MARKER);
                    g2.fillRect(0, y, 3, LINE_HEIGHT);
                }

                // Gutter
                g2.setColor(LINE_NUM_BG);
                g2.fillRect(0, y, GUTTER_WIDTH, LINE_HEIGHT);
                g2.setColor(LINE_NUM_FG);
                g2.drawString(String.format("%3d", lineNum), 10, y + 16);

                // Breakpoint
                if (breakpoints.contains(lineNum)) {
                    g2.setColor(BREAKPOINT_RED);
                    g2.fillOval(38, y + 5, 10, 10);
                }

                // Code
                drawSyntaxLine(g2, sourceLines.get(i), GUTTER_WIDTH + 15, y + 16);
            }
        }

        private void drawSyntaxLine(Graphics2D g2, String line, int x, int y) {
            // Configuration des couleurs Darcula
            final Color KEYWORD = new Color(204, 120, 50);
            final Color STRING = new Color(106, 135, 89);
            final Color NUMBER = new Color(104, 151, 187);
            final Color COMMENT = new Color(128, 128, 128);
            final Color ANNOTATION = new Color(187, 181, 41);
            final Color METHOD = new Color(255, 198, 109);
            final Color DEFAULT = new Color(169, 183, 198);

            Set<String> keywords = new HashSet<>(Arrays.asList(
                    "public", "private", "protected", "class", "void", "static", "final",
                    "new", "return", "if", "else", "for", "while", "int", "double",
                    "float", "boolean", "import", "package", "this", "true", "false"
            ));

            // Regex robuste : Capture les commentaires, les strings, les mots/nombres, ou le reste
            Pattern pattern = Pattern.compile("//.*|\"[^\"]*\"|\\b\\w+\\b|\\s+|.");
            Matcher matcher = pattern.matcher(line);

            int currentX = x;

            while (matcher.find()) {
                String token = matcher.group();

                // 1. Détection des Commentaires
                if (token.startsWith("//")) {
                    g2.setColor(COMMENT);
                }
                // 2. Détection des Strings
                else if (token.startsWith("\"")) {
                    g2.setColor(STRING);
                }
                // 3. Détection des Nombres
                else if (token.matches("\\d+")) {
                    g2.setColor(NUMBER);
                }
                // 4. Détection des Mots-clés
                else if (keywords.contains(token)) {
                    g2.setColor(KEYWORD);
                }
                // 5. Détection des Annotations (ex: @Override)
                else if (token.startsWith("@") || (line.contains("@") && line.indexOf("@") < line.indexOf(token) && token.matches("\\w+"))) {
                    // Simplification : si on est juste après un @
                    g2.setColor(ANNOTATION);
                }
                // 6. Détection des Méthodes (mot suivi d'une parenthèse)
                else if (token.matches("\\w+")) {
                    int end = matcher.end();
                    if (end < line.length() && line.charAt(end) == '(') {
                        g2.setColor(METHOD);
                    } else {
                        g2.setColor(DEFAULT);
                    }
                }
                else {
                    g2.setColor(DEFAULT);
                }

                g2.drawString(token, currentX, y);
                currentX += g2.getFontMetrics().stringWidth(token);
            }
        }
    }
}