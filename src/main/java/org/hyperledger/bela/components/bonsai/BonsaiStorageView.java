package org.hyperledger.bela.components.bonsai;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.bela.context.BelaContext;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.storage.StorageProvider;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier;
import org.hyperledger.besu.ethereum.trie.CompactEncoding;
import org.hyperledger.besu.ethereum.trie.Node;
import org.hyperledger.besu.ethereum.trie.patricia.TrieNodeDecoder;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorage;

import static org.hyperledger.besu.ethereum.trie.CompactEncoding.bytesToPath;

public class BonsaiStorageView extends AbstractBonsaiNodeView {
    private final BelaContext belaContext;
    private KeyValueStorage accountStorage;
    private KeyValueStorage storageStorage;
    private KeyValueStorage trieBranchStorage;
    private KeyValueStorage codeStorage;

    public BonsaiStorageView(final BelaContext belaContext) {
        this.belaContext = belaContext;
    }

    private void initStorage() {
        if (accountStorage != null) {
            return;
        }
        final StorageProvider provider = belaContext.getProvider();
        accountStorage = provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.ACCOUNT_INFO_STATE);
        codeStorage = provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.CODE_STORAGE);
        storageStorage = provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.ACCOUNT_STORAGE_STORAGE);
        trieBranchStorage =
                provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.TRIE_BRANCH_STORAGE);
    }

    public BonsaiNode findRoot() {
        initStorage();
        final Hash rootHash =
                trieBranchStorage
                        .get(Bytes.EMPTY.toArrayUnsafe())
                        .map(Bytes::wrap)
                        .map(Hash::hash)
                        .orElseThrow();
        Node<Bytes> root = getAccountNodeValue(rootHash, Bytes.EMPTY);
        BonsaiNode rootNodeView = new AccountTreeNode(this, root);
        return rootNodeView;
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

    public Optional<Bytes> getCode(final Hash codeOrAccountHash) {
        return codeStorage.get(codeOrAccountHash.toArrayUnsafe()).map(Bytes::wrap);
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

    public void findByHash(final Hash accountHash) {
        clear();
        final Bytes targetPath = bytesToPath(accountHash);
        BonsaiNode node = findRoot();

        while (node != null) {
            final List<BonsaiNode> children = node.getChildren();
            for (BonsaiNode child : children) {
                if (child instanceof AccountTreeNode accountTreeNode) {
                    final Bytes path = accountTreeNode.getLocation();
                    if (targetPath.toHexString().startsWith(path.toHexString())) {
                        selectNode(accountTreeNode);
                        node = accountTreeNode;
                        break;
                    }
                } else {
                    return;
                }
            }
        }

    }


    public void selectRoot() {
        clear();
        selectNode(findRoot());
    }

    public void findByAddress(final Address address) {
        findByHash(Hash.hash(address));
    }
}
