package org.hyperledger.bela.utils;

import org.hyperledger.bela.config.BonsaiUtil;
import org.hyperledger.bela.context.BelaContext;
import org.hyperledger.bela.utils.ConsensusDetector.CONSENSUS_TYPE;
import org.hyperledger.besu.consensus.common.bft.BftBlockHeaderFunctions;
import org.hyperledger.besu.consensus.ibft.IbftExtraDataCodec;
import org.hyperledger.besu.consensus.qbft.QbftExtraDataCodec;
import org.hyperledger.besu.ethereum.chain.DefaultBlockchain;
import org.hyperledger.besu.ethereum.chain.MutableBlockchain;
import org.hyperledger.besu.ethereum.chain.VariablesStorage;
import org.hyperledger.besu.ethereum.core.BlockHeaderFunctions;
import org.hyperledger.besu.ethereum.mainnet.MainnetBlockHeaderFunctions;
import org.hyperledger.besu.ethereum.storage.StorageProvider;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueStoragePrefixedKeyBlockchainStorage;
import org.hyperledger.besu.ethereum.storage.keyvalue.VariablesKeyValueStorage;
import org.hyperledger.besu.ethereum.trie.diffbased.bonsai.BonsaiWorldStateProvider;
import org.hyperledger.besu.metrics.noop.NoOpMetricsSystem;
import org.hyperledger.besu.plugin.services.storage.DataStorageFormat;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorage;

import static org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier.BLOCKCHAIN;
import static org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier.VARIABLES;

public class BlockChainContextFactory {

    public static BlockChainContext createBlockChainContext(final BelaContext belaContext) {
        //init
        StorageProvider storageProvider = belaContext.getProvider();
        final KeyValueStorage keyValueStorage = storageProvider.getStorageBySegmentIdentifier(BLOCKCHAIN);
        final VariablesStorage variablesStorage = new VariablesKeyValueStorage(storageProvider.getStorageBySegmentIdentifier(VARIABLES));
        final CONSENSUS_TYPE consensusType = ConsensusDetector.detectConsensusMechanism(
                keyValueStorage, variablesStorage);
        final BlockHeaderFunctions blockHeaderFunction = switch (consensusType) {
            case IBFT2 -> BftBlockHeaderFunctions.forOnchainBlock(new IbftExtraDataCodec());
            case QBFT -> BftBlockHeaderFunctions.forOnchainBlock(new QbftExtraDataCodec());
            default -> new MainnetBlockHeaderFunctions();
        };

        final NoOpMetricsSystem noOpMetricsSystem = new NoOpMetricsSystem();
        // TODO: need to detect this from config rather than hardcoding
        var blockchainStorage = new KeyValueStoragePrefixedKeyBlockchainStorage(keyValueStorage,
            variablesStorage, blockHeaderFunction, false);
        var blockchain = (MutableBlockchain) DefaultBlockchain
                .create(blockchainStorage, new NoOpMetricsSystem(), 0L);
        //TODO: we assume bonsai here but we should check the database metadata and load the appropriate worldstate
        if (DataStorageFormat.BONSAI.equals(belaContext.getDataStorageFormat())) {
            BonsaiWorldStateProvider worldStateArchive = BonsaiUtil.getBonsaiWorldStateArchive(storageProvider, blockchain);
            return new BlockChainContext(blockchain, worldStateArchive);
        } else {
            // e.g. FOREST
            return new BlockChainContext(blockchain, null);
        }
    }


}
