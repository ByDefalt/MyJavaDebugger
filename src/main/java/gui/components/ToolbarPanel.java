package gui.components;

import gui.theme.Theme;
import gui.theme.ThemeManager;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;

/**
 * Barre d'outils du debugger (SRP)
 * Responsabilité : afficher les boutons de contrôle du debugger
 */
public class ToolbarPanel extends JPanel {

    private final StyledButton continueButton;
    private final StyledButton stepOverButton;
    private final StyledButton stepIntoButton;
    private final StyledButton stopButton;

    private ToolbarListener listener;

    public interface ToolbarListener {
        void onContinue();
        void onStepOver();
        void onStepInto();
        void onStop();
    }

    public ToolbarPanel() {
        Theme theme = ThemeManager.getInstance().getTheme();

        setLayout(new FlowLayout(FlowLayout.LEFT, 15, 10));
        setBackground(theme.getBackgroundTertiary());
        setBorder(new MatteBorder(0, 0, 1, 0, theme.getBorderColor()));

        continueButton = new StyledButton("Continue", StyledButton.ButtonType.SUCCESS);
        stepOverButton = new StyledButton("Step Over", StyledButton.ButtonType.PRIMARY);
        stepIntoButton = new StyledButton("Step Into", StyledButton.ButtonType.PRIMARY);
        stopButton = new StyledButton("Stop", StyledButton.ButtonType.DANGER);

        // Actions
        continueButton.addActionListener(e -> { if (listener != null) listener.onContinue(); });
        stepOverButton.addActionListener(e -> { if (listener != null) listener.onStepOver(); });
        stepIntoButton.addActionListener(e -> { if (listener != null) listener.onStepInto(); });
        stopButton.addActionListener(e -> { if (listener != null) listener.onStop(); });

        add(continueButton);
        add(new JSeparator(JSeparator.VERTICAL));
        add(stepOverButton);
        add(stepIntoButton);
        add(Box.createHorizontalStrut(20));
        add(stopButton);
    }

    public void setToolbarListener(ToolbarListener listener) {
        this.listener = listener;
    }

    public void setControlsEnabled(boolean enabled) {
        continueButton.setEnabled(enabled);
        stepOverButton.setEnabled(enabled);
        stepIntoButton.setEnabled(enabled);
    }
}
