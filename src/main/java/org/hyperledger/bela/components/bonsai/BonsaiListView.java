package org.hyperledger.bela.components.bonsai;

import java.util.List;
import java.util.stream.Collectors;

public class BonsaiListView extends AbstractBonsaiNodeView {
    private List<? extends BonsaiView> list;

    public BonsaiListView(final String label, final List<? extends BonsaiView> list, final int depth) {
        super("L" + label, depth);
        this.list = list;
    }

    @Override
    public void expand() {
        setChildren(list.stream().peek(BonsaiView::collapse).collect(Collectors.toList()));
        redraw();
        takeFocus();
    }
}
