package org.hyperledger.bela.components.bonsai;

import kr.pe.kwonnam.slf4jlambda.LambdaLogger;

import static kr.pe.kwonnam.slf4jlambda.LambdaLoggerFactory.getLogger;

public abstract class AbstractBonsaiNode implements BonsaiNode {
    protected static final LambdaLogger log = getLogger(AbstractBonsaiNode.class);

    private final String label;

    public AbstractBonsaiNode(final String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }

}
