package org.hyperledger.bela.utils;

import org.hyperledger.besu.ethereum.bonsai.BonsaiWorldStateProvider;
import org.hyperledger.besu.ethereum.bonsai.storage.BonsaiWorldStateKeyValueStorage;
import org.hyperledger.besu.ethereum.chain.Blockchain;

public class BlockChainContext {

    private final Blockchain blockchain;
    private final BonsaiWorldStateKeyValueStorage worldStateStorage;
    private final BonsaiWorldStateProvider bonsaiWorldStateArchive;

    public BlockChainContext(final Blockchain blockchain,
                             final BonsaiWorldStateKeyValueStorage worldStateStorage,
                             final BonsaiWorldStateProvider bonsaiWorldStateArchive) {
        this.blockchain = blockchain;
        this.worldStateStorage = worldStateStorage;
        this.bonsaiWorldStateArchive = bonsaiWorldStateArchive;
    }

    public Blockchain getBlockchain() {
        return blockchain;
    }

    public BonsaiWorldStateKeyValueStorage getWorldStateStorage() {
        return worldStateStorage;
    }

    public BonsaiWorldStateProvider getBonsaiWorldStateProvider() {
        return bonsaiWorldStateArchive;
    }

}
