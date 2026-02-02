package gui.components;

import gui.theme.Theme;
import gui.theme.ThemeManager;

import javax.swing.*;
import java.awt.*;

/**
 * Bouton stylisé réutilisable (SRP - Single Responsibility)
 */
public class StyledButton extends JButton {

    public enum ButtonType {
        PRIMARY,
        SUCCESS,
        DANGER,
        DEFAULT
    }

    private final ButtonType type;
    private final Theme theme;

    public StyledButton(String text) {
        this(text, ButtonType.PRIMARY);
    }

    public StyledButton(String text, ButtonType type) {
        super(text);
        this.type = type;
        this.theme = ThemeManager.getInstance().getTheme();
        applyStyle();
    }

    private void applyStyle() {
        setFont(theme.getUIFontBold());
        setForeground(Color.WHITE);
        setFocusPainted(false);
        setBorder(BorderFactory.createEmptyBorder(6, 15, 6, 15));
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setBackground(getColorForType());
    }

    private Color getColorForType() {
        switch (type) {
            case SUCCESS: return theme.getAccentSuccess();
            case DANGER: return theme.getAccentDanger();
            case DEFAULT: return theme.getBackgroundTertiary();
            case PRIMARY:
            default: return theme.getAccentPrimary();
        }
    }
}
