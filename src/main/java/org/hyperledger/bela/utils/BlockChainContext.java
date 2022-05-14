package org.hyperledger.bela.utils;

import org.hyperledger.besu.ethereum.bonsai.BonsaiWorldStateArchive;
import org.hyperledger.besu.ethereum.bonsai.BonsaiWorldStateKeyValueStorage;
import org.hyperledger.besu.ethereum.chain.Blockchain;

public class BlockChainContext {

    private final Blockchain blockchain;
    private final BonsaiWorldStateKeyValueStorage worldStateStorage;
    private final BonsaiWorldStateArchive bonsaiWorldStateArchive;

    public BlockChainContext(Blockchain blockchain,
                             BonsaiWorldStateKeyValueStorage worldStateStorage,
                             final BonsaiWorldStateArchive bonsaiWorldStateArchive) {
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

    public BonsaiWorldStateArchive getBonsaiWorldStateArchive() {
        return bonsaiWorldStateArchive;
    }

}
