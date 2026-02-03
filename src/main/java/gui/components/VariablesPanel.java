package gui.components;
import com.sun.jdi.*;
import gui.theme.Theme;
import gui.theme.ThemeManager;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
public class VariablesPanel extends JPanel {
    private final JTree variablesTree;
    private final DefaultTreeModel treeModel;
    private final DefaultMutableTreeNode rootNode;
    private final Theme theme;
    private final Map<DefaultMutableTreeNode, VariableInfo> nodeToVariableMap = new HashMap<>();
    public interface VariableSelectionListener {
        void onVariableDoubleClicked(String variableId, String variableName);
    }
    private VariableSelectionListener selectionListener;
    public static class VariableInfo {
        public final String name;
        public final String uniqueId;
        public VariableInfo(String name, String uniqueId) {
            this.name = name;
            this.uniqueId = uniqueId;
        }
    }
    public VariablesPanel() {
        this.theme = ThemeManager.getInstance().getTheme();
        this.rootNode = new DefaultMutableTreeNode("Variables");
        this.treeModel = new DefaultTreeModel(rootNode);
        setLayout(new BorderLayout());
        setBackground(theme.getBackgroundPrimary());
        variablesTree = createTree();
        JScrollPane scrollPane = new JScrollPane(variablesTree);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
        applyTitledBorder("Variables");
    }
    public void setSelectionListener(VariableSelectionListener listener) {
        this.selectionListener = listener;
    }
    private JTree createTree() {
        JTree tree = new JTree(treeModel);
        tree.setBackground(theme.getBackgroundPrimary());
        tree.setForeground(theme.getTextPrimary());
        tree.setFont(theme.getUIFont());
        tree.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value,
                    boolean sel, boolean exp, boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, sel, exp, leaf, row, hasFocus);
                setBackgroundNonSelectionColor(theme.getBackgroundPrimary());
                setTextNonSelectionColor(theme.getTextPrimary());
                setBackgroundSelectionColor(theme.getAccentPrimary());
                setTextSelectionColor(theme.getTextPrimary());
                setBorderSelectionColor(null);
                return this;
            }
        });
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        VariableInfo info = nodeToVariableMap.get(node);
                        if (info != null && selectionListener != null) {
                            selectionListener.onVariableDoubleClicked(info.uniqueId, info.name);
                        }
                    }
                }
            }
        });
        return tree;
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
    public void updateVariables(java.util.List<models.Variable> variables) {
        SwingUtilities.invokeLater(() -> {
            rootNode.removeAllChildren();
            nodeToVariableMap.clear();
            if (variables != null) {
                for (models.Variable v : variables) {
                    DefaultMutableTreeNode varNode = createVariableNode(v.getName(), v.getValue());
                    nodeToVariableMap.put(varNode, new VariableInfo(v.getName(), null));
                    rootNode.add(varNode);
                }
            }
            treeModel.reload();
            expandAllNodes();
        });
    }
    public void updateFromSnapshots(java.util.List<models.VariableSnapshot> snapshots) {
        SwingUtilities.invokeLater(() -> {
            rootNode.removeAllChildren();
            nodeToVariableMap.clear();
            if (snapshots != null) {
                for (models.VariableSnapshot vs : snapshots) {
                    DefaultMutableTreeNode varNode = createSnapshotNode(vs);
                    rootNode.add(varNode);
                }
            }
            treeModel.reload();
            expandFirstLevel();
        });
    }
    private DefaultMutableTreeNode createSnapshotNode(models.VariableSnapshot vs) {
        String display = vs.getName() + " (" + vs.getType() + ") = " + vs.getValue();
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(display);
        nodeToVariableMap.put(node, new VariableInfo(vs.getName(), vs.getUniqueId()));
        if (vs.hasChildren()) {
            for (models.VariableSnapshot child : vs.getChildren()) {
                node.add(createSnapshotNode(child));
            }
        }
        return node;
    }
    private void expandFirstLevel() {
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            variablesTree.expandRow(i + 1);
        }
    }
    private DefaultMutableTreeNode createVariableNode(String name, Value value) {
        String display = name + " = " + formatValue(value);
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(display);
        if (value instanceof ObjectReference) {
            ObjectReference obj = (ObjectReference) value;
            if (value instanceof ArrayReference) {
                ArrayReference array = (ArrayReference) value;
                int maxItems = Math.min(array.length(), 100); 
                for (int i = 0; i < maxItems; i++) {
                    node.add(createVariableNode("[" + i + "]", array.getValue(i)));
                }
                if (array.length() > maxItems) {
                    node.add(new DefaultMutableTreeNode("... (" + (array.length() - maxItems) + " more)"));
                }
            } else {
                ReferenceType type = obj.referenceType();
                for (Field field : type.allFields()) {
                    try {
                        Value fieldValue = obj.getValue(field);
                        node.add(createVariableNode(field.name(), fieldValue));
                    } catch (Exception e) {
                        node.add(new DefaultMutableTreeNode(field.name() + " = <inaccessible>"));
                    }
                }
            }
        }
        return node;
    }
    private String formatValue(Value v) {
        if (v == null) return "null";
        if (v instanceof StringReference) return "\"" + ((StringReference) v).value() + "\"";
        if (v instanceof PrimitiveValue) return v.toString();
        if (v instanceof ArrayReference) return "Array[" + ((ArrayReference) v).length() + "]";
        if (v instanceof ObjectReference) {
            ObjectReference obj = (ObjectReference) v;
            return obj.type().name() + " (id=" + obj.uniqueID() + ")";
        }
        return v.toString();
    }
    private void expandAllNodes() {
        for (int i = 0; i < variablesTree.getRowCount(); i++) {
            variablesTree.expandRow(i);
        }
    }
    public void clear() {
        SwingUtilities.invokeLater(() -> {
            rootNode.removeAllChildren();
            treeModel.reload();
        });
    }
}
