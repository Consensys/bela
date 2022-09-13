package org.hyperledger.bela.components.bonsai;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.bela.components.bonsai.queries.TrieQueryValidator;
import org.hyperledger.bela.utils.StorageProviderFactory;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.bonsai.TrieLogLayer;
import org.hyperledger.besu.ethereum.storage.StorageProvider;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorage;

import static kr.pe.kwonnam.slf4jlambda.LambdaLoggerFactory.getLogger;

public class BonsaiTrieLogView extends AbstractBonsaiNodeView {
    private static final LambdaLogger log = getLogger(BonsaiTrieLogView.class);


    private final StorageProviderFactory storageProviderFactory;
    private BonsaiTrieLogNode bonsaiTrieLogNode;

    public BonsaiTrieLogView(final StorageProviderFactory storageProviderFactory) {
        this.storageProviderFactory = storageProviderFactory;
    }

    public static Optional<TrieLogLayer> getTrieLog(final KeyValueStorage storage, final Hash blockHash) {
        return storage.get(blockHash.toArrayUnsafe()).map(bytes -> {
            try {
                Method method = TrieLogLayer.class.getDeclaredMethod("fromBytes", byte[].class);
                method.setAccessible(true);
                return (TrieLogLayer) method.invoke(null, bytes);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void updateFromHash(final Hash hash) {

        final StorageProvider provider = storageProviderFactory.createProvider();
        final KeyValueStorage storage = provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.TRIE_LOG_STORAGE);
        final Optional<TrieLogLayer> trieLog = getTrieLog(storage, hash);


        if (trieLog.isPresent()) {
            clear();
            bonsaiTrieLogNode = new BonsaiTrieLogNode(hash, trieLog.get());
            selectNode(bonsaiTrieLogNode);
        } else {
            log.error("Trie log not found for hash: {}", hash);
        }
    }

    public TrieLogLayer getLayer() {
        return bonsaiTrieLogNode.getLayer();
    }

    public void showAllTries() {

        final StorageProvider provider = storageProviderFactory.createProvider();
        final KeyValueStorage storage = provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.TRIE_LOG_STORAGE);
        final List<BonsaiNode> blocks = storage.streamKeys().map(entry -> Hash.wrap(Bytes32.wrap(entry)))
                .map(hash -> new RootTrieLogSearchResult(storage, hash))
                .collect(Collectors.toList());
        clear();
        selectNode(new SearchResultNode(storage, blocks));
    }

    public void executeQuery(final TrieQueryValidator validator) {
        final StorageProvider provider = storageProviderFactory.createProvider();
        final KeyValueStorage storage = provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.TRIE_LOG_STORAGE);
        final List<BonsaiNode> results = storage.streamKeys().map(entry -> Hash.wrap(Bytes32.wrap(entry)))
                .map(hash -> getTrieLog(storage, hash))
                .flatMap(Optional::stream)
                .filter(validator::validate)
                .map(trieLogLayer -> new RootTrieLogSearchResult(storage, trieLogLayer.getBlockHash()))
                .collect(Collectors.toList());
        clear();
        selectNode(new SearchResultNode(storage, results));
    }

    public void shoResults(final KeyValueStorage storage, final List<BonsaiNode> results) {

        clear();
        selectNode(new SearchResultNode(storage, results));

    }
}
