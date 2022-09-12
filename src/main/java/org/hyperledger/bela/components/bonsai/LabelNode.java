package org.hyperledger.bela.components.bonsai;

import java.util.Collections;
import java.util.List;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;

public class LabelNode extends AbstractBonsaiNode {
    private final String value;

    public LabelNode(final String title, final String value, final int depth) {
        super(title, depth);
        this.value = value;
    }


    @Override
    public List<BonsaiNode> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public Component createComponent() {
        Panel panel = new Panel();
        final TextBox component = new TextBox(value);
        component.setReadOnly(true);
        panel.addComponent(component);
        return panel.withBorder(Borders.singleLine(getLabel()));
    }

    @Override
    public void log() {
        log.info("{} {}", getLabel(), value);
    }
}
