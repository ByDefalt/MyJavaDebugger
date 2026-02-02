package gui.theme;

import java.awt.*;

/**
 * Interface pour les thèmes de l'application (OCP - Open/Closed Principle)
 * Permet d'ajouter de nouveaux thèmes sans modifier le code existant
 */
public interface Theme {

    // Couleurs de fond
    Color getBackgroundPrimary();
    Color getBackgroundSecondary();
    Color getBackgroundTertiary();

    // Couleurs de texte
    Color getTextPrimary();
    Color getTextSecondary();
    Color getTextMuted();

    // Couleurs d'accentuation
    Color getAccentPrimary();
    Color getAccentSuccess();
    Color getAccentDanger();
    Color getAccentWarning();

    // Couleurs spécifiques au code
    Color getCodeKeyword();
    Color getCodeString();
    Color getCodeNumber();
    Color getCodeComment();
    Color getCodeAnnotation();
    Color getCodeMethod();
    Color getCodeDefault();

    // Couleurs du debugger
    Color getCurrentLineHighlight();
    Color getCurrentLineMarker();
    Color getBreakpointColor();
    Color getLineNumberBackground();
    Color getLineNumberForeground();

    // Bordures
    Color getBorderColor();

    // Fonts
    Font getCodeFont();
    Font getUIFont();
    Font getUIFontBold();
    Font getSmallFont();
}
