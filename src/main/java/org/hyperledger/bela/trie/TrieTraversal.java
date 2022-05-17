package org.hyperledger.bela.trie;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.core.BlockHeaderFunctions;
import org.hyperledger.besu.ethereum.mainnet.MainnetBlockHeaderFunctions;
import org.hyperledger.besu.ethereum.rlp.RLP;
import org.hyperledger.besu.ethereum.storage.StorageProvider;
import org.hyperledger.besu.ethereum.storage.keyvalue.KeyValueSegmentIdentifier;
import org.hyperledger.besu.ethereum.trie.CompactEncoding;
import org.hyperledger.besu.ethereum.trie.MerklePatriciaTrie;
import org.hyperledger.besu.ethereum.trie.Node;
import org.hyperledger.besu.ethereum.trie.TrieNodeDecoder;
import org.hyperledger.besu.ethereum.worldstate.StateTrieAccountValue;
import org.hyperledger.besu.plugin.services.storage.KeyValueStorage;

public class TrieTraversal {

    private static final Bytes CHAIN_HEAD_KEY =
            Bytes.wrap("chainHeadHash".getBytes(StandardCharsets.UTF_8));
    private static final Bytes VARIABLES_PREFIX = Bytes.of(1);
    static final Bytes BLOCK_HEADER_PREFIX = Bytes.of(2);


    private final Optional<Bytes> pathToCheck;
    private final StorageNodeFinder storageNodeFinder;
    private final NodeFoundListener nodeFoundListener;

    private final KeyValueStorage blockchainStorage;
    private final BlockHeaderFunctions blockHeaderFunctions;
    private final TrieTraversalListener trieTraversalListener;
    private boolean isInvalidWorldstate;

    public TrieTraversal(
            final Optional<Bytes> pathToCheck,
            final StorageProvider storageProvider,
            final StorageNodeFinder storageNodeFinder,
            final NodeFoundListener nodeFoundListener,
            final TrieTraversalListener trieTraversalListener) {
        this(pathToCheck, storageProvider, storageNodeFinder, nodeFoundListener, new MainnetBlockHeaderFunctions(), trieTraversalListener);
    }

    public TrieTraversal(
            final Optional<Bytes> pathToCheck,
            final StorageProvider storageProvider,
            final StorageNodeFinder storageNodeFinder,
            final TrieTraversalListener trieTraversalListener) {
        this(pathToCheck, storageProvider, storageNodeFinder, new NodeFoundListener() {
            @Override
            public void onAccountNode(Bytes location, Bytes value) {}
            @Override
            public void onStorageNode(Bytes32 accountHash, Bytes location, Bytes value) {}
            @Override
            public void onCode(Bytes32 accountHash, Bytes value) {}
        }, new MainnetBlockHeaderFunctions(), trieTraversalListener);
    }

