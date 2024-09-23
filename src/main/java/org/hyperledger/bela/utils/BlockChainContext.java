package org.hyperledger.bela.utils;

import org.hyperledger.besu.ethereum.chain.Blockchain;
import org.hyperledger.besu.ethereum.chain.MutableBlockchain;
import org.hyperledger.besu.ethereum.trie.diffbased.bonsai.BonsaiWorldStateProvider;
import org.hyperledger.besu.ethereum.trie.diffbased.bonsai.storage.BonsaiWorldStateKeyValueStorage;

public class BlockChainContext {

    private final MutableBlockchain blockchain;
    private final BonsaiWorldStateProvider bonsaiWorldStateArchive;

    public BlockChainContext(final MutableBlockchain blockchain,
                             final BonsaiWorldStateProvider bonsaiWorldStateArchive) {
        this.blockchain = blockchain;
        this.bonsaiWorldStateArchive = bonsaiWorldStateArchive;
    }

    public MutableBlockchain getBlockchain() {
        return blockchain;
    }

    public BonsaiWorldStateProvider getBonsaiWorldStateProvider() {
        return bonsaiWorldStateArchive;
    }

}
