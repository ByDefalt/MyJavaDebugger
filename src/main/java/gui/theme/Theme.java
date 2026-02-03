package gui.theme;

import java.awt.*;

public interface Theme {

    Color getBackgroundPrimary();
    Color getBackgroundSecondary();
    Color getBackgroundTertiary();

    Color getTextPrimary();
    Color getTextSecondary();
    Color getTextMuted();

    Color getAccentPrimary();
    Color getAccentSuccess();
    Color getAccentDanger();
    Color getAccentWarning();

    Color getCodeKeyword();
    Color getCodeString();
    Color getCodeNumber();
    Color getCodeComment();
    Color getCodeAnnotation();
    Color getCodeMethod();
    Color getCodeDefault();

    Color getCurrentLineHighlight();
    Color getCurrentLineMarker();
    Color getBreakpointColor();
    Color getLineNumberBackground();
    Color getLineNumberForeground();

    Color getBorderColor();

    Font getCodeFont();
    Font getUIFont();
    Font getUIFontBold();
    Font getSmallFont();
}
