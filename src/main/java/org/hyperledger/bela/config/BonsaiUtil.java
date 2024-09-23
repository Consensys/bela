package org.hyperledger.bela.config;

import org.hyperledger.besu.ethereum.chain.Blockchain;
import org.hyperledger.besu.ethereum.storage.StorageProvider;
import org.hyperledger.besu.ethereum.trie.diffbased.bonsai.BonsaiWorldStateProvider;
import org.hyperledger.besu.ethereum.trie.diffbased.bonsai.cache.BonsaiCachedMerkleTrieLoader;
import org.hyperledger.besu.ethereum.trie.diffbased.bonsai.storage.BonsaiWorldStateKeyValueStorage;
import org.hyperledger.besu.ethereum.worldstate.DataStorageConfiguration;
import org.hyperledger.besu.evm.internal.EvmConfiguration;
import org.hyperledger.besu.metrics.noop.NoOpMetricsSystem;
import org.hyperledger.besu.services.BesuPluginContextImpl;

import java.util.Optional;

public class BonsaiUtil {

    public static BonsaiWorldStateProvider getBonsaiWorldStateArchive(StorageProvider provider, Blockchain blockchain) {
        final NoOpMetricsSystem noOpMetricsSystem = new NoOpMetricsSystem();
        return new BonsaiWorldStateProvider(
                (BonsaiWorldStateKeyValueStorage)
                        provider.createWorldStateStorage(DataStorageConfiguration.DEFAULT_BONSAI_CONFIG),
                blockchain,
                Optional.empty(),
                new BonsaiCachedMerkleTrieLoader(noOpMetricsSystem),
                new BesuPluginContextImpl(),
                EvmConfiguration.DEFAULT
        );
    }
}
