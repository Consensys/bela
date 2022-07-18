package org.hyperledger.bela.components.bonsai;

import java.util.ArrayList;
import java.util.List;

public class BonsaiListView extends AbstractBonsaiNodeView {
    private List<? extends BonsaiView> list;

    public BonsaiListView(final String label, final List<? extends BonsaiView> list, final int depth) {
        super(label, depth);
        this.list = list;
    }

    @Override
    public void expand() {
        setChildren(new ArrayList<>(list));
        redraw();
        takeFocus();
    }
}
