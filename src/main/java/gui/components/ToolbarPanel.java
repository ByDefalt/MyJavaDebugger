package gui.components;
import gui.theme.Theme;
import gui.theme.ThemeManager;
import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
public class ToolbarPanel extends JPanel {
    private final StyledButton continueButton;
    private final StyledButton stepOverButton;
    private final StyledButton stepIntoButton;
    private final StyledButton stepBackButton;
    private final StyledButton stopButton;
    private final StyledButton methodCallsButton;
    private ToolbarListener listener;
    public interface ToolbarListener {
        void onContinue();
        void onStepOver();
        void onStepInto();
        void onStepBack();
        void onStop();
        void onShowMethodCalls();
    }
    public ToolbarPanel() {
        Theme theme = ThemeManager.getInstance().getTheme();
        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 8));
        setBackground(theme.getBackgroundSecondary());
        setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, theme.getBorderColor()),
            BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));
        continueButton = new StyledButton("▶ Resume", StyledButton.ButtonType.SUCCESS);
        stepBackButton = new StyledButton("◀ Step Back", StyledButton.ButtonType.PRIMARY);
        stepOverButton = new StyledButton("↓ Step Over", StyledButton.ButtonType.PRIMARY);
        stepIntoButton = new StyledButton("↓ Step Into", StyledButton.ButtonType.PRIMARY);
        methodCallsButton = new StyledButton("📞 Calls", StyledButton.ButtonType.PRIMARY);
        stopButton = new StyledButton("⏹ Stop", StyledButton.ButtonType.DANGER);
        continueButton.addActionListener(e -> { if (listener != null) listener.onContinue(); });
        stepBackButton.addActionListener(e -> { if (listener != null) listener.onStepBack(); });
        stepOverButton.addActionListener(e -> { if (listener != null) listener.onStepOver(); });
        stepIntoButton.addActionListener(e -> { if (listener != null) listener.onStepInto(); });
        methodCallsButton.addActionListener(e -> { if (listener != null) listener.onShowMethodCalls(); });
        stopButton.addActionListener(e -> { if (listener != null) listener.onStop(); });
        add(continueButton);
        addSeparator();
        add(stepBackButton);
        add(stepOverButton);
        add(stepIntoButton);
        addSeparator();
        add(methodCallsButton);
        add(Box.createHorizontalGlue());
        add(stopButton);
    }

    private void addSeparator() {
        Theme theme = ThemeManager.getInstance().getTheme();
        JSeparator separator = new JSeparator(JSeparator.VERTICAL);
        separator.setPreferredSize(new Dimension(1, 24));
        separator.setForeground(theme.getBorderColor());
        separator.setBackground(theme.getBorderColor());
        add(separator);
    }
    public void setToolbarListener(ToolbarListener listener) {
        this.listener = listener;
    }
    public void setControlsEnabled(boolean enabled) {
        continueButton.setEnabled(enabled);
        stepBackButton.setEnabled(enabled);
        stepOverButton.setEnabled(enabled);
        stepIntoButton.setEnabled(enabled);
    }
}
