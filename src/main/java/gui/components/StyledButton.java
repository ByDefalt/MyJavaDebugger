package gui.components;
import gui.theme.Theme;
import gui.theme.ThemeManager;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
public class StyledButton extends JButton {
    public enum ButtonType {
        PRIMARY,
        SUCCESS,
        DANGER,
        DEFAULT
    }
    private final ButtonType type;
    private final Theme theme;
    private boolean isHovered = false;
    public StyledButton(String text) {
        this(text, ButtonType.PRIMARY);
    }
    public StyledButton(String text, ButtonType type) {
        super(text);
        this.type = type;
        this.theme = ThemeManager.getInstance().getTheme();
        applyStyle();
        addHoverEffect();
    }
    private void applyStyle() {
        setFont(theme.getUIFont());
        setForeground(theme.getTextPrimary());
        setFocusPainted(false);
        setBorderPainted(false);
        setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setBackground(getColorForType());
        setOpaque(false);
        setContentAreaFilled(false);
    }

    private void addHoverEffect() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color bgColor = getColorForType();
        if (isHovered && isEnabled()) {
            bgColor = brighten(bgColor, 0.15f);
        }
        if (!isEnabled()) {
            bgColor = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 80);
        }

        g2.setColor(bgColor);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);

        g2.dispose();
        super.paintComponent(g);
    }

    private Color brighten(Color color, float factor) {
        int r = Math.min(255, (int)(color.getRed() * (1 + factor)));
        int g = Math.min(255, (int)(color.getGreen() * (1 + factor)));
        int b = Math.min(255, (int)(color.getBlue() * (1 + factor)));
        return new Color(r, g, b);
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
