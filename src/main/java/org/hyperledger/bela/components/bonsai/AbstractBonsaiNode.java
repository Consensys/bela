package org.hyperledger.bela.components.bonsai;

public abstract class AbstractBonsaiNode implements BonsaiNode {
    protected final int depth;
    private final String label;

    public AbstractBonsaiNode(final String label, final int depth) {
        this.depth = depth;
        this.label = label;
    }
//
//    @Override
//    public String getLabel() {
//        return " ".repeat(depth * 2) + "└─" + label;
//    }

    @Override
    public String getLabel() {
        return label;
    }
}
