package gui.components;

import gui.theme.Theme;
import gui.theme.ThemeManager;
import models.DebugFrame;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;

public class CallStackPanel extends JPanel {

    private final JList<String> callStackList;
    private final DefaultListModel<String> callStackModel;
    private final Theme theme;

    private CallStackListener listener;

    public interface CallStackListener {
        void onFrameSelected(int frameIndex);
    }

    public CallStackPanel() {
        this.theme = ThemeManager.getInstance().getTheme();
        this.callStackModel = new DefaultListModel<>();

        setLayout(new BorderLayout());
        setBackground(theme.getBackgroundSecondary());

        callStackList = createList();
        JScrollPane scrollPane = new JScrollPane(callStackList);
        scrollPane.setBorder(null);

        add(scrollPane, BorderLayout.CENTER);
        applyTitledBorder("CALL STACK");
    }

    private JList<String> createList() {
        JList<String> list = new JList<>(callStackModel);
        list.setBackground(theme.getBackgroundPrimary());
        list.setForeground(theme.getTextSecondary());
        list.setSelectionBackground(new Color(theme.getAccentPrimary().getRed(),
                theme.getAccentPrimary().getGreen(),
                theme.getAccentPrimary().getBlue(), 150));
        list.setSelectionForeground(Color.WHITE);
        list.setFont(theme.getUIFont());

        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && listener != null) {
                int selectedIndex = list.getSelectedIndex();
                if (selectedIndex != -1) {
                    listener.onFrameSelected(selectedIndex);
                }
            }
        });

        return list;
    }

    private void applyTitledBorder(String title) {
        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, theme.getBorderColor()),
                title);
        border.setTitleColor(theme.getTextMuted());
        border.setTitleFont(theme.getSmallFont());
        setBorder(border);
    }

    public void setCallStackListener(CallStackListener listener) {
        this.listener = listener;
    }

    public void updateStack(List<DebugFrame> frames) {
        SwingUtilities.invokeLater(() -> {
            callStackModel.clear();
            if (frames != null) {
                frames.forEach(f -> callStackModel.addElement(f.toString()));
            }
        });
    }

    public void selectFrame(int index) {
        if (index >= 0 && index < callStackModel.getSize()) {
            callStackList.setSelectedIndex(index);
        }
    }

    public int getSelectedIndex() {
        return callStackList.getSelectedIndex();
    }
}
