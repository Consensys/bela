package org.hyperledger.bela.utils.bonsai;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.rlp.RLP;
import org.hyperledger.besu.ethereum.storage.StorageProvider;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier;
import org.hyperledger.besu.ethereum.trie.CompactEncoding;
import org.hyperledger.besu.ethereum.trie.Node;
import org.hyperledger.besu.ethereum.trie.patricia.TrieNodeDecoder;
import org.hyperledger.besu.ethereum.worldstate.StateTrieAccountValue;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorage;

import static kr.pe.kwonnam.slf4jlambda.LambdaLoggerFactory.getLogger;
import static org.hyperledger.besu.ethereum.trie.CompactEncoding.bytesToPath;
import static org.hyperledger.besu.ethereum.trie.MerkleTrie.EMPTY_TRIE_NODE_HASH;

public class BonsaiTraversal {
    private static final LambdaLogger log = getLogger(BonsaiTraversal.class);
    private final KeyValueStorage accountStorage;
    private final KeyValueStorage storageStorage;
    private final KeyValueStorage trieBranchStorage;
    private final BonsaiListener listener;
    private final KeyValueStorage codeStorage;
    int visited = 0;
    private Node<Bytes> root;
    private volatile boolean shouldStop = false;

    public BonsaiTraversal(final StorageProvider provider, BonsaiListener listener) {
        accountStorage = provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.ACCOUNT_INFO_STATE);
        codeStorage = provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.CODE_STORAGE);
        storageStorage = provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.ACCOUNT_STORAGE_STORAGE);
        trieBranchStorage =
                provider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.TRIE_BRANCH_STORAGE);

        this.listener = listener;
    }

    public void traverse() {
        log.info("Starting bonsai traverse....");
        final Hash x =
                trieBranchStorage
                        .get(Bytes.EMPTY.toArrayUnsafe())
                        .map(Bytes::wrap)
                        .map(Hash::hash)
                        .orElseThrow();
        root = getAccountNodeValue(x, Bytes.EMPTY);
        traverseStartingFrom(root);
    }

    public void traverse(final Hash hash) {
        final Bytes targetPath = bytesToPath(hash);
        root = getAccountNodeValue(hash, targetPath);
        traverseStartingFrom(root);
    }

    private void traverseStartingFrom(final Node<Bytes> node) {
        if (node == null) {
            log.info("Root is null");
            return;
        }
        log.info("Starting from root {}", node.getHash());
        listener.root(node.getHash());
        traverseAccountTrie(node);
    }

    public void traverseAccountTrie(final Node<Bytes> parentNode) {
        if (shouldStop) {
            return;
        }
        if (parentNode == null) {
            return;
        }

        final List<Node<Bytes>> nodes =
                TrieNodeDecoder.decodeNodes(parentNode.getLocation().orElseThrow(), parentNode.getEncodedBytes());
        nodes.forEach(
                node -> {
                    if (nodeIsHashReferencedDescendant(parentNode, node)) {
                        traverseAccountTrie(
                                getAccountNodeValue(node.getHash(), node.getLocation().orElseThrow()));
                    } else {
                        if (node.getValue().isPresent()) {
                            final StateTrieAccountValue accountValue =
                                    StateTrieAccountValue.readFrom(RLP.input(node.getValue().orElseThrow()));
                            final Hash accountHash =
                                    Hash.wrap(
                                            Bytes32.wrap(
                                                    CompactEncoding.pathToBytes(
                                                            Bytes.concatenate(
                                                                    parentNode.getLocation()
                                                                            .orElseThrow(), node.getPath()))));
                            // check the account in the flat database
                            final Optional<Bytes> accountInFlatDB = accountStorage.get(accountHash.toArrayUnsafe())
                                    .map(Bytes::wrap);
                            if (accountInFlatDB.isPresent() && !accountInFlatDB.get()
                                    .equals(node.getValue().orElseThrow())) {
                                listener.differentDataInFlatDatabaseForAccount(accountHash);
                            }
                            // Add code, if appropriate
                            if (!accountValue.getCodeHash().equals(Hash.EMPTY)) {
                                // traverse code
                                // stored by code hash (CodeHashCodeStorageStrategy) by default since 24.5.2
                                Optional<Bytes> code = codeStorage.get(accountValue.getCodeHash().toArrayUnsafe()).map(Bytes::wrap);
                                // if empty, try by account hash instead
                                if (code.isEmpty()) code =
                                        codeStorage.get(accountHash.toArrayUnsafe()).map(Bytes::wrap);
                                // if still empty, it's missing
                                if (code.isEmpty()) {
                                    listener.missingCodeHash(accountValue.getCodeHash(), accountHash);
                                } else {
                                    final Hash foundCodeHash = Hash.hash(code.orElseThrow());
                                    if (!foundCodeHash.equals(accountValue.getCodeHash())) {
                                        listener.invalidCode(accountHash, accountValue.getCodeHash(), foundCodeHash);
                                    }
                                }
                            }
                            // Add storage, if appropriate
                            if (!accountValue.getStorageRoot().equals(EMPTY_TRIE_NODE_HASH)) {
                                traverseStorageTrie(
                                        accountHash,
                                        getStorageNodeValue(accountValue.getStorageRoot(), accountHash, Bytes.EMPTY));
                            }
                        } else if (nodes.size() > 1 && node.getHash().equals(parentNode.getHash())) {
                            listener.visited(BonsaiTraversalTrieType.Account);
                        } else {
                            listener.missingValueForNode(node.getHash());
                        }
                    }
                });
    }

    public void traverseStorageTrie(final Bytes32 accountHash, final Node<Bytes> parentNode) {
        if (shouldStop) {
            return;
        }
        if (parentNode == null) {
            return;
        }
        listener.visited(BonsaiTraversalTrieType.Storage);

        final List<Node<Bytes>> nodes =
                TrieNodeDecoder.decodeNodes(parentNode.getLocation().orElseThrow(), parentNode.getEncodedBytes());
        nodes.forEach(
                node -> {
                    if (nodeIsHashReferencedDescendant(parentNode, node)) {
                        traverseStorageTrie(
                                accountHash,
                                getStorageNodeValue(node.getHash(), accountHash, node.getLocation().orElseThrow()));
                    } else {
                        if (node.getValue().isPresent()) {
                            // check the storage in the flat database
                            final Optional<Bytes> storageInFlatDB = storageStorage.get(Bytes.concatenate(accountHash, getSlotHash(node.getLocation()
                                    .orElseThrow(), node.getPath())).toArrayUnsafe()).map(Bytes::wrap);
                            final Bytes value = Bytes32.leftPad(org.apache.tuweni.rlp.RLP.decodeValue(node.getValue()
                                    .orElseThrow()));
                            if (storageInFlatDB.isPresent() && !storageInFlatDB.get().equals(value)) {
                                listener.differentDataInFlatDatabaseForStorage(accountHash, node.getHash());
                            }
                        } else if (nodes.size() > 1 && node.getHash().equals(parentNode.getHash())) {
                            listener.visited(BonsaiTraversalTrieType.Account);
                        } else {
                            listener.missingValueForNode(node.getHash());
                        }
                    }
                });
    }

    @Nullable
    private Node<Bytes> getAccountNodeValue(final Bytes32 hash, final Bytes location) {
        final Optional<Bytes> bytes = trieBranchStorage.get(location.toArrayUnsafe()).map(Bytes::wrap);
        if (bytes.isEmpty()) {
            listener.missingAccountTrieForHash(hash, location);
            return null;
        }
        final Hash foundHashNode = Hash.hash(bytes.orElseThrow());
        if (!foundHashNode.equals(hash)) {
            listener.invalidAccountTrieForHash(hash, location, foundHashNode);
            return null;
        }
        return TrieNodeDecoder.decode(location, bytes.get());
    }

    private Node<Bytes> getStorageNodeValue(
            final Bytes32 hash, final Bytes32 accountHash, final Bytes location) {
        final Optional<Bytes> bytes =
                trieBranchStorage
                        .get(Bytes.concatenate(accountHash, location).toArrayUnsafe())
                        .map(Bytes::wrap);
        if (bytes.isEmpty()) {
            listener.missingStorageTrieForHash(hash, location);
            return null;
        }
        final Hash foundHashNode = Hash.hash(bytes.orElseThrow());
        if (!foundHashNode.equals(hash)) {
            listener.invalidStorageTrieForHash(accountHash, hash, location, foundHashNode);
            return null;
        }
        return TrieNodeDecoder.decode(location, bytes.get());
    }

    private boolean nodeIsHashReferencedDescendant(
            final Node<Bytes> parentNode, final Node<Bytes> node) {
        return !Objects.equals(node.getHash(), parentNode.getHash()) && node.isReferencedByHash();
    }


    private Hash getSlotHash(final Bytes location, final Bytes path) {
        return Hash.wrap(Bytes32.wrap(CompactEncoding.pathToBytes(Bytes.concatenate(location, path))));
    }

    public String getRoot() {
        return root.getHash().toHexString();
    }

    public void stop() {
        shouldStop = true;
    }

}
