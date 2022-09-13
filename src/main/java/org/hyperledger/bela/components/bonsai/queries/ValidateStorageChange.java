package org.hyperledger.bela.components.bonsai.queries;

import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.bonsai.BonsaiValue;
import org.hyperledger.besu.ethereum.bonsai.TrieLogLayer;
import org.hyperledger.besu.ethereum.worldstate.StateTrieAccountValue;

import static kr.pe.kwonnam.slf4jlambda.LambdaLoggerFactory.getLogger;

public class ValidateStorageChange implements TrieQueryValidator {
    private static final LambdaLogger log = getLogger(ValidateStorageChange.class);

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
            if ((prior != null && targetHash.equals(prior.getStorageRoot())) || (updated != null && targetHash.equals(updated.getStorageRoot()))) {
                log.info("Found storage change for account {}", accountChange.getKey());
                return true;
            }
            return false;
        });
    }
}
