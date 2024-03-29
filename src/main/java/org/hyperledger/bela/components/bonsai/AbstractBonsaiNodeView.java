package org.hyperledger.bela.components.bonsai;

import java.util.List;
import java.util.Optional;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.ActionListBox;
import com.googlecode.lanterna.gui2.Border;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import org.hyperledger.bela.components.BelaComponent;

public abstract class AbstractBonsaiNodeView implements BelaComponent<Panel> {
    private final ActionListBox pathListBox;
    private final Panel detailsPanel;
    private final ActionListBox childrenListBox;
    private final Border childrenBorder;
    Panel panel = new Panel(new LinearLayout(Direction.HORIZONTAL));
    private BonsaiNode currentNode;

    public AbstractBonsaiNodeView() {
        pathListBox = new ActionListBox(new TerminalSize(30, 20));
        detailsPanel = new Panel(new LinearLayout(Direction.VERTICAL));
        resetDetailsPanel();
        childrenListBox = new ActionListBox(new TerminalSize(30, 20));
        childrenBorder = childrenListBox.withBorder(Borders.singleLine("Children"));
    }

    private void resetDetailsPanel() {
        final Border border = new Panel().withBorder(Borders.singleLine("Details"));
        border.setPreferredSize(new TerminalSize(30, 22));
        detailsPanel.removeAllComponents();
        detailsPanel.addComponent(border);
    }

    @Override
    public Panel createComponent() {
        panel.removeAllComponents();
        panel.addComponent(pathListBox.withBorder(Borders.singleLine("Location Path")));
        panel.addComponent(detailsPanel);
        panel.addComponent(childrenBorder);
        return panel;
    }

    protected void selectNode(final BonsaiNode newLeaf) {
        updatePath(newLeaf);
        this.currentNode = newLeaf;
        detailsPanel.removeAllComponents();
        final Component component = newLeaf.createComponent();
        final List<BonsaiNode> children = newLeaf.getChildren();
        childrenListBox.clearItems();
        if (children.isEmpty()) {
            childrenBorder.setVisible(false);
            component.setPreferredSize(new TerminalSize(70, 22));
        } else {
            childrenBorder.setVisible(true);
            children.forEach(child -> childrenListBox.addItem(child.getLabel(), () -> selectNode(child)));
            component.setPreferredSize(new TerminalSize(30, 22));
        }

        detailsPanel.addComponent(component);
    }

    private void updatePath(final BonsaiNode node) {
        final int selectedIndex = pathListBox.getSelectedIndex();
        if (selectedIndex >= 0 && pathListBox.getSelectedItem().toString().equals(node.getLabel())) {
            for (int i = (pathListBox.getItemCount() - 1); i >= selectedIndex; --i) {
                pathListBox.removeItem(i);
            }
        }
        pathListBox.addItem(new Runnable() {
            @Override
            public void run() {
                selectNode(node);
            }

            @Override
            public String toString() {
                return node.getLabel();
            }
        });
    }


    public void logCurrent() {
        final Optional<BonsaiNode> current = getCurrentNode();
        current.ifPresent(BonsaiNode::log);
    }

    private Optional<BonsaiNode> getCurrentNode() {
        return Optional.ofNullable(currentNode);
    }


    protected void clear() {
        this.pathListBox.clearItems();
        this.childrenListBox.clearItems();
        resetDetailsPanel();
        this.currentNode = null;
    }
}
