package org.hyperledger.bela.components.bonsai;

import kr.pe.kwonnam.slf4jlambda.LambdaLogger;

import static kr.pe.kwonnam.slf4jlambda.LambdaLoggerFactory.getLogger;

public abstract class AbstractBonsaiNode implements BonsaiNode {
    protected static final LambdaLogger log = getLogger(AbstractBonsaiNode.class);

    protected final int depth;
    private final String label;

    public AbstractBonsaiNode(final String label, final int depth) {
        this.depth = depth;
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }

}
