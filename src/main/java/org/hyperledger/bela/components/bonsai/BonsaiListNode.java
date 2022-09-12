package org.hyperledger.bela.components.bonsai;

import java.util.ArrayList;
import java.util.List;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Component;
import com.googlecode.lanterna.gui2.Panel;

public class BonsaiListNode extends AbstractBonsaiNode {
    private final List<? extends BonsaiNode> list;
    private final String title;

    public BonsaiListNode(final String title, final List<? extends BonsaiNode> list, final int depth) {
        super(title, depth);
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
        return panel.withBorder(Borders.singleLine(title));
    }

    @Override
    public void log() {
        log.info(title);
    }
}
