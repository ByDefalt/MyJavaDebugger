package gui.theme;
import java.awt.*;
public class DarkTheme implements Theme {
    private static final DarkTheme INSTANCE = new DarkTheme();
    public static DarkTheme getInstance() { return INSTANCE; }
    private DarkTheme() {}
    @Override public Color getBackgroundPrimary() { return new Color(43, 43, 43); }
    @Override public Color getBackgroundSecondary() { return new Color(60, 63, 65); }
    @Override public Color getBackgroundTertiary() { return new Color(69, 73, 74); }
    @Override public Color getTextPrimary() { return new Color(187, 187, 187); }
    @Override public Color getTextSecondary() { return new Color(169, 183, 198); }
    @Override public Color getTextMuted() { return new Color(128, 128, 128); }
    @Override public Color getAccentPrimary() { return new Color(76, 130, 197); }
    @Override public Color getAccentSuccess() { return new Color(98, 150, 85); }
    @Override public Color getAccentDanger() { return new Color(204, 82, 82); }
    @Override public Color getAccentWarning() { return new Color(188, 166, 88); }
    @Override public Color getCodeKeyword() { return new Color(204, 120, 50); }
    @Override public Color getCodeString() { return new Color(106, 135, 89); }
    @Override public Color getCodeNumber() { return new Color(104, 151, 187); }
    @Override public Color getCodeComment() { return new Color(128, 128, 128); }
    @Override public Color getCodeAnnotation() { return new Color(187, 181, 41); }
    @Override public Color getCodeMethod() { return new Color(255, 198, 109); }
    @Override public Color getCodeDefault() { return new Color(169, 183, 198); }
    @Override public Color getCurrentLineHighlight() { return new Color(50, 53, 55); }
    @Override public Color getCurrentLineMarker() { return new Color(76, 130, 197); }
    @Override public Color getBreakpointColor() { return new Color(218, 68, 83); }
    @Override public Color getLineNumberBackground() { return new Color(49, 51, 53); }
    @Override public Color getLineNumberForeground() { return new Color(96, 99, 102); }
    @Override public Color getBorderColor() { return new Color(82, 85, 87); }
    @Override
    public Font getCodeFont() {
        Font font = new Font("JetBrains Mono", Font.PLAIN, 13);
        if (font.getFamily().equals("Dialog")) {
            font = new Font("Consolas", Font.PLAIN, 13);
        }
        return font;
    }
    @Override public Font getUIFont() { return new Font("Inter", Font.PLAIN, 13); }
    @Override public Font getUIFontBold() { return new Font("Inter", Font.BOLD, 13); }
    @Override public Font getSmallFont() { return new Font("Inter", Font.PLAIN, 11); }
}
