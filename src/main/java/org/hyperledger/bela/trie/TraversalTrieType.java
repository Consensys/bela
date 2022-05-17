package org.hyperledger.bela.trie;

public enum TraversalTrieType {
    Storage("#"), Account("@");

    private String text;

    TraversalTrieType(final String text) {

        this.text = text;
    }

    public String getText() {
        return text;
    }
}
