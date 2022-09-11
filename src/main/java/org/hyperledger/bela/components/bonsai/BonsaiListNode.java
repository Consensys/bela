package org.hyperledger.bela.components.bonsai;

import java.util.ArrayList;
import java.util.List;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;

public class BonsaiListNode extends AbstractBonsaiNode {
    private final List<? extends BonsaiNode> list;
    private final String title;

    public BonsaiListNode(final String title, final List<? extends BonsaiNode> list, final int depth) {
        super("L" + title, depth);
        this.title = title;
        this.list = list;
    }

    @Override
    public List<BonsaiNode> getChildren() {
        return new ArrayList<>(list);
    }

    @Override
    public Component createComponent() {
        Panel panel = new Panel();
        panel.addComponent(new Label("Title: " + title));
        return panel.withBorder(Borders.singleLine("List of stuff"));
    }
}
