package org.hyperledger.bela.utils;

import org.hyperledger.besu.ethereum.chain.Blockchain;
import org.hyperledger.besu.ethereum.chain.MutableBlockchain;
import org.hyperledger.besu.ethereum.trie.diffbased.bonsai.BonsaiWorldStateProvider;
import org.hyperledger.besu.ethereum.trie.diffbased.bonsai.storage.BonsaiWorldStateKeyValueStorage;

public class BlockChainContext {

    private final MutableBlockchain blockchain;
    private final BonsaiWorldStateKeyValueStorage worldStateStorage;
    private final BonsaiWorldStateProvider bonsaiWorldStateArchive;

    public BlockChainContext(final MutableBlockchain blockchain,
                             final BonsaiWorldStateKeyValueStorage worldStateStorage,
                             final BonsaiWorldStateProvider bonsaiWorldStateArchive) {
        this.blockchain = blockchain;
        this.worldStateStorage = worldStateStorage;
        this.bonsaiWorldStateArchive = bonsaiWorldStateArchive;
    }

    public MutableBlockchain getBlockchain() {
        return blockchain;
    }

    public BonsaiWorldStateKeyValueStorage getWorldStateStorage() {
        return worldStateStorage;
    }

    public BonsaiWorldStateProvider getBonsaiWorldStateProvider() {
        return bonsaiWorldStateArchive;
    }

}
