package gui.components;
import gui.theme.Theme;
import gui.theme.ThemeManager;
import models.DebugFrame;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
public class CallStackPanel extends JPanel {
    private final JList<String> callStackList;
    private final DefaultListModel<String> callStackModel;
    private final Theme theme;
    private CallStackListener listener;
    private FindCallsListener findCallsListener;
    private List<DebugFrame> frames = new ArrayList<>();
    public interface CallStackListener {
        void onFrameSelected(int frameIndex);
    }
    public interface FindCallsListener {
        void onFindCalls(String className, String methodName);
    }
    public CallStackPanel() {
        this.theme = ThemeManager.getInstance().getTheme();
        this.callStackModel = new DefaultListModel<>();
        setLayout(new BorderLayout());
        setBackground(theme.getBackgroundPrimary());
        callStackList = createList();
        JScrollPane scrollPane = new JScrollPane(callStackList);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
        applyTitledBorder("Frames");
    }
    private JList<String> createList() {
        JList<String> list = new JList<>(callStackModel);
        list.setBackground(theme.getBackgroundPrimary());
        list.setForeground(theme.getTextPrimary());
        list.setSelectionBackground(theme.getAccentPrimary());
        list.setSelectionForeground(theme.getTextPrimary());
        list.setFont(theme.getUIFont());
        list.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && listener != null) {
                int selectedIndex = list.getSelectedIndex();
                if (selectedIndex != -1) {
                    listener.onFrameSelected(selectedIndex);
                }
            }
        });
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem findCallsItem = new JMenuItem("🔍 Find Calls");
        findCallsItem.setFont(theme.getUIFont());
        findCallsItem.addActionListener(e -> {
            int index = list.getSelectedIndex();
            if (index >= 0 && index < frames.size() && findCallsListener != null) {
                DebugFrame frame = frames.get(index);
                String className = extractClassName(frame);
                String methodName = extractMethodName(frame);
                if (className != null && methodName != null) {
                    findCallsListener.onFindCalls(className, methodName);
                }
            }
        });
        popupMenu.add(findCallsItem);
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handlePopup(e);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                handlePopup(e);
            }
            private void handlePopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int index = list.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        list.setSelectedIndex(index);
                        popupMenu.show(list, e.getX(), e.getY());
                    }
                }
            }
        });
        return list;
    }
    private String extractClassName(DebugFrame frame) {
        if (frame.getLocation() != null) {
            return frame.getLocation().declaringType().name();
        }
        if (frame.getDisplayName() != null && frame.getDisplayName().contains(".")) {
            return frame.getDisplayName().substring(0, frame.getDisplayName().lastIndexOf('.'));
        }
        return null;
    }
    private String extractMethodName(DebugFrame frame) {
        if (frame.getLocation() != null) {
            return frame.getLocation().method().name();
        }
        if (frame.getDisplayName() != null && frame.getDisplayName().contains(".")) {
            String afterDot = frame.getDisplayName().substring(frame.getDisplayName().lastIndexOf('.') + 1);
            if (afterDot.endsWith("()")) {
                return afterDot.substring(0, afterDot.length() - 2);
            }
            return afterDot;
        }
        return null;
    }
    private void applyTitledBorder(String title) {
        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, theme.getBorderColor()),
                    BorderFactory.createEmptyBorder(4, 8, 4, 8)
                ),
                title);
        border.setTitleColor(theme.getTextMuted());
        border.setTitleFont(theme.getUIFontBold());
        setBorder(border);
    }
    public void setCallStackListener(CallStackListener listener) {
        this.listener = listener;
    }
    public void setFindCallsListener(FindCallsListener listener) {
        this.findCallsListener = listener;
    }
    public void updateStack(List<DebugFrame> frames) {
        SwingUtilities.invokeLater(() -> {
            callStackModel.clear();
            this.frames = frames != null ? new ArrayList<>(frames) : new ArrayList<>();
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
