package org.hyperledger.bela.components.bonsai.queries;

import org.hyperledger.besu.ethereum.bonsai.trielog.TrieLogLayer;

public interface TrieQueryValidator {
    boolean validate(TrieLogLayer layer);
}
