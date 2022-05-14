package org.hyperledger.bela.utils;

import org.hyperledger.bela.utils.ConsensusDetector.CONSENSUS_TYPE;
import org.hyperledger.besu.consensus.common.bft.BftBlockHeaderFunctions;
import org.hyperledger.besu.consensus.ibft.IbftExtraDataCodec;
import org.hyperledger.besu.consensus.qbft.QbftExtraDataCodec;
import org.hyperledger.besu.ethereum.bonsai.BonsaiWorldStateArchive;
import org.hyperledger.besu.ethereum.bonsai.BonsaiWorldStateKeyValueStorage;
import org.hyperledger.besu.ethereum.chain.DefaultBlockchain;
import org.hyperledger.besu.ethereum.core.BlockHeaderFunctions;
import org.hyperledger.besu.ethereum.mainnet.MainnetBlockHeaderFunctions;
import org.hyperledger.besu.ethereum.storage.StorageProvider;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueStoragePrefixedKeyBlockchainStorage;
import org.hyperledger.besu.ethereum.worldstate.DataStorageConfiguration;
import org.hyperledger.besu.metrics.noop.NoOpMetricsSystem;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorage;

import static org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier.BLOCKCHAIN;

public class BlockChainContextFactory {

    public static BlockChainContext createBlockChainContext(final StorageProvider provider) {
        final KeyValueStorage keyValueStorage = provider.getStorageBySegmentIdentifier(BLOCKCHAIN);
        final CONSENSUS_TYPE consensusType = ConsensusDetector.detectConsensusMechanism(
                keyValueStorage);
        final BlockHeaderFunctions blockHeaderFunction = switch (consensusType) {
            case IBFT2 -> BftBlockHeaderFunctions.forOnchainBlock(new IbftExtraDataCodec());
            case QBFT -> BftBlockHeaderFunctions.forOnchainBlock(new QbftExtraDataCodec());
            default -> new MainnetBlockHeaderFunctions();
        };

        var blockchainStorage = new KeyValueStoragePrefixedKeyBlockchainStorage(keyValueStorage,
                blockHeaderFunction);
        var blockchain = DefaultBlockchain
                .create(blockchainStorage, new NoOpMetricsSystem(), 0L);
        var worldStateStorage = new BonsaiWorldStateKeyValueStorage(provider);
        var worldStateArchive = new BonsaiWorldStateArchive(provider, blockchain, DataStorageConfiguration.DEFAULT_CONFIG.getBonsaiMaxLayersToLoad());

        return new BlockChainContext(blockchain, worldStateStorage, worldStateArchive);
    }


}
