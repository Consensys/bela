package org.hyperledger.bela.components.bonsai;

import java.util.List;
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

    public AbstractBonsaiNodeView() {
        pathListBox = new ActionListBox(new TerminalSize(30, 20));
        detailsPanel = new Panel(new LinearLayout(Direction.VERTICAL));
        final Border border = new Panel().withBorder(Borders.singleLine("Details"));
        border.setPreferredSize(new TerminalSize(30, 22));
        detailsPanel.addComponent(border);
        childrenListBox = new ActionListBox(new TerminalSize(30, 20));
    }

    @Override
    public Panel createComponent() {
        Panel panel = new Panel(new LinearLayout(Direction.HORIZONTAL));
        panel.addComponent(pathListBox.withBorder(Borders.singleLine("Location Path")));
        panel.addComponent(detailsPanel);
        panel.addComponent(childrenListBox.withBorder(Borders.singleLine("Children")));
        return panel;
    }

    protected void selectChild(final BonsaiNode newLeaf) {
        //TODO: if not exists
        pathListBox.addItem(newLeaf.getLabel(), () -> select(newLeaf));
        detailsPanel.removeAllComponents();
        final Component component = newLeaf.createComponent();
        component.setPreferredSize(new TerminalSize(30, 22));
        detailsPanel.addComponent(component);
        childrenListBox.clearItems();
        final List<BonsaiNode> children = newLeaf.getChildren();
        children.forEach(child -> childrenListBox.addItem(child.getLabel(), () -> selectChild(child)));

        // else
    }


    private void select(final BonsaiNode newLeaf) {

    }
}