    public TrieTraversal(
            final Optional<Bytes> pathToCheck,
            final StorageProvider storageProvider,
            final StorageNodeFinder storageNodeFinder,
            final NodeFoundListener nodeFoundListener,
            final BlockHeaderFunctions blockHeaderFunctions,
            final TrieTraversalListener trieTraversalListener) {
        this.pathToCheck = pathToCheck;
        this.storageNodeFinder = storageNodeFinder;
        blockchainStorage = storageProvider.getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.BLOCKCHAIN);
        this.nodeFoundListener = nodeFoundListener;
        this.blockHeaderFunctions = blockHeaderFunctions;
        this.trieTraversalListener = trieTraversalListener;
    }

    public void traverse() {
        final Hash rootHash = blockchainStorage.get(Bytes.concatenate(VARIABLES_PREFIX, CHAIN_HEAD_KEY)
                        .toArrayUnsafe()).map(Bytes32::wrap)
                .flatMap(blockHash -> get(BLOCK_HEADER_PREFIX, blockHash)
                        .map(b -> BlockHeader.readFrom(RLP.input(b), blockHeaderFunctions)))
                .map(BlockHeader::getStateRoot)
                .orElseThrow(() -> new RuntimeException("chain head not found"));
        Node<Bytes> root = getAccountNodeValue(rootHash, Bytes.EMPTY);
        trieTraversalListener.root(rootHash);
        traverseAccountTrie(pathToCheck, root);
    }

    Optional<Bytes> get(final org.apache.tuweni.bytes.Bytes prefix, final org.apache.tuweni.bytes.Bytes key) {
        return blockchainStorage.get(org.apache.tuweni.bytes.Bytes.concatenate(prefix, key).toArrayUnsafe())
                .map(org.apache.tuweni.bytes.Bytes::wrap);
    }


    public void traverseAccountTrie(final Optional<Bytes> path, final Node<Bytes> parentNode) {
        if (parentNode == null) {
            return;
        }
        final List<Node<Bytes>> nodes =
                TrieNodeDecoder.decodeNodes(parentNode.getLocation().orElseThrow(), parentNode.getRlp());
        for (var index = new Object() {int i = 0;}; index.i < nodes.size(); index.i++) {
            if(path.map(location -> ((int)location.get(0))== index.i).orElse(true)){
                final Node<Bytes> node = nodes.get(index.i);
                if (nodeIsHashReferencedDescendant(parentNode, node)) {
                    Optional<Bytes> subPath = Optional.empty();
                    if(path.isPresent()){
                        subPath = Optional.of(path.get().slice(1));
                    }
                    traverseAccountTrie(subPath,
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
                        // Add code, if appropriate
                        if (!accountValue.getCodeHash().equals(Hash.EMPTY)) {
                            // traverse code
                            final Optional<Bytes> code =
                                    storageNodeFinder.getCode(accountHash, accountValue.getCodeHash());
                            if (code.isEmpty()) {
                                isInvalidWorldstate = true;
                                trieTraversalListener.missingCodeHash(accountValue.getCodeHash(), accountHash);
                            } else {
                                final Hash foundCodeHash = Hash.hash(code.orElseThrow());
                                if (!foundCodeHash.equals(accountValue.getCodeHash())) {
                                    isInvalidWorldstate = true;
                                    trieTraversalListener.invalidCode(accountHash, accountValue.getCodeHash(), foundCodeHash);
                                } else {
                                    nodeFoundListener.onCode(accountHash, code.orElseThrow());
                                }
                            }
                        }
                        // Add storage, if appropriate
                        if (!accountValue.getStorageRoot().equals(MerklePatriciaTrie.EMPTY_TRIE_NODE_HASH)) {
                            traverseStorageTrie(
                                    accountHash,
                                    getStorageNodeValue(accountValue.getStorageRoot(), accountHash, Bytes.EMPTY));
                        }
                    }
                }
            }
            index.i++;
        }
    }


    public void traverseStorageTrie(final Bytes32 accountHash, final Node<Bytes> parentNode) {
        if (parentNode == null) {
            return;
        }
        final List<Node<Bytes>> nodes =
                TrieNodeDecoder.decodeNodes(parentNode.getLocation().orElseThrow(), parentNode.getRlp());
        nodes.forEach(
                node -> {
                    if (nodeIsHashReferencedDescendant(parentNode, node)) {
                        traverseStorageTrie(
                                accountHash,
                                getStorageNodeValue(node.getHash(), accountHash, node.getLocation().orElseThrow()));
                    }
                });
    }

    @Nullable
    private Node<Bytes> getAccountNodeValue(final Bytes32 hash, final Bytes location) {
        final Optional<Bytes> bytes = storageNodeFinder.getAccountNode(location, hash);
        if (bytes.isEmpty()) {
            isInvalidWorldstate = true;
            trieTraversalListener.missingAccountTrieForHash(hash, location);
            return null;
        }
        final Hash foundHashNode = Hash.hash(bytes.orElseThrow());
        if (!foundHashNode.equals(hash)) {
            isInvalidWorldstate = true;
            trieTraversalListener.invalidAccountTrieForHash(hash, location, foundHashNode);
            return null;
        } else {
            nodeFoundListener.onAccountNode(location, bytes.orElseThrow());
        }
        return TrieNodeDecoder.decode(location, bytes.get());
    }

    private Node<Bytes> getStorageNodeValue(
            final Bytes32 hash, final Bytes32 accountHash, final Bytes location) {
        final Optional<Bytes> bytes =
                storageNodeFinder.getStorageNode(accountHash, location, hash);
        if (bytes.isEmpty()) {
            isInvalidWorldstate = true;
            trieTraversalListener.missingStorageTrieForHash(hash, location);
            return null;
        }
        final Hash foundHashNode = Hash.hash(bytes.orElseThrow());
        if (!foundHashNode.equals(hash)) {
            isInvalidWorldstate = true;
            trieTraversalListener.invalidStorageTrieForHash(hash, location, foundHashNode);
            return null;
        } else {
            nodeFoundListener.onStorageNode(accountHash, location, bytes.orElseThrow());
        }
        return TrieNodeDecoder.decode(location, bytes.get());
    }

    private boolean nodeIsHashReferencedDescendant(
            final Node<Bytes> parentNode, final Node<Bytes> node) {
        return !Objects.equals(node.getHash(), parentNode.getHash()) && node.isReferencedByHash();
    }

    public boolean isInvalidWorldstate() {
        return isInvalidWorldstate;
    }
}
