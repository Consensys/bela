package org.hyperledger.bela.utils;

import org.hyperledger.besu.ethereum.bonsai.BonsaiWorldStateKeyValueStorage;
import org.hyperledger.besu.ethereum.chain.Blockchain;

public class BlockChainContext {

    private final Blockchain blockchain;
    private final BonsaiWorldStateKeyValueStorage worldStateStorage;

    public BlockChainContext(Blockchain blockchain,
                             BonsaiWorldStateKeyValueStorage worldStateStorage) {
        this.blockchain = blockchain;
        this.worldStateStorage = worldStateStorage;
    }

    public Blockchain getBlockchain() {
        return blockchain;
    }

    public BonsaiWorldStateKeyValueStorage getWorldStateStorage() {
        return worldStateStorage;
    }
}
