package org.hyperledger.bela.utils.bonsai;

public enum BonsaiTraversalTrieType {
    Storage("#"), Account("@");

    private String text;

    BonsaiTraversalTrieType(final String text) {

        this.text = text;
    }

    public String getText() {
        return text;
    }
}
