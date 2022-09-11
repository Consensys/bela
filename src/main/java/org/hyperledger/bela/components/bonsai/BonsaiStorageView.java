package org.hyperledger.bela.components.bonsai;

import java.util.Objects;
import java.util.Optional;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.bela.utils.StorageProviderFactory;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.storage.StorageProvider;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier;
import org.hyperledger.besu.ethereum.trie.CompactEncoding;
import org.hyperledger.besu.ethereum.trie.Node;
import org.hyperledger.besu.ethereum.trie.TrieNodeDecoder;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorage;

public class BonsaiStorageView extends AbstractBonsaiNodeView {
    private final StorageProviderFactory storageProviderFactory;
    private KeyValueStorage accountStorage;
    private KeyValueStorage storageStorage;
    private KeyValueStorage trieBranchStorage;
    private KeyValueStorage codeStorage;

    public BonsaiStorageView(final StorageProviderFactory storageProviderFactory) {
        this.storageProviderFactory = storageProviderFactory;
    }

    private void initStorage() {
        final StorageProvider provider = storageProviderFactory.createProvider();
        accountStorage = provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.ACCOUNT_INFO_STATE);
        codeStorage = provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.CODE_STORAGE);
        storageStorage = provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.ACCOUNT_STORAGE_STORAGE);
        trieBranchStorage =
                provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.TRIE_BRANCH_STORAGE);
    }

    public void findRoot() {
        initStorage();
        final Hash rootHash =
                trieBranchStorage
                        .get(Bytes.EMPTY.toArrayUnsafe())
                        .map(Bytes::wrap)
                        .map(Hash::hash)
                        .orElseThrow();
        Node<Bytes> root = getAccountNodeValue(rootHash, Bytes.EMPTY);
        BonsaiNode rootNodeView = new AccountTreeNode(this, root, 0);
        selectNode(rootNodeView);
    }

    public Node<Bytes> getAccountNodeValue(final Bytes32 hash, final Bytes location) {
        final Optional<Bytes> bytes = trieBranchStorage.get(location.toArrayUnsafe()).map(Bytes::wrap);
        if (bytes.isEmpty()) {
            return null;
        }
        final Hash foundHashNode = Hash.hash(bytes.orElseThrow());
        if (!foundHashNode.equals(hash)) {
            return null;
        }
        return TrieNodeDecoder.decode(location, bytes.get());
    }

    public Optional<Bytes> getAccountFromFlatDatabase(final Hash accountHash) {
        return accountStorage.get(accountHash.toArrayUnsafe()).map(Bytes::wrap);
    }

    public Optional<Bytes> getCode(final Hash accountHash) {
        return codeStorage.get(accountHash.toArrayUnsafe()).map(Bytes::wrap);
    }

    public Node<Bytes> getStorageNodeValue(
            final Bytes32 hash, final Bytes32 accountHash, final Bytes location) {
        final Optional<Bytes> bytes =
                trieBranchStorage
                        .get(Bytes.concatenate(accountHash, location).toArrayUnsafe())
                        .map(Bytes::wrap);
        if (bytes.isEmpty()) {
            return null;
        }
        final Hash foundHashNode = Hash.hash(bytes.orElseThrow());
        if (!foundHashNode.equals(hash)) {
            return null;
        }
        return TrieNodeDecoder.decode(location, bytes.get());
    }

    public boolean nodeIsHashReferencedDescendant(
            final Node<Bytes> parentNode, final Node<Bytes> node) {
        return !Objects.equals(node.getHash(), parentNode.getHash()) && node.isReferencedByHash();
    }

    public Optional<Bytes> getStorageInFlatDB(final Bytes32 accountHash, final Bytes location, final Bytes path) {
        return storageStorage.get(Bytes.concatenate(accountHash, getSlotHash(location, path)).toArrayUnsafe())
                .map(Bytes::wrap);
    }

    private Hash getSlotHash(final Bytes location, final Bytes path) {
        return Hash.wrap(Bytes32.wrap(CompactEncoding.pathToBytes(Bytes.concatenate(location, path))));
    }
}
