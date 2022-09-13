package org.hyperledger.bela.components.bonsai.queries;

import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.bonsai.BonsaiValue;
import org.hyperledger.besu.ethereum.bonsai.TrieLogLayer;
import org.hyperledger.besu.ethereum.worldstate.StateTrieAccountValue;

public class ValidateStorageChange implements TrieQueryValidator {
    final Hash targetHash;

    public ValidateStorageChange(final Hash targetHash) {
        this.targetHash = targetHash;
    }

    @Override
    public boolean validate(final TrieLogLayer layer) {
        return layer.streamAccountChanges().anyMatch(accountChange -> {
            final BonsaiValue<StateTrieAccountValue> value = accountChange.getValue();
            final StateTrieAccountValue prior = value.getPrior();
            final StateTrieAccountValue updated = value.getUpdated();
            return (prior != null && targetHash.equals(prior.getStorageRoot())) || (updated != null && targetHash.equals(updated.getStorageRoot()));
        });
    }
}
