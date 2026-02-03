package gui.theme;
import java.awt.*;
public class DarkTheme implements Theme {
    private static final DarkTheme INSTANCE = new DarkTheme();
    public static DarkTheme getInstance() { return INSTANCE; }
    private DarkTheme() {}
    @Override public Color getBackgroundPrimary() { return new Color(30, 30, 30); }
    @Override public Color getBackgroundSecondary() { return new Color(37, 37, 38); }
    @Override public Color getBackgroundTertiary() { return new Color(45, 45, 48); }
    @Override public Color getTextPrimary() { return new Color(212, 212, 212); }
    @Override public Color getTextSecondary() { return new Color(200, 200, 200); }
    @Override public Color getTextMuted() { return new Color(150, 150, 150); }
    @Override public Color getAccentPrimary() { return new Color(0, 122, 204); }
    @Override public Color getAccentSuccess() { return new Color(30, 150, 70); }
    @Override public Color getAccentDanger() { return new Color(200, 50, 50); }
    @Override public Color getAccentWarning() { return new Color(255, 230, 0); }
    @Override public Color getCodeKeyword() { return new Color(204, 120, 50); }
    @Override public Color getCodeString() { return new Color(106, 135, 89); }
    @Override public Color getCodeNumber() { return new Color(104, 151, 187); }
    @Override public Color getCodeComment() { return new Color(128, 128, 128); }
    @Override public Color getCodeAnnotation() { return new Color(187, 181, 41); }
    @Override public Color getCodeMethod() { return new Color(255, 198, 109); }
    @Override public Color getCodeDefault() { return new Color(169, 183, 198); }
    @Override public Color getCurrentLineHighlight() { return new Color(45, 45, 45); }
    @Override public Color getCurrentLineMarker() { return new Color(255, 230, 0); }
    @Override public Color getBreakpointColor() { return new Color(230, 50, 50); }
    @Override public Color getLineNumberBackground() { return new Color(35, 35, 35); }
    @Override public Color getLineNumberForeground() { return new Color(133, 133, 133); }
    @Override public Color getBorderColor() { return new Color(60, 60, 60); }
    @Override
    public Font getCodeFont() {
        Font font = new Font("JetBrains Mono", Font.PLAIN, 13);
        if (font.getFamily().equals("Dialog")) {
            font = new Font("Consolas", Font.PLAIN, 13);
        }
        return font;
    }
    @Override public Font getUIFont() { return new Font("Segoe UI", Font.PLAIN, 12); }
    @Override public Font getUIFontBold() { return new Font("Segoe UI", Font.BOLD, 12); }
    @Override public Font getSmallFont() { return new Font("Segoe UI", Font.BOLD, 10); }
}
