package org.hyperledger.bela.components.bonsai;

import java.util.Collections;
import java.util.List;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;

public class LabelNode extends AbstractBonsaiNode {
    public LabelNode(final String label, final int depth) {
        super(label, depth);
    }


    @Override
    public List<BonsaiNode> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public Component createComponent() {
        Panel panel = new Panel();
        panel.addComponent(new Label("Label: " + getLabel()));
        return panel.withBorder(Borders.singleLine("Label"));
    }
}
