package gui.components;

import com.sun.jdi.*;
import gui.theme.Theme;
import gui.theme.ThemeManager;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;

/**
 * Panel d'inspection des variables (SRP)
 * Responsabilité : afficher les variables et leurs valeurs
 */
public class VariablesPanel extends JPanel {

    private final JTree variablesTree;
    private final DefaultTreeModel treeModel;
    private final DefaultMutableTreeNode rootNode;
    private final Theme theme;

    public VariablesPanel() {
        this.theme = ThemeManager.getInstance().getTheme();
        this.rootNode = new DefaultMutableTreeNode("Variables");
        this.treeModel = new DefaultTreeModel(rootNode);

        setLayout(new BorderLayout());
        setBackground(theme.getBackgroundSecondary());

        variablesTree = createTree();
        JScrollPane scrollPane = new JScrollPane(variablesTree);
        scrollPane.setBorder(null);

        add(scrollPane, BorderLayout.CENTER);
        applyTitledBorder("VARIABLES");
    }

    private JTree createTree() {
        JTree tree = new JTree(treeModel);
        tree.setBackground(theme.getBackgroundPrimary());
        tree.setForeground(theme.getTextSecondary());
        tree.setFont(theme.getUIFont());

        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value,
                    boolean sel, boolean exp, boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, sel, exp, leaf, row, hasFocus);
                setBackgroundNonSelectionColor(theme.getBackgroundPrimary());
                setTextNonSelectionColor(theme.getTextSecondary());
                setBackgroundSelectionColor(theme.getAccentPrimary());
                setTextSelectionColor(Color.WHITE);
                return this;
            }
        });

        return tree;
    }

    private void applyTitledBorder(String title) {
        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, theme.getBorderColor()),
                title);
        border.setTitleColor(theme.getTextMuted());
        border.setTitleFont(theme.getSmallFont());
        setBorder(border);
    }

    /**
     * Met à jour l'arbre des variables
     */
    public void updateVariables(java.util.List<models.Variable> variables) {
        SwingUtilities.invokeLater(() -> {
            rootNode.removeAllChildren();
            if (variables != null) {
                for (models.Variable v : variables) {
                    DefaultMutableTreeNode varNode = createVariableNode(v.getName(), v.getValue());
                    rootNode.add(varNode);
                }
            }
            treeModel.reload();
            expandAllNodes();
        });
    }

    /**
     * Crée un nœud pour une variable (récursif pour les objets)
     */
    private DefaultMutableTreeNode createVariableNode(String name, Value value) {
        String display = name + " = " + formatValue(value);
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(display);

        if (value instanceof ObjectReference) {
            ObjectReference obj = (ObjectReference) value;

            if (value instanceof ArrayReference) {
                ArrayReference array = (ArrayReference) value;
                int maxItems = Math.min(array.length(), 100); // Limiter pour performance
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

    /**
     * Formate une valeur JDI pour l'affichage
     */
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

    /**
     * Expand le premier niveau de l'arbre
     */
    private void expandAllNodes() {
        for (int i = 0; i < variablesTree.getRowCount(); i++) {
            variablesTree.expandRow(i);
        }
    }

    /**
     * Efface les variables
     */
    public void clear() {
        SwingUtilities.invokeLater(() -> {
            rootNode.removeAllChildren();
            treeModel.reload();
        });
    }
}
